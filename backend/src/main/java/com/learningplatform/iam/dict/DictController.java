package com.learningplatform.iam.dict;

import com.learningplatform.common.api.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典公共查询，对应规格 {@code GET /api/v1/dicts/{typeCode}}。
 * 首期只读，管理界面和写操作后置到第二期。
 */
@RestController
@RequestMapping("/api/v1/dicts")
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    /**
     * 返回一个字典类型下的启用项。
     *
     * @param typeCode 路径中的字典类型代码
     * @return 统一响应壳中的字典项列表
     */
    @GetMapping("/{typeCode}")
    public ApiResponse<List<DictItemView>> findEnabledItems(@PathVariable String typeCode) {
        return ApiResponse.success(dictService.findEnabledItems(typeCode));
    }
}
