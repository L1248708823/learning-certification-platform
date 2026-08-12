package com.learningplatform.iam.registration;

/** 生成注册流程使用的一次性验证码。 */
@FunctionalInterface
public interface VerificationCodeGenerator {

    String generate();
}
