package com.learningplatform.iam.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.learningplatform.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * IAM 用户落库模型。其他模块只保存 user_id，不直接引用此模型。
 *
 * <p>和 {@code DictItemEntity} 一样：MP 需要可变 JavaBean，访问器交给 Lombok。
 * 不用 {@code @Data}，尤其这里有 password，{@code toString} 会把哈希打进日志。
 *
 * <p>创建时间、更新时间和乐观锁版本由 {@link BaseEntity} 承载，{@code iam.user} 表存在对应列；
 * {@code version} 由 MyBatis-Plus 乐观锁插件在更新时维护。
 */
@Getter
@Setter
@TableName("iam.`user`")
public class UserAccountEntity extends BaseEntity {

    /** 用户主键，由 MySQL 自增生成。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录时使用的用户名，在 {@code iam.user} 中唯一。 */
    private String username;

    /** BCrypt 密码哈希，不保存、返回或记录明文密码。 */
    private String password;

    /** 用户绑定的中国大陆手机号，在 {@code iam.user} 中唯一。 */
    private String phone;

    /** 可选邮箱地址；首期注册不要求提供。 */
    private String email;

    /** 对外展示名称；首期注册时默认等于用户名。 */
    private String displayName;

    /**
     * 账号状态。持久化编码由 {@link UserStatus} 定义，数据库约束限制为已定义值；新增状态时需要同时
     * 定义迁移规则和鉴权行为。
     */
    private UserStatus status;
}
