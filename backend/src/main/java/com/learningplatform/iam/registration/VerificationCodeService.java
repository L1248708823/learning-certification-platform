package com.learningplatform.iam.registration;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 注册验证码规则：五分钟有效，同一手机号六十秒内只能发送一次。
 *
 * <p>{@link #assertValid} 只核对，{@link #consume} 才删除。
 * 两个动作拆开，是因为码在 Redis、用户在 MySQL，没有分布式事务。
 * 注册流程必须先确认码对，写库成功后再消费，失败时用户不用重新发短信。
 */
@Service
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class VerificationCodeService {

    static final Duration CODE_TTL = Duration.ofMinutes(5);
    static final Duration SEND_INTERVAL = Duration.ofSeconds(60);

    private final VerificationCodeStore store;
    private final SmsSender smsSender;
    private final VerificationCodeGenerator generator;

    public VerificationCodeService(
            VerificationCodeStore store,
            SmsSender smsSender,
            VerificationCodeGenerator generator) {
        this.store = store;
        this.smsSender = smsSender;
        this.generator = generator;
    }

    public void sendCode(String phone) {
        if (!store.acquireSendPermit(phone, SEND_INTERVAL)) {
            throw new BusinessException(ErrorCode.SMS_TOO_FREQUENT, "验证码发送过于频繁");
        }

        String code = generator.generate();
        store.saveCode(phone, code, CODE_TTL);
        smsSender.send(phone, code);
    }

    public void assertValid(String phone, String code) {
        if (!store.matches(phone, code)) {
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID, "验证码无效或已过期");
        }
    }

    public void consume(String phone, String code) {
        if (!store.consumeCode(phone, code)) {
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID, "验证码无效或已过期");
        }
    }
}
