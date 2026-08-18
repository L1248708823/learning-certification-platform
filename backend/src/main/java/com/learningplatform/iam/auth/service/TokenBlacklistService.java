package com.learningplatform.iam.auth.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * 登出黑名单服务。
 *
 * <p>JWT 是无状态的，令牌撤销没有服务端状态可查，登出必须主动把 jti 写进 Redis。
 * 资源服务器解码 token 时先查黑名单（见 {@code JwtDecoderWithBlacklist}），
 * 命中的一律按失效处理，达到登出立即失效的效果。
 *
 * <p>黑名单键的 TTL 取 token 剩余有效期。token 过期后本身已失效，键无需保留，
 * 由 Redis 自动清理，不会无限堆积。
 */
@Service
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class TokenBlacklistService {

    /** 登出黑名单键的固定前缀，完整键为前缀加 JWT 的 jti。 */
    private static final String BLACKLIST_KEY_PREFIX = "iam:token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 把 JWT 加入黑名单，登出后该 token 立即失效。
     *
     * <p>token 已过期或即将过期时直接跳过：过期 token 本就无法通过签名校验，无需占用黑名单空间。
     *
     * @param jwt 待撤销的访问令牌
     * @throws IllegalArgumentException token 缺少 jti 或过期时间时抛出
     */
    public void block(Jwt jwt) {
        String jti = jwt.getId();
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("token 缺少 jti，无法加入黑名单");
        }
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            throw new IllegalArgumentException("token 缺少过期时间，无法计算黑名单保留时长");
        }
        long remainingSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (remainingSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(
                BLACKLIST_KEY_PREFIX + jti, "1", Duration.ofSeconds(remainingSeconds));
    }

    /**
     * 判断 jti 是否已被登出拉黑。
     *
     * @param jti JWT 的 jti claim
     * @return 黑名单命中返回 {@code true}
     */
    public boolean isBlocked(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
    }
}
