package com.learningplatform.iam.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登出黑名单规则：token 必须带 jti 才能拉黑，黑名单 TTL 取 token 剩余有效期，
 * 已过期 token 无需拉黑，按 jti 判断是否命中。
 */
class TokenBlacklistServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private final TokenBlacklistService service = new TokenBlacklistService(redisTemplate);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    /** 构造一个带 jti 和过期时间的测试 token，issuedAt 固定取过期前一小时以满足 Jwt 校验。 */
    private Jwt jwt(String jti, Instant expiresAt) {
        return new Jwt(
                "header.payload.signature",
                expiresAt.minusSeconds(3600),
                expiresAt,
                Map.of("alg", "RS256"),
                Map.of("jti", jti, "sub", "alice"));
    }

    @Test
    void block_withValidToken_shouldStoreJtiWithRemainingTtl() {
        Instant expiresAt = Instant.now().plusSeconds(300);
        service.block(jwt("jti-1", expiresAt));

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(eq("iam:token:blacklist:jti-1"), eq("1"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue().getSeconds()).isBetween(0L, 300L);
    }

    @Test
    void block_whenTokenMissingJti_shouldThrowIllegalArgument() {
        Jwt noJti = new Jwt(
                "header.payload.signature",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("sub", "alice"));

        assertThatThrownBy(() -> service.block(noJti))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jti");
        verify(valueOps, never()).set(any(), any(), any());
    }

    @Test
    void block_whenTokenAlreadyExpired_shouldSkipBlacklist() {
        Instant expiresAt = Instant.now().minusSeconds(60);
        service.block(jwt("jti-3", expiresAt));

        verify(valueOps, never()).set(any(), any(), any());
    }

    @Test
    void isBlocked_whenKeyExists_shouldReturnTrue() {
        when(redisTemplate.hasKey("iam:token:blacklist:jti-9")).thenReturn(true);

        assertThat(service.isBlocked("jti-9")).isTrue();
    }

    @Test
    void isBlocked_whenKeyAbsent_shouldReturnFalse() {
        when(redisTemplate.hasKey("iam:token:blacklist:jti-9")).thenReturn(false);

        assertThat(service.isBlocked("jti-9")).isFalse();
    }
}
