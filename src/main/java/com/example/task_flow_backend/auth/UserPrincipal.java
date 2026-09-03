package com.example.task_flow_backend.auth;

import com.example.task_flow_backend.user.Role;

/**
 * The authenticated principal stored in the security context. Carries just
 * enough identity for controllers and {@code @PreAuthorize} checks; anything
 * else is loaded from the repository.
 */
public record UserPrincipal(Long id, String email, Role role) {
}
