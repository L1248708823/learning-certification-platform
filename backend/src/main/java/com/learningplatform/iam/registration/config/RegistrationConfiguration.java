package com.learningplatform.iam.registration.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 注册模块只引入密码哈希，不引入整套 Spring Security 过滤器链。
 * 认证协议、资源保护和 RBAC 由 #24 负责，避免注册票提前长出登录骨架。
 *
 * <p>{@code app.iam.enabled=false} 时这个配置和整包业务 Bean 一起不装配，
 * 给不连库、不连 Redis 的冒烟测试用。开关挂在每个 Bean 上是权宜，不是长期模块边界。
 */
@Configuration
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class RegistrationConfiguration {

    /**
     * 提供 BCrypt 密码哈希器。注册只保存哈希，认证流程在 #24 再复用同一个算法验证密码。
     *
     * @return Spring Security 的密码哈希器
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
