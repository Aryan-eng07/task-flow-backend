package com.example.task_flow_backend.skill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Join row carrying proficiency. Modelled with an {@code @IdClass} composite key
 * rather than an entity relationship to keep the assignment-engine queries flat.
 */
@Entity
@Table(name = "user_skills")
@IdClass(UserSkillId.class)
@Getter
@Setter
public class UserSkill {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "skill_id")
    private Long skillId;

    /** 1..5 */
    @Column(nullable = false)
    private Integer proficiency;
}
