package org.example.myextension.logger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.example.myextension.logger.enums.LogOutput;

/**
 * 方法日志记录注解
 * <p>
 * 用于标记需要进行方法调用日志记录的方法。
 * 使用 AOP 切面拦截带有此注解的方法，在方法执行前后记录日志。
 * <p>
 * <b>功能特性：</b>
 * <ul>
 *   <li>记录方法调用的标题和方法签名</li>
 *   <li>可配置是否记录方法入参</li>
 *   <li>可配置是否记录方法返回值</li>
 *   <li>自动记录方法执行耗时</li>
 *   <li>捕获异常并记录错误信息</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * // 示例 1：记录入参和结果（默认）
 * &#64;MethodLogger(title = "创建订单")
 * public Order createOrder(CreateOrderRequest request) {
 *     // 方法实现
 * }
 *
 * // 示例 2：仅记录入参，不记录结果
 * &#64;MethodLogger(title = "发送通知", output = {LogOutput.PARAMS})
 * public void sendNotification(NotificationRequest request) {
 *     // 方法实现
 * }
 *
 * // 示例 3：仅记录结果，不记录入参
 * &#64;MethodLogger(title = "查询订单", output = {LogOutput.RESULT})
 * public Order getOrder(Long orderId) {
 *     // 方法实现
 * }
 *
 * // 示例 4：不记录任何内容（仅记录标题和耗时）
 * &#64;MethodLogger(title = "清理缓存", output = {})
 * public void cleanCache() {
 *     // 方法实现
 * }
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>入参和返回值会被序列化为 JSON，如果对象过大可能影响性能</li>
   *   <li>敏感信息（如密码、密钥）不会被自动脱敏，需要手动处理</li>
 *   <li>对于高频调用的方法，建议谨慎使用以避免日志量过大</li>
 * </ul>
 *
 * @see LogOutput 日志输出范围枚举
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodLogger {
    /**
     * 日志标题
     * <p>
     * 用于标识方法调用的业务含义，日志中会显示此标题。
     * 建议使用描述业务操作的中文标题，如"创建订单"、"查询用户"等。
     *
     * @return 日志标题
     */
    String title();

    /**
     * 日志输出内容
     * <p>
     * 指定需要记录的日志内容类型，支持记录入参、返回值或两者都记录。
     * 默认为记录入参和返回值。
     * <p>
     * 对应 Kotlin 代码中的 vararg val output。
     *
     * @return 日志输出内容列表
     */
    LogOutput[] output() default {LogOutput.PARAMS, LogOutput.RESULT};
}
