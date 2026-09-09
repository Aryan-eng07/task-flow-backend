package com.example.task_flow_backend.assignment;

import java.util.Map;

/**
 * Flattened input to the scoring strategy: one project member with everything
 * the formula needs. {@code avgResolutionHours} is null for a user who has never
 * closed a ticket.
 */
public record Candidate(
        long userId,
        int wipLimit,
        long openCount,
        Double avgResolutionHours,
        Map<Long, Integer> skills) {
}
