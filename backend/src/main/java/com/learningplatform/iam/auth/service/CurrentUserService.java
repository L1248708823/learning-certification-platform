package com.learningplatform.iam.auth.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.iam.user.UserAccountEntity;
import com.learningplatform.iam.user.UserAccountMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 当前登录用户查询。
 *
 * <p>users/me 的业务只有一个动作：按 JWT 里的用户名查用户资料。用户不存在时
 * 抛 {@link BusinessException}，由全局异常处理器映射成 401。
 */
@Service
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class CurrentUserService {

    private final UserAccountMapper userMapper;

    public CurrentUserService(UserAccountMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 按用户名查询用户资料。
     *
     * @param username 登录用户名，即 JWT 的 sub claim
     * @return 用户账号实体
     * @throws BusinessException 用户不存在时抛 {@link ErrorCode#USER_NOT_AVAILABLE}
     */
    public UserAccountEntity findByUsername(String username) {
        UserAccountEntity account = userMapper.findByUsername(username);
        if (account == null) {
            throw new BusinessException(ErrorCode.USER_NOT_AVAILABLE);
        }
        return account;
    }
}
