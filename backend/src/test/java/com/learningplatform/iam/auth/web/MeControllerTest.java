package com.learningplatform.iam.auth.web;

import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.iam.auth.service.CurrentUserService;
import com.learningplatform.iam.user.UserAccountEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * users/me 接口规则：用户名取自认证上下文，返回最小公开资料；用户不可用时透传业务异常。
 */
class MeControllerTest {

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final MeController controller = new MeController(currentUserService);

    private UserAccountEntity account(String username) {
        UserAccountEntity account = new UserAccountEntity();
        account.setId(1L);
        account.setUsername(username);
        account.setDisplayName(username);
        account.setPhone("13800138000");
        account.setEmail("alice@example.com");
        return account;
    }

    @Test
    void me_whenUserExists_shouldReturnPublicProfile() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(currentUserService.findByUsername("alice")).thenReturn(account("alice"));

        ApiResponse<MeResult> response = controller.me(authentication);

        MeResult result = response.data();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.displayName()).isEqualTo("alice");
        assertThat(result.phone()).isEqualTo("13800138000");
        assertThat(result.email()).isEqualTo("alice@example.com");
    }

    @Test
    void me_whenUserUnavailable_shouldPropagateBusinessException() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("ghost");
        when(currentUserService.findByUsername("ghost"))
                .thenThrow(new BusinessException(com.learningplatform.common.api.ErrorCode.USER_NOT_AVAILABLE));

        assertThatThrownBy(() -> controller.me(authentication))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不可用");
    }
}
