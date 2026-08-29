package com.smartshop.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String password;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;
    private final List<BranchRoleGrant> branchRoleGrants;

    public UserPrincipal(UUID userId, String email, String password, boolean active,
                         Collection<? extends GrantedAuthority> authorities,
                         List<BranchRoleGrant> branchRoleGrants) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.active = active;
        this.authorities = authorities;
        this.branchRoleGrants = branchRoleGrants == null ? List.of() : branchRoleGrants;
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

    public boolean hasRole(String roleName) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + roleName));
    }

    public static UserPrincipal of(UUID userId, String email, String password, boolean active,
                                   List<String> roleNames, List<BranchRoleGrant> grants) {
        List<GrantedAuthority> authorities = roleNames.stream()
                .distinct()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .map(GrantedAuthority.class::cast)
                .toList();
        return new UserPrincipal(userId, email, password, active, authorities, grants);
    }
}