package com.taskboard.global.security;

import com.taskboard.user.User.SystemRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.List;

/** 인증된 요청의 사용자 (세션에 저장되므로 Serializable) */
@Getter
@AllArgsConstructor
public class CurrentUser implements Serializable {
    private final Long id;
    private final String username;
    private final String role;

    public boolean isGuest() {
        return SystemRole.GUEST.name().equals(role);
    }

    public Authentication toAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                this, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
