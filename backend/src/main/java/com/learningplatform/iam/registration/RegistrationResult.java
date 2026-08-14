package com.learningplatform.iam.registration;

import io.swagger.v3.oas.annotations.media.Schema;

/** 注册成功后的公开结果，不暴露密码哈希或后续认证票据负责的角色资料。 */
public record RegistrationResult(

        /** 新创建用户的公开标识。 */
        @Schema(description = "新创建用户的公开标识。", example = "1")
        long id,

        /** 注册成功后的登录用户名。 */
        @Schema(description = "注册成功后的登录用户名。", example = "learner_zhang")
        String username,

        /** 已完成验证码校验的绑定手机号。 */
        @Schema(description = "已完成验证码校验的绑定手机号。", example = "13800138000")
        String phone,

        /** 用户当前对外展示名称。 */
        @Schema(description = "用户当前对外展示名称。", example = "learner_zhang")
        String displayName) {
}
