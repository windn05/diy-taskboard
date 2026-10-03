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

/**
 * 세션 기반 인증(로그인 성공 시 서버가 세션을 만들고, 브라우저는 세션 쿠키만 들고 다님).
 * 세션은 Redis에 저장(application.yml의 spring.session.*)해 백엔드 재시작에도 유지된다.
 * 역할별 허용 범위는 URL 단위로만 거르고, 프로젝트 멤버 여부 같은 리소스 단위 권한은 각 서비스에서 검사
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 로그인 성공 시 AuthController가 이 저장소로 SecurityContext를 세션에 직접 저장한다 */
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
