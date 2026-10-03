package com.taskboard.global.config;

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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/** 인증·인가 설정 (세션 기반, 세션은 Redis에 저장) */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*************************************************************************
     * 목적 : 로그인 정보를 HTTP 세션에 저장하는 저장소 등록
     * 이유 : 폼 로그인 대신 AuthController가 직접 로그인을 처리하므로 세션 저장도 명시적으로 수행
     * 파라미터
     * -
     * 반환
     * - 세션 기반 SecurityContext 저장소
     *************************************************************************/
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository securityContextRepository)
            throws Exception {
        http
                // ponytail: 세션 쿠키가 SameSite=Lax(application.yml)라 크로스사이트 요청엔 안 실려서
                // 별도 CSRF 토큰 없이도 안전 — 쿠키가 SameSite=None(진짜 크로스오리진 배포)으로 바뀌면 CSRF 토큰 도입 필요
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(ctx -> ctx.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        // /error를 막으면 404·405가 전부 403으로 바뀌어 나감 (에러율 통계도 왜곡)
                        .requestMatchers("/error").permitAll()
                        // /auth 전체를 열면 비밀번호 변경까지 인증 없이 열림. 필요한 것만 허용
                        .requestMatchers("/auth/login", "/auth/guest").permitAll()
                        // WebSocket은 핸드셰이크가 아닌 STOMP CONNECT에서 인증 (StompAuthChannelInterceptor)
                        .requestMatchers("/ws/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/**").authenticated()
                        // 조회 외의 모든 요청은 실제 사용자만 — 게스트는 읽기 전용
                        .anyRequest().hasAnyRole("ADMIN", "USER")
                )
                // 인증이 없으면 401. 기본값 403으로는 프론트가 세션 만료를 구분하지 못함
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}
