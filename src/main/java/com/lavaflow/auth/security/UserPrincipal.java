package com.lavaflow.auth.security;

import com.lavaflow.auth.entity.User;
import com.lavaflow.common.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;


@Getter
public class UserPrincipal implements UserDetails {

    private final User user;
    private final UUID userId;
    private final UserRole role;
    private final UUID restaurantId;

    public UserPrincipal(User user) {
        this.user         = user;
        this.userId       = user.getId();
        this.role         = user.getRole();
        this.restaurantId = user.getRestaurant() != null ? user.getRestaurant().getId() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return Collections.emptyList();
        }
        java.util.List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        if (role == UserRole.RESTAURANT_OWNER) {
            authorities.add(new SimpleGrantedAuthority("ROLE_RESTAURANT_ADMIN"));
        } else if (role == UserRole.OUTLET_MANAGER) {
            authorities.add(new SimpleGrantedAuthority("ROLE_RESTAURANT_STAFF"));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return user != null && user.isActive();
    }
}
