package org.example.myextension.utils;


import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 上下文工具类
 * 
 * 该类实现 ApplicationContextAware，用于在 Spring 启动时保存 ApplicationContext 静态引用，
 * 以便在非 Spring 管理的静态方法或工具类中获取 Spring Bean。
 * 
 * 注意：持有静态 ApplicationContext 会引入全局状态，需谨慎使用以免影响测试和类加载生命周期。
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringContextUtil.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}