package com.taskboard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authorization 헤더의 Bearer 토큰으로 SecurityContext 설정.
 * 토큰이 없거나 잘못돼도 여기서 막지 않음 — 접근 허용 여부는 SecurityConfig가 판단
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenAuthenticator tokenAuthenticator;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        tokenAuthenticator.resolveBearer(request.getHeader("Authorization"))
                .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(user.toAuthentication()));
        filterChain.doFilter(request, response);
    }
}
