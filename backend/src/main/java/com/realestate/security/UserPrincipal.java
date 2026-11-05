package com.realestate.security;

import com.realestate.user.Role;
import com.realestate.user.User;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Authenticated identity. Built either from the database (login) or from verified JWT claims
 * (every subsequent request), so authenticated requests never hit the database to resolve the user.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String fullName;
    private final Role role;
    private final String password;
    private final boolean enabled;

    public UserPrincipal(Long id, String email, String fullName, Role role, String password, boolean enabled) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.password = password;
        this.enabled = enabled;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getPassword(), user.isEnabled());
    }

    public static UserPrincipal fromClaims(Long id, String email, String fullName, Role role) {
        return new UserPrincipal(id, email, fullName, role, null, true);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
