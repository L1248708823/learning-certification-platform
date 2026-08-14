package com.learningplatform.iam.registration;

/** 注册成功后的公开结果，不暴露密码哈希或后续认证票据负责的角色资料。 */
public record RegistrationResult(

        /** 新创建用户的公开标识。 */
        long id,

        /** 注册成功后的登录用户名。 */
        String username,

        /** 已完成验证码校验的绑定手机号。 */
        String phone,

        /** 用户当前对外展示名称。 */
        String displayName) {
}
