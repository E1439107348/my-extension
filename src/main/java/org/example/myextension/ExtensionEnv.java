package org.example.myextension;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 扩展环境配置类
 * 将配置文件中的属性映射到 Java 对象中
 */
@Component
public class ExtensionEnv {


    /**
     * 消息分发处理器的生命周期（秒） (对应配置项: extension.msg.dispatch.handlerLifeTime)
     * 默认值为 "21600" (6小时)
     */
    @Value("${extension.msg.dispatch.handlerLifeTime:21600}")
    private String dispatchHandlerLifeTime;
    @Value("${extension.auth.manage.youzanIsvTokenResolverUrl:}")
    private String tokenResolverUrl;

    @Value("${extension.auth.manage.enable:false}")
    private String manageAuthEnable;

    // Getter
    public String getTokenResolverUrl() { return tokenResolverUrl; }
    public String getManageAuthEnable() { return manageAuthEnable; }



    public String getDispatchHandlerLifeTime() {
        return dispatchHandlerLifeTime;
    }

}