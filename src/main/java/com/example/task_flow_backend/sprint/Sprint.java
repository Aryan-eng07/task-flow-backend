package com.example.task_flow_backend.sprint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import com.example.task_flow_backend.common.BaseEntity;

@Entity
@Table(name = "sprints")
@Getter
@Setter
public class Sprint extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(length = 120)
    private String name;

    @Column(length = 255)
    private String goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SprintStatus status = SprintStatus.PLANNED;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
