package com.example.task_flow_backend.project;

import com.example.task_flow_backend.auth.UserPrincipal;
import com.example.task_flow_backend.project.dto.ProjectDtos.AddMemberRequest;
import com.example.task_flow_backend.project.dto.ProjectDtos.BoardResponse;
import com.example.task_flow_backend.project.dto.ProjectDtos.CreateProjectRequest;
import com.example.task_flow_backend.project.dto.ProjectDtos.MemberResponse;
import com.example.task_flow_backend.project.dto.ProjectDtos.ProjectResponse;
import com.example.task_flow_backend.project.dto.ProjectDtos.UpdateProjectRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.create(request, principal);
    }

    @GetMapping
    public List<ProjectResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return projectService.listVisible(principal);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.get(id, principal);
    }

    @PatchMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateProjectRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        projectService.softDelete(id);
    }

    @GetMapping("/{id}/members")
    public List<MemberResponse> members(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.listMembers(id, principal);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public void addMember(@PathVariable Long id,
                          @Valid @RequestBody AddMemberRequest request,
                          @AuthenticationPrincipal UserPrincipal principal) {
        projectService.addMember(id, request, principal);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id,
                             @PathVariable Long userId,
                             @AuthenticationPrincipal UserPrincipal principal) {
        projectService.removeMember(id, userId, principal);
    }

    @GetMapping("/{id}/board")
    public BoardResponse board(@PathVariable Long id,
                               @RequestParam(required = false) Long sprintId,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.board(id, sprintId, principal);
    }
}
