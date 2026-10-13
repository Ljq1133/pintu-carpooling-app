package com.pintu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启用 {@code @Async}，为领域事件监听器和未来的异步任务提供支持。
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
