package com.example.task_flow_backend.realtime;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A small delta pushed to {@code /topic/projects/{projectId}/board}. We publish
 * deltas, not whole boards, so payload size stays constant as a project grows.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BoardEvent(
        String type,
        String issueKey,
        String status,
        Integer boardOrder,
        Long assigneeId,
        Long sprintId,
        Long actorId) {

    public static BoardEvent issueCreated(String issueKey, String status, Long assigneeId, Long actorId) {
        return new BoardEvent("ISSUE_CREATED", issueKey, status, null, assigneeId, null, actorId);
    }

    public static BoardEvent issueAssigned(String issueKey, String status, Long assigneeId, Long actorId) {
        return new BoardEvent("ISSUE_ASSIGNED", issueKey, status, null, assigneeId, null, actorId);
    }

    public static BoardEvent statusChanged(String issueKey, String status, Integer boardOrder, Long actorId) {
        return new BoardEvent("ISSUE_MOVED", issueKey, status, boardOrder, null, null, actorId);
    }

    public static BoardEvent issueUpdated(String issueKey, String status, Long actorId) {
        return new BoardEvent("ISSUE_UPDATED", issueKey, status, null, null, null, actorId);
    }

    public static BoardEvent issueDeleted(String issueKey, Long actorId) {
        return new BoardEvent("ISSUE_DELETED", issueKey, null, null, null, null, actorId);
    }
}
