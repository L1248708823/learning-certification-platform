package com.learningplatform.iam.auth.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2 客户端种子初始化。
 *
 * <p>Web 端是没有能力保管 client_secret 的公开客户端（SPA），用 PKCE 强制
 * {@code requireProofKey(true)}，即使授权码被截获也无法换 token，防止降级攻击。
 * 客户端和授权记录都落 {@code oauth2_registered_client} 表，符合规格的 JDBC 存储。
 *
 * <p>幂等：已存在同 clientId 时跳过，重复启动不会产生重复数据。
 * 当前 Web 端登录页归 #22，redirect_uri 先用占位，届时按前端实际地址更新。
 */
@Component
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2ClientSeeder implements ApplicationRunner {

    /** Web 端客户端 ID，前端 #22 按此对接。 */
    public static final String WEB_CLIENT_ID = "learning-web";

    /** 前端回调地址占位，待 #22 前端闭环时最终确认。 */
    private static final String WEB_REDIRECT_URI =
            "http://127.0.0.1:5173/login/oauth2/code/learning-web";

    private final RegisteredClientRepository clientRepository;

    public OAuth2ClientSeeder(RegisteredClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (clientRepository.findByClientId(WEB_CLIENT_ID) != null) {
            return;
        }
        RegisteredClient webClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(WEB_CLIENT_ID)
                .clientName("学习平台 Web 端")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(WEB_REDIRECT_URI)
                .scope("openid")
                .scope("profile")
                .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(30))
                        .build())
                .build();
        clientRepository.save(webClient);
    }
}
