package com.example.task_flow_backend.assignment;

import com.example.task_flow_backend.assignment.ScoredCandidate.Reason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the rules engine — no Spring context. This is the class the
 * scoring formula is pinned by.
 */
class WeightedScoringStrategyTest {

    private final AssignmentProperties props = new AssignmentProperties();
    private final WeightedScoringStrategy strategy = new WeightedScoringStrategy(props);

    private static Candidate candidate(long id, int wip, long open, Double avg, Map<Long, Integer> skills) {
        return new Candidate(id, wip, open, avg, skills);
    }

    @Test
    @DisplayName("a perfect skill match scores 1.0 on the skill term")
    void perfectMatchScoresOne() {
        Candidate c = candidate(1, 5, 0, null, Map.of(10L, 5));

        ScoredCandidate result = strategy.rank(List.of(c), Map.of(10L, 1), null).get(0);

        assertThat(result.skillScore()).isEqualTo(1.0);
        assertThat(result.eligible()).isTrue();
    }

    @Test
    @DisplayName("zero skill overlap scores 0 and is filtered below the threshold")
    void zeroOverlapIsFiltered() {
        Candidate c = candidate(1, 5, 0, null, Map.of(99L, 5));

        ScoredCandidate result = strategy.rank(List.of(c), Map.of(10L, 1), null).get(0);

        assertThat(result.skillScore()).isEqualTo(0.0);
        assertThat(result.eligible()).isFalse();
        assertThat(result.reason()).isEqualTo(Reason.BELOW_SKILL_THRESHOLD);
    }

    @Test
    @DisplayName("a candidate at their WIP cap is filtered out")
    void wipCapFiltersCandidate() {
        Candidate c = candidate(1, 3, 3, null, Map.of(10L, 5));

        ScoredCandidate result = strategy.rank(List.of(c), Map.of(10L, 1), null).get(0);

        assertThat(result.eligible()).isFalse();
        assertThat(result.reason()).isEqualTo(Reason.WIP_CAP_EXCEEDED);
        assertThat(result.loadScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("a user with no resolution history gets the neutral 0.5 speed score")
    void noHistoryGetsNeutralSpeedScore() {
        Candidate c = candidate(1, 5, 0, null, Map.of(10L, 4));

        ScoredCandidate result = strategy.rank(List.of(c), Map.of(10L, 1), null).get(0);

        assertThat(result.speedScore()).isEqualTo(props.getNeutralSpeedScore());
    }

    @Test
    @DisplayName("the issue reporter is not auto-assigned their own issue")
    void reporterIsBlockedFromSelfAssign() {
        Candidate c = candidate(7, 5, 0, null, Map.of(10L, 5));

        ScoredCandidate result = strategy.rank(List.of(c), Map.of(10L, 1), 7L).get(0);

        assertThat(result.eligible()).isFalse();
        assertThat(result.reason()).isEqualTo(Reason.SELF_ASSIGN_BLOCKED);
    }

    @Test
    @DisplayName("no candidates in -> no candidates out (engine turns this into TRIAGE)")
    void emptyCandidateListYieldsEmptyRanking() {
        assertThat(strategy.rank(List.of(), Map.of(10L, 1), null)).isEmpty();
    }

    @Test
    @DisplayName("with no required skills everyone clears the skill gate")
    void noRequiredSkillsMeansSkillScoreOne() {
        Candidate c = candidate(1, 5, 0, null, Map.of());

        ScoredCandidate result = strategy.rank(List.of(c), Map.of(), null).get(0);

        assertThat(result.skillScore()).isEqualTo(1.0);
        assertThat(result.eligible()).isTrue();
    }

    @Test
    @DisplayName("higher weighted skill match ranks first")
    void betterMatchRanksFirst() {
        Candidate strong = candidate(1, 5, 0, null, Map.of(10L, 5, 20L, 5));
        Candidate weak = candidate(2, 5, 0, null, Map.of(10L, 2));

        List<ScoredCandidate> ranked = strategy.rank(List.of(weak, strong), Map.of(10L, 1, 20L, 1), null);

        assertThat(ranked.get(0).userId()).isEqualTo(1);
        assertThat(ranked.get(0).totalScore()).isGreaterThan(ranked.get(1).totalScore());
    }

    @Test
    @DisplayName("load term rewards spare WIP capacity")
    void loadScoreReflectsSpareCapacity() {
        Candidate busy = candidate(1, 10, 8, null, Map.of(10L, 5));
        Candidate idle = candidate(2, 10, 0, null, Map.of(10L, 5));

        Map<Long, Integer> req = Map.of(10L, 1);
        double busyLoad = strategy.rank(List.of(busy), req, null).get(0).loadScore();
        double idleLoad = strategy.rank(List.of(idle), req, null).get(0).loadScore();

        assertThat(idleLoad).isGreaterThan(busyLoad);
        assertThat(idleLoad).isEqualTo(1.0);
    }
}
