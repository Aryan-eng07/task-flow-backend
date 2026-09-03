package com.example.task_flow_backend.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import com.example.task_flow_backend.common.BaseEntity;

import java.time.Instant;

/**
 * Optional per-field change history for an issue (section 1 of the plan).
 * Cheap to write from the same points that publish realtime deltas.
 */
@Entity
@Table(name = "activity_log")
@Getter
@Setter
public class ActivityLog extends BaseEntity {

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(length = 60)
    private String field;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;
}
