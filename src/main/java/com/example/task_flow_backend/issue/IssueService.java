package com.example.task_flow_backend.issue;

import com.example.task_flow_backend.assignment.AssignmentEngine;
import com.example.task_flow_backend.assignment.AssignmentResult;
import com.example.task_flow_backend.auth.UserPrincipal;
import com.example.task_flow_backend.common.PageResponse;
import com.example.task_flow_backend.common.exception.BusinessRuleException;
import com.example.task_flow_backend.common.exception.ForbiddenOperationException;
import com.example.task_flow_backend.issue.dto.IssueDtos.AssigneeUpdateRequest;
import com.example.task_flow_backend.issue.dto.IssueDtos.CreateIssueRequest;
import com.example.task_flow_backend.issue.dto.IssueDtos.IssueResponse;
import com.example.task_flow_backend.issue.dto.IssueDtos.RequiredSkillItem;
import com.example.task_flow_backend.issue.dto.IssueDtos.StatusUpdateRequest;
import com.example.task_flow_backend.issue.dto.IssueDtos.UpdateIssueRequest;
import com.example.task_flow_backend.project.Project;
import com.example.task_flow_backend.project.ProjectService;
import com.example.task_flow_backend.realtime.BoardEvent;
import com.example.task_flow_backend.realtime.BoardEventPublisher;
import com.example.task_flow_backend.skill.SkillRepository;
import com.example.task_flow_backend.user.Role;
import com.example.task_flow_backend.user.User;
import com.example.task_flow_backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueRequiredSkillRepository requiredSkillRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final ApplicationEventPublisher events;
    private final BoardEventPublisher boardEvents;
    private final AssignmentEngine assignmentEngine;
    private final IssueService self;

    public IssueService(IssueRepository issueRepository,
                        IssueRequiredSkillRepository requiredSkillRepository,
                        ProjectService projectService,
                        UserRepository userRepository,
                        SkillRepository skillRepository,
                        ApplicationEventPublisher events,
                        BoardEventPublisher boardEvents,
                        AssignmentEngine assignmentEngine,
                        @org.springframework.context.annotation.Lazy IssueService self) {
        this.issueRepository = issueRepository;
        this.requiredSkillRepository = requiredSkillRepository;
        this.projectService = projectService;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.events = events;
        this.boardEvents = boardEvents;
        this.assignmentEngine = assignmentEngine;
        this.self = self;
    }

    @Transactional
    public IssueResponse create(Long projectId, CreateIssueRequest request, UserPrincipal principal) {
        projectService.requireProject(projectId);
        projectService.assertMember(projectId, principal);

        Issue issue = new Issue();
        issue.setProjectId(projectId);
        issue.setIssueKey(projectService.nextIssueKey(projectId));
        issue.setTitle(request.title().trim());
        issue.setDescription(request.description());
        issue.setType(request.type() == null ? IssueType.TASK : request.type());
        issue.setPriority(request.priority() == null ? Priority.MEDIUM : request.priority());
        issue.setStatus(IssueStatus.TODO);
        issue.setReporterId(principal.id());
        issue.setEstimateHours(request.estimateHours());
        if (request.sprintId() != null) {
            issue.setSprintId(request.sprintId());
        }

        boolean manualAssignee = request.assigneeId() != null;
        if (manualAssignee) {
            requireProjectMember(projectId, request.assigneeId());
            issue.setAssigneeId(request.assigneeId());
            issue.setAssignmentMode(AssignmentMode.MANUAL);
        } else {
            issue.setAssignmentMode(AssignmentMode.UNASSIGNED);
        }

        issueRepository.save(issue);
        replaceRequiredSkills(issue.getId(), request.requiredSkills());

        boardEvents.publish(projectId,
                BoardEvent.issueCreated(issue.getIssueKey(), issue.getStatus().name(),
                        issue.getAssigneeId(), principal.id()));

        if (!manualAssignee) {
            // AFTER_COMMIT + @Async: 201 returns before the engine runs.
            events.publishEvent(new IssueCreatedEvent(issue.getId(), projectId, issue.getIssueKey()));
        }
        return toResponse(issue);
    }

    @Transactional(readOnly = true)
    public IssueResponse get(String key, UserPrincipal principal) {
        Issue issue = require(key);
        projectService.assertMember(issue.getProjectId(), principal);
        return toResponse(issue);
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> search(Long projectId, Long assigneeId, Long reporterId,
                                              IssueStatus status, Long sprintId, Pageable pageable) {
        return PageResponse.from(
                issueRepository.search(projectId, assigneeId, reporterId, status, sprintId, pageable),
                this::toResponse);
    }

    @Transactional
    public IssueResponse update(String key, UpdateIssueRequest request, UserPrincipal principal) {
        Issue issue = require(key);
        projectService.assertMember(issue.getProjectId(), principal);

        if (StringUtils.hasText(request.title())) {
            issue.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            issue.setDescription(request.description());
        }
        if (request.type() != null) {
            issue.setType(request.type());
        }
        if (request.priority() != null) {
            issue.setPriority(request.priority());
        }
        if (request.estimateHours() != null) {
            issue.setEstimateHours(request.estimateHours());
        }
        if (request.sprintId() != null) {
            issue.setSprintId(request.sprintId() == 0 ? null : request.sprintId());
        }
        if (request.requiredSkills() != null) {
            replaceRequiredSkills(issue.getId(), request.requiredSkills());
        }

        boardEvents.publish(issue.getProjectId(),
                BoardEvent.issueUpdated(issue.getIssueKey(), issue.getStatus().name(), principal.id()));
        return toResponse(issue);
    }

    @Transactional
    public IssueResponse changeStatus(String key, StatusUpdateRequest request, UserPrincipal principal) {
        Issue issue = require(key);
        projectService.assertMember(issue.getProjectId(), principal);

        if (request.status() == IssueStatus.TRIAGE) {
            throw new BusinessRuleException("STATUS_NOT_ALLOWED", "TRIAGE is managed by the assignment engine");
        }

        IssueStatus previous = issue.getStatus();
        issue.setStatus(request.status());
        if (request.boardOrder() != null) {
            issue.setBoardOrder(request.boardOrder());
        }
        if (request.status() == IssueStatus.DONE && previous != IssueStatus.DONE) {
            issue.setResolvedAt(Instant.now());
        } else if (request.status() != IssueStatus.DONE && previous == IssueStatus.DONE) {
            issue.setResolvedAt(null);
        }

        boardEvents.publish(issue.getProjectId(),
                BoardEvent.statusChanged(issue.getIssueKey(), issue.getStatus().name(),
                        issue.getBoardOrder(), principal.id()));
        return toResponse(issue);
    }

    @Transactional
    public IssueResponse changeAssignee(String key, AssigneeUpdateRequest request, UserPrincipal principal) {
        Issue issue = require(key);
        projectService.assertMember(issue.getProjectId(), principal);
        requireProjectMember(issue.getProjectId(), request.userId());

        issue.setAssigneeId(request.userId());
        issue.setAssignmentMode(AssignmentMode.MANUAL);
        if (issue.getStatus() == IssueStatus.TRIAGE) {
            issue.setStatus(IssueStatus.TODO);
        }

        boardEvents.publish(issue.getProjectId(),
                BoardEvent.issueAssigned(issue.getIssueKey(), issue.getStatus().name(),
                        request.userId(), principal.id()));
        return toResponse(issue);
    }

    /** Clears the current assignee and re-runs the engine synchronously. */
    public IssueResponse reassign(String key, UserPrincipal principal) {
        Issue issue = require(key);
        projectService.assertMember(issue.getProjectId(), principal);
        self.clearAssignee(issue.getId());

        AssignmentResult result = assignmentEngine.assign(issue.getId());
        Issue refreshed = issueRepository.findById(issue.getId()).orElseThrow();
        if (result.triaged()) {
            throw new BusinessRuleException("NO_CANDIDATE",
                    "No eligible assignee; issue " + key + " moved to TRIAGE");
        }
        return toResponse(refreshed);
    }

    @Transactional
    public void clearAssignee(Long issueId) {
        Issue issue = issueRepository.findByIdForUpdate(issueId).orElseThrow();
        issue.setAssigneeId(null);
        issue.setAssignmentMode(AssignmentMode.UNASSIGNED);
    }

    @Transactional
    public void delete(String key, UserPrincipal principal) {
        Issue issue = require(key);
        Project project = projectService.requireProject(issue.getProjectId());
        if (principal.role() != Role.ADMIN && !principal.id().equals(project.getLeadId())) {
            throw new ForbiddenOperationException("Only the project lead or an admin can delete issues");
        }
        requiredSkillRepository.deleteByIssueId(issue.getId());
        issueRepository.delete(issue);
        boardEvents.publish(issue.getProjectId(), BoardEvent.issueDeleted(key, principal.id()));
    }

    @Transactional(readOnly = true)
    public Issue require(String key) {
        return issueRepository.findByIssueKey(key)
                .orElseThrow(() -> new EntityNotFoundException("Issue " + key + " not found"));
    }

    // ---------- helpers ----------

    private void replaceRequiredSkills(Long issueId, List<RequiredSkillItem> items) {
        requiredSkillRepository.deleteByIssueId(issueId);
        requiredSkillRepository.flush();
        if (items == null || items.isEmpty()) {
            return;
        }
        for (RequiredSkillItem item : items) {
            if (!skillRepository.existsById(item.skillId())) {
                throw new EntityNotFoundException("Skill " + item.skillId() + " not found");
            }
            IssueRequiredSkill row = new IssueRequiredSkill();
            row.setIssueId(issueId);
            row.setSkillId(item.skillId());
            row.setWeight(item.weight() == null ? 1 : item.weight());
            requiredSkillRepository.save(row);
        }
    }

    private void requireProjectMember(Long projectId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User " + userId + " not found"));
        if (!user.isActive()) {
            throw new BusinessRuleException("USER_INACTIVE", "User " + userId + " is inactive");
        }
        if (!projectService.isMember(projectId, userId)) {
            throw new BusinessRuleException("NOT_PROJECT_MEMBER",
                    "User " + userId + " is not a member of project " + projectId);
        }
    }

    private IssueResponse toResponse(Issue issue) {
        return IssueResponse.from(issue, requiredSkillRepository.findByIssueId(issue.getId()));
    }
}
