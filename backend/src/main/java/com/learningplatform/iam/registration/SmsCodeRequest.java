package com.learningplatform.iam.registration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 发送验证码的入参。手机号格式和注册请求共用同一条大陆号段规则。 */
public record SmsCodeRequest(
        /** 接收注册验证码的中国大陆手机号。 */
        @Schema(description = "接收注册验证码的中国大陆手机号。", example = "13800138000")
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone) {
}
