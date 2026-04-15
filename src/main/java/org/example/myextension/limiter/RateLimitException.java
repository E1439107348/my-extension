package org.example.myextension.limiter;

/**
 * 限流异常
 * <p>
 * 当请求超过限流阈值时抛出此异常。
 * 通常由 {@link RateLimitAspect} 在检测到请求频率超过限制时抛出。
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *   <li>API 请求过于频繁</li>
 *   <li>用户操作过快</li>
 *   <li>接口被恶意调用</li>
 * </ul>
 * <p>
 * <b>异常处理：</b>
 * <pre>
 * // 全局异常处理器示例
 * &#64;RestControllerAdvice
 * public class GlobalExceptionHandler {
 *
 *     &#64;ExceptionHandler(RateLimitException.class)
 *     public ResponseEntity&lt;ApiResult&lt;Void&gt;&gt; handleRateLimit(RateLimitException e) {
 *         // 返回 429 状态码（Too Many Requests）
 *         return ResponseEntity
 *                 .status(HttpStatus.TOO_MANY_REQUESTS)
 *                 .body(ApiResult.fail(429, e.getMessage()));
 *     }
 * }
 * </pre>
 * <p>
 * <b>HTTP 状态码说明：</b>
 * <ul>
 *   <li>429 Too Many Requests：表示用户在给定的时间内发送了太多的请求</li>
 *   <li>这是 HTTP 标准状态码，客户端应识别并等待后重试</li>
 * </ul>
 * <p>
 * <b>客户端处理建议：</b>
 * <ul>
 *   <li>收到 429 状态码后，等待一段时间再重试</li>
 *   <li>使用指数退避策略，避免立即重试</li>
 *   <li>在 UI 中显示友好的限流提示信息</li>
 *   <li>记录限流事件，便于分析和优化</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>此异常继承自 {@link RuntimeException}，是未检查异常</li>
 *   <li>需要在全局异常处理器中处理，避免返回 500 错误</li>
 *   <li>异常消息由 {@link RateLimit} 注解的 {@code throwsDesc} 指定</li>
 *   <li>建议在日志中记录限流事件，便于监控和分析</li>
 * </ul>
 *
 * @see RateLimit 限流注解
 * @see RateLimitAspect 限流切面
 */
public class RateLimitException extends RuntimeException {
    /**
     * 构造函数：指定异常消息
     *
     * @param message 异常消息，描述限流原因
     */
    public RateLimitException(String message) {
        super(message);
    }
}
