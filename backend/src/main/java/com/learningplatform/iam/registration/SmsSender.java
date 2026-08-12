package com.learningplatform.iam.registration;

/** 短信发送边界，首期使用 Mock 实现。 */
@FunctionalInterface
public interface SmsSender {

    void send(String phone, String code);
}
