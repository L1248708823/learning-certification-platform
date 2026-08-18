package com.learningplatform.iam.registration.web;

import com.learningplatform.iam.validation.MainlandMobilePhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体，注解校验走参数校验。
 *
 * <p>手机号用中国大陆格式，验证码六位数字，用户名 3-50 字符，密码 8-100 字符。
 * 后续 #24 登录再加密码复杂度。
 */
public record RegisterRequest(

        /** 用于接收验证码和识别注册人的中国大陆手机号。 */
        @Schema(description = "用于接收验证码和识别注册人的中国大陆手机号。", example = "13800138000")
        @NotBlank(message = "手机号不能为空")
        @MainlandMobilePhone
        String phone,

        /** 发送到 {@link #phone()} 的六位数字验证码。 */
        @Schema(description = "发送到手机号的六位数字验证码。", example = "123456")
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须是六位数字")
        String code,

        /** 用户自行选择的登录用户名。 */
        @Schema(description = "用户自行选择的登录用户名，长度为 3 到 50 个字符。", example = "learner_zhang")
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须在 3 到 50 个字符之间")
        String username,

        /** 仅在请求传输中存在的明文密码，服务层会立即哈希，响应和日志均不包含它。 */
        @Schema(description = "明文密码只用于本次注册，服务端会立即哈希，响应和日志均不包含它。",
                example = "Password123!", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 100, message = "密码长度必须在 8 到 100 个字符之间")
        String password) {
}
