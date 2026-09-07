package com.taskboard.security;

import com.taskboard.domain.User.SystemRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Getter
@AllArgsConstructor
public class CurrentUser {
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
