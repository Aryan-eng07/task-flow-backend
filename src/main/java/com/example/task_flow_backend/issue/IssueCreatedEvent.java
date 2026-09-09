package com.example.task_flow_backend.issue;

/**
 * Published by {@code IssueService.create()} once the issue row is saved.
 * Consumed after commit so the assignment engine never logs against an issue
 * whose creating transaction rolled back.
 */
public record IssueCreatedEvent(Long issueId, Long projectId, String issueKey) {
}
