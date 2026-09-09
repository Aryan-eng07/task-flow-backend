package com.example.task_flow_backend.project.dto;

import com.example.task_flow_backend.project.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(
            @NotBlank @Size(max = 10) @Pattern(regexp = "[A-Za-z][A-Za-z0-9]{1,9}",
                    message = "must be 2-10 letters/digits starting with a letter") String keyCode,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 5000) String description) {
    }

    public record UpdateProjectRequest(
            @Size(max = 120) String name,
            @Size(max = 5000) String description,
            Long leadId) {
    }

    public record ProjectResponse(
            Long id,
            String keyCode,
            String name,
            String description,
            Long leadId,
            int issueCounter,
            boolean active,
            Instant createdAt) {

        public static ProjectResponse from(Project p) {
            return new ProjectResponse(p.getId(), p.getKeyCode(), p.getName(), p.getDescription(),
                    p.getLeadId(), p.getIssueCounter(), p.isActive(), p.getCreatedAt());
        }
    }

    public record AddMemberRequest(@NotNull Long userId) {
    }

    public record MemberResponse(Long userId, String fullName, String email, String role) {
    }

    // ----- Board -----

    public record BoardIssue(
            String issueKey,
            String title,
            String type,
            String priority,
            String status,
            Long assigneeId,
            int boardOrder,
            Long sprintId) {
    }

    public record BoardColumn(String status, List<BoardIssue> issues) {
    }

    public record BoardResponse(Long projectId, Long sprintId, List<BoardColumn> columns) {
    }
}
