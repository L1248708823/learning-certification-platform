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

    @Select("""
            SELECT id, username, password, phone, email, display_name, status,
                   created_at, updated_at, version
            FROM iam.`user`
            WHERE phone = #{phone}
            """)
    UserAccountEntity findByPhone(String phone);

    @Select("""
            SELECT id, username, password, phone, email, display_name, status,
                   created_at, updated_at, version
            FROM iam.`user`
            WHERE username = #{username}
            """)
    UserAccountEntity findByUsername(String username);

    @Insert("""
            INSERT INTO iam.`user`
                (username, password, phone, email, display_name, status, version)
            VALUES
                (#{username}, #{password}, #{phone}, #{email}, #{displayName}, #{status}, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(UserAccountEntity user);

    @Insert("""
            INSERT INTO iam.user_role (user_id, role_id)
            SELECT #{userId}, id
            FROM iam.role
            WHERE code = #{roleCode}
            """)
    int assignRole(long userId, String roleCode);

}
