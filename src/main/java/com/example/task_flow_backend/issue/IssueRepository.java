package com.example.task_flow_backend.issue;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    Optional<Issue> findByIssueKey(String issueKey);

    boolean existsByIssueKey(String issueKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Issue i WHERE i.id = :id")
    Optional<Issue> findByIdForUpdate(@Param("id") Long id);

    List<Issue> findByProjectIdAndStatusInOrderByBoardOrderAscIdAsc(Long projectId, List<IssueStatus> statuses);

    List<Issue> findByProjectIdAndSprintIdAndStatusInOrderByBoardOrderAscIdAsc(
            Long projectId, Long sprintId, List<IssueStatus> statuses);

    long countByAssigneeIdAndStatusIn(Long assigneeId, List<IssueStatus> statuses);

    List<Issue> findBySprintIdAndStatusIn(Long sprintId, List<IssueStatus> statuses);

    List<Issue> findBySprintId(Long sprintId);

    List<Issue> findByStatusAndUpdatedAtBefore(IssueStatus status, Instant cutoff);

    @Query("""
            SELECT i FROM Issue i
            WHERE (:projectId IS NULL OR i.projectId = :projectId)
              AND (:assigneeId IS NULL OR i.assigneeId = :assigneeId)
              AND (:reporterId IS NULL OR i.reporterId = :reporterId)
              AND (:status IS NULL OR i.status = :status)
              AND (:sprintId IS NULL OR i.sprintId = :sprintId)
            """)
    Page<Issue> search(@Param("projectId") Long projectId,
                       @Param("assigneeId") Long assigneeId,
                       @Param("reporterId") Long reporterId,
                       @Param("status") IssueStatus status,
                       @Param("sprintId") Long sprintId,
                       Pageable pageable);

    @Query("""
            SELECT i FROM Issue i
            WHERE i.status = com.example.task_flow_backend.issue.IssueStatus.DONE
              AND i.resolvedAt IS NOT NULL
              AND i.assigneeId IS NOT NULL
              AND i.resolvedAt >= :since
            """)
    List<Issue> findResolvedSince(@Param("since") Instant since);
}
