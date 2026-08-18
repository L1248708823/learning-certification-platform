package com.learningplatform.iam.auth.config;

import com.learningplatform.iam.auth.mapper.RoleMapper;
import com.learningplatform.iam.user.UserAccountEntity;
import com.learningplatform.iam.user.UserAccountMapper;
import com.learningplatform.iam.user.UserStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 把 {@code iam.user} 表适配成 Spring Security 的 {@link UserDetailsService}。
 *
 * <p>SAS 授权码流程的 formLogin 提交用户名密码时调用这里，查库、校验 BCrypt 哈希。
 * 角色从 {@code iam.user_role} 读出后拼成 {@code ROLE_} 前缀，供 OAuth2TokenCustomizer
 * 写进 JWT 的 roles claim，hasRole 权限判断依赖它。
 *
 * <p>账号状态为 {@link UserStatus#DISABLED} 时返回禁用用户，Spring Security 登录直接拒绝。
 */
@Service
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityUserDetailsService implements UserDetailsService {

    private final UserAccountMapper userMapper;
    private final RoleMapper roleMapper;

    public SecurityUserDetailsService(UserAccountMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccountEntity account = userMapper.findByUsername(username);
        if (account == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        List<GrantedAuthority> authorities = roleMapper.findByUserId(account.getId()).stream()
                .map(roleCode -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + roleCode))
                .toList();
        boolean active = account.getStatus() == UserStatus.ACTIVE;
        return User.withUsername(account.getUsername())
                .password(account.getPassword())
                .authorities(authorities)
                .disabled(!active)
                .build();
    }
}
