package com.learningplatform.common.mybatis;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件装配。
 *
 * <p>当前只注册乐观锁插件：实体字段带 {@code @Version} 时，MP 的 {@code updateById} 等更新会自动追加
 * {@code WHERE version = 旧值} 并把版本号加一，0 行受影响表示存在并发修改，调用方据此处理冲突。
 * 后续需要分页等能力时在同一拦截器里追加 InnerInterceptor，顺序即执行顺序。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
