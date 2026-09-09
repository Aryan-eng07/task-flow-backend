package com.example.task_flow_backend.assignment;

/**
 * Result of scoring one {@link Candidate}. Every candidate (eligible or not) gets
 * one of these; it is persisted verbatim as an {@code assignment_log} row so the
 * "why this assignee" panel can show the full ranking.
 */
public record ScoredCandidate(
        long userId,
        double skillScore,
        double loadScore,
        double speedScore,
        double totalScore,
        long openCount,
        boolean eligible,
        Reason reason) {

    public enum Reason {
        SELECTED,
        NOT_SELECTED,
        WIP_CAP_EXCEEDED,
        BELOW_SKILL_THRESHOLD,
        SELF_ASSIGN_BLOCKED
    }

    public ScoredCandidate asSelected() {
        return new ScoredCandidate(userId, skillScore, loadScore, speedScore, totalScore,
                openCount, true, Reason.SELECTED);
    }
}
