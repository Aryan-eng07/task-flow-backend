package com.example.task_flow_backend.assignment;

import java.util.List;
import java.util.Map;

/**
 * Ranks project members for an issue. Kept behind an interface so a future
 * ML-based strategy (plan section 9.5) can slot in with the rules engine as the
 * fallback.
 */
public interface ScoringStrategy {

    /**
     * @param candidates    project members in scope (active), with load + skills resolved
     * @param requiredSkills skillId -&gt; weight for the issue; empty means "no skill constraint"
     * @param reporterId    the issue reporter, for the self-assign guard
     * @return every candidate scored, sorted best-first; ineligible ones sort last
     */
    List<ScoredCandidate> rank(List<Candidate> candidates,
                               Map<Long, Integer> requiredSkills,
                               Long reporterId);
}
