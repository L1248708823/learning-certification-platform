package com.learningplatform.iam.registration;

/** 注册成功后的公开结果，不暴露密码哈希或后续认证票据负责的角色资料。 */
public record RegistrationResult(
        long id,
        String username,
        String phone,
        String displayName) {
}
