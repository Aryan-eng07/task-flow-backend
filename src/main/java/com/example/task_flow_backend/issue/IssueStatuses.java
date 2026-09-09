package com.example.task_flow_backend.issue;

import java.util.List;

/**
 * Common {@link IssueStatus} groupings so services and the assignment engine
 * agree on what "open" and "on the board" mean.
 */
public final class IssueStatuses {

    private IssueStatuses() {
    }

    /** Counts against a user's WIP limit. */
    public static final List<IssueStatus> OPEN = List.of(
            IssueStatus.TODO, IssueStatus.IN_PROGRESS, IssueStatus.IN_REVIEW);

    /** Columns shown on the Kanban board. */
    public static final List<IssueStatus> BOARD = List.of(
            IssueStatus.TODO, IssueStatus.IN_PROGRESS, IssueStatus.IN_REVIEW, IssueStatus.DONE, IssueStatus.BLOCKED);
}
