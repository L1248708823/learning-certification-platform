package com.learningplatform.iam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #24 的真实依赖集成测试：SAS 授权码 + PKCE 登录闭环。
 *
 * <p>测试覆盖：注册 LEARNER 用户 → 浏览器走 /oauth2/authorize + 登录表单 + 授权确认，
 * 拿 code 换 access_token → users/me 返回当前用户 → 登出 → 旧 token 立即失效返回 401。
 * 同时解码 JWT 校验 roles claim 携带 LEARNER，验证角色进 claims 的决策。
 *
 * <p>本机无 Docker 不执行，作为待办在服务器 {@code mvn verify} 下运行，与 #17 一致。
 * PKCE 的 code_challenge 用 S256 对 verifier 求 SHA-256 后做 Base64URL。
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "app.iam.verification-code.fixed=123456",
                "spring.flyway.clean-disabled=true"
        })
@AutoConfigureMockMvc
class IamAuthLoginIT {

    private static final String REDIRECT_URI =
            "http://127.0.0.1:5173/login/oauth2/code/learning-web";
    private static final String SCOPES = "openid profile";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("iam")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static final AtomicLong PHONE_SEQUENCE = new AtomicLong(1);

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    @Test
    void loginLogout_whenLearnerFollowsAuthorizationCodeWithPkce_shouldIssueTokenThenRevokeIt() throws Exception {
        String username = registerLearner();

        String codeVerifier = "verifier_" + UUID.randomUUID();
        String codeChallenge = sha256UrlEncoded(codeVerifier);
        String state = UUID.randomUUID().toString();
        String authorizeUrl = authorizeUrl(codeChallenge, state);

        MockHttpSession session = new MockHttpSession();

        // 未登录访问授权端点，重定向到登录页
        mockMvc.perform(get(authorizeUrl).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // 登录表单提交用户名密码，SAS 保存认证上下文到 session
        mockMvc.perform(post("/login")
                        .session(session)
                        .param("username", username)
                        .param("password", "Password123!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // 带 session 重放授权请求，进入授权确认页
        MvcResult consentRedirect = mockMvc.perform(get(authorizeUrl).session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String consentUrl = consentRedirect.getResponse().getRedirectedUrl();
        assertThat(consentUrl).startsWith("/oauth2/consent");

        mockMvc.perform(get(consentUrl).session(session))
                .andExpect(status().isOk());

        // 提交授权确认，SAS 回跳 redirect_uri 并携带一次性 code
        MvcResult codeResult = mockMvc.perform(post("/oauth2/consent")
                        .session(session)
                        .param("client_id", "learning-web")
                        .param("state", state)
                        .param("scope", "openid")
                        .param("scope", "profile")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String code = UriComponentsBuilder.fromUriString(codeResult.getResponse().getRedirectedUrl())
                .build().getQueryParams().getFirst("code");
        assertThat(code).isNotBlank();

        // 用 code + verifier 换 access_token
        String tokenBody = mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", REDIRECT_URI)
                        .param("client_id", "learning-web")
                        .param("code_verifier", codeVerifier))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode tokenJson = objectMapper.readTree(tokenBody);
        String accessToken = tokenJson.get("access_token").asText();

        // 角色写进 JWT roles claim，注册用户默认 LEARNER
        JsonNode claims = decodeJwtPayload(accessToken);
        assertThat(claims.get("roles")).hasSize(1);
        assertThat(claims.get("roles")).anyMatch(node -> node.asText().equals("LEARNER"));

        // users/me 带有效 token 返回当前用户
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(username));

        // 登出把 jti 写入黑名单
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 旧 token 再次访问被拒绝
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersMe_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    /** 注册一个新用户，返回用户名。注册默认授予 LEARNER 角色。 */
    private String registerLearner() throws Exception {
        String phone = "%011d".formatted(13900000000L + PHONE_SEQUENCE.getAndIncrement());
        String username = "it_login_" + UUID.randomUUID().toString().replace("-", "");

        mockMvc.perform(post("/api/v1/sms/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\","
                                + "\"code\":\"123456\","
                                + "\"username\":\"" + username + "\","
                                + "\"password\":\"Password123!\"}"))
                .andExpect(status().isOk());

        return username;
    }

    private String authorizeUrl(String codeChallenge, String state) {
        return "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=learning-web"
                + "&redirect_uri=" + REDIRECT_URI
                + "&scope=" + SCOPES.replace(' ', '+')
                + "&state=" + state
                + "&code_challenge=" + codeChallenge
                + "&code_challenge_method=S256";
    }

    /** S256 PKCE challenge：verifier 的 SHA-256，Base64URL 无填充。 */
    private static String sha256UrlEncoded(String verifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(digest.digest(verifier.getBytes(StandardCharsets.US_ASCII)));
    }

    /** 解码 JWT 的 payload 段，用于校验 claims 内容。 */
    private JsonNode decodeJwtPayload(String token) throws Exception {
        String payload = token.split("\\.")[1];
        return objectMapper.readTree(Base64.getUrlDecoder().decode(payload));
    }
}
