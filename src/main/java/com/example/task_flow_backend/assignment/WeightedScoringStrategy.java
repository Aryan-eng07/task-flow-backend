package com.example.task_flow_backend.assignment;

import com.example.task_flow_backend.assignment.ScoredCandidate.Reason;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The default rules engine.
 *
 * <pre>
 * skillScore = Σ(weight_i · proficiency_i/5) / Σ(weight_i)      over the issue's required skills
 * loadScore  = clamp(1 − openCount / wipLimit, 0, 1)
 * speedScore = clamp(1 − avgResolutionHours / maxAvgHours, 0, 1)   (neutral 0.5 with no history)
 * total      = w_skill·skillScore + w_load·loadScore + w_speed·speedScore
 * </pre>
 *
 * Weights and thresholds come from {@link AssignmentProperties} — never hardcoded.
 */
@Component
public class WeightedScoringStrategy implements ScoringStrategy {

    private final AssignmentProperties props;

    public WeightedScoringStrategy(AssignmentProperties props) {
        this.props = props;
    }

    @Override
    public List<ScoredCandidate> rank(List<Candidate> candidates,
                                      Map<Long, Integer> requiredSkills,
                                      Long reporterId) {
        double maxAvgHours = candidates.stream()
                .map(Candidate::avgResolutionHours)
                .filter(v -> v != null && v > 0)
                .max(Double::compareTo)
                .orElse(props.getDefaultMaxAvgHours());
        if (maxAvgHours <= 0) {
            maxAvgHours = props.getDefaultMaxAvgHours();
        }

        final double denom = maxAvgHours;
        return candidates.stream()
                .map(c -> score(c, requiredSkills, reporterId, denom))
                .sorted(ranking())
                .toList();
    }

    private ScoredCandidate score(Candidate c, Map<Long, Integer> requiredSkills,
                                  Long reporterId, double maxAvgHours) {
        double skillScore = skillScore(c.skills(), requiredSkills);

        int wipLimit = Math.max(c.wipLimit(), 1);
        double loadScore = clamp(1.0 - (double) c.openCount() / wipLimit);

        double speedScore = c.avgResolutionHours() == null
                ? props.getNeutralSpeedScore()
                : clamp(1.0 - c.avgResolutionHours() / maxAvgHours);

        double total = props.getSkillWeight() * skillScore
                + props.getLoadWeight() * loadScore
                + props.getSpeedWeight() * speedScore;

        Reason reason = Reason.NOT_SELECTED;
        boolean eligible = true;
        if (c.openCount() >= c.wipLimit()) {
            eligible = false;
            reason = Reason.WIP_CAP_EXCEEDED;
        } else if (!requiredSkills.isEmpty() && skillScore < props.getMinSkillThreshold()) {
            eligible = false;
            reason = Reason.BELOW_SKILL_THRESHOLD;
        } else if (props.isPreventSelfAssign() && reporterId != null && reporterId == c.userId()) {
            eligible = false;
            reason = Reason.SELF_ASSIGN_BLOCKED;
        }

        return new ScoredCandidate(c.userId(), round(skillScore), round(loadScore), round(speedScore),
                round(total), c.openCount(), eligible, reason);
    }

    /** Weighted match of the issue's required skills against the user's proficiencies. */
    static double skillScore(Map<Long, Integer> userSkills, Map<Long, Integer> requiredSkills) {
        if (requiredSkills.isEmpty()) {
            return 1.0;
        }
        double weightSum = 0;
        double matched = 0;
        for (Map.Entry<Long, Integer> req : requiredSkills.entrySet()) {
            int weight = Math.max(req.getValue() == null ? 1 : req.getValue(), 1);
            weightSum += weight;
            Integer proficiency = userSkills.get(req.getKey());
            if (proficiency != null) {
                matched += weight * (Math.min(proficiency, 5) / 5.0);
            }
        }
        return weightSum == 0 ? 0.0 : matched / weightSum;
    }

    /** Eligible first; then total desc, skillScore desc, openCount asc, userId asc (deterministic). */
    private static Comparator<ScoredCandidate> ranking() {
        return Comparator
                .comparing(ScoredCandidate::eligible).reversed()
                .thenComparing(Comparator.comparingDouble(ScoredCandidate::totalScore).reversed())
                .thenComparing(Comparator.comparingDouble(ScoredCandidate::skillScore).reversed())
                .thenComparingLong(ScoredCandidate::openCount)
                .thenComparingLong(ScoredCandidate::userId);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static double round(double v) {
        return Math.round(v * 10000) / 10000.0;
    }
}
