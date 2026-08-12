package com.learningplatform.iam.user;

import java.util.List;

/** 对外返回的用户资料，不包含密码哈希。 */
public record UserProfile(
        long id,
        String username,
        String phone,
        String email,
        String displayName,
        String status,
        List<String> roles) {
}
