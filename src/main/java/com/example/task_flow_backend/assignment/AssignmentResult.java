package com.example.task_flow_backend.assignment;

import java.util.List;

/**
 * Outcome of one engine run. {@code selectedUserId} is null when nothing was
 * eligible (the issue goes to TRIAGE) or when the run was a preview.
 */
public record AssignmentResult(
        Long issueId,
        Long selectedUserId,
        boolean triaged,
        List<ScoredCandidate> ranked) {
}
