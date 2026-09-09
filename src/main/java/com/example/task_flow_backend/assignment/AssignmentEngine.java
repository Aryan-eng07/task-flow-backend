package com.example.task_flow_backend.assignment;

import com.example.task_flow_backend.issue.Issue;
import com.example.task_flow_backend.issue.IssueRepository;
import com.example.task_flow_backend.issue.IssueRequiredSkill;
import com.example.task_flow_backend.issue.IssueRequiredSkillRepository;
import com.example.task_flow_backend.issue.IssueStatus;
import com.example.task_flow_backend.issue.IssueStatuses;
import com.example.task_flow_backend.project.ProjectMember;
import com.example.task_flow_backend.project.ProjectMemberRepository;
import com.example.task_flow_backend.realtime.BoardEvent;
import com.example.task_flow_backend.realtime.BoardEventPublisher;
import com.example.task_flow_backend.skill.UserSkill;
import com.example.task_flow_backend.skill.UserSkillRepository;
import com.example.task_flow_backend.user.User;
import com.example.task_flow_backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Orchestrates auto-assignment: fetch candidates, score them with the
 * {@link ScoringStrategy}, persist the full ranking to {@code assignment_log},
 * and set the winner on the issue.
 *
 * <p><b>Concurrency:</b> two issues created at the same moment can both read
 * {@code openCount = n} for the same person and both pick them, blowing past the
 * WIP cap. This is closed with a per-project application lock (below) so
 * assignment runs for a project are serialized, plus a fresh WIP re-check on the
 * winner inside the locked transaction. {@code @Version} on {@code User} /
 * {@code Issue} is the database-level backstop.
 */
@Service
public class AssignmentEngine {

    private static final Logger log = LoggerFactory.getLogger(AssignmentEngine.class);

    private final IssueRepository issueRepository;
    private final IssueRequiredSkillRepository requiredSkillRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final AssignmentLogRepository logRepository;
    private final ScoringStrategy scoringStrategy;
    private final BoardEventPublisher boardEventPublisher;
    private final AssignmentEngine self;

    private final Map<Long, ReentrantLock> projectLocks = new ConcurrentHashMap<>();

    public AssignmentEngine(IssueRepository issueRepository,
                            IssueRequiredSkillRepository requiredSkillRepository,
                            ProjectMemberRepository memberRepository,
                            UserRepository userRepository,
                            UserSkillRepository userSkillRepository,
                            AssignmentLogRepository logRepository,
                            ScoringStrategy scoringStrategy,
                            BoardEventPublisher boardEventPublisher,
                            @Lazy AssignmentEngine self) {
        this.issueRepository = issueRepository;
        this.requiredSkillRepository = requiredSkillRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.userSkillRepository = userSkillRepository;
        this.logRepository = logRepository;
        this.scoringStrategy = scoringStrategy;
        this.boardEventPublisher = boardEventPublisher;
        this.self = self;
    }

    /** Score + persist + assign. Serialized per project. Safe to call again for a reassign. */
    public AssignmentResult assign(Long issueId) {
        Long projectId = issueRepository.findById(issueId)
                .map(Issue::getProjectId)
                .orElseThrow(() -> new EntityNotFoundException("Issue " + issueId + " not found"));

        ReentrantLock lock = projectLocks.computeIfAbsent(projectId, k -> new ReentrantLock());
        lock.lock();
        try {
            return self.assignInTransaction(issueId);
        } finally {
            lock.unlock();
        }
    }

    /** Score only, persist nothing — backs {@code POST /assignment/preview}. */
    @Transactional(readOnly = true)
    public List<ScoredCandidate> preview(Long projectId, Map<Long, Integer> requiredSkills, Long reporterId) {
        return scoringStrategy.rank(buildCandidates(projectId), requiredSkills, reporterId);
    }

    @Transactional
    public AssignmentResult assignInTransaction(Long issueId) {
        Issue issue = issueRepository.findByIdForUpdate(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Issue " + issueId + " not found"));

        if (issue.getAssigneeId() != null) {
            log.debug("issue {} already has assignee {}, skipping auto-assign", issue.getIssueKey(), issue.getAssigneeId());
            return new AssignmentResult(issueId, issue.getAssigneeId(), false, List.of());
        }

        Map<Long, Integer> requiredSkills = requiredSkillRepository.findByIssueId(issueId).stream()
                .collect(Collectors.toMap(IssueRequiredSkill::getSkillId,
                        rs -> rs.getWeight() == null ? 1 : rs.getWeight()));

        List<Candidate> candidates = buildCandidates(issue.getProjectId());
        List<ScoredCandidate> ranked = scoringStrategy.rank(candidates, requiredSkills, issue.getReporterId());

        ScoredCandidate winner = pickWinner(ranked);

        logRepository.deleteByIssueId(issueId);
        persistLog(issueId, ranked, winner);

        if (winner == null) {
            issue.setStatus(IssueStatus.TRIAGE);
            log.info("issue {} -> TRIAGE (no eligible candidate among {})", issue.getIssueKey(), candidates.size());
            boardEventPublisher.publish(issue.getProjectId(),
                    BoardEvent.issueUpdated(issue.getIssueKey(), issue.getStatus().name(), null));
            return new AssignmentResult(issueId, null, true, ranked);
        }

        issue.setAssigneeId(winner.userId());
        issue.setAssignmentMode(com.example.task_flow_backend.issue.AssignmentMode.AUTO);
        if (issue.getStatus() == IssueStatus.TRIAGE) {
            issue.setStatus(IssueStatus.TODO);
        }
        log.info("issue {} auto-assigned to user {} (score {})",
                issue.getIssueKey(), winner.userId(), winner.totalScore());

        boardEventPublisher.publish(issue.getProjectId(),
                BoardEvent.issueAssigned(issue.getIssueKey(), issue.getStatus().name(), winner.userId(), null));
        return new AssignmentResult(issueId, winner.userId(), false, ranked);
    }

    /**
     * First eligible candidate whose live WIP count still has room. The re-check
     * matters: the count baked into the score was taken before this locked
     * transaction and a parallel assignment for the same project may have moved it.
     */
    private ScoredCandidate pickWinner(List<ScoredCandidate> ranked) {
        for (ScoredCandidate c : ranked) {
            if (!c.eligible()) {
                continue;
            }
            User user = userRepository.findById(c.userId()).orElse(null);
            if (user == null || !user.isActive()) {
                continue;
            }
            long liveOpen = issueRepository.countByAssigneeIdAndStatusIn(c.userId(), IssueStatuses.OPEN);
            if (liveOpen < user.getWipLimit()) {
                return c;
            }
            log.debug("candidate {} passed scoring but is now at WIP cap ({}/{})",
                    c.userId(), liveOpen, user.getWipLimit());
        }
        return null;
    }

    private List<Candidate> buildCandidates(Long projectId) {
        List<Long> memberIds = memberRepository.findByProjectId(projectId).stream()
                .map(ProjectMember::getUserId).toList();
        if (memberIds.isEmpty()) {
            return List.of();
        }

        List<User> users = userRepository.findAllById(memberIds).stream()
                .filter(User::isActive)
                .toList();

        Map<Long, Map<Long, Integer>> skillsByUser = new HashMap<>();
        for (UserSkill us : userSkillRepository.findByUserIdIn(memberIds)) {
            skillsByUser.computeIfAbsent(us.getUserId(), k -> new HashMap<>())
                    .put(us.getSkillId(), us.getProficiency());
        }

        return users.stream().map(u -> {
            long openCount = issueRepository.countByAssigneeIdAndStatusIn(u.getId(), IssueStatuses.OPEN);
            Double avg = u.getAvgResolutionHours() == null ? null
                    : u.getAvgResolutionHours().doubleValue();
            return new Candidate(u.getId(), u.getWipLimit(), openCount, avg,
                    skillsByUser.getOrDefault(u.getId(), Map.of()));
        }).toList();
    }

    private void persistLog(Long issueId, List<ScoredCandidate> ranked, ScoredCandidate winner) {
        List<AssignmentLog> rows = ranked.stream().map(c -> {
            AssignmentLog row = new AssignmentLog();
            row.setIssueId(issueId);
            row.setCandidateId(c.userId());
            row.setSkillScore(BigDecimal.valueOf(c.skillScore()));
            row.setLoadScore(BigDecimal.valueOf(c.loadScore()));
            row.setSpeedScore(BigDecimal.valueOf(c.speedScore()));
            row.setTotalScore(BigDecimal.valueOf(c.totalScore()));
            boolean isWinner = winner != null && winner.userId() == c.userId();
            row.setSelected(isWinner);
            row.setReason(isWinner ? ScoredCandidate.Reason.SELECTED.name() : c.reason().name());
            return row;
        }).toList();
        logRepository.saveAll(rows);
    }
}
