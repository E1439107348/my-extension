package org.example.myextension.auth.open;

import lombok.Data;

/**
 * 开放 API OAuth2 应用配置模型
 * <p>
 * 封装单个 OAuth2 应用的配置信息，用于定义允许访问开放 API 的客户端。
 * <p>
 * <b>配置说明：</b>
 * <table border="1">
 *   <tr><th>字段</th><th>说明</th></tr>
 *   <tr><td>clientId</td><td>客户端唯一标识，用于标识不同的应用</td></tr>
 *   <tr><td>clientSecret</td><td>客户端秘钥，用于验证客户端身份</td></tr>
   <tr><td>allowUrl</td><td>允许的授权回调地址，默认 "*" 表示不限制</td></tr>
   <tr><td>contractScope</td><td>授权范围，默认 "*" 表示拥有所有权限</td></tr>
 * </table>
 * <p>
 * <b>配置方式：</b>
 * 在 {@code application.properties} 或 {@code application.yml} 中配置：
 * <pre>
 * extension:
 *   auth:
 *     open:
 *       enable: true
 *       clients:
 *         myapp:
 *           clientId: "my_app_id"
 *           clientSecret: "my_app_secret"
 *           allowUrl: "*"
 *           contractScope: "*"
 * </pre>
 * <p>
 * <b>安全注意事项：</b>
 * <ul>
 *   <li>{@code clientSecret} 应该保密，不要提交到代码仓库</li>
 *   <li>生产环境建议使用环境变量或密钥管理服务存储敏感信息</li>
 *   <li>{@code allowUrl} 不建议使用 "*"，应该明确指定允许的回调地址</li>
 *   <li>{@code contractScope} 建议按需配置，避免授予过多权限</li>
 * </ul>
 *
 * @see OpenOAuth2ClientProperties OAuth2 客户端属性配置类
 * @see OpenOAuth2TemplateImpl OAuth2 模板实现
 */
@Data
public class OpenOAuth2AppConfig {
    /**
     * 客户端 ID
     * <p>
     * 客户端的唯一标识，用于区分不同的应用。
     * 在获取 Token 和调用 API 时需要提供此 ID。
     */
    private String clientId;

    /**
     * 客户端秘钥
     * <p>
     * 用于验证客户端身份的秘钥，在获取 Token 时需要提供。
     * <b>注意：此字段包含敏感信息，请妥善保管。</b>
     */
    private String clientSecret;

    /**
     * 允许的授权回调地址
     * <p>
     * 限制 OAuth2 授权后的回调地址，防止授权码被劫持。
     * 默认值为 "*"，表示不限制回调地址。
     * <p>
     * <b>建议：</b> 生产环境应该明确指定允许的回调地址，如 {@code https://example.com/callback}。
     */
    private String allowUrl = "*";

    /**
     * 授权范围（Scope）
     * <p>
     * 定义客户端可以访问的 API 权限范围。
     * 默认值为 "*"，表示拥有所有权限。
     * <p>
     * <b>建议：</b> 应该按需配置具体的权限范围，避免授予过多权限。
     * 例如：{@code read,write} 表示只授予读写权限。
     */
    private String contractScope = "*";

    /**
     * Access-Token 过期时间（秒）
     * <p>
     * 可选配置。未配置时走 Sa-Token 默认行为。
     */
    private Long accessTokenTimeout;

    /**
     * Refresh-Token 过期时间（秒）
     * <p>
     * 可选配置。未配置时走 Sa-Token 默认行为。
     */
    private Long refreshTokenTimeout;

    /**
     * Client-Token 过期时间（秒）
     * <p>
     * 可选配置。支持按客户端单独设置 client_token 的有效期。
     */
    private Long clientTokenTimeout;

    /**
     * 历史 Client-Token 保留时间（秒）
     * <p>
     * 可选配置。用于控制过去 token 的短暂可用窗口。
     */
    private Long pastClientTokenTimeout;

    /**
     * 是否启用刷新令牌轮换
     * <p>
     * 可选配置。true 表示每次刷新后签发新的 refresh_token。
     */
    private Boolean isNewRefresh;

    /**
     * 无参构造函数
     * <p>
     * 用于 JSON 反序列化时创建实例。
     */
    public OpenOAuth2AppConfig() {}
}
