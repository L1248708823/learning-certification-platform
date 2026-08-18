package com.learningplatform.iam.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MyBatis-Plus 乐观锁的端到端验证。
 *
 * <p>两个实体副本持有同一个 {@code version = 0}，先后执行 {@code updateById}：先到的更新成功并把版本加一，
 * 后到的因为 {@code WHERE version = 0} 条件不再成立更新 0 行，模拟并发下丢失更新被拦截。
 */
@Testcontainers
@SpringBootTest(properties = "spring.flyway.clean-disabled=true")
class UserAccountOptimisticLockIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("iam")
            .withUsername("test")
            .withPassword("test");

    private static final AtomicLong PHONE_SEQUENCE = new AtomicLong(1);

    @Autowired
    private UserAccountMapper userMapper;

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void twoStaleCopies_shouldOnlyLetFirstUpdateSucceed() {
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername("it_lock_" + UUID.randomUUID().toString().replace("-", ""));
        user.setPassword(new BCryptPasswordEncoder().encode("Password123!"));
        user.setPhone(nextPhone());
        user.setDisplayName("initial");
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insertUser(user);

        // 两个"请求"各自从库读到同一份数据，都持有 version = 0
        UserAccountEntity copyA = userMapper.selectById(user.getId());
        UserAccountEntity copyB = userMapper.selectById(user.getId());
        assertThat(copyA.getVersion()).isZero();

        // 先到的更新成功，版本号从 0 变成 1
        copyA.setDisplayName("copyA-won");
        assertThat(userMapper.updateById(copyA)).isEqualTo(1);

        // 后到的仍带着 version = 0，版本条件不成立，更新 0 行
        copyB.setDisplayName("copyB-lost");
        assertThat(userMapper.updateById(copyB)).isZero();

        // 库里保留的是先到的写，后到的写被乐观锁丢弃
        UserAccountEntity latest = userMapper.selectById(user.getId());
        assertThat(latest.getDisplayName()).isEqualTo("copyA-won");
        assertThat(latest.getVersion()).isEqualTo(1L);
    }

    private static String nextPhone() {
        return "%011d".formatted(13900000000L + PHONE_SEQUENCE.getAndIncrement());
    }
}
