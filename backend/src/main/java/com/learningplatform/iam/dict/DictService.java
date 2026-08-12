package com.learningplatform.iam.dict;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

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
