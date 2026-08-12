package com.learningplatform.iam.registration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** 使用 Redis 的验证码存储，验证码消费通过 Lua 保证校验和删除的原子性。 */
@Component
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class RedisVerificationCodeStore implements VerificationCodeStore {

    private static final String CODE_KEY_PREFIX = "iam:sms:registration:code:";
    private static final String PERMIT_KEY_PREFIX = "iam:sms:registration:permit:";
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisVerificationCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean acquireSendPermit(String phone, Duration ttl) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(PERMIT_KEY_PREFIX + phone, "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void saveCode(String phone, String code, Duration ttl) {
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + phone, code, ttl);
    }

    @Override
    public boolean consumeCode(String phone, String code) {
        Long result = redisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(CODE_KEY_PREFIX + phone),
                code);
        return Long.valueOf(1L).equals(result);
    }
}
