package com.learningplatform.iam.auth.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客户端种子初始化规则：已存在同 clientId 时跳过，不存在时才入库，且配置符合
 * 公开客户端 + PKCE 的安全要求（无 secret、强制 proof key）。
 */
class OAuth2ClientSeederTest {

    private final RegisteredClientRepository clientRepository = mock(RegisteredClientRepository.class);
    private final OAuth2ClientSeeder seeder = new OAuth2ClientSeeder(clientRepository);

    @Test
    void run_whenClientAlreadyExists_shouldSkipSaving() {
        when(clientRepository.findByClientId(OAuth2ClientSeeder.WEB_CLIENT_ID))
                .thenReturn(mock(RegisteredClient.class));

        seeder.run(null);

        verify(clientRepository, never()).save(any());
    }

    @Test
    void run_whenClientMissing_shouldSavePublicClientWithPkce() {
        when(clientRepository.findByClientId(OAuth2ClientSeeder.WEB_CLIENT_ID)).thenReturn(null);

        seeder.run(null);

        ArgumentCaptor<RegisteredClient> captor = ArgumentCaptor.forClass(RegisteredClient.class);
        verify(clientRepository).save(captor.capture());
        RegisteredClient saved = captor.getValue();
        assertThat(saved.getClientId()).isEqualTo(OAuth2ClientSeeder.WEB_CLIENT_ID);
        assertThat(saved.getClientAuthenticationMethods())
                .containsExactly(ClientAuthenticationMethod.NONE);
        assertThat(saved.getAuthorizationGrantTypes())
                .containsExactlyInAnyOrder(
                        AuthorizationGrantType.AUTHORIZATION_CODE,
                        AuthorizationGrantType.REFRESH_TOKEN);
        assertThat(saved.getClientSettings().isRequireProofKey()).isTrue();
        assertThat(saved.getTokenSettings().getAccessTokenTimeToLive())
                .isEqualTo(Duration.ofMinutes(30));
    }
}
