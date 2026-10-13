package com.pintu.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * CORS 跨域配置。
 * <p>
 * 白名单从环境变量 {@code CORS_ALLOWED_ORIGINS} 读取，逗号分隔。
 * 默认值仅含本地开发与构建预览端口；生产部署必须显式覆盖为真实域名，
 * 不允许使用 {@code "*"} 等宽泛配置。
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    @Value("${pintu.cors.allowed-origins:http://127.0.0.1:5173,http://localhost:5173,http://127.0.0.1:4173,http://localhost:4173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);

        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Trace-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
