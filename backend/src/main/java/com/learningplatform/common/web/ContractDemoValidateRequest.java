package com.learningplatform.common.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 契约演示用的校验 DTO，演示参数校验错误如何被格式化。
 *
 * <p>只服务于 ContractDemoController 的校验演示，业务请求 DTO 由后续模块票据
 * 各自定义，规则一样：record + jakarta.validation 注解，message 给前端能看懂的文案。
 */
public record ContractDemoValidateRequest(

        @NotBlank(message = "名称不能为空")
        @Size(max = 20, message = "名称最长 20 个字符")
        String name,

        @NotNull(message = "年龄不能为空")
        @Min(value = 1, message = "年龄最小 1")
        @Max(value = 150, message = "年龄最大 150")
        Integer age
) {
}
