package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync(proxyTargetClass = true)
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Value("${app.async.email.core-pool-size:4}")
    private int corePoolSize;

    @Value("${app.async.email.max-pool-size:16}")
    private int maxPoolSize;

    @Value("${app.async.email.queue-capacity:500}")
    private int queueCapacity;

    @Bean(name = "emailTaskExecutor")
    public ThreadPoolTaskExecutor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("email-async-");
        // Chống rớt tác vụ khi hàng đợi đầy và chờ gửi nốt email trước khi tắt server
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return emailTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("[Async Error] Lỗi trong tác vụ nền {}({}) : {}",
                        method.getName(), Arrays.toString(params), ex.getMessage(), ex);
    }
}
