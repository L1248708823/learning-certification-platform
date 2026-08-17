package com.learningplatform.iam.registration.application;

/**
 * 注册学习者用例的输入。
 *
 * <p>调用者需要先完成空值、长度和格式校验。本类型不带 HTTP 或 OpenAPI 注解，
 * 因此注册规则可以被其他入口复用。
 */
public record RegisterLearnerCommand(

        /** 接收验证码并绑定到新用户的中国大陆手机号。 */
        String phone,

        /** 发送到手机号的六位数字验证码。 */
        String code,

        /** 新用户选择的登录用户名。 */
        String username,

        /** 仅在注册用例执行期间存在的明文密码。 */
        String password) {
}
