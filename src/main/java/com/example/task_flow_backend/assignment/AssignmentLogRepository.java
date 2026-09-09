package com.example.task_flow_backend.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentLogRepository extends JpaRepository<AssignmentLog, Long> {

    List<AssignmentLog> findByIssueIdOrderByTotalScoreDesc(Long issueId);

    void deleteByIssueId(Long issueId);
}
