package com.example.task_flow_backend.model.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import com.example.task_flow_backend.model.common.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role = Role.MEMBER;

    /** Max concurrent open tickets the assignment engine will hand this user. */
    @Column(name = "wip_limit", nullable = false)
    private int wipLimit = 5;

    @Column(nullable = false)
    private boolean active = true;

    /** Rolling average, written by the nightly job; null until they close a ticket. */
    @Column(name = "avg_resolution_hours", precision = 8, scale = 2)
    private BigDecimal avgResolutionHours;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
