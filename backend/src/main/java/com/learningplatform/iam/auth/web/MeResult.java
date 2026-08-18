package com.learningplatform.iam.auth.web;

import com.learningplatform.iam.user.UserAccountEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前用户最小公开资料，对应 {@code GET /api/v1/users/me} 的响应。
 *
 * <p>只暴露展示需要的信息，密码哈希、状态等内部字段不回传。
 */
@Schema(description = "当前登录用户的最小公开资料")
public record MeResult(
        @Schema(description = "用户主键", example = "1")
        Long id,

        @Schema(description = "登录用户名", example = "alice")
        String username,

        @Schema(description = "对外展示名称", example = "alice")
        String displayName,

        @Schema(description = "中国大陆手机号", example = "13800138000")
        String phone,

        @Schema(description = "可选邮箱，未填写时为 null", example = "alice@example.com")
        String email) {

    /**
     * 从账号实体构建响应结果。
     *
     * @param account 已查出的用户账号
     * @return 最小公开资料
     */
    public static MeResult from(UserAccountEntity account) {
        return new MeResult(
                account.getId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getPhone(),
                account.getEmail());
    }
}
