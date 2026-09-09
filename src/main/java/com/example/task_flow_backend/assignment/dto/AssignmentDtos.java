package com.example.task_flow_backend.assignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class AssignmentDtos {

    private AssignmentDtos() {
    }

    public record AssignmentConfigResponse(
            double skillWeight,
            double loadWeight,
            double speedWeight,
            double minSkillThreshold,
            boolean preventSelfAssign,
            double neutralSpeedScore,
            double defaultMaxAvgHours,
            int triageRetryHours,
            int resolutionWindowDays) {
    }

    /** All optional; null leaves the current value untouched. Weights need not sum to 1. */
    public record UpdateAssignmentConfigRequest(
            @DecimalMin("0.0") @DecimalMax("1.0") Double skillWeight,
            @DecimalMin("0.0") @DecimalMax("1.0") Double loadWeight,
            @DecimalMin("0.0") @DecimalMax("1.0") Double speedWeight,
            @DecimalMin("0.0") @DecimalMax("1.0") Double minSkillThreshold,
            Boolean preventSelfAssign,
            @DecimalMin("0.0") @DecimalMax("1.0") Double neutralSpeedScore,
            @DecimalMin("0.1") Double defaultMaxAvgHours,
            @Min(1) @Max(720) Integer triageRetryHours,
            @Min(1) @Max(365) Integer resolutionWindowDays) {
    }

    public record RequiredSkillInput(
            @NotNull Long skillId,
            @Min(1) @Max(5) Integer weight) {
    }

    public record PreviewRequest(
            @NotNull Long projectId,
            Long reporterId,
            List<@Valid RequiredSkillInput> requiredSkills) {
    }

    public record CandidateScoreResponse(
            Long userId,
            String fullName,
            BigDecimal skillScore,
            BigDecimal loadScore,
            BigDecimal speedScore,
            BigDecimal totalScore,
            long openCount,
            boolean eligible,
            String reason) {
    }

    public record AssignmentLogEntryResponse(
            Long candidateId,
            String fullName,
            BigDecimal skillScore,
            BigDecimal loadScore,
            BigDecimal speedScore,
            BigDecimal totalScore,
            boolean selected,
            String reason,
            java.time.Instant createdAt) {
    }

    public record RebalanceResponse(int scanned, int reassigned, int stillTriaged) {
    }
}
