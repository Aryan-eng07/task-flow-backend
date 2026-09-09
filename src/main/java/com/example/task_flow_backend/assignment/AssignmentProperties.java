package com.example.task_flow_backend.assignment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Live-tunable knobs for the assignment engine, bound from
 * {@code taskflow.assignment.*}. Mutable (not a record) so
 * {@code PUT /assignment/config} can adjust weights at runtime without a restart.
 * Never hardcode these numbers anywhere else.
 */
@ConfigurationProperties(prefix = "taskflow.assignment")
public class AssignmentProperties {

    /** Weight of the skill-match term in the total score. */
    private double skillWeight = 0.5;
    /** Weight of the load (spare WIP capacity) term. */
    private double loadWeight = 0.3;
    /** Weight of the historical resolution-speed term. */
    private double speedWeight = 0.2;

    /** A candidate whose weighted skill match is below this is filtered out. */
    private double minSkillThreshold = 0.2;

    /** When true, the issue reporter is never auto-assigned their own issue. */
    private boolean preventSelfAssign = true;

    /** Speed score given to a user with no resolution history (avoids starving newcomers). */
    private double neutralSpeedScore = 0.5;

    /** Fallback denominator for the speed term when no project member has any history. */
    private double defaultMaxAvgHours = 80.0;

    /** A TRIAGE issue older than this many hours is retried by the nightly job. */
    private int triageRetryHours = 24;

    /** Rolling window the nightly job uses to recompute avg_resolution_hours. */
    private int resolutionWindowDays = 30;

    public double getSkillWeight() {
        return skillWeight;
    }

    public void setSkillWeight(double skillWeight) {
        this.skillWeight = skillWeight;
    }

    public double getLoadWeight() {
        return loadWeight;
    }

    public void setLoadWeight(double loadWeight) {
        this.loadWeight = loadWeight;
    }

    public double getSpeedWeight() {
        return speedWeight;
    }

    public void setSpeedWeight(double speedWeight) {
        this.speedWeight = speedWeight;
    }

    public double getMinSkillThreshold() {
        return minSkillThreshold;
    }

    public void setMinSkillThreshold(double minSkillThreshold) {
        this.minSkillThreshold = minSkillThreshold;
    }

    public boolean isPreventSelfAssign() {
        return preventSelfAssign;
    }

    public void setPreventSelfAssign(boolean preventSelfAssign) {
        this.preventSelfAssign = preventSelfAssign;
    }

    public double getNeutralSpeedScore() {
        return neutralSpeedScore;
    }

    public void setNeutralSpeedScore(double neutralSpeedScore) {
        this.neutralSpeedScore = neutralSpeedScore;
    }

    public double getDefaultMaxAvgHours() {
        return defaultMaxAvgHours;
    }

    public void setDefaultMaxAvgHours(double defaultMaxAvgHours) {
        this.defaultMaxAvgHours = defaultMaxAvgHours;
    }

    public int getTriageRetryHours() {
        return triageRetryHours;
    }

    public void setTriageRetryHours(int triageRetryHours) {
        this.triageRetryHours = triageRetryHours;
    }

    public int getResolutionWindowDays() {
        return resolutionWindowDays;
    }

    public void setResolutionWindowDays(int resolutionWindowDays) {
        this.resolutionWindowDays = resolutionWindowDays;
    }
}
