package com.researchi.admin.legacy.matching.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class LegacyMatchingAsyncConfig {

    @Bean("legacyMatchingTaskExecutor")
    public Executor legacyMatchingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("legacy-matching-");
        executor.initialize();
        return executor;
    }
}
