package com.example.task_flow_backend.issue;

public enum IssueStatus {
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE,
    BLOCKED,
    /**
     * Holding state for an issue the engine could not assign (no candidate passed
     * the filters). Not a board column; the nightly rebalance job retries these.
     */
    TRIAGE
}
