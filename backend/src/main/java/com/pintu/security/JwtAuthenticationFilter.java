package com.pintu.security;

import com.pintu.entity.User;
import com.pintu.enums.UserStatus;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.UserRoleMapper;
import com.pintu.entity.UserRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                User user = userMapper.selectById(claims.getSubject());
                String activeRole = claims.get("role", String.class);
                Number tokenVersion = claims.get("ver", Number.class);
                int currentVersion = user == null || user.getTokenVersion() == null
                        ? 0 : user.getTokenVersion();
                if (user != null
                        && !UserStatus.BANNED.getValue().equals(user.getStatus())
                        && tokenVersion != null
                        && tokenVersion.intValue() == currentVersion
                        && hasActiveRole(user.getId(), activeRole)) {
                    SecurityUser principal = new SecurityUser(user.getId(), user.getUsername(), activeRole);
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + activeRole.toUpperCase()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasActiveRole(String userId, String role) {
        return role != null && userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRole, role)
                .eq(UserRole::getStatus, "ACTIVE")) > 0;
    }
}
