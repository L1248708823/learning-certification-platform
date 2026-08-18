package com.learningplatform.iam.registration.port;

/** 生成注册流程使用的一次性验证码。 */
@FunctionalInterface
public interface VerificationCodeGenerator {

    /**
     * 生成一次可发送的六位数字验证码。
     *
     * @return 六位数字字符串
     */
    String generate();
}
