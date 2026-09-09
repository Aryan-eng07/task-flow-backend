package com.example.task_flow_backend.assignment;

import com.example.task_flow_backend.assignment.dto.AssignmentDtos.RebalanceResponse;
import com.example.task_flow_backend.issue.Issue;
import com.example.task_flow_backend.issue.IssueRepository;
import com.example.task_flow_backend.user.User;
import com.example.task_flow_backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nightly maintenance (plan section 4.7):
 * <ol>
 *   <li>recompute {@code avg_resolution_hours} per user over the rolling window,</li>
 *   <li>retry issues stuck in TRIAGE past the configured age,</li>
 *   <li>emit a metrics snapshot (auto-assigned %, avg selected score, triage backlog).</li>
 * </ol>
 */
@Component
public class RebalanceJob {

    private static final Logger log = LoggerFactory.getLogger(RebalanceJob.class);

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final AssignmentLogRepository logRepository;
    private final AssignmentService assignmentService;
    private final AssignmentProperties props;

    public RebalanceJob(IssueRepository issueRepository,
                        UserRepository userRepository,
                        AssignmentLogRepository logRepository,
                        AssignmentService assignmentService,
                        AssignmentProperties props) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.logRepository = logRepository;
        this.assignmentService = assignmentService;
        this.props = props;
    }

    @Scheduled(cron = "${taskflow.assignment.nightly-cron:0 0 2 * * *}")
    public void runNightly() {
        log.info("nightly assignment job starting");
        int updatedUsers = recomputeResolutionAverages();
        RebalanceResponse rebalance = assignmentService.rebalance();
        emitMetricsSnapshot();
        log.info("nightly assignment job done: {} user averages updated, triage rebalance {}",
                updatedUsers, rebalance);
    }

    @Transactional
    public int recomputeResolutionAverages() {
        Instant since = Instant.now().minus(props.getResolutionWindowDays(), ChronoUnit.DAYS);
        List<Issue> resolved = issueRepository.findResolvedSince(since);

        Map<Long, double[]> acc = new HashMap<>(); // userId -> [sumHours, count]
        for (Issue i : resolved) {
            if (i.getResolvedAt() == null || i.getCreatedAt() == null) {
                continue;
            }
            double hours = Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes() / 60.0;
            if (hours < 0) {
                continue;
            }
            double[] a = acc.computeIfAbsent(i.getAssigneeId(), k -> new double[2]);
            a[0] += hours;
            a[1] += 1;
        }

        int updated = 0;
        for (User user : userRepository.findAll()) {
            double[] a = acc.get(user.getId());
            BigDecimal avg = (a == null || a[1] == 0)
                    ? null
                    : BigDecimal.valueOf(a[0] / a[1]).setScale(2, RoundingMode.HALF_UP);
            if (avg != null || user.getAvgResolutionHours() != null) {
                user.setAvgResolutionHours(avg);
                updated++;
            }
        }
        return updated;
    }

    private void emitMetricsSnapshot() {
        long selected = logRepository.count();
        List<Issue> triaged = issueRepository.findByStatusAndUpdatedAtBefore(
                com.example.task_flow_backend.issue.IssueStatus.TRIAGE, Instant.now());
        log.info("assignment metrics: assignment_log_rows={} triage_backlog={}", selected, triaged.size());
    }
}
