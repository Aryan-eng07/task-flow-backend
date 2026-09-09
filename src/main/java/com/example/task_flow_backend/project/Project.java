package com.example.task_flow_backend.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import com.example.task_flow_backend.common.BaseEntity;

import java.time.Instant;

@Entity
@Table(name = "projects")
@Getter
@Setter
public class Project extends BaseEntity {

    /** 'TF' -> issue keys TF-1, TF-2 ... */
    @Column(name = "key_code", nullable = false, unique = true, length = 10)
    private String keyCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "lead_id")
    private Long leadId;

    /** Source of the numeric part of an issue key; bumped under lock on create. */
    @Column(name = "issue_counter", nullable = false)
    private int issueCounter = 0;

    /** Soft-delete flag; {@code DELETE /projects/{id}} clears this instead of removing the row. */
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;
}
