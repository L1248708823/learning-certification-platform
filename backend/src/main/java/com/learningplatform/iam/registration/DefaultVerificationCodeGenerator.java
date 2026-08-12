package com.learningplatform.iam.registration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** 生产默认生成随机六位数字，开发环境可通过配置固定验证码。 */
@Component
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class DefaultVerificationCodeGenerator implements VerificationCodeGenerator {

    private final SecureRandom random = new SecureRandom();
    private final String fixedCode;

    public DefaultVerificationCodeGenerator(
            @Value("${app.iam.verification-code.fixed:123456}") String fixedCode) {
        this.fixedCode = fixedCode;
    }

    @Override
    public String generate() {
        if (fixedCode != null && !fixedCode.isBlank()) {
            return fixedCode;
        }
        return "%06d".formatted(random.nextInt(1_000_000));
    }
}
