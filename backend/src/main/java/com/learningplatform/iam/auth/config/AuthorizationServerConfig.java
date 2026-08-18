package com.learningplatform.iam.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spring Authorization Server 授权服务器装配（#24）。
 *
 * <p>授权码 + PKCE 标准流程：{@code /oauth2/authorize} 发起授权，formLogin 登录，
 * {@code /oauth2/token} 用 code 换 JWT。JWT 用 RSA 密钥签名，公钥通过
 * {@code /oauth2/jwks} 暴露，资源服务器用同一 JWKS 校验。
 *
 * <p>客户端和授权记录走 JDBC 存储（表在 V2 迁移建好），启动时由
 * {@link OAuth2ClientSeeder} 幂等写入客户端种子。
 *
 * <p>RSA 密钥每次启动随机生成，注释见 {@link #jwkSource()}。
 *
 * <p>本类受 {@code app.iam.enabled} 控制：关闭时完全不装配 Spring Security，
 * 现有公开接口保持不保护的状态，供不连库的冒烟测试使用。
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class AuthorizationServerConfig {

    /**
     * 授权服务器安全链，只处理 SAS 的协议端点。
     *
     * <p>{@code securityMatcher} 限定在 /oauth2/** 等 SAS 端点，避免拦截业务接口。
     * 未登录的浏览器请求重定向到 formLogin 登录页。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationService authorizationService,
            OAuth2AuthorizationConsentService authorizationConsentService,
            AuthorizationServerSettings authorizationServerSettings,
            OAuth2TokenGenerator tokenGenerator) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer
                                .authorizationService(authorizationService)
                                .authorizationConsentService(authorizationConsentService)
                                .authorizationServerSettings(authorizationServerSettings)
                                .tokenGenerator(tokenGenerator))
                .authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .csrf((csrf) -> csrf.ignoringRequestMatchers(
                        authorizationServerConfigurer.getEndpointsMatcher()));
        return http.build();
    }

    /** 接口文档与健康检查端点，不属于业务接口，默认链直接放行。 */
    private static final String[] INFRA_PATHS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/doc.html",
            "/actuator/health",
            "/actuator/info"
    };

    /**
     * 默认安全链，处理登录页和授权服务器链之外的请求。
     *
     * <p>提供 formLogin 默认登录页，用户输入用户名密码后由
     * {@link SecurityUserDetailsService} 校验并加载角色。
     * 接口文档与健康检查端点在 {@link #INFRA_PATHS} 显式放行，其余需登录。
     * 首期是 API 场景，禁用 CSRF 简化测试；前端登录页归 #22。
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(INFRA_PATHS).permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .csrf((csrf) -> csrf.disable());
        return http.build();
    }

    /** OAuth2 客户端仓库，JDBC 持久化到 {@code iam.oauth2_registered_client}。 */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    /** 授权记录服务，JDBC 持久化到 {@code iam.oauth2_authorization}。 */
    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    /** 授权确认服务，JDBC 持久化到 {@code iam.oauth2_authorization_consent}。 */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    /** 授权服务器端点设置，issuer 不固定，由请求推断，方便本地与测试环境切换。 */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    /**
     * JWT 签名密钥源。
     *
     * <p>首期每次启动随机生成 RSA 2048 密钥对，公钥通过 {@code /oauth2/jwks} 暴露。
     * 代价是应用重启后旧 token 签名校验失败，需要重新登录；开发环境可接受。
     * 后续如果要做固定密钥或密钥轮换，再引入配置化的密钥来源（摊牌点）。
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * token 生成器链：access token 用 JWT，refresh token 用随机不透明值。
     * JWT 生成时应用 {@code jwtTokenCustomizer} 把角色写进 claims。
     */
    @Bean
    public OAuth2TokenGenerator tokenGenerator(
            JWKSource<SecurityContext> jwkSource,
            OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer) {
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(jwtTokenCustomizer);
        return new DelegatingOAuth2TokenGenerator(jwtGenerator, new OAuth2RefreshTokenGenerator());
    }

    /**
     * access token 的角色自定义。
     *
     * <p>把登录时加载的 {@code ROLE_} 前缀授权剥离前缀，写成 {@code roles} claim。
     * 资源服务器 hasRole 判断前把 {@code roles} 映射回 {@code ROLE_} 授权。
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return (context) -> {
            if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN) {
                java.util.Set<String> roles = context.getPrincipal().getAuthorities().stream()
                        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .map(authority -> authority.substring("ROLE_".length()))
                        .collect(Collectors.toSet());
                context.getClaims().claim("roles", roles);
            }
        };
    }

    /** 授权服务器本地 JWT 解码器，用同一 JWKS 校验，供登出接口解析 token。 */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /** 生成 RSA 2048 密钥对，供 {@link #jwkSource()} 使用。 */
    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("生成 RSA 密钥失败", ex);
        }
    }
}
