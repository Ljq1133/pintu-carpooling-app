package com.pintu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pintu.common.api.Result;
import com.pintu.security.JwtAuthenticationFilter;
import com.pintu.security.AuthRateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/auth/**", "/api/files/**", "/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**", "/ws/**",
                                "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/api/safety/share-links/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/driver/**").hasRole("DRIVER")
                        .requestMatchers("/api/passenger/**").hasRole("PASSENGER")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, 1001, "请先登录或登录已过期"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, HttpStatus.FORBIDDEN, 1004, "无权访问该资源")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authRateLimitFilter, JwtAuthenticationFilter.class)
                .build();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, int code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.fail(code, message));
    }
}
