package org.example.myextension.auth.open;

import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.SaClientModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sa-Token OAuth2 配置模板实现
 * <p>
 * 实现 Sa-Token 的 {@code SaOAuth2Template} 接口，用于根据 clientId 加载具体的客户端信息。
 * 此类是 Sa-Token OAuth2 框架与自定义配置之间的桥梁。
 * <p>
 * <b>主要职责：</b>
 * <ul>
 *   <li>根据 clientId 从配置中查找对应的客户端信息</li>
 *   <li>将配置信息转换为 Sa-Token 内部使用的 {@link SaClientModel}</li>
 *   <li>设置非自动模式和客户端标识</li>
 * </ul>
 * <p>
 * <b>工作流程：</b>
 * <ol>
 *   <li>客户端请求 Access Token 时，提供 {@code clientId} 和 {@code clientSecret}</li>
 *   <li>Sa-Token 调用此类的 {@code getClientModel} 方法获取客户端配置</li>
 *   <li>此方法从 {@link OpenOAuth2ClientProperties} 中查找匹配的配置</li>
 *   <li>将配置转换为 {@code SaClientModel} 并返回给 Sa-Token</li>
 *   <li>Sa-Token 验证 {@code clientSecret} 并生成 Token</li>
 * </ol>
 * <p>
 * <b>启用条件：</b>
 * 需要配置 {@code extension.auth.open.enable=true} 才会启用此类。
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>如果配置中没有对应的 clientId，方法返回 null，Sa-Token 会抛出异常</li>
 *   <li>客户端配置的修改需要重启应用才能生效（除非配置动态刷新）</li>
 *   <li>{@code setIsAutoMode(false)} 表示禁用自动模式，使用自定义的客户端配置</li>
 * </ul>
 *
 * @see OpenOAuth2AppConfig 单个应用的配置模型
 * @see OpenOAuth2ClientProperties OAuth2 客户端属性配置类
 * @see cn.dev33.satoken.oauth2 Sa-Token OAuth2
 */
@Component
@ConditionalOnProperty(name = "extension.auth.open.enable", havingValue = "true")
public class OpenOAuth2TemplateImpl extends SaOAuth2Template {

    /**
     * OAuth2 客户端属性配置
     * <p>
     * 包含所有客户端的配置信息，用于查找特定 clientId 的配置。
     */
    private final OpenOAuth2ClientProperties clientProperties;

    /**
     * 构造函数：通过依赖注入初始化
     *
     * @param clientProperties OAuth2 客户端属性配置
     */
    public OpenOAuth2TemplateImpl(OpenOAuth2ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    /**
     * 根据客户端 ID 获取客户端模型
     * <p>
     * 此方法是 Sa-Token OAuth2 框架的核心接口实现，用于加载客户端配置信息。
     * <p>
     * <b>查找逻辑：</b>
     * <ol>
     *   <li>遍历配置中的所有客户端</li>
     *   <li>查找 {@code clientId} 与参数匹配的配置</li>
     *   <li>如果找到，转换为 {@code SaClientModel} 并返回</li>
     *   <li>如果未找到，返回 null</li>
     * </ol>
     * <p>
     * <b>返回的模型配置：</b>
     * <ul>
     *   <li>{@code clientId}: 客户端 ID</li>
     *   <li>{@code clientSecret}: 客户端秘钥</li>
     *   <li>{@code allowUrl}: 允许的回调地址</li>
     *   <li>{@code contractScope}: 授权范围</li>
     *   <li>{@code isAutoMode}: 设为 false（非自动模式）</li>
     *   <li>{@code isClient}: 设为 true（标识为客户端）</li>
     * </ul>
     *
     * @param clientId 客户端 ID
     * @return Sa-Token 内部使用的客户端模型，如果未找到则返回 null
     */
    @Override
    public SaClientModel getClientModel(String clientId) {
        // 在配置列表中查找匹配 clientId 的应用配置
        OpenOAuth2AppConfig appConfig = clientProperties.getClients().values().stream()
                .filter(config -> clientId.equals(config.getClientId()))
                .findFirst()
                .orElse(null);

        if (appConfig == null) {
            return null;
        }

        // 构造并返回 Sa-Token 内部使用的客户端模型
        return new SaClientModel()
                .setClientId(clientId)
                .setClientSecret(appConfig.getClientSecret())
                .setAllowUrl(appConfig.getAllowUrl())
                .setContractScope(appConfig.getContractScope())
                .setIsAutoMode(false) // 设为非自动模式
                .setIsClient(true);   // 标识为客户端
    }
}
