package com.example.task_flow_backend.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "project_members")
@IdClass(ProjectMemberId.class)
@Getter
@Setter
public class ProjectMember {

    @Id
    @Column(name = "project_id")
    private Long projectId;

    @Id
    @Column(name = "user_id")
    private Long userId;
}
