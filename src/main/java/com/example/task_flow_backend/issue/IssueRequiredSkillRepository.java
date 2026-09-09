package com.example.task_flow_backend.issue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRequiredSkillRepository
        extends JpaRepository<IssueRequiredSkill, IssueRequiredSkillId> {

    List<IssueRequiredSkill> findByIssueId(Long issueId);

    void deleteByIssueId(Long issueId);
}
