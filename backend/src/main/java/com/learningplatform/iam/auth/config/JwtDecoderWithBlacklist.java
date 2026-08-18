package com.learningplatform.iam.auth.config;

import com.learningplatform.iam.auth.service.TokenBlacklistService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 资源服务器用的 JWT 解码器，在标准签名与过期校验之外查登出黑名单。
 *
 * <p>委托给授权服务器的 {@link JwtDecoder}（同一 JWKS 校验签名），解码成功后
 * 拿 jti 查 Redis 黑名单，命中说明该 token 已登出，抛校验异常，由资源服务器
 * 转成 401。这样登出的 token 立即失效，不依赖 JWT 无状态签名的局限。
 */
@Component
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class JwtDecoderWithBlacklist implements JwtDecoder {

    private final JwtDecoder delegate;
    private final TokenBlacklistService tokenBlacklist;

    public JwtDecoderWithBlacklist(JwtDecoder delegate, TokenBlacklistService tokenBlacklist) {
        this.delegate = delegate;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = delegate.decode(token);
        String jti = jwt.getId();
        if (jti != null && tokenBlacklist.isBlocked(jti)) {
            throw new JwtValidationException(
                    "token 已被登出拉黑",
                    List.of(new OAuth2Error("token_revoked", "token has been logged out", null)));
        }
        return jwt;
    }
}
