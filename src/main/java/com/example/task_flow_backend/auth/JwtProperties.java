package com.example.task_flow_backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Bound from {@code taskflow.jwt.*}. Registered via
 * {@code @EnableConfigurationProperties} on the security config.
 */
@ConfigurationProperties(prefix = "taskflow.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl) {
}
