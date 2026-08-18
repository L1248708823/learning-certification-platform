package com.learningplatform.iam.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 资源服务器安全链，用 JWT 保护 {@code /api/v1/**} 下的业务接口。
 *
 * <p>JWT 认证走 {@link JwtDecoderWithBlacklist}，签名校验之外查登出黑名单。
 * 注册、验证码、字典是公共接口，显式放行；其余接口必须携带有效 token。
 * hasRole 权限点由具体接口的授权规则控制（见 users/me 的集成测试场景）。
 */
@Configuration
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class ResourceServerConfig {

    /** 公共访问的业务接口：验证码发送、注册、字典查询，均无需登录。 */
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/sms/code",
            "/api/v1/auth/register",
            "/api/v1/dicts/**"
    };

    @Bean
    @Order(2)
    public SecurityFilterChain resourceServerSecurityFilterChain(
            HttpSecurity http, JwtDecoderWithBlacklist jwtDecoder) throws Exception {
        http
                .securityMatcher("/api/v1/**")
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt((jwt) -> jwt.decoder(jwtDecoder)))
                .csrf((csrf) -> csrf.disable());
        return http.build();
    }
}
