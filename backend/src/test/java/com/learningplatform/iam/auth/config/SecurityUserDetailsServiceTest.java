package com.learningplatform.iam.auth.config;

import com.learningplatform.iam.auth.mapper.RoleMapper;
import com.learningplatform.iam.user.UserAccountEntity;
import com.learningplatform.iam.user.UserAccountMapper;
import com.learningplatform.iam.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UserDetails 适配规则：角色拼成 ROLE_ 前缀，ACTIVE 账号可用，DISABLED 账号禁用，
 * 用户不存在抛 UsernameNotFoundException。
 */
class SecurityUserDetailsServiceTest {

    private final UserAccountMapper userMapper = mock(UserAccountMapper.class);
    private final RoleMapper roleMapper = mock(RoleMapper.class);
    private final SecurityUserDetailsService service =
            new SecurityUserDetailsService(userMapper, roleMapper);

    private UserAccountEntity activeAccount() {
        UserAccountEntity account = new UserAccountEntity();
        account.setId(1L);
        account.setUsername("alice");
        account.setPassword("$2a$10$hash");
        account.setStatus(UserStatus.ACTIVE);
        return account;
    }

    @Test
    void loadUserByUsername_whenUserExistsAndActive_shouldReturnUserWithRolePrefixes() {
        when(userMapper.findByUsername("alice")).thenReturn(activeAccount());
        when(roleMapper.findByUserId(1L)).thenReturn(List.of("LEARNER", "TEACHER"));

        var user = service.loadUserByUsername("alice");

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(user.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_LEARNER", "ROLE_TEACHER");
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_whenUserHasNoRole_shouldReturnUserWithoutAuthorities() {
        when(userMapper.findByUsername("alice")).thenReturn(activeAccount());
        when(roleMapper.findByUserId(1L)).thenReturn(List.of());

        var user = service.loadUserByUsername("alice");

        assertThat(user.getAuthorities()).isEmpty();
    }

    @Test
    void loadUserByUsername_whenUserDisabled_shouldReturnDisabledUser() {
        UserAccountEntity account = activeAccount();
        account.setStatus(UserStatus.DISABLED);
        when(userMapper.findByUsername("alice")).thenReturn(account);
        when(roleMapper.findByUserId(1L)).thenReturn(List.of("LEARNER"));

        var user = service.loadUserByUsername("alice");

        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_whenUserNotFound_shouldThrowUsernameNotFound() {
        when(userMapper.findByUsername("ghost")).thenReturn(null);

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }
}
