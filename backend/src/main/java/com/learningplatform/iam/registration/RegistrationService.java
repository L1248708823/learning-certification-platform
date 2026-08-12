package com.learningplatform.iam.registration;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.iam.user.UserAccountEntity;
import com.learningplatform.iam.user.UserAccountMapper;
import com.learningplatform.iam.user.UserProfile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public UserProfile register(RegisterRequest request) {
        if (userMapper.findByPhone(request.phone()) != null
                || userMapper.findByUsername(request.username()) != null) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }

        verificationCodeService.verifyAndConsume(request.phone(), request.code());

        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setDisplayName(request.username());
        user.setStatus("ACTIVE");

        try {
            userMapper.insertUser(user);
            userMapper.assignRole(user.getId(), "LEARNER");
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }

        return new UserProfile(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatus(),
                java.util.List.of("LEARNER"));
    }
}
