package com.example.task_flow_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables {@code @Async} (used by the after-commit assignment listener) and
 * {@code @Scheduled} (the nightly rebalance / metrics job).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    public static final String ASSIGNMENT_EXECUTOR = "assignmentExecutor";

    @Bean(name = ASSIGNMENT_EXECUTOR)
    public TaskExecutor assignmentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("assign-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
