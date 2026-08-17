package com.learningplatform.iam.registration.application;

/**
 * 注册学习者用例成功后的用户公开资料。
 *
 * <p>不包含密码、密码哈希、验证码或认证票据，避免这些信息从应用层流向调用方。
 */
public record RegisteredUser(

        /** 新创建用户的公开标识。 */
        long id,

        /** 注册成功后的登录用户名。 */
        String username,

        /** 已完成验证码校验的绑定手机号。 */
        String phone,

        /** 用户当前对外展示名称。 */
        String displayName) {
}
