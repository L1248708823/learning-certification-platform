package com.learningplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 学习平台后端入口。
 *
 * <p>模块化单体的三个业务模块（IAM / Content / Learning）按包分层放在
 * com.learningplatform 下，后续票据各自引入：
 * com.learningplatform.iam、com.learningplatform.content、com.learningplatform.learning。
 */
@SpringBootApplication
public class LearningPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningPlatformApplication.class, args);
    }
}
