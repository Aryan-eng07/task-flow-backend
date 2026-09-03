package com.example.task_flow_backend.model.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import com.example.task_flow_backend.model.common.BaseEntity;

import java.time.Instant;

@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comment_issue", columnList = "issue_id, created_at")
})
@Getter
@Setter
public class Comment extends BaseEntity {

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;
}
