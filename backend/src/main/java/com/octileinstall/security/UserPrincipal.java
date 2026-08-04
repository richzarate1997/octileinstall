package com.octileinstall.security;

import com.octileinstall.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The authenticated principal placed in the {@link org.springframework.security.core.context.SecurityContext}.
 *
 * <p>Controllers can inject this with {@code @AuthenticationPrincipal UserPrincipal principal}
 * to get the acting user's id without another database round trip.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String fullName;
    private final User.Role role;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.role = user.getRole();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public User.Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring's hasRole("ADMIN") checks for the "ROLE_ADMIN" authority, hence the prefix.
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Always {@code null}. The credential was verified at login; nothing downstream of the
     * JWT filter should ever need the password hash, so it is deliberately not carried here.
     */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
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
        return true;
    }
}
