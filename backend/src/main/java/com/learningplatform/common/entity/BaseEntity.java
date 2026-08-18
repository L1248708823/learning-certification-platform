package com.learningplatform.common.entity;

import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 审计字段基类：需要记录创建时间、更新时间和并发版本的持久化实体继承它。
 *
 * <p>{@code createdAt}/{@code updatedAt} 的值由数据库默认值写入（{@code DEFAULT CURRENT_TIMESTAMP}），
 * 本基类不引入 MetaObjectHandler 自动填充，值的归属仍归数据库。{@code version} 由 MyBatis-Plus
 * 乐观锁插件在更新时维护，见 {@code MybatisPlusConfig}。
 *
 * <p>只有表里真实存在这三列时才继承本类。例如 {@code iam.dict_item} 没有审计列，
 * {@code DictItemEntity} 就保持独立，避免 MP 生成引用不存在列的 SQL。
 */
@Getter
@Setter
public abstract class BaseEntity {

    /** 记录创建时间，由数据库默认值写入。 */
    private LocalDateTime createdAt;

    /** 记录最近更新时间，由数据库在更新行时自动维护。 */
    private LocalDateTime updatedAt;

    /**
     * 记录版本号，MyBatis-Plus 乐观锁字段。更新时插件自动追加
     * {@code WHERE version = 旧值} 并把版本号加一；条件不成立表示存在并发修改，更新 0 行。
     */
    @Version
    private Long version;
}
