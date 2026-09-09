package com.example.task_flow_backend.assignment;

import com.example.task_flow_backend.config.AsyncConfig;
import com.example.task_flow_backend.issue.IssueCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs the assignment engine after the issue-creating transaction commits, on a
 * separate thread so {@code POST /issues} returns 201 immediately.
 */
@Component
public class AssignmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AssignmentEventListener.class);

    private final AssignmentEngine engine;

    public AssignmentEventListener(AssignmentEngine engine) {
        this.engine = engine;
    }

    @Async(AsyncConfig.ASSIGNMENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueCreated(IssueCreatedEvent event) {
        try {
            engine.assign(event.issueId());
        } catch (Exception ex) {
            log.error("auto-assignment failed for issue {} ({})", event.issueKey(), event.issueId(), ex);
        }
    }
}
