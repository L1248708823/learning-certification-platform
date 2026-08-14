package com.learningplatform.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档的全局元信息。
 *
 * <p>接口路径、请求字段和 Bean Validation 约束由 Springdoc 从 Controller 自动生成；
 * 这里仅声明所有接口共享的名称、版本和统一响应规则。
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    /**
     * 创建平台的 OpenAPI 根对象。
     *
     * @return 带服务名称和响应契约说明的 OpenAPI 定义
     */
    @Bean
    public OpenAPI learningPlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("在线教育与职业认证平台 API")
                        .version("v1")
                        .description("所有接口均返回 code、message、data 三字段的统一响应体；"
                                + "code 为 0 表示成功，失败时 HTTP 状态码和业务 code 共同表达错误语义。"));
    }
}
