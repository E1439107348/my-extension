package org.example.myextension;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 扩展自动配置类
 * <p>
 * 用于扫描和注册扩展模块中的组件
 *
 */
@Configuration
@ComponentScan
public class ExtensionAutoConfiguration {
}
