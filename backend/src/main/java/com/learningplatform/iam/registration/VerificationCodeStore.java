package com.learningplatform.iam.registration;

import java.time.Duration;

/** 验证码和发送频率状态的存储边界。 */
public interface VerificationCodeStore {

    boolean acquireSendPermit(String phone, Duration ttl);

    void saveCode(String phone, String code, Duration ttl);

    boolean consumeCode(String phone, String code);
}
