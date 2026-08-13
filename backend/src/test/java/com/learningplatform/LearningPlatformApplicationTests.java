package com.learningplatform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 冒烟测试：验证应用上下文能完整组装（Web、Actuator、日志配置）。
 *
 * <p>测试排除数据源与 Flyway 自动配置，因此不需要 Docker 里的 MySQL 就能运行。
 * 真实依赖的集成测试从首期-02 起用 Testcontainers（MySQL / Redis / MinIO 容器）补上。
 */
@SpringBootTest(properties = {
        "app.iam.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class LearningPlatformApplicationTests {

    @Test
    void contextLoads() {
        // 上下文能启动即通过
    }
}
