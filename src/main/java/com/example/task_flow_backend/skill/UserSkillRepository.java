package com.example.task_flow_backend.skill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, UserSkillId> {

    List<UserSkill> findByUserId(Long userId);

    List<UserSkill> findByUserIdIn(List<Long> userIds);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);

    @Query("SELECT us FROM UserSkill us WHERE us.userId = :userId AND us.skillId IN :skillIds")
    List<UserSkill> findByUserIdAndSkillIds(@Param("userId") Long userId, @Param("skillIds") List<Long> skillIds);
}
