package com.example.task_flow_backend.project;

import com.example.task_flow_backend.auth.UserPrincipal;
import com.example.task_flow_backend.common.exception.BusinessRuleException;
import com.example.task_flow_backend.common.exception.ForbiddenOperationException;
import com.example.task_flow_backend.issue.Issue;
import com.example.task_flow_backend.issue.IssueRepository;
import com.example.task_flow_backend.issue.IssueStatus;
import com.example.task_flow_backend.issue.IssueStatuses;
import com.example.task_flow_backend.project.dto.ProjectDtos.AddMemberRequest;
import com.example.task_flow_backend.project.dto.ProjectDtos.BoardColumn;
import com.example.task_flow_backend.project.dto.ProjectDtos.BoardIssue;
import com.example.task_flow_backend.project.dto.ProjectDtos.BoardResponse;
import com.example.task_flow_backend.project.dto.ProjectDtos.CreateProjectRequest;
import com.example.task_flow_backend.project.dto.ProjectDtos.MemberResponse;
import com.example.task_flow_backend.project.dto.ProjectDtos.ProjectResponse;
import com.example.task_flow_backend.project.dto.ProjectDtos.UpdateProjectRequest;
import com.example.task_flow_backend.user.Role;
import com.example.task_flow_backend.user.User;
import com.example.task_flow_backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMemberRepository memberRepository,
                          UserRepository userRepository,
                          IssueRepository issueRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
    }

    // ---------- authorization helpers reused by other modules ----------

    @Transactional(readOnly = true)
    public Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project " + projectId + " not found"));
    }

    public boolean isMember(Long projectId, Long userId) {
        return memberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public void assertMember(Long projectId, UserPrincipal principal) {
        if (principal.role() == Role.ADMIN) {
            return;
        }
        if (!memberRepository.existsByProjectIdAndUserId(projectId, principal.id())) {
            throw new ForbiddenOperationException("You are not a member of project " + projectId);
        }
    }

    public void assertLeadOrAdmin(Project project, UserPrincipal principal) {
        if (principal.role() == Role.ADMIN) {
            return;
        }
        if (!principal.id().equals(project.getLeadId())) {
            throw new ForbiddenOperationException("Requires the project lead or an admin");
        }
    }

    // ---------- CRUD ----------

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, UserPrincipal principal) {
        String keyCode = request.keyCode().trim().toUpperCase();
        if (projectRepository.existsByKeyCodeIgnoreCase(keyCode)) {
            throw new BusinessRuleException("PROJECT_KEY_EXISTS", "Project key '" + keyCode + "' is taken");
        }
        Project project = new Project();
        project.setKeyCode(keyCode);
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setLeadId(principal.id());
        projectRepository.save(project);

        addMemberRow(project.getId(), principal.id());
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listVisible(UserPrincipal principal) {
        List<Project> projects = principal.role() == Role.ADMIN
                ? projectRepository.findAll().stream().filter(Project::isActive).toList()
                : projectRepository.findVisibleTo(principal.id());
        return projects.stream().map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id, UserPrincipal principal) {
        Project project = requireProject(id);
        assertMember(id, principal);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest request, UserPrincipal principal) {
        Project project = requireProject(id);
        assertLeadOrAdmin(project, principal);
        if (StringUtils.hasText(request.name())) {
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.leadId() != null) {
            if (!userRepository.existsById(request.leadId())) {
                throw new EntityNotFoundException("User " + request.leadId() + " not found");
            }
            project.setLeadId(request.leadId());
            addMemberRow(id, request.leadId());
        }
        return ProjectResponse.from(project);
    }

    @Transactional
    public void softDelete(Long id) {
        Project project = requireProject(id);
        project.setActive(false);
    }

    // ---------- members ----------

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long projectId, UserPrincipal principal) {
        requireProject(projectId);
        assertMember(projectId, principal);
        List<Long> ids = memberRepository.findByProjectId(projectId).stream()
                .map(ProjectMember::getUserId).toList();
        return userRepository.findAllById(ids).stream()
                .map(u -> new MemberResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name()))
                .toList();
    }

    @Transactional
    public void addMember(Long projectId, AddMemberRequest request, UserPrincipal principal) {
        Project project = requireProject(projectId);
        assertLeadOrAdmin(project, principal);
        if (!userRepository.existsById(request.userId())) {
            throw new EntityNotFoundException("User " + request.userId() + " not found");
        }
        addMemberRow(projectId, request.userId());
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, UserPrincipal principal) {
        Project project = requireProject(projectId);
        assertLeadOrAdmin(project, principal);
        if (userId.equals(project.getLeadId())) {
            throw new BusinessRuleException("CANNOT_REMOVE_LEAD", "Reassign the project lead before removing them");
        }
        memberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    private void addMemberRow(Long projectId, Long userId) {
        if (!memberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            ProjectMember member = new ProjectMember();
            member.setProjectId(projectId);
            member.setUserId(userId);
            memberRepository.save(member);
        }
    }

    // ---------- board ----------

    @Transactional(readOnly = true)
    public BoardResponse board(Long projectId, Long sprintId, UserPrincipal principal) {
        requireProject(projectId);
        assertMember(projectId, principal);

        List<Issue> issues = sprintId == null
                ? issueRepository.findByProjectIdAndStatusInOrderByBoardOrderAscIdAsc(projectId, IssueStatuses.BOARD)
                : issueRepository.findByProjectIdAndSprintIdAndStatusInOrderByBoardOrderAscIdAsc(
                        projectId, sprintId, IssueStatuses.BOARD);

        Map<String, List<BoardIssue>> grouped = new LinkedHashMap<>();
        for (IssueStatus status : IssueStatuses.BOARD) {
            grouped.put(status.name(), new ArrayList<>());
        }
        for (Issue i : issues) {
            grouped.get(i.getStatus().name()).add(new BoardIssue(
                    i.getIssueKey(), i.getTitle(), i.getType().name(), i.getPriority().name(),
                    i.getStatus().name(), i.getAssigneeId(), i.getBoardOrder(), i.getSprintId()));
        }
        List<BoardColumn> columns = grouped.entrySet().stream()
                .map(e -> new BoardColumn(e.getKey(), e.getValue()))
                .toList();
        return new BoardResponse(projectId, sprintId, columns);
    }

    // ---------- issue-key generation ----------

    /** Bumps {@code issue_counter} under a row lock and returns the new key, e.g. {@code TF-14}. */
    @Transactional
    public String nextIssueKey(Long projectId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project " + projectId + " not found"));
        int next = project.getIssueCounter() + 1;
        project.setIssueCounter(next);
        return project.getKeyCode() + "-" + next;
    }
}
