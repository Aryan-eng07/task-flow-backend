package com.example.task_flow_backend.sprint.dto;

import com.example.task_flow_backend.sprint.Sprint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class SprintDtos {

    private SprintDtos() {
    }

    public record CreateSprintRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 255) String goal,
            LocalDate startDate,
            LocalDate endDate) {
    }

    public record CompleteSprintRequest(Long moveUnfinishedToSprintId) {
    }

    public record SprintResponse(
            Long id,
            Long projectId,
            String name,
            String goal,
            String status,
            LocalDate startDate,
            LocalDate endDate) {

        public static SprintResponse from(Sprint s) {
            return new SprintResponse(s.getId(), s.getProjectId(), s.getName(), s.getGoal(),
                    s.getStatus().name(), s.getStartDate(), s.getEndDate());
        }
    }

    public record SprintReportResponse(
            Long sprintId,
            String status,
            int totalIssues,
            int completedIssues,
            int carriedOverIssues,
            double completionRate) {
    }
}
