package com.learningplatform.iam.registration.application;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.iam.registration.port.SmsSender;
import com.learningplatform.iam.registration.port.VerificationCodeGenerator;
import com.learningplatform.iam.registration.port.VerificationCodeStore;
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

    /** 单个验证码的有效期。 */
    static final Duration CODE_TTL = Duration.ofMinutes(5);

    /** 同一手机号两次发送验证码之间的最短间隔。 */
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

    /**
     * 生成并发送注册验证码，同时限制同一手机号的发送频率。
     *
     * @param phone 接收验证码的手机号
     * @throws BusinessException 仍在发送间隔内时抛出频率限制错误
     */
    public void sendCode(String phone) {
        if (!store.acquireSendPermit(phone, SEND_INTERVAL)) {
            throw new BusinessException(ErrorCode.SMS_TOO_FREQUENT, "验证码发送过于频繁");
        }

        String code = generator.generate();
        store.saveCode(phone, code, CODE_TTL);
        smsSender.send(phone, code);
    }

    /**
     * 核对验证码有效性，不改变 Redis 中的验证码。
     *
     * @param phone 验证码所属手机号
     * @param code 待核对的验证码
     * @throws BusinessException 验证码错误或过期时抛出业务错误
     */
    public void assertValid(String phone, String code) {
        if (!store.matches(phone, code)) {
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID, "验证码无效或已过期");
        }
    }

    /**
     * 核对并消费验证码，防止同一验证码重复注册。
     *
     * @param phone 验证码所属手机号
     * @param code 待消费的验证码
     * @throws BusinessException 验证码错误、过期或已消费时抛出业务错误
     */
    public void consume(String phone, String code) {
        if (!store.consumeCode(phone, code)) {
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID, "验证码无效或已过期");
        }
    }
}
