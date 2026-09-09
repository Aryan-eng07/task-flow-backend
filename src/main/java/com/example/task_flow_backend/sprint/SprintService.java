package com.example.task_flow_backend.sprint;

import com.example.task_flow_backend.auth.UserPrincipal;
import com.example.task_flow_backend.common.exception.BusinessRuleException;
import com.example.task_flow_backend.issue.Issue;
import com.example.task_flow_backend.issue.IssueRepository;
import com.example.task_flow_backend.issue.IssueStatus;
import com.example.task_flow_backend.project.Project;
import com.example.task_flow_backend.project.ProjectService;
import com.example.task_flow_backend.sprint.dto.SprintDtos.CompleteSprintRequest;
import com.example.task_flow_backend.sprint.dto.SprintDtos.CreateSprintRequest;
import com.example.task_flow_backend.sprint.dto.SprintDtos.SprintReportResponse;
import com.example.task_flow_backend.sprint.dto.SprintDtos.SprintResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final ProjectService projectService;

    public SprintService(SprintRepository sprintRepository,
                         IssueRepository issueRepository,
                         ProjectService projectService) {
        this.sprintRepository = sprintRepository;
        this.issueRepository = issueRepository;
        this.projectService = projectService;
    }

    @Transactional
    public SprintResponse create(Long projectId, CreateSprintRequest request, UserPrincipal principal) {
        Project project = projectService.requireProject(projectId);
        projectService.assertLeadOrAdmin(project, principal);
        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("SPRINT_DATES_INVALID", "endDate must not be before startDate");
        }
        Sprint sprint = new Sprint();
        sprint.setProjectId(projectId);
        sprint.setName(request.name().trim());
        sprint.setGoal(request.goal());
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());
        sprint.setStatus(SprintStatus.PLANNED);
        return SprintResponse.from(sprintRepository.save(sprint));
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> list(Long projectId, SprintStatus status, UserPrincipal principal) {
        projectService.requireProject(projectId);
        projectService.assertMember(projectId, principal);
        List<Sprint> sprints = status == null
                ? sprintRepository.findByProjectIdOrderByIdDesc(projectId)
                : sprintRepository.findByProjectIdAndStatusOrderByIdDesc(projectId, status);
        return sprints.stream().map(SprintResponse::from).toList();
    }

    @Transactional
    public SprintResponse start(Long sprintId, UserPrincipal principal) {
        Sprint sprint = require(sprintId);
        Project project = projectService.requireProject(sprint.getProjectId());
        projectService.assertLeadOrAdmin(project, principal);

        if (sprint.getStatus() == SprintStatus.COMPLETED) {
            throw new BusinessRuleException("SPRINT_COMPLETED", "A completed sprint cannot be restarted");
        }
        if (sprintRepository.existsByProjectIdAndStatus(sprint.getProjectId(), SprintStatus.ACTIVE)) {
            throw new BusinessRuleException("SPRINT_ALREADY_ACTIVE",
                    "Project already has an active sprint; complete it first");
        }
        sprint.setStatus(SprintStatus.ACTIVE);
        return SprintResponse.from(sprint);
    }

    @Transactional
    public SprintResponse complete(Long sprintId, CompleteSprintRequest request, UserPrincipal principal) {
        Sprint sprint = require(sprintId);
        Project project = projectService.requireProject(sprint.getProjectId());
        projectService.assertLeadOrAdmin(project, principal);
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new BusinessRuleException("SPRINT_NOT_ACTIVE", "Only an active sprint can be completed");
        }

        Long targetSprintId = null;
        if (request != null && request.moveUnfinishedToSprintId() != null) {
            Sprint target = require(request.moveUnfinishedToSprintId());
            if (!target.getProjectId().equals(sprint.getProjectId())) {
                throw new BusinessRuleException("SPRINT_PROJECT_MISMATCH",
                        "Target sprint belongs to a different project");
            }
            targetSprintId = target.getId();
        }

        List<Issue> unfinished = issueRepository.findBySprintIdAndStatusIn(
                sprintId, List.of(IssueStatus.TODO, IssueStatus.IN_PROGRESS, IssueStatus.IN_REVIEW,
                        IssueStatus.BLOCKED, IssueStatus.TRIAGE));
        for (Issue issue : unfinished) {
            issue.setSprintId(targetSprintId);
        }
        sprint.setStatus(SprintStatus.COMPLETED);
        return SprintResponse.from(sprint);
    }

    @Transactional(readOnly = true)
    public SprintReportResponse report(Long sprintId, UserPrincipal principal) {
        Sprint sprint = require(sprintId);
        projectService.assertMember(sprint.getProjectId(), principal);

        List<Issue> all = issueRepository.findBySprintId(sprintId);
        int total = all.size();
        int completed = (int) all.stream().filter(i -> i.getStatus() == IssueStatus.DONE).count();
        int carriedOver = total - completed;
        double rate = total == 0 ? 0.0 : (double) completed / total;
        return new SprintReportResponse(sprintId, sprint.getStatus().name(), total, completed, carriedOver,
                Math.round(rate * 10000) / 10000.0);
    }

    private Sprint require(Long id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sprint " + id + " not found"));
    }
}
