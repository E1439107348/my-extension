package org.example.myextension.limiter;

import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * <p>
 * 用于标记需要进行限流控制的方法。通过此注解可以声明式的配置限流规则，
 * 结合 {@link RateLimitAspect} 实现分布式限流功能。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *   <li>支持基于 Redis 的分布式限流</li>
 *   <li>支持 SpEL 表达式动态生成限流 Key</li>
 *   <li>支持两种限流模式：整体限流和实例限流</li>
 *   <li>可自定义限流速率和时间间隔</li>
 *   <li>限流时抛出 {@link RateLimitException}</li>
 * </ul>
 * <p>
 * <b>工作原理：</b>
 * <ol>
 *   <li>拦截带有此注解的方法</li>
 *   <li>解析限流 Key（支持 SpEL 表达式）</li>
 *   <li>根据 Key 获取或创建限流器</li>
 *   <li>尝试获取访问令牌</li>
 *   <li>获取成功则执行方法，否则抛出限流异常</li>
 * </ol>
 * <p>
 * <b>限流算法：</b>
 * 基于 Redisson 的 {@code RRateLimiter} 实现，使用令牌桶算法。
 * 算法特点：
 * <ul>
 *   <li>平滑限流：可以处理突发流量</li>
 *   <li>分布式支持：基于 Redis 实现，适用于多实例场景</li>
 *   <li>高性能：使用 Redis 高效实现</li>
 * </ul>
 * <p>
 * <b>限流模式说明：</b>
 * <table border="1">
 *   <tr><th>模式</th><th>说明</th><th>适用场景</th></tr>
 *   <tr><td>OVERALL</td><td>整体限流</td><td>所有请求共享限流配额</td></tr>
 *   <tr><td>PER_CLIENT</td><td>按客户端限流</td><td>不同客户端独立限流</td></tr>
 *   <tr><td>PER_INSTANCE</td><td>按实例限流</td><td>不同服务实例独立限流</td></tr>
 * </table>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * // 示例 1：简单的固定 Key 限流
 * // 限制：每秒最多 10 次请求
 * &#64;RateLimit(key = "api:order:create", rate = 10, rateInterval = 1, rateIntervalUnit = RateIntervalUnit.SECONDS)
 * public void createOrder(CreateOrderRequest request) {
 *     // 方法实现
 * }
 *
 * // 示例 2：使用 SpEL 表达式动态 Key（按用户限流）
 * // 限制：每个用户每分钟最多 100 次请求
 * &#64;RateLimit(key = "api:user:query:#userId", rate = 100, rateInterval = 1, rateIntervalUnit = RateIntervalUnit.MINUTES)
 * public User getUser(Long userId) {
 *     // 方法实现
 * }
 *
 * // 示例 3：组合 SpEL 表达式（按用户和接口限流）
 * // 限制：每个用户每个接口每分钟最多 50 次请求
 * &#64;RateLimit(key = "api:#userId:#apiName", rate = 50, rateInterval = 1, rateIntervalUnit = RateIntervalUnit.MINUTES)
 * public void callApi(Long userId, String apiName) {
 *     // 方法实现
 * }
 *
 * // 示例 4：按客户端限流模式
 * // 限制：每个客户端每秒最多 5 次请求
 * &#64;RateLimit(key = "api:upload", mode = RateType.PER_CLIENT, rate = 5, rateInterval = 1, rateIntervalUnit = RateIntervalUnit.SECONDS)
 * public void uploadFile(UploadRequest request) {
 *     // 方法实现
 * }
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>限流基于 Redis 实现，需要确保 Redis 可用</li>
 *   <li>限流器会缓存在内存中，避免重复创建</li>
 *   <li>限流失败抛出 {@link RateLimitException}，需要全局异常处理器处理</li>
 *   <li>SpEL 表达式中的变量名必须与方法参数名一致</li>
 *   <li>建议为高频接口添加限流，防止系统过载</li>
 * </ul>
 * <p>
 * <b>SpEL 表达式说明：</b>
 * <ul>
 *   <li>格式：以 {@code #} 开头，如 {@code #userId}</li>
 *   <li>变量：引用方法参数的变量名，如 {@code args[0]} 或参数名 {@code userId}</li>
 *   <li>支持：基本类型、对象、集合等</li>
 *   <li>示例：{@code #userId}、{@code #order.id}、{@code #user.name}</li>
 * </ul>
 * <p>
 * <b>异常处理：</b>
 * <pre>
 * // 全局异常处理器示例
 * &#64;RestControllerAdvice
 * public class GlobalExceptionHandler {
 *
 *     &#64;ExceptionHandler(RateLimitException.class)
 *     public ApiResult&lt;Void&gt; handleRateLimit(RateLimitException e) {
 *         return ApiResult.fail(429, e.getMessage());
 *     }
 * }
 * </pre>
 *
 * @see RateLimitAspect 限流切面
 * @see RateLimitException 限流异常
 * @see org.redisson.api.RRateLimiter Redisson 限流器
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * 限流键
     * <p>
     * 用于标识限流规则的唯一 Key，支持：
     * <ul>
     *   <li>固定字符串：如 {@code "api:order:create"}</li>
     *   <li>SpEL 表达式：如 {@code "api:user:#userId"}</li>
     * </ul>
     * <p>
     * <b>命名建议：</b>
     * <ul>
     *   <li>使用冒号分隔层级：{@code 模块:功能:标识}</li>
     *   <li>使用小写和下划线：{@code api:user:query}</li>
     *   <li>包含业务含义：{@code order:create, user:query}</li>
     * </ul>
     *
     * @return 限流 Key
     */
    String key();

    /**
     * 限流模式
     * <p>
     * 指定限流的作用范围，默认为 {@link org.redisson.api.RateType#OVERALL}。
     * <p>
     * <b>模式说明：</b>
     * <ul>
     *   <li>{@code OVERALL}：整体限流，所有请求共享配额</li>
     *   <li>{@code PER_CLIENT}：按客户端限流，不同客户端独立配额</li>
     *   <li>{@code PER_INSTANCE}：按实例限流，不同服务实例独立配额</li>
     * </ul>
     *
     * @return 限流模式
     */
    RateType mode() default RateType.OVERALL;

    /**
     * 限流速率
     * <p>
     * 指定在限流间隔内允许的最大请求数量。
     * 例如 rate=10 表示在指定时间间隔内最多允许 10 次请求。
     *
     * @return 限流速率（次数）
     */
    long rate();

    /**
     * 限流速率间隔
     * <p>
     * 指定限流的时间间隔长度，与 {@code rateIntervalUnit} 配合使用。
     * 例如 rate=10, rateInterval=1, rateIntervalUnit=SECONDS 表示每秒最多 10 次请求。
     *
     * @return 时间间隔数值
     */
    long rateInterval();

    /**
     * 限流速率间隔单位
     * <p>
     * 指定 {@code rateInterval} 的时间单位，支持：
     * <ul>
     *   <li>{@link org.redisson.api.RateIntervalUnit#SECONDS}：秒</li>
     *   <li>{@link org.redisson.api.RateIntervalUnit#MILLISECONDS}：毫秒</li>
     *   <li>{@link org.redisson.api.RateIntervalUnit#MICROSECONDS}：微秒</li>
     *   <li>{@link org.redisson.api.RateIntervalUnit#MINUTES}：分钟</li>
     *   <li>{@link org.redisson.api.RateIntervalUnit#HOURS}：小时</li>
     *   <li>{@link org.redisson.api.RateIntervalUnit#DAYS}：天</li>
     * </ul>
     *
     * @return 时间单位
     */
    RateIntervalUnit rateIntervalUnit();

    /**
     * 异常提示语
     * <p>
     * 限流失败时抛出的异常消息，用于告知用户限流原因。
     * 默认值为"请求过于频繁，请稍后再试"。
     *
     * @return 异常提示语
     */
    String throwsDesc() default "请求过于频繁，请稍后再试";
}
