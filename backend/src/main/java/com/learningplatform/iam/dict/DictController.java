package com.learningplatform.iam.dict;

import com.learningplatform.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "IAM / 字典", description = "供客户端读取受控代码和展示标签。")
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
    @Operation(summary = "查询启用字典项", description = "类型不存在时返回 404；类型存在但没有启用项时返回空列表。")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "字典类型不存在，错误码为 1002")
    })
    public ApiResponse<List<DictItemView>> findEnabledItems(
            @Parameter(description = "字典类型代码，例如 COURSE_CATEGORY", example = "COURSE_CATEGORY")
            @PathVariable String typeCode) {
        return ApiResponse.success(dictService.findEnabledItems(typeCode));
    }
}
