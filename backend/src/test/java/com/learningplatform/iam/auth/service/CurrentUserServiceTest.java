package com.learningplatform.iam.auth.service;

import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.iam.user.UserAccountEntity;
import com.learningplatform.iam.user.UserAccountMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * users/me 查询规则：用户存在时返回账号，不存在时抛业务异常（映射为 401）。
 */
class CurrentUserServiceTest {

    private final UserAccountMapper userMapper = mock(UserAccountMapper.class);
    private final CurrentUserService service = new CurrentUserService(userMapper);

    @Test
    void findByUsername_whenUserExists_shouldReturnAccount() {
        UserAccountEntity account = new UserAccountEntity();
        account.setUsername("alice");
        when(userMapper.findByUsername("alice")).thenReturn(account);

        assertThat(service.findByUsername("alice")).isSameAs(account);
    }

    @Test
    void findByUsername_whenUserMissing_shouldThrowBusinessException() {
        when(userMapper.findByUsername("ghost")).thenReturn(null);

        assertThatThrownBy(() -> service.findByUsername("ghost"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不可用");
    }
}
