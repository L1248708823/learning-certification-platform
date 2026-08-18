package com.learningplatform.iam.auth.config;

import com.learningplatform.iam.auth.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 黑名单 JWT 解码规则：签名校验通过后查登出黑名单，命中抛校验异常，未命中原样返回。
 */
class JwtDecoderWithBlacklistTest {

    private final JwtDecoder delegate = mock(JwtDecoder.class);
    private final TokenBlacklistService tokenBlacklist = mock(TokenBlacklistService.class);
    private final JwtDecoderWithBlacklist decoder = new JwtDecoderWithBlacklist(delegate, tokenBlacklist);

    private final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("jti", "jti-1")
            .subject("alice")
            .issuedAt(Instant.parse("2026-08-18T00:00:00Z"))
            .expiresAt(Instant.parse("2026-08-18T00:30:00Z"))
            .build();

    @Test
    void decode_whenJtiNotBlacklisted_shouldReturnOriginalToken() {
        when(delegate.decode("token")).thenReturn(jwt);
        when(tokenBlacklist.isBlocked("jti-1")).thenReturn(false);

        Jwt result = decoder.decode("token");

        assertThat(result).isSameAs(jwt);
        verify(tokenBlacklist).isBlocked("jti-1");
    }

    @Test
    void decode_whenJtiBlacklisted_shouldThrowValidationException() {
        when(delegate.decode("token")).thenReturn(jwt);
        when(tokenBlacklist.isBlocked("jti-1")).thenReturn(true);

        assertThatThrownBy(() -> decoder.decode("token"))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("登出");
    }

    @Test
    void decode_whenTokenWithoutJti_shouldNotQueryBlacklist() {
        Jwt noJti = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("alice")
                .issuedAt(Instant.parse("2026-08-18T00:00:00Z"))
                .expiresAt(Instant.parse("2026-08-18T00:30:00Z"))
                .build();
        when(delegate.decode("token")).thenReturn(noJti);

        Jwt result = decoder.decode("token");

        assertThat(result).isSameAs(noJti);
        verify(tokenBlacklist, never()).isBlocked(anyString());
    }
}
