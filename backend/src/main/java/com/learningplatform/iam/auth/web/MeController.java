package com.learningplatform.iam.auth.web;

import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.iam.auth.service.CurrentUserService;
import com.learningplatform.iam.user.UserAccountEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户信息入口，对应规格 {@code GET /api/v1/users/me}。
 *
 * <p>用户名来自 JWT 的 sub claim（登录时写入），由 Spring Security 认证上下文携带，
 * Controller 通过方法参数注入 {@link Authentication} 取用，不手动解析 token。
 */
@RestController
@RequestMapping("/api/v1/users")
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "认证", description = "登出与当前用户信息。登录走 SAS 标准授权码流程。")
public class MeController {

    private final CurrentUserService currentUserService;

    public MeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    /**
     * 返回当前登录用户的最小公开资料。
     *
     * @param authentication 当前认证上下文，用户名为 JWT sub
     * @return 当前用户的最小公开资料
     */
    @GetMapping("/me")
    @Operation(summary = "当前用户信息", description = "返回当前登录用户的最小公开资料，需要有效 token。")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "返回用户资料"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "token 缺失或用户不可用，错误码为 1004 或 2004")
    })
    public ApiResponse<MeResult> me(Authentication authentication) {
        UserAccountEntity account = currentUserService.findByUsername(authentication.getName());
        return ApiResponse.success(MeResult.from(account));
    }
}
