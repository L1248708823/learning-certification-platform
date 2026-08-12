package com.learningplatform.iam.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Spring Security 用户主体，保存 IAM 用户 ID 供授权和资料接口使用。 */
public final class IamUserPrincipal implements UserDetails {

    private final long userId;
    private final String username;
    private final String password;
    private final String status;
    private final List<String> roleCodes;

    public IamUserPrincipal(
            long userId,
            String username,
            String password,
            String status,
            List<String> roleCodes) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.status = status;
        this.roleCodes = List.copyOf(roleCodes);
    }

    public long getUserId() {
        return userId;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleCodes.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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
        return "ACTIVE".equals(status);
    }
}
