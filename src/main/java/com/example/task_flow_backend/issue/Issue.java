package com.example.task_flow_backend.issue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.task_flow_backend.common.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "issues", indexes = {
        @Index(name = "idx_issue_board", columnList = "project_id, status, board_order"),
        @Index(name = "idx_issue_assignee", columnList = "assignee_id, status"),
        @Index(name = "idx_issue_sprint", columnList = "sprint_id, status")
})
@Getter
@Setter
public class Issue extends BaseEntity {

    /** 'TF-14' */
    @Column(name = "issue_key", nullable = false, unique = true, length = 20)
    private String issueKey;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "sprint_id")
    private Long sprintId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IssueType type = IssueType.TASK;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private IssueStatus status = IssueStatus.TODO;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_mode", nullable = false, length = 12)
    private AssignmentMode assignmentMode = AssignmentMode.UNASSIGNED;

    @Column(name = "estimate_hours", precision = 6, scale = 2)
    private BigDecimal estimateHours;

    /** Position within its board column. */
    @Column(name = "board_order", nullable = false)
    private int boardOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant updatedAt;

    /** Set when status -> DONE. */
    @Column(name = "resolved_at", columnDefinition = "datetime(6)")
    private Instant resolvedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
