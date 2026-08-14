package com.learningplatform.common.web;

import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API 契约基线演示控制器。
 *
 * <p>首期-02 为验证「统一响应 + 全局异常 + 参数校验格式化」而建，承载集成测试的
 * 触发入口，同时给后续业务 controller 一个可参考的样板。路径带 {@code _demo}
 * 前缀，避免与真实业务接口混淆；后续票据引入真实业务后可整体删除。
 */
@RestController
@RequestMapping("/api/v1/_demo")
@Hidden
public class ContractDemoController {

    /** 成功响应演示：原样回显请求体。 */
    @PostMapping("/echo")
    public ApiResponse<Object> echo(@RequestBody Object body) {
        return ApiResponse.success(body);
    }

    /**
     * 业务异常演示：type 决定抛哪种通用错误，覆盖 404 / 400 / 409 三种语义。
     */
    @GetMapping("/business-error")
    public ApiResponse<Void> businessError(@RequestParam(defaultValue = "conflict") String type) {
        throw switch (type) {
            case "not-found" -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            case "param" -> new BusinessException(ErrorCode.PARAM_INVALID);
            default -> new BusinessException(ErrorCode.STATE_CONFLICT);
        };
    }

    /** 参数校验演示：name 必填且最长 20，age 必填且 1 到 150。 */
    @PostMapping("/validate")
    public ApiResponse<ContractDemoValidateRequest> validate(@Valid @RequestBody ContractDemoValidateRequest request) {
        return ApiResponse.success(request);
    }

    /** 兜底异常演示：模拟未预期的运行时错误，验证不会泄漏内部细节。 */
    @GetMapping("/internal-error")
    public ApiResponse<Void> internalError() {
        throw new IllegalStateException("模拟的未预期异常，不应出现在响应里");
    }
}
