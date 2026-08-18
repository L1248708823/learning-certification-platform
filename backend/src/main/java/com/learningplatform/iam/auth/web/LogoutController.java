package com.learningplatform.iam.auth.web;

import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.iam.auth.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登出入口，对应规格 {@code POST /api/v1/auth/logout}。
 *
 * <p>登出的动作是拿当前访问 token 的 jti 写进 Redis 黑名单，之后该 token 再访问
 * 会被 {@code JwtDecoderWithBlacklist} 拒绝，达到登出立即失效。Controller 只做
 * token 解析，业务在 {@link TokenBlacklistService}。
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "认证", description = "登出与当前用户信息。登录走 SAS 标准授权码流程。")
public class LogoutController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenBlacklistService tokenBlacklist;
    private final JwtDecoder jwtDecoder;

    public LogoutController(TokenBlacklistService tokenBlacklist, JwtDecoder jwtDecoder) {
        this.tokenBlacklist = tokenBlacklist;
        this.jwtDecoder = jwtDecoder;
    }

    /**
     * 把当前访问 token 的 jti 写入黑名单并返回成功。
     *
     * @param request 请求，从中读取 Authorization 头
     * @return 登出成功，无业务数据
     */
    @PostMapping("/logout")
    @Operation(summary = "登出", description = "把当前访问 token 加入黑名单，登出后该 token 立即失效。")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登出成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "token 缺失或无效，错误码为 1004")
    })
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = bearerToken(request.getHeader("Authorization"));
        if (token == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少 Bearer token");
        }
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (JwtException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token 无效");
        }
        tokenBlacklist.block(jwt);
        return ApiResponse.success();
    }

    /** 从 Authorization 头解析 Bearer token，非 Bearer 格式返回 null。 */
    private String bearerToken(String authorization) {
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
