package com.example.task_flow_backend.auth.dto;

import com.example.task_flow_backend.user.Role;
import com.example.task_flow_backend.user.User;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        int wipLimit,
        boolean active) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getWipLimit(),
                user.isActive());
    }
}
