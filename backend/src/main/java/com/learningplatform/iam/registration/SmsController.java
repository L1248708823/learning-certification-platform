package com.learningplatform.iam.registration;

import com.learningplatform.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发送注册验证码，对应规格 {@code POST /api/v1/sms/code}。
 * 成功不回验证码本身，开发环境看 MockSmsSender 打出来的日志。
 */
@RestController
@RequestMapping("/api/v1/sms")
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "IAM / 注册", description = "学习者使用手机号验证码注册。")
public class SmsController {

    private final VerificationCodeService verificationCodeService;

    public SmsController(VerificationCodeService verificationCodeService) {
        this.verificationCodeService = verificationCodeService;
    }

    /**
     * 请求发送注册验证码。
     *
     * @param request 含手机号的验证码请求
     * @return 无业务数据的成功响应；验证码本身不会返回
     */
    @PostMapping("/code")
    @Operation(summary = "发送注册验证码", description = "验证码有效期为五分钟；同一手机号六十秒内只能发送一次；响应不返回验证码本身。")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "验证码已发送"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "手机号为空或格式不正确"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "发送间隔未结束，错误码为 2001")
    })
    public ApiResponse<Void> sendCode(@Valid @RequestBody SmsCodeRequest request) {
        verificationCodeService.sendCode(request.phone());
        return ApiResponse.success();
    }
}
