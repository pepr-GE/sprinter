package com.sprinter.security;

import com.sprinter.domain.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Implementace {@link UserDetails} pro Spring Security.
 *
 * <p>Obaluje doménovou entitu {@link User} a poskytuje Spring Security
 * potřebné informace pro autentizaci a autorizaci.</p>
 */
public class SprinterUserDetails implements UserDetails {

    private final User user;

    public SprinterUserDetails(User user) {
        this.user = user;
    }

    /** Vrátí zabalený doménový objekt uživatele. */
    public User getUser() {
        return user;
    }

    /** Vrátí ID uživatele. */
    public Long getUserId() {
        return user.getId();
    }

    /** Vrátí celé jméno uživatele. */
    public String getFullName() {
        return user.getFullName();
    }

    /** Vrátí preferované UI téma uživatele. */
    public String getUiTheme() {
        return user.getUiTheme();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Systémová role jako Spring Security autorita (prefix ROLE_)
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getSystemRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
