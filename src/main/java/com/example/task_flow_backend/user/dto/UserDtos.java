package com.example.task_flow_backend.user.dto;

import com.example.task_flow_backend.user.Role;
import com.example.task_flow_backend.user.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(
            Long id,
            String email,
            String fullName,
            Role role,
            int wipLimit,
            boolean active,
            BigDecimal avgResolutionHours) {

        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    user.getWipLimit(),
                    user.isActive(),
                    user.getAvgResolutionHours());
        }
    }

    /** PATCH /users/{id} — every field optional; null means "leave unchanged". */
    public record UpdateUserRequest(
            @Size(max = 120) String fullName,
            @Min(1) Integer wipLimit,
            Boolean active,
            Role role) {
    }

    public record WorkloadResponse(
            Long userId,
            long openIssues,
            int wipLimit,
            BigDecimal avgResolutionHours) {
    }
}
