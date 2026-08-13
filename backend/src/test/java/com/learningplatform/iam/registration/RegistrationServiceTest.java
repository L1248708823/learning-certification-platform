package com.learningplatform.iam.registration;

import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.iam.user.UserAccountEntity;
import com.learningplatform.iam.user.UserAccountMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 注册规则：验证码在 Redis，用户在 MySQL，两边没有分布式事务。
 * 写库失败时码必须还能用，调用方不必重新发短信。
 */
class RegistrationServiceTest {

    private final InMemoryVerificationCodeStore store = new InMemoryVerificationCodeStore();
    private final UserAccountMapper userMapper = mock(UserAccountMapper.class);
    private final VerificationCodeService codes = new VerificationCodeService(
            store,
            (phone, code) -> {
            },
            () -> "123456");
    private final RegistrationService service = new RegistrationService(
            userMapper,
            codes,
            new BCryptPasswordEncoder());

    @Test
    void register_whenInsertHitsUniqueConstraint_shouldKeepCodeUsable() {
        codes.sendCode("13800138000");
        when(userMapper.findByPhone(any())).thenReturn(null);
        when(userMapper.findByUsername(any())).thenReturn(null);
        when(userMapper.insertUser(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "13800138000", "123456", "alice", "Password123!")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户已存在");

        codes.consume("13800138000", "123456");
    }

    @Test
    void register_withoutValidCode_shouldNotRevealExistingUsername() {
        when(userMapper.findByUsername("alice")).thenReturn(new UserAccountEntity());

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "13800138000", "000000", "alice", "Password123!")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码无效或已过期");
    }
}
