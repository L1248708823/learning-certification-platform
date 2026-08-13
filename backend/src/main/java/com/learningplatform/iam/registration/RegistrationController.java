package com.learningplatform.iam.registration;

import com.learningplatform.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册入口，对应规格 {@code POST /api/v1/auth/register}。
 *
 * <p>Controller 只做校验和响应壳，业务规则在 {@link RegistrationService}。
 * 返回 {@link RegistrationResult} 而不是完整用户资料，密码哈希和角色留给 #24 的资料接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ApiResponse<RegistrationResult> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(registrationService.register(request));
    }
}
