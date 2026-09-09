package com.example.task_flow_backend.sprint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProjectIdOrderByIdDesc(Long projectId);

    List<Sprint> findByProjectIdAndStatusOrderByIdDesc(Long projectId, SprintStatus status);

    Optional<Sprint> findByProjectIdAndStatus(Long projectId, SprintStatus status);

    boolean existsByProjectIdAndStatus(Long projectId, SprintStatus status);
}
