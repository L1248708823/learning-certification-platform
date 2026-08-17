package com.learningplatform.iam.registration;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.iam.user.UserAccountEntity;
import com.learningplatform.iam.user.UserAccountMapper;
import com.learningplatform.iam.user.UserStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 手机号验证码注册。登录、Token、RBAC 不在这里，见 #24。
 *
 * <p>验证码住在 Redis，用户住在 MySQL，两边没有分布式事务。
 * {@code @Transactional} 只能回滚 MySQL，回滚不了 Redis。
 * 所以先核对验证码、再写库，写成功才消费。写库失败时码还在，调用方不用重新发短信。
 *
 * <p>查重放在验码之后：没有有效验证码，接口不会告诉你这个用户名或手机号是否已经注册。
 * 并发下两个请求都过了查重，靠 {@code iam.user} 的唯一约束收口，
 * {@link DataIntegrityViolationException} 转成 {@link ErrorCode#USER_EXISTS}。
 *
 * <p>{@code app.iam.enabled=false} 时整包不装配，给不连库的冒烟测试用。
 */
@Service
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class RegistrationService {

    private final UserAccountMapper userMapper;
    private final VerificationCodeService verificationCodeService;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserAccountMapper userMapper,
            VerificationCodeService verificationCodeService,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.verificationCodeService = verificationCodeService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用有效手机号验证码创建学习者账号，并授予 {@code LEARNER} 角色。
     *
     * <p>数据库写入成功前验证码保持可用，避免 MySQL 回滚后用户失去重试凭据。
     *
     * @param request 已通过 Web 参数校验的注册请求
     * @return 新用户的最小公开资料
     * @throws BusinessException 验证码无效或用户名、手机号已存在时抛出业务错误
     */
    @Transactional
    public RegistrationResult register(RegisterRequest request) {
        verificationCodeService.assertValid(request.phone(), request.code());

        if (userMapper.findByPhone(request.phone()) != null
                || userMapper.findByUsername(request.username()) != null) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }

        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setDisplayName(request.username());
        user.setStatus(UserStatus.ACTIVE);

        try {
            userMapper.insertUser(user);
            if (userMapper.assignRole(user.getId(), "LEARNER") != 1) {
                throw new IllegalStateException("LEARNER 角色种子不存在");
            }
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }

        verificationCodeService.consume(request.phone(), request.code());
        return new RegistrationResult(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getDisplayName());
    }
}
