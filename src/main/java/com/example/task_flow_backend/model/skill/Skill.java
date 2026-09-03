package com.example.task_flow_backend.model.skill;

import com.example.task_flow_backend.model.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "skills")
@Getter
@Setter
public class Skill extends BaseEntity {

    /** e.g. 'java', 'flutter', 'mysql', 'devops'. */
    @Column(nullable = false, unique = true, length = 60)
    private String name;
}
