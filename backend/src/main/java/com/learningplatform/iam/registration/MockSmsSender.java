package com.learningplatform.iam.registration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 开发环境短信适配器，只把验证码写入日志，不连接真实短信服务。 */
@Component
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public void send(String phone, String code) {
        log.info("Mock 注册验证码已发送 phone={}, code={}", phone, code);
    }
}
