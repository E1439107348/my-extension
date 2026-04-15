package org.example.myextension.logger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开放接口日志注解
 * <p>
 * 用于标记需要进行调用日志记录的开放 API 接口方法。
 * 使用 AOP 切面拦截带有此注解的方法，在方法执行前后记录 API 调用日志。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *   <li>记录接口名称、版本和 URL</li>
 *   <li>自动获取客户端 IP 地址（支持代理和负载均衡场景）</li>
 *   <li>记录接口请求参数和响应结果</li>
 *   <li>自动记录接口执行耗时</li>
 *   <li>捕获异常并记录错误信息</li>
 * </ul>
 * <p>
 * <b>日志格式：</b>
 * <pre>
 * OpenApi
 * API Name: {name}
 * API Version: {version}
 * API Url: {url}
 * ClientIP: {ip}
 * Times: {耗时} ms
 * Args: {请求参数JSON}
 * Result: {响应结果JSON}
 * </pre>
 * <p>
 * <b>IP 获取逻辑：</b>
 * <ol>
 *   <li>首先检查 {@code X-Real-IP} 请求头（Nginx 等代理通常使用）</li>
 *   <li>然后检查 {@code X-Forwarded-For} 请求头（支持多级代理）</li>
 *   <li>最后使用 {@code request.getRemoteAddr()} 作为备选</li>
 * </ol>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * &#64;OpenApiLog(name = "获取商品列表", version = "1.0.0", url = "/v1/items")
 * public ApiResult&lt;List&lt;Item&gt;&gt; getItems(ItemQueryRequest request) {
 *     // 方法实现
 * }
 *
 * &#64;OpenApiLog(name = "创建订单", version = "1.2.0", url = "/v1/orders")
 * public ApiResult&lt;Order&gt; createOrder(CreateOrderRequest request) {
 *     // 方法实现
 * }
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>请求参数和响应结果会被序列化为 JSON，如果对象过大可能影响性能</li>
 *   <li>敏感信息（如密码、密钥）不会被自动脱敏，需要手动处理</li>
 *   <li>对于高频调用的接口，建议谨慎使用以避免日志量过大</li>
 *   <li>IP 获取依赖于请求头配置，请确保代理服务器正确传递原始 IP</li>
 * </ul>
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpenApiLog {
    /**
     * 接口名称
     * <p>
     * 用于标识接口的业务含义，日志中会显示此名称。
     * 建议使用描述接口功能的中文或英文名称。
     *
     * @return 接口名称
     */
    String name();

    /**
     * 接口版本
     * <p>
     * 用于标识接口的版本号，支持接口版本管理和追踪。
     * 建议使用语义化版本号格式（如 "1.0.0"、"1.2.3"）。
     *
     * @return 接口版本号
     */
    String version();

    /**
     * 接口 URL
     * <p>
     * 用于标识接口的访问路径，便于追踪和定位接口。
     * 默认值为 "unknown"，建议填写实际的接口路径。
     *
     * @return 接口 URL 路径
     */
    String url() default "unknown";
}
