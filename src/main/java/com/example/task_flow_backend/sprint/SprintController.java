package com.example.task_flow_backend.sprint;

import com.example.task_flow_backend.auth.UserPrincipal;
import com.example.task_flow_backend.sprint.dto.SprintDtos.CompleteSprintRequest;
import com.example.task_flow_backend.sprint.dto.SprintDtos.CreateSprintRequest;
import com.example.task_flow_backend.sprint.dto.SprintDtos.SprintReportResponse;
import com.example.task_flow_backend.sprint.dto.SprintDtos.SprintResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @PostMapping("/projects/{projectId}/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    public SprintResponse create(@PathVariable Long projectId,
                                 @Valid @RequestBody CreateSprintRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return sprintService.create(projectId, request, principal);
    }

    @GetMapping("/projects/{projectId}/sprints")
    public List<SprintResponse> list(@PathVariable Long projectId,
                                     @RequestParam(required = false) SprintStatus status,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return sprintService.list(projectId, status, principal);
    }

    @PostMapping("/sprints/{id}/start")
    public SprintResponse start(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return sprintService.start(id, principal);
    }

    @PostMapping("/sprints/{id}/complete")
    public SprintResponse complete(@PathVariable Long id,
                                   @RequestBody(required = false) CompleteSprintRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return sprintService.complete(id, request, principal);
    }

    @GetMapping("/sprints/{id}/report")
    public SprintReportResponse report(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return sprintService.report(id, principal);
    }
}
