package com.learningplatform.iam.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/**
 * IAM 用户表访问。规格说简单 CRUD 走 BaseMapper + LambdaWrapper，
 * 这里仍用手写注解 SQL，是为了让全限定表名 {@code iam.`user`} 和回填主键写在看得见的地方。
 * {@code user} 是 MySQL 保留字，必须反引号。跨 schema join 仍然禁止。
 *
 * <p>{@link #assignRole} 不是简单 CRUD：一次插入要查 role 种子，种子缺失时影响行数是 0，
 * 注册服务据此炸成 500，而不是假装注册成功却没有 LEARNER。
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountEntity> {

    /**
     * 按手机号查询用户，用于注册前查重。
     *
     * @param phone 中国大陆手机号
     * @return 已存在的用户；未注册时返回 {@code null}
     */
    @Select("""
            SELECT id, username, password, phone, email, display_name, status,
                   created_at, updated_at, version
            FROM iam.`user`
            WHERE phone = #{phone}
            """)
    UserAccountEntity findByPhone(String phone);

    /**
     * 按用户名查询用户，用于注册前查重。
     *
     * @param username 登录用户名
     * @return 已存在的用户；未注册时返回 {@code null}
     */
    @Select("""
            SELECT id, username, password, phone, email, display_name, status,
                   created_at, updated_at, version
            FROM iam.`user`
            WHERE username = #{username}
            """)
    UserAccountEntity findByUsername(String username);

    /**
     * 插入用户，并将数据库生成的主键回填到 {@code user.id}。
     *
     * @param user 待保存的用户；其中密码必须已经是哈希值
     * @return 受影响行数，成功时为 {@code 1}
     */
    @Insert("""
            INSERT INTO iam.`user`
                (username, password, phone, email, display_name, status, version)
            VALUES
                (#{username}, #{password}, #{phone}, #{email}, #{displayName}, #{status}, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(UserAccountEntity user);

    /**
     * 为用户授予一个已有的角色代码。
     *
     * @param userId 用户主键
     * @param roleCode {@code iam.role} 中的角色代码
     * @return 受影响行数；角色种子缺失时为 {@code 0}
     */
    @Insert("""
            INSERT INTO iam.user_role (user_id, role_id)
            SELECT #{userId}, id
            FROM iam.role
            WHERE code = #{roleCode}
            """)
    int assignRole(long userId, String roleCode);

}
