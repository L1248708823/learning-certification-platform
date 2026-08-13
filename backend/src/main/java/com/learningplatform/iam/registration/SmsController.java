package com.learningplatform.iam.registration;

import com.learningplatform.common.api.ApiResponse;
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
public class SmsController {

    private final VerificationCodeService verificationCodeService;

    public SmsController(VerificationCodeService verificationCodeService) {
        this.verificationCodeService = verificationCodeService;
    }

    @PostMapping("/code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SmsCodeRequest request) {
        verificationCodeService.sendCode(request.phone());
        return ApiResponse.success();
    }
}
