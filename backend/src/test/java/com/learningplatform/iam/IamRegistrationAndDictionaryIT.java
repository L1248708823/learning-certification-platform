package com.learningplatform.iam;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #17 的真实依赖集成测试。
 *
 * <p>测试只跨越公开 HTTP 接口。MySQL 执行 Flyway 迁移，Redis 保存验证码和发送许可，
 * 用于验证注册与字典在真实依赖上的可运行行为。认证、Token 和 RBAC 测试留给 #24。
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "app.iam.verification-code.fixed=123456",
                "spring.flyway.clean-disabled=true"
        })
@AutoConfigureMockMvc
class IamRegistrationAndDictionaryIT {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void dictionary_shouldReturnSeededEnabledCourseCategories() throws Exception {
        mockMvc.perform(get("/api/v1/dicts/COURSE_CATEGORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].code").value("FRONTEND"))
                .andExpect(jsonPath("$.data[0].label").value("前端开发"))
                .andExpect(jsonPath("$.data[1].code").value("BACKEND"))
                .andExpect(jsonPath("$.data[2].code").value("AI"));
    }

    @Test
    void unknownDictionaryType_shouldReturnNotFoundContract() throws Exception {
        mockMvc.perform(get("/api/v1/dicts/UNKNOWN_" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message").value("字典类型不存在"));
    }

    /** IAM OpenAPI 分组只收录 IAM 包中的真实公开接口，供 Knife4j 的文档资源列表使用。 */
    @Test
    void iamOpenApiGroup_shouldContainBusinessEndpointsAndTags() throws Exception {
        mockMvc.perform(get("/v3/api-docs/IAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/sms/code'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/dicts/{typeCode}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/_demo/echo']").doesNotExist())
                .andExpect(jsonPath("$.tags[?(@.name == '注册')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == '字典')]").exists());
    }

    @Test
    void registration_shouldSendCodeAndCreateLearnerAccount() throws Exception {
        RegistrationData data = nextRegistrationData();

        mockMvc.perform(post("/api/v1/sms/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + data.phone() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(data, "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(data.username()))
                .andExpect(jsonPath("$.data.phone").value(data.phone()))
                .andExpect(jsonPath("$.data.displayName").value(data.username()))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT `password` FROM iam.`user` WHERE username = ?",
                String.class,
                data.username());
        Integer learnerRoleCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM iam.user_role user_role
                INNER JOIN iam.`user` user ON user.id = user_role.user_id
                INNER JOIN iam.role role ON role.id = user_role.role_id
                WHERE user.username = ? AND role.code = 'LEARNER'
                """, Integer.class, data.username());

        assertThat(passwordHash).isNotEqualTo("Password123!").startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches("Password123!", passwordHash)).isTrue();
        assertThat(learnerRoleCount).isEqualTo(1);
    }

    @Test
    void registration_withWrongCode_shouldBeRejectedWithoutConsumingTheActualCode() throws Exception {
        RegistrationData data = nextRegistrationData();

        mockMvc.perform(post("/api/v1/sms/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + data.phone() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(data, "000000")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.message").value("验证码无效或已过期"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(data, "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void sendCode_twiceWithinSixtySeconds_shouldBeRejectedByRedisPermit() throws Exception {
        String phone = nextRegistrationData().phone();
        String body = "{\"phone\":\"" + phone + "\"}";

        mockMvc.perform(post("/api/v1/sms/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sms/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.message").value("验证码发送过于频繁"));
    }

    @Test
    void registration_withDuplicateUsername_shouldKeepCodeForRetryWithNewName() throws Exception {
        RegistrationData first = nextRegistrationData();
        sendCode(first.phone());
        register(first, "123456");

        RegistrationData duplicate = new RegistrationData(nextPhone(), first.username());
        sendCode(duplicate.phone());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(duplicate, "123456")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2003));

        RegistrationData retry = new RegistrationData(duplicate.phone(), "it_retry_" + UUID.randomUUID().toString().replace("-", ""));
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(retry, "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(retry.username()));
    }

    private void sendCode(String phone) throws Exception {
        mockMvc.perform(post("/api/v1/sms/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk());
    }

    private void register(RegistrationData data, String code) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(data, code)))
                .andExpect(status().isOk());
    }

    private static RegistrationData nextRegistrationData() {
        String username = "it_user_" + UUID.randomUUID().toString().replace("-", "");
        return new RegistrationData(nextPhone(), username);
    }

    private static String nextPhone() {
        return "%011d".formatted(13900000000L + PHONE_SEQUENCE.getAndIncrement());
    }

    private static String registerJson(RegistrationData data, String code) {
        return "{\"phone\":\"" + data.phone() + "\","
                + "\"code\":\"" + code + "\","
                + "\"username\":\"" + data.username() + "\","
                + "\"password\":\"Password123!\"}";
    }

    private record RegistrationData(String phone, String username) {
    }
}
