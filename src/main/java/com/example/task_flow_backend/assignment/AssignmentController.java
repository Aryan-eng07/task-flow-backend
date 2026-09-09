package com.example.task_flow_backend.assignment;

import com.example.task_flow_backend.assignment.dto.AssignmentDtos.AssignmentConfigResponse;
import com.example.task_flow_backend.assignment.dto.AssignmentDtos.AssignmentLogEntryResponse;
import com.example.task_flow_backend.assignment.dto.AssignmentDtos.CandidateScoreResponse;
import com.example.task_flow_backend.assignment.dto.AssignmentDtos.PreviewRequest;
import com.example.task_flow_backend.assignment.dto.AssignmentDtos.RebalanceResponse;
import com.example.task_flow_backend.assignment.dto.AssignmentDtos.UpdateAssignmentConfigRequest;
import com.example.task_flow_backend.auth.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/assignment/config")
    public AssignmentConfigResponse getConfig() {
        return assignmentService.getConfig();
    }

    @PutMapping("/assignment/config")
    @PreAuthorize("hasRole('ADMIN')")
    public AssignmentConfigResponse updateConfig(@Valid @RequestBody UpdateAssignmentConfigRequest request) {
        return assignmentService.updateConfig(request);
    }

    @PostMapping("/assignment/preview")
    public List<CandidateScoreResponse> preview(@Valid @RequestBody PreviewRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return assignmentService.preview(request, principal);
    }

    @PostMapping("/assignment/rebalance")
    @PreAuthorize("hasRole('ADMIN')")
    public RebalanceResponse rebalance() {
        return assignmentService.rebalance();
    }

    @GetMapping("/issues/{key}/assignment-log")
    public List<AssignmentLogEntryResponse> issueLog(@PathVariable String key,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return assignmentService.issueLog(key, principal);
    }
}
