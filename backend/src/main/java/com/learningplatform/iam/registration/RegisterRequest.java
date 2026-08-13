package com.learningplatform.iam.registration;

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

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须是六位数字")
        String code,

        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须在 3 到 50 个字符之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 100, message = "密码长度必须在 8 到 100 个字符之间")
        String password) {
}
