package org.example.myextension.logger.enums;

/**
 * 日志输出范围枚举
 * <p>
 * 定义方法日志记录中可以输出的内容类型。
 * 用于 {@link org.example.myextension.logger.annotation.MethodLogger} 注解的配置，
 * 控制是否记录方法的入参和返回值。
 * <p>
 * <b>枚举说明：</b>
 * <table border="1">
 *   <tr><th>枚举值</th><th>说明</th></tr>
 *   <tr><td>PARAMS</td><td>记录方法调用时的入参</td></tr>
 *   <tr><td>RESULT</td><td>记录方法执行后的返回值</td></tr>
 * </table>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * // 示例 1：记录入参和结果（默认）
 * &#64;MethodLogger(title = "创建订单", output = {LogOutput.PARAMS, LogOutput.RESULT})
 * public Order createOrder(CreateOrderRequest request) { ... }
 *
 * // 示例 2：仅记录入参
 * &#64;MethodLogger(title = "发送通知", output = {LogOutput.PARAMS})
 * public void sendNotification(NotificationRequest request) { ... }
 *
 * // 示例 3：仅记录结果
 * &#64;MethodLogger(title = "查询订单", output = {LogOutput.RESULT})
 * public Order getOrder(Long orderId) { ... }
 *
 * // 示例 4：不记录任何内容（仅记录标题和耗时）
 * &#64;MethodLogger(title = "清理缓存", output = {})
 * public void cleanCache() { ... }
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>入参和返回值会被序列化为 JSON，如果对象过大可能影响性能</li>
 *   <li>敏感信息（如密码、密钥）不会被自动脱敏，需要手动处理</li>
 *   <li>对于包含循环引用的对象，序列化可能失败</li>
 *   <li>不建议在包含敏感数据的接口中使用 RESULT 输出</li>
 * </ul>
 *
 * @see org.example.myextension.logger.annotation.MethodLogger 方法日志注解
 */
public enum LogOutput {
    /**
     * 记录方法调用时的入参
     * <p>
     * 将方法的调用参数序列化为 JSON 格式记录到日志中。
     * 便于追踪和分析方法调用时的输入数据。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>参数对象会被完整序列化，可能包含敏感信息</li>
     *   <li>大对象或深度嵌套的对象可能影响序列化性能</li>
     *   <li>包含循环引用的对象可能导致序列化失败</li>
     * </ul>
     */
    PARAMS,

    /**
     * 记录方法执行后的返回值
     * <p>
     * 将方法的返回值序列化为 JSON 格式记录到日志中。
     * 便于追踪和分析方法执行后的输出数据。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>返回值对象会被完整序列化，可能包含敏感信息</li>
     *   <li>大对象或深度嵌套的对象可能影响序列化性能</li>
     *   <li>包含循环引用的对象可能导致序列化失败</li>
     *   <li>对于 void 方法，此选项无效</li>
     * </ul>
     */
    RESULT
}
