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

    /**
     * 尝试取得一次发送许可。
     *
     * @param phone 请求发送验证码的手机号
     * @param ttl 许可有效期，即同一手机号的最短重发间隔
     * @return 本次取得许可时为 {@code true}；已有未过期许可时为 {@code false}
     */
    boolean acquireSendPermit(String phone, Duration ttl);

    /**
     * 保存验证码并设置有效期。
     *
     * @param phone 验证码所属手机号
     * @param code 六位数字验证码
     * @param ttl 验证码有效期
     */
    void saveCode(String phone, String code, Duration ttl);

    /**
     * 只核对，不删除。
     * 注册写库成功后再 {@link #consumeCode}，避免 Redis 和 MySQL 没有分布式事务时把码提前烧掉。
     */
    boolean matches(String phone, String code);

    /**
     * 原子核对并消费验证码，成功后该验证码不能再次使用。
     *
     * @param phone 验证码所属手机号
     * @param code 待消费的验证码
     * @return 码匹配且已成功删除时为 {@code true}
     */
    boolean consumeCode(String phone, String code);
}
