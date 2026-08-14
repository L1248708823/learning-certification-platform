package com.learningplatform.iam.dict;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 字典项落库模型。MyBatis-Plus 靠无参构造和 setter 填值，所以用可变 class，不用 record。
 *
 * <p>访问器用 Lombok 生成，手写 getter 没有业务。不用 {@code @Data}：
 * 它还会生成 {@code equals}/{@code hashCode}/{@code toString}，
 * 持久化实体按全部字段比相等、把内部字段打进日志，都容易踩坑。
 */
@Getter
@Setter
@TableName("iam.dict_item")
public class DictItemEntity {

    /** 字典项主键，由 MySQL 自增生成。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属字典类型代码，对应 {@code iam.dict_type.code}。 */
    private String typeCode;

    /** 类型内唯一的机器可读代码，例如 {@code BACKEND}。 */
    private String code;

    /** 展示给调用方的中文标签。 */
    private String label;

    /** 同一类型内的升序排列值；相同排序值再按主键排序。 */
    private Integer sort;

    /** 是否对公共查询接口可见；{@code false} 的项不会返回给调用方。 */
    private Boolean enabled;
}
