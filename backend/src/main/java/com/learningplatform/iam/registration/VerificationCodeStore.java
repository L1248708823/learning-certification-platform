package com.learningplatform.iam.registration;

import java.time.Duration;

/**
 * 验证码和发送频率的存储边界。
 *
 * <p>生产实现是 Redis，测试实现是内存 Map。业务规则（五分钟、六十秒、先核对再消费）
 * 写在 {@link VerificationCodeService}，这里只负责原子读写。
 * 把存储切出来，是为了规则测试不必起容器。
 */
public interface VerificationCodeStore {

    boolean acquireSendPermit(String phone, Duration ttl);

    void saveCode(String phone, String code, Duration ttl);

    /**
     * 只核对，不删除。
     * 注册写库成功后再 {@link #consumeCode}，避免 Redis 和 MySQL 没有分布式事务时把码提前烧掉。
     */
    boolean matches(String phone, String code);

    boolean consumeCode(String phone, String code);
}
