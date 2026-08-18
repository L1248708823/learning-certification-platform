package com.learningplatform.iam.registration.port;

/** 短信发送边界，首期使用 Mock 实现。 */
@FunctionalInterface
public interface SmsSender {

    /**
     * 向指定手机号发送验证码。
     *
     * @param phone 接收验证码的手机号
     * @param code 待发送的六位数字验证码
     */
    void send(String phone, String code);
}
