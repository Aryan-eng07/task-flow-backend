package com.example.task_flow_backend.project;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByKeyCodeIgnoreCase(String keyCode);

    Optional<Project> findByKeyCodeIgnoreCase(String keyCode);

    /**
     * Locks the project row so the {@code issue_counter} bump that produces the
     * next issue key is serialized under concurrent issue creation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT p FROM Project p
            WHERE p.active = true
              AND (p.id IN (SELECT pm.projectId FROM ProjectMember pm WHERE pm.userId = :userId)
                   OR p.leadId = :userId)
            ORDER BY p.createdAt DESC
            """)
    List<Project> findVisibleTo(@Param("userId") Long userId);
}
