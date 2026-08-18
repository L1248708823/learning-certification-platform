package com.learningplatform.iam.auth.web;

import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.iam.auth.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登出接口规则：Bearer token 解码后写黑名单，缺失或无效 token 抛 401 业务异常。
 */
class LogoutControllerTest {

    private final TokenBlacklistService tokenBlacklist = mock(TokenBlacklistService.class);
    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    private final LogoutController controller = new LogoutController(tokenBlacklist, jwtDecoder);

    private final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("jti", "jti-1")
            .subject("alice")
            .issuedAt(Instant.parse("2026-08-18T00:00:00Z"))
            .expiresAt(Instant.parse("2026-08-18T00:30:00Z"))
            .build();

    private MockHttpServletRequest bearerRequest(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }

    @Test
    void logout_whenValidBearerToken_shouldBlacklistJtiAndReturnSuccess() {
        when(jwtDecoder.decode("token")).thenReturn(jwt);

        ApiResponse<Void> response = controller.logout(bearerRequest("Bearer token"));

        assertThat(response.code()).isZero();
        verify(tokenBlacklist).block(jwt);
    }

    @Test
    void logout_whenAuthorizationHeaderMissing_shouldThrowUnauthorized() {
        assertThatThrownBy(() -> controller.logout(new MockHttpServletRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("缺少 Bearer token");
    }

    @Test
    void logout_whenNotBearerScheme_shouldThrowUnauthorized() {
        assertThatThrownBy(() -> controller.logout(bearerRequest("Basic dXNlcjpwYXNz")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("缺少 Bearer token");
    }

    @Test
    void logout_whenTokenInvalid_shouldThrowUnauthorized() {
        when(jwtDecoder.decode("token")).thenThrow(new BadJwtException("签名校验失败"));

        assertThatThrownBy(() -> controller.logout(bearerRequest("Bearer token")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("token 无效");
    }
}
