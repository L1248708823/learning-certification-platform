package com.learningplatform.iam.user;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 用户账号状态。
 *
 * <p>持久化编码由 {@link #getCode()} 返回，并通过 {@code @EnumValue} 写入
 * {@code iam.user.status}。数据库约束与本枚举在同一变更中维护，防止出现未定义状态。
 */
public enum UserStatus {

    /** 账号可以正常登录和使用平台功能。 */
    ACTIVE("ACTIVE"),

    /** 账号被停用，认证流程需要拒绝该账号。 */
    DISABLED("DISABLED");

    /** 数据库存储的稳定状态编码。 */
    @EnumValue
    private final String code;

    /**
     * 创建一个用户账号状态。
     *
     * @param code 数据库存储的状态编码
     */
    UserStatus(String code) {
        this.code = code;
    }

    /**
     * 获取数据库持久化使用的状态编码。
     *
     * @return 稳定状态编码
     */
    public String getCode() {
        return code;
    }
}
