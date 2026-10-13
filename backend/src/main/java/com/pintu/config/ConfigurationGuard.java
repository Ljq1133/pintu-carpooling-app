package com.pintu.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 启动期配置检查。
 * <p>
 * 在 {@code prod} profile 下，要求关键密钥与外部地址必须显式提供，否则直接终止进程
 * 防止把开发默认值带到生产环境。{@code dev} 与 {@code test} profile 仅作 WARN 日志提示。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigurationGuard {

    private final Environment environment;

    @Value("${security.jwt.secret:}")
    private String jwtSecret;

    @Value("${security.data-encryption-key:}")
    private String dataEncryptionKey;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${pintu.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @EventListener(ApplicationReadyEvent.class)
    public void verify() {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        List<String> missing = new ArrayList<>();

        if (isWeakSecret(jwtSecret)) {
            missing.add("security.jwt.secret (env JWT_SECRET) 必须设置为高强度随机值");
        }
        if (prod && isBlank(dataEncryptionKey)) {
            missing.add("security.data-encryption-key (env DATA_ENCRYPTION_KEY) 缺失");
        }
        if (prod && isBlank(dbPassword)) {
            missing.add("spring.datasource.password (env DB_PASSWORD) 缺失");
        }
        if (prod && isBlank(corsAllowedOrigins)) {
            missing.add("pintu.cors.allowed-origins (env CORS_ALLOWED_ORIGINS) 必须显式配置");
        }

        if (missing.isEmpty()) {
            log.info("configuration_guard ok profiles={}", Arrays.toString(environment.getActiveProfiles()));
            return;
        }

        if (prod) {
            log.error("启动失败：生产环境缺少关键配置 {}", missing);
            throw new IllegalStateException("生产环境缺少关键配置：" + missing);
        }
        log.warn("非生产环境检测到弱配置（仅警告）：{}", missing);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isWeakSecret(String value) {
        if (isBlank(value)) return true;
        if (value.length() < 32) return true;
        return value.startsWith("dev-only-") || value.startsWith("test-only-");
    }
}
