package com.example.task_flow_backend.issue.dto;

import com.example.task_flow_backend.issue.Issue;
import com.example.task_flow_backend.issue.IssueRequiredSkill;
import com.example.task_flow_backend.issue.IssueStatus;
import com.example.task_flow_backend.issue.IssueType;
import com.example.task_flow_backend.issue.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class IssueDtos {

    private IssueDtos() {
    }

    public record RequiredSkillItem(
            @NotNull Long skillId,
            @Min(1) @Max(5) Integer weight) {
    }

    public record CreateIssueRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 20000) String description,
            IssueType type,
            Priority priority,
            Long sprintId,
            @PositiveOrZero BigDecimal estimateHours,
            Long assigneeId,
            List<@Valid RequiredSkillItem> requiredSkills) {
    }

    /** PATCH /issues/{key} — all fields optional. */
    public record UpdateIssueRequest(
            @Size(max = 200) String title,
            @Size(max = 20000) String description,
            IssueType type,
            Priority priority,
            Long sprintId,
            @PositiveOrZero BigDecimal estimateHours,
            List<@Valid RequiredSkillItem> requiredSkills) {
    }

    public record StatusUpdateRequest(
            @NotNull IssueStatus status,
            Integer boardOrder) {
    }

    public record AssigneeUpdateRequest(@NotNull Long userId) {
    }

    public record IssueResponse(
            Long id,
            String issueKey,
            Long projectId,
            Long sprintId,
            String title,
            String description,
            String type,
            String priority,
            String status,
            Long reporterId,
            Long assigneeId,
            String assignmentMode,
            BigDecimal estimateHours,
            int boardOrder,
            List<RequiredSkillItem> requiredSkills,
            Instant createdAt,
            Instant updatedAt,
            Instant resolvedAt,
            Long version) {

        public static IssueResponse from(Issue i, List<IssueRequiredSkill> skills) {
            List<RequiredSkillItem> reqs = skills.stream()
                    .map(s -> new RequiredSkillItem(s.getSkillId(), s.getWeight()))
                    .toList();
            return new IssueResponse(
                    i.getId(), i.getIssueKey(), i.getProjectId(), i.getSprintId(),
                    i.getTitle(), i.getDescription(), i.getType().name(), i.getPriority().name(),
                    i.getStatus().name(), i.getReporterId(), i.getAssigneeId(), i.getAssignmentMode().name(),
                    i.getEstimateHours(), i.getBoardOrder(), reqs,
                    i.getCreatedAt(), i.getUpdatedAt(), i.getResolvedAt(), i.getVersion());
        }
    }
}
