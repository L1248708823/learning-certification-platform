package com.learningplatform.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 契约层集成测试：验证统一响应、全局异常、HTTP 状态语义化与参数校验格式化，
 * 对应 docs/spec/0001 第 5 章和首期-02 完成定义。
 *
 * <p>本票只测 Web 契约层，不碰持久化，因此通过 spring.autoconfigure.exclude 排除
 * DataSource / Flyway 自动配置，测试离线可跑、不依赖服务器隧道。后续持久化票据
 * 再按规格第 9 章引入 Testcontainers 连真实 MySQL。
 */
@SpringBootTest(properties = {
        "app.iam.enabled=false",
        "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void success_shouldReturnCodeZero() throws Exception {
        mockMvc.perform(post("/api/v1/_demo/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"foo": "bar", "count": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.foo").value("bar"))
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    void businessError_conflict_shouldMapTo409() throws Exception {
        mockMvc.perform(get("/api/v1/_demo/business-error?type=conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1003))
                .andExpect(jsonPath("$.message").value("请求与当前状态冲突"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void businessError_notFound_shouldMapTo404() throws Exception {
        mockMvc.perform(get("/api/v1/_demo/business-error?type=not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void paramInvalid_emptyBody_shouldReturnFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/_demo/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("参数校验失败"))
                .andExpect(jsonPath("$.data.length()").value(2))
                // 用过滤表达式断言字段明细，避免依赖字段错误顺序；过滤可能命中多条，用 hasItem 匹配
                .andExpect(jsonPath("$.data[?(@.field == 'name')].message")
                        .value(hasItem(containsString("不能为空"))))
                .andExpect(jsonPath("$.data[?(@.field == 'age')].message")
                        .value(hasItem(containsString("不能为空"))));
    }

    @Test
    void paramInvalid_overLengthName_shouldReturnFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/_demo/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "这个名称实在是太长太长太长太长太长太长太长太长太", "age": 25}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.data[?(@.field == 'name')].message")
                        .value(hasItem(containsString("最长 20"))));
    }

    @Test
    void malformedJson_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/_demo/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    @Test
    void internalError_shouldReturnSafeMessage() throws Exception {
        mockMvc.perform(get("/api/v1/_demo/internal-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(1999))
                .andExpect(jsonPath("$.message").value("系统内部错误"))
                // 兜底响应的 message 是安全文案，不泄漏异常内部细节
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /** OpenAPI 页面只展示真实业务接口，契约演示入口不应出现在调用方目录中。 */
    @Test
    void openApi_shouldExposeMetadataAndHideContractDemoEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("在线教育与职业认证平台 API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/_demo/echo']").doesNotExist());
    }

    /** Knife4j 是本地学习和人工调试的入口，依赖升级后也必须保持可访问。 */
    @Test
    void knife4jUi_shouldBeAvailable() throws Exception {
        mockMvc.perform(get("/doc.html"))
                .andExpect(status().isOk());
    }
}
