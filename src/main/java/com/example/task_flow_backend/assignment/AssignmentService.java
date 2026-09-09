package com.example.task_flow_backend.assignment;

import com.example.task_flow_backend.assignment.dto.AssignmentDtos.AssignmentConfigResponse;
import com.example.task_flow_backend.assignment.dto.AssignmentDtos.AssignmentLogEntryResponse;
import com.example.task_flow_backend.assignment.dto.AssignmentDtos.CandidateScoreResponse;
import com.example.task_flow_backend.assignment.dto.AssignmentDtos.PreviewRequest;
import com.example.task_flow_backend.assignment.dto.AssignmentDtos.RebalanceResponse;
import com.example.task_flow_backend.assignment.dto.AssignmentDtos.RequiredSkillInput;
import com.example.task_flow_backend.assignment.dto.AssignmentDtos.UpdateAssignmentConfigRequest;
import com.example.task_flow_backend.auth.UserPrincipal;
import com.example.task_flow_backend.issue.Issue;
import com.example.task_flow_backend.issue.IssueRepository;
import com.example.task_flow_backend.issue.IssueStatus;
import com.example.task_flow_backend.project.ProjectService;
import com.example.task_flow_backend.user.User;
import com.example.task_flow_backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentProperties props;
    private final AssignmentEngine engine;
    private final AssignmentLogRepository logRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    public AssignmentService(AssignmentProperties props,
                             AssignmentEngine engine,
                             AssignmentLogRepository logRepository,
                             IssueRepository issueRepository,
                             UserRepository userRepository,
                             ProjectService projectService) {
        this.props = props;
        this.engine = engine;
        this.logRepository = logRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
    }

    public AssignmentConfigResponse getConfig() {
        return new AssignmentConfigResponse(
                props.getSkillWeight(), props.getLoadWeight(), props.getSpeedWeight(),
                props.getMinSkillThreshold(), props.isPreventSelfAssign(), props.getNeutralSpeedScore(),
                props.getDefaultMaxAvgHours(), props.getTriageRetryHours(), props.getResolutionWindowDays());
    }

    public AssignmentConfigResponse updateConfig(UpdateAssignmentConfigRequest r) {
        if (r.skillWeight() != null) {
            props.setSkillWeight(r.skillWeight());
        }
        if (r.loadWeight() != null) {
            props.setLoadWeight(r.loadWeight());
        }
        if (r.speedWeight() != null) {
            props.setSpeedWeight(r.speedWeight());
        }
        if (r.minSkillThreshold() != null) {
            props.setMinSkillThreshold(r.minSkillThreshold());
        }
        if (r.preventSelfAssign() != null) {
            props.setPreventSelfAssign(r.preventSelfAssign());
        }
        if (r.neutralSpeedScore() != null) {
            props.setNeutralSpeedScore(r.neutralSpeedScore());
        }
        if (r.defaultMaxAvgHours() != null) {
            props.setDefaultMaxAvgHours(r.defaultMaxAvgHours());
        }
        if (r.triageRetryHours() != null) {
            props.setTriageRetryHours(r.triageRetryHours());
        }
        if (r.resolutionWindowDays() != null) {
            props.setResolutionWindowDays(r.resolutionWindowDays());
        }
        log.info("assignment config updated: {}", getConfig());
        return getConfig();
    }

    @Transactional(readOnly = true)
    public List<CandidateScoreResponse> preview(PreviewRequest request, UserPrincipal principal) {
        projectService.requireProject(request.projectId());
        projectService.assertMember(request.projectId(), principal);

        Map<Long, Integer> requiredSkills = toWeightMap(request.requiredSkills());
        List<ScoredCandidate> ranked = engine.preview(request.projectId(), requiredSkills, request.reporterId());
        Map<Long, String> names = nameLookup(ranked.stream().map(ScoredCandidate::userId).toList());

        return ranked.stream()
                .map(c -> new CandidateScoreResponse(
                        c.userId(), names.get(c.userId()),
                        bd(c.skillScore()), bd(c.loadScore()), bd(c.speedScore()), bd(c.totalScore()),
                        c.openCount(), c.eligible(),
                        c.eligible() ? "ELIGIBLE" : c.reason().name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentLogEntryResponse> issueLog(String issueKey, UserPrincipal principal) {
        Issue issue = issueRepository.findByIssueKey(issueKey)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Issue " + issueKey + " not found"));
        projectService.assertMember(issue.getProjectId(), principal);

        List<AssignmentLog> rows = logRepository.findByIssueIdOrderByTotalScoreDesc(issue.getId());
        Map<Long, String> names = nameLookup(rows.stream().map(AssignmentLog::getCandidateId).toList());
        return rows.stream()
                .map(r -> new AssignmentLogEntryResponse(
                        r.getCandidateId(), names.get(r.getCandidateId()),
                        r.getSkillScore(), r.getLoadScore(), r.getSpeedScore(), r.getTotalScore(),
                        r.isSelected(), r.getReason(), r.getCreatedAt()))
                .toList();
    }

    /** ADMIN — retry every TRIAGE issue that has been stuck past the configured window. */
    public RebalanceResponse rebalance() {
        Instant cutoff = Instant.now().minus(props.getTriageRetryHours(), ChronoUnit.HOURS);
        List<Issue> stale = issueRepository.findByStatusAndUpdatedAtBefore(IssueStatus.TRIAGE, cutoff);
        int reassigned = 0;
        for (Issue issue : stale) {
            try {
                AssignmentResult result = engine.assign(issue.getId());
                if (result.selectedUserId() != null) {
                    reassigned++;
                }
            } catch (Exception ex) {
                log.warn("rebalance failed for issue {}", issue.getIssueKey(), ex);
            }
        }
        return new RebalanceResponse(stale.size(), reassigned, stale.size() - reassigned);
    }

    // ---------- helpers ----------

    private Map<Long, Integer> toWeightMap(List<RequiredSkillInput> inputs) {
        Map<Long, Integer> map = new LinkedHashMap<>();
        if (inputs != null) {
            for (RequiredSkillInput in : inputs) {
                map.put(in.skillId(), in.weight() == null ? 1 : in.weight());
            }
        }
        return map;
    }

    private Map<Long, String> nameLookup(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }
}
