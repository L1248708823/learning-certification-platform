package com.learningplatform.iam.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * IAM 用户角色查询。
 *
 * <p>轻量 RBAC 的角色数据来自 {@code iam.role} 与 {@code iam.user_role} 两张表，
 * 登录和权限判断（hasRole）都从这里取角色。只读查询，角色维护不在首期范围。
 */
@Mapper
public interface RoleMapper {

    /**
     * 查询用户已授予的角色代码。
     *
     * @param userId 用户主键
     * @return 角色代码列表；用户没有任何角色时返回空列表
     */
    @Select("""
            SELECT r.code
            FROM iam.user_role ur
            JOIN iam.role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<String> findByUserId(long userId);
}
