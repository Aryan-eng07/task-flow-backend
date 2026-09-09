package com.example.task_flow_backend.issue;

import com.example.task_flow_backend.auth.UserPrincipal;
import com.example.task_flow_backend.common.PageResponse;
import com.example.task_flow_backend.issue.dto.IssueDtos.AssigneeUpdateRequest;
import com.example.task_flow_backend.issue.dto.IssueDtos.CreateIssueRequest;
import com.example.task_flow_backend.issue.dto.IssueDtos.IssueResponse;
import com.example.task_flow_backend.issue.dto.IssueDtos.StatusUpdateRequest;
import com.example.task_flow_backend.issue.dto.IssueDtos.UpdateIssueRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/v1")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping("/projects/{projectId}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse create(@PathVariable Long projectId,
                                @Valid @RequestBody CreateIssueRequest request,
                                @AuthenticationPrincipal UserPrincipal principal) {
        return issueService.create(projectId, request, principal);
    }

    @GetMapping("/issues/{key}")
    public IssueResponse get(@PathVariable String key, @AuthenticationPrincipal UserPrincipal principal) {
        return issueService.get(key, principal);
    }

    @GetMapping("/issues")
    public PageResponse<IssueResponse> search(@RequestParam(required = false) Long projectId,
                                              @RequestParam(required = false) String assignee,
                                              @RequestParam(required = false) String reporter,
                                              @RequestParam(required = false) IssueStatus status,
                                              @RequestParam(required = false) Long sprintId,
                                              @PageableDefault(size = 20) Pageable pageable,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        Long assigneeId = resolveUserRef(assignee, principal);
        Long reporterId = resolveUserRef(reporter, principal);
        return issueService.search(projectId, assigneeId, reporterId, status, sprintId, pageable);
    }

    @PatchMapping("/issues/{key}")
    public IssueResponse update(@PathVariable String key,
                                @Valid @RequestBody UpdateIssueRequest request,
                                @AuthenticationPrincipal UserPrincipal principal) {
        return issueService.update(key, request, principal);
    }

    @PatchMapping("/issues/{key}/status")
    public IssueResponse changeStatus(@PathVariable String key,
                                      @Valid @RequestBody StatusUpdateRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return issueService.changeStatus(key, request, principal);
    }

    @PatchMapping("/issues/{key}/assignee")
    public IssueResponse changeAssignee(@PathVariable String key,
                                        @Valid @RequestBody AssigneeUpdateRequest request,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return issueService.changeAssignee(key, request, principal);
    }

    @PostMapping("/issues/{key}/reassign")
    public IssueResponse reassign(@PathVariable String key, @AuthenticationPrincipal UserPrincipal principal) {
        return issueService.reassign(key, principal);
    }

    @DeleteMapping("/issues/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String key, @AuthenticationPrincipal UserPrincipal principal) {
        issueService.delete(key, principal);
    }

    private Long resolveUserRef(String ref, UserPrincipal principal) {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        if (ref.equalsIgnoreCase("me")) {
            return principal.id();
        }
        try {
            return Long.parseLong(ref.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Expected a user id or 'me', got '" + ref + "'");
        }
    }
}
