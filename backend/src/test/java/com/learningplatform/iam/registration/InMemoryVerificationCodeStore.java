package com.learningplatform.iam.registration;

import com.learningplatform.iam.registration.port.VerificationCodeStore;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 验证码存储的内存替身，只给单元测试用。
 * 它站在 Redis 这条系统边界上，让规则测试不必起容器。
 */
final class InMemoryVerificationCodeStore implements VerificationCodeStore {

    final Map<String, String> savedCodes = new HashMap<>();
    Duration savedCodeTtl;
    private final Map<String, Boolean> permits = new HashMap<>();

    @Override
    public boolean acquireSendPermit(String phone, Duration ttl) {
        if (permits.containsKey(phone)) {
            return false;
        }
        permits.put(phone, true);
        return true;
    }

    @Override
    public void saveCode(String phone, String code, Duration ttl) {
        savedCodes.put(phone, code);
        savedCodeTtl = ttl;
    }

    @Override
    public boolean matches(String phone, String code) {
        return code.equals(savedCodes.get(phone));
    }

    @Override
    public boolean consumeCode(String phone, String code) {
        if (!code.equals(savedCodes.get(phone))) {
            return false;
        }
        savedCodes.remove(phone);
        return true;
    }
}
