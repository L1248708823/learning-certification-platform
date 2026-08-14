package com.learningplatform.iam.dict;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 字典项查询。排序键和表上 {@code idx_iam_dict_item_type_enabled_sort} 对齐，
 * 列表走覆盖索引，不在内存里再排一次。
 */
@Mapper
public interface DictItemMapper extends BaseMapper<DictItemEntity> {

    /**
     * 查询指定类型的全部启用字典项。
     *
     * @param typeCode 字典类型代码
     * @return 按 {@code sort}、{@code id} 升序排列的字典项
     */
    @Select("""
            SELECT id, type_code, code, label, sort, enabled
            FROM iam.dict_item
            WHERE type_code = #{typeCode} AND enabled = TRUE
            ORDER BY sort ASC, id ASC
            """)
    List<DictItemEntity> findEnabledByTypeCode(String typeCode);

    /**
     * 判断字典类型是否存在，用于区分未知类型和空字典。
     *
     * @param typeCode 字典类型代码
     * @return 匹配类型的数量，当前设计中为 {@code 0} 或 {@code 1}
     */
    @Select("SELECT COUNT(*) FROM iam.dict_type WHERE code = #{typeCode}")
    long countType(String typeCode);
}
