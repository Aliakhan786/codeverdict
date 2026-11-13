package com.aaliya.codeverdict.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reviews take several seconds, so they run in the background on a small pool
 * (2 workers by default - the free AI tier and one VM cannot do much more).
 * The HTTP request returns at once and the browser follows progress over SSE.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "reviewExecutor")
    public ThreadPoolTaskExecutor reviewExecutor(CodeVerdictProperties props) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(props.review().workerThreads());
        ex.setMaxPoolSize(props.review().workerThreads());
        ex.setQueueCapacity(props.review().queueCapacity());
        ex.setThreadNamePrefix("review-");
        ex.setWaitForTasksToCompleteOnShutdown(false);
        ex.initialize();
        return ex;
    }

    /** Zone-aware clock so "today" (streaks, heatmap) matches the user's day, not UTC. */
    @Bean
    public Clock clock(CodeVerdictProperties props) {
        return Clock.system(ZoneId.of(props.timezone()));
    }
}
