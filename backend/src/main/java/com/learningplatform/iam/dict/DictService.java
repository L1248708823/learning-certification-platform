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
 * <p>返回 {@link DictItemView}，不把 {@code enabled} 和内部主键策略泄漏给调用方。
 */
@Service
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class DictService {

    private final DictItemMapper dictItemMapper;

    public DictService(DictItemMapper dictItemMapper) {
        this.dictItemMapper = dictItemMapper;
    }

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
