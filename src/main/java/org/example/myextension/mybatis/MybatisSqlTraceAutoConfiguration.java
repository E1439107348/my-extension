package org.example.myextension.mybatis;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis SQL Trace 自动装配。
 * <p>
 * 目的：兼容不同 MyBatis 集成方式（包括手工构建 SqlSessionFactory），
 * 在 Bean 初始化后将 SQL Trace 拦截器注册到 MyBatis Configuration。
 */
@Configuration
@ConditionalOnClass(SqlSessionFactory.class)
@ConditionalOnProperty(prefix = "extension.mybatis.sql-trace", name = "enable", havingValue = "true")
public class MybatisSqlTraceAutoConfiguration {

    /**
     * 在 SqlSessionFactory 初始化后注入 SQL Trace 拦截器。
     */
    @Bean
    public BeanPostProcessor sqlTraceBeanPostProcessor(Interceptor sqlTraceMybatisInterceptor) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof SqlSessionFactory) {
                    SqlSessionFactory factory = (SqlSessionFactory) bean;
                    org.apache.ibatis.session.Configuration configuration = factory.getConfiguration();
                    if (configuration != null) {
                        boolean alreadyExists = configuration.getInterceptors().stream()
                                .anyMatch(i -> i.getClass().equals(sqlTraceMybatisInterceptor.getClass()));
                        if (!alreadyExists) {
                            configuration.addInterceptor(sqlTraceMybatisInterceptor);
                        }
                    }
                }
                return bean;
            }
        };
    }
}
