package com.learningplatform.iam.dict;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 按类型查启用中的字典项。首期真实场景是课程分类 {@code COURSE_CATEGORY}。
 *
 * <p>先数 {@code dict_type} 再查项：类型不存在回 404，类型在但没有启用项回空列表。
 * 这两种对前端不是一回事，前者是码写错了，后者是暂时没得选。
 *
 * <p>返回 {@link DictItemView}，只提供客户端识别和展示所需的字段；{@code enabled} 不暴露给调用方。
 */
@Service
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class DictService {

    private final DictItemMapper dictItemMapper;

    public DictService(DictItemMapper dictItemMapper) {
        this.dictItemMapper = dictItemMapper;
    }

    /**
     * 查询指定类型的启用字典项。
     *
     * @param typeCode 字典类型代码，例如 {@code COURSE_CATEGORY}
     * @return 按展示顺序排列的公开字典项
     * @throws BusinessException 类型不存在时抛出资源不存在错误
     */
    public List<DictItemView> findEnabledItems(String typeCode) {
        if (dictItemMapper.countType(typeCode) == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "字典类型不存在");
        }
        return dictItemMapper.findEnabledByTypeCode(typeCode).stream()
                .map(item -> new DictItemView(
                        item.getId(), item.getCode(), item.getLabel(), item.getSort()))
                .toList();
    }
}
