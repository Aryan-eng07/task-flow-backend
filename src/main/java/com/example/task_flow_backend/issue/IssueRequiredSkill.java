package com.example.task_flow_backend.issue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "issue_required_skills")
@IdClass(IssueRequiredSkillId.class)
@Getter
@Setter
public class IssueRequiredSkill {

    @Id
    @Column(name = "issue_id")
    private Long issueId;

    @Id
    @Column(name = "skill_id")
    private Long skillId;

    @Column(nullable = false)
    private Integer weight = 1;
}
