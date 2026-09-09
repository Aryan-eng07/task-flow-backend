package com.example.task_flow_backend.skill.dto;

import com.example.task_flow_backend.skill.Skill;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request/response records for the skill catalogue and per-user proficiencies.
 */
public final class SkillDtos {

    private SkillDtos() {
    }

    public record SkillResponse(Long id, String name) {
        public static SkillResponse from(Skill skill) {
            return new SkillResponse(skill.getId(), skill.getName());
        }
    }

    public record CreateSkillRequest(@NotBlank @Size(max = 60) String name) {
    }

    /** One row of {@code PUT /users/{id}/skills}. */
    public record UserSkillItem(
            @NotNull Long skillId,
            @NotNull @Min(1) @Max(5) Integer proficiency) {
    }

    public record ReplaceUserSkillsRequest(@NotNull List<@jakarta.validation.Valid UserSkillItem> skills) {
    }

    public record UserSkillView(Long skillId, String skillName, int proficiency) {
    }
}
