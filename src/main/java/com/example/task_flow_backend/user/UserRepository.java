package com.example.task_flow_backend.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN UserSkill us ON us.userId = u.id
            LEFT JOIN Skill s ON s.id = us.skillId
            WHERE (:active IS NULL OR u.active = :active)
              AND (:skill IS NULL OR LOWER(s.name) = LOWER(:skill))
            """)
    Page<User> search(@Param("skill") String skill,
                      @Param("active") Boolean active,
                      Pageable pageable);

    List<User> findAllByActiveTrue();
}
