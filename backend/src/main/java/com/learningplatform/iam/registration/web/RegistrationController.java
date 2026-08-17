package com.learningplatform.iam.registration.web;

import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.iam.registration.application.RegisterLearnerCommand;
import com.learningplatform.iam.registration.application.RegistrationService;
import com.learningplatform.iam.registration.application.RegisteredUser;
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
 * 注册入口，对应规格 {@code POST /api/v1/auth/register}。
 *
 * <p>Controller 只做校验和响应壳，业务规则在 {@link RegistrationService}。
 * 返回 {@link RegistrationResult} 而不是完整用户资料，密码哈希和角色留给 #24 的资料接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "注册", description = "学习者使用手机号验证码注册。")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * 校验注册请求并创建学习者用户。
     *
     * @param request 手机号验证码注册请求
     * @return 新建用户的最小公开资料
     */
    @PostMapping("/register")
    @Operation(summary = "注册学习者账号", description = "验证码校验通过后创建学习者账号。密码只在请求中使用，成功响应不会返回密码或密码哈希。")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "账号创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数不合法，或验证码无效、过期，错误码为 1001 或 2002"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "手机号或用户名已注册，错误码为 2003")
    })
    public ApiResponse<RegistrationResult> register(@Valid @RequestBody RegisterRequest request) {
        RegisteredUser registeredUser = registrationService.register(new RegisterLearnerCommand(
                request.phone(),
                request.code(),
                request.username(),
                request.password()));
        return ApiResponse.success(RegistrationResult.from(registeredUser));
    }
}
