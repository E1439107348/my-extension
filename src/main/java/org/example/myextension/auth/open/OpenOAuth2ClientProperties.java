package org.example.myextension.auth.open;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * 开放 OAuth2 客户端属性配置类
 * <p>
 * 从配置文件中加载 OAuth2 客户端的配置信息，维护多个客户端的配置 Map。
 * 使用 Spring Boot 的 {@code @ConfigurationProperties} 自动绑定配置属性。
 * <p>
 * <b>配置前缀：</b> {@code extension.auth.open}
 * <p>
 * <b>配置格式：</b>
 * <pre>
 * extension:
 *   auth:
 *     open:
 *       enable: true
 *       clients:
 *         app1:
 *           clientId: "app1_id"
 *           clientSecret: "app1_secret"
 *           allowUrl: "https://app1.com/callback"
 *           contractScope: "read,write"
 *         app2:
 *           clientId: "app2_id"
 *           clientSecret: "app2_secret"
 *           allowUrl: "https://app2.com/callback"
 *           contractScope: "read"
 * </pre>
 * <p>
 * <b>启用条件：</b>
 * 需要配置 {@code extension.auth.open.enable=true} 才会启用此配置类。
 * <p>
 * <b>使用方式：</b>
 * 通过依赖注入获取此类的实例，然后访问 {@code clients} Map 获取各个客户端的配置：
 * <pre>
 * &#64;Autowired
 * private OpenOAuth2ClientProperties clientProperties;
 *
 * public void printClientConfigs() {
 *     clientProperties.getClients().forEach((clientId, config) -> {
 *         System.out.println("Client: " + clientId);
 *         System.out.println("Secret: " + config.getClientSecret());
 *     });
 * }
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>配置文件的修改需要重启应用才能生效（除非配置动态刷新）</li>
 *   <li>{@code clientSecret} 包含敏感信息，建议使用环境变量或密钥管理服务</li>
 *   <li>客户端 ID 不能重复，否则后面的配置会覆盖前面的</li>
 * </ul>
 *
 * @see OpenOAuth2AppConfig 单个应用的配置模型
 * @see OpenOAuth2TemplateImpl OAuth2 模板实现
 */
@Component
@ConfigurationProperties(prefix = "extension.auth.open")
@ConditionalOnProperty(name = "extension.auth.open.enable", havingValue = "true")
public class OpenOAuth2ClientProperties {
    /**
     * 存储多个客户端配置的 Map
     * <p>
     * Key 为客户端标识（如 "app1"、"app2"），Value 为该客户端的详细配置。
     * 使用 HashMap 存储，便于通过客户端标识快速查找配置。
     */
    private Map<String, OpenOAuth2AppConfig> clients = new HashMap<>();

    /**
     * 获取客户端配置 Map
     *
     * @return 客户端配置 Map
     */
    public Map<String, OpenOAuth2AppConfig> getClients() {
        return clients;
    }

    /**
     * 设置客户端配置 Map
     * <p>
     * 主要用于 Spring Boot 自动属性绑定，一般不需要手动调用。
     *
     * @param clients 客户端配置 Map
     */
    public void setClients(Map<String, OpenOAuth2AppConfig> clients) {
        this.clients = clients;
    }
}
