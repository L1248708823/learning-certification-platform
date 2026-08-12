package com.learningplatform.iam.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
    int insertUser(UserAccountEntity user);

    @Insert("""
            INSERT INTO iam.user_role (user_id, role_id)
            SELECT #{userId}, id
            FROM iam.role
            WHERE code = #{roleCode}
            """)
    int assignRole(long userId, String roleCode);

    @Select("""
            SELECT r.code
            FROM iam.role r
            INNER JOIN iam.user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.id
            """)
    List<String> findRoleCodes(long userId);
}
