package com.example.task_flow_backend.model.assignment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import com.example.task_flow_backend.model.common.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Audit trail for every scored candidate on an assignment run. The whole
 * candidate list is persisted, with {@link #selected} set on the winner, so the
 * "why this assignee" panel can show the full ranking.
 */
@Entity
@Table(name = "assignment_log")
@Getter
@Setter
public class AssignmentLog extends BaseEntity {

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "skill_score", precision = 5, scale = 4)
    private BigDecimal skillScore;

    @Column(name = "load_score", precision = 5, scale = 4)
    private BigDecimal loadScore;

    @Column(name = "speed_score", precision = 5, scale = 4)
    private BigDecimal speedScore;

    @Column(name = "total_score", precision = 5, scale = 4)
    private BigDecimal totalScore;

    @Column(nullable = false)
    private boolean selected;

    /** 'WIP_CAP_EXCEEDED', 'BELOW_SKILL_THRESHOLD', 'SELECTED', ... */
    @Column(length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;
}
