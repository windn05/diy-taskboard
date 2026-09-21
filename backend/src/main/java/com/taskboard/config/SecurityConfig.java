package com.taskboard.config;

import com.taskboard.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT 기반 무상태 인증. 역할별 허용 범위는 URL 단위로만 거르고,
 * 프로젝트 멤버 여부 같은 리소스 단위 권한은 각 서비스에서 검사
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 쿠키가 아닌 Authorization 헤더로 인증하므로 CSRF 토큰 불필요
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /error를 막으면 404·405가 전부 403으로 바뀌어 나감 (에러율 통계도 왜곡)
                        .requestMatchers("/error").permitAll()
                        // /auth 전체를 열면 비밀번호 변경까지 인증 없이 열림. 필요한 것만 허용
                        .requestMatchers("/auth/login", "/auth/guest", "/auth/refresh").permitAll()
                        // WebSocket은 핸드셰이크가 아닌 STOMP CONNECT에서 인증 (StompAuthChannelInterceptor)
                        .requestMatchers("/ws/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/**").authenticated()
                        // 조회 외의 모든 요청은 실제 사용자만 — 게스트는 읽기 전용
                        .anyRequest().hasAnyRole("ADMIN", "USER")
                )
                // 인증이 없으면 401. 기본값 403으로는 프론트가 토큰 만료를 구분하지 못함
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
