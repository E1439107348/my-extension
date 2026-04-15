package org.example.myextension.limiter;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * 限流切面
 * <p>
 * 使用 Spring AOP 拦截带有 {@link RateLimit} 注解的方法，
 * 实现基于 Redis 的分布式限流功能。
 * <p>
 * <b>核心功能：</b>
 * <ul>
 *   <li>拦截带有 {@code RateLimit} 注解的方法</li>
 *   <li>支持 SpEL 表达式动态生成限流 Key</li>
 *   <li>基于 Redisson 的 {@code RRateLimiter} 实现分布式限流</li>
 *   <li>缓存限流器实例，避免重复创建</li>
 *   <li>限流失败时抛出 {@link RateLimitException}</li>
 * </ul>
 * <p>
 * <b>工作流程：</b>
 * <ol>
 *   <li>拦截方法调用，获取 {@code RateLimit} 注解配置</li>
 *   <li>解析限流 Key（支持 SpEL 表达式）</li>
 *   <li>根据 Key 获取或创建 {@code RRateLimiter} 实例</li>
 *   <li>尝试获取访问令牌</li>
 *   <li>获取成功则执行方法，否则抛出限流异常</li>
 * </ol>
 * <p>
 * <b>限流算法：</b>
 * 基于 Redisson 的令牌桶算法实现：
 * <ul>
 *   <li>桶中存放固定数量的令牌</li>
 *   <li>每次请求消耗一个令牌</li>
 *   <li>令牌按固定速率补充到桶中</li>
 *   <li>请求速率超过补充速率时触发限流</li>
 * </ul>
 * <p>
 * <b>SpEL 表达式解析：</b>
 * <p>
 * 支持动态生成限流 Key，根据方法参数值生成不同的 Key。
 * <p>
 * <b>示例：</b>
 * <ul>
 *   <li>固定字符串：{@code "api:order:create"}</li>
 *   <li>引用参数：{@code "api:user:#userId"}</li>
 *   <li>引用对象属性：{@code "api:order:#order.id"}</li>
 *   <li>组合表达式：{@code "api:#userId:#apiName"}</li>
 * </ul>
 * <p>
 * <b>限流器缓存：</b>
 * <p>
 * 使用 {@code ConcurrentHashMap} 缓存已创建的限流器实例，
 * 避免每次请求都重新创建限流器，提升性能。
 * <p>
 * <b>线程安全：</b>
 * <ul>
 *   <li>使用 {@code ConcurrentHashMap} 保证线程安全</li>
 *   <li>Redisson 的限流器本身是线程安全的</li>
 *   <li>支持高并发场景下的限流控制</li>
 * </ul>
 * <p>
 * <b>异常处理：</b>
 * <ul>
 *   <li>限流失败抛出 {@link RateLimitException}</li>
 *   <li>异常消息由注解的 {@code throwsDesc} 指定</li>
 *   <li>需要在全局异常处理器中处理</li>
 *   <li>建议返回 HTTP 429 状态码</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * // Controller 方法
 * &#64;RestController
 * public class OrderController {
 *
 *     // 简单限流：所有请求共享配额
 *     &#64;RateLimit(key = "api:order:create", rate = 10, rateInterval = 1, rateIntervalUnit = RateIntervalUnit.SECONDS)
 *     &#64;PostMapping("/orders")
 *     public ApiResult&lt;Order&gt; createOrder(@RequestBody CreateOrderRequest request) {
 *         // 方法实现
 *     }
 *
 *     // 按用户限流：每个用户独立配额
 *     &#64;RateLimit(key = "api:user:query:#userId", rate = 100, rateInterval = 1, rateIntervalUnit = RateIntervalUnit.MINUTES)
 *     &#64;GetMapping("/users/{userId}")
 *     public ApiResult&lt;User&gt; getUser(@PathVariable Long userId) {
 *         // 方法实现
 *     }
 * }
 * }
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>限流基于 Redis 实现，需要确保 Redis 可用</li>
 *   <li>限流器缓存会占用一定内存，需要考虑限流 Key 的数量</li>
 *   <li>SpEL 表达式中的变量名必须与方法参数名一致</li>
 *   <li>建议为高频接口添加限流，防止系统过载</li>
 *   <li>限流器实例一旦创建会持续存在，建议定期清理不使用的限流器</li>
 * </ul>
 * <p>
 * <b>性能考虑：</b>
 * <ul>
 *   <li>使用缓存避免重复创建限流器</li>
 *   <li>Redis 限流器实现高效，适合高并发场景</li>
 *   <li>SpEL 解析有一定开销，Key 中不包含 SpEL 时性能更优</li>
 * </ul>
 *
 * @see RateLimit 限流注解
 * @see RateLimitException 限流异常
 * @see org.redisson.api.RRateLimiter Redisson 限流器
 */
@Aspect
@Component
public class RateLimitAspect {

    /**
     * Redisson 客户端
     * <p>
     * 用于创建和管理限流器实例。
     */
    private final RedissonClient redisson;

    /**
     * 限流器缓存
     * <p>
     * 使用 ConcurrentHashMap 缓存已创建的限流器实例，
     * Key 为限流 Key，Value 为对应的限流器实例。
     * 避免每次请求都重新创建限流器，提升性能。
     */
    private final Map<String, RRateLimiter> rateLimiters = new ConcurrentHashMap<>();

    /**
     * SpEL 表达式解析器
     * <p>
     * 用于解析限流 Key 中的 SpEL 表达式。
     */
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 参数名发现器
     * <p>
     * 用于获取方法的参数名称，建立参数名到参数值的映射。
     */
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * SpEL 表达式匹配模式
     * <p>
     * 用于识别 Key 中的 SpEL 表达式（以 # 开头的部分）。
     * 例如：{@code "api:user:#userId"} 中的 {@code #userId}。
     */
    private final Pattern pattern = Pattern.compile("#[\\w.]+");

    /**
     * 构造函数：通过依赖注入初始化
     *
     * @param redisson Redisson 客户端
     */
    public RateLimitAspect(RedissonClient redisson) {
        this.redisson = redisson;
    }

    /**
     * 环绕通知：拦截方法调用并执行限流逻辑
     * <p>
     * 此方法在目标方法执行前被调用，负责：
     * <ul>
     *   <li>解析限流 Key</li>
     *   <li>获取或创建限流器</li>
     *   <li>尝试获取访问令牌</li>
     *   <li>根据结果执行方法或抛出异常</li>
     * </ul>
     *
     * @param point      连接点，包含被拦截方法的信息和调用参数
     * @param rateLimit 限流注解
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的异常或限流异常
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        // 解析限流 Key（支持 SpEL 表达式）
        String key = generateKey(point, rateLimit);

        // 获取或创建限流器
        RRateLimiter rateLimiter = rateLimiters.computeIfAbsent("RATE_LIMIT::" + key, k -> {
            RRateLimiter limiter = redisson.getRateLimiter(k);
            // 设置限流规则：模式、速率、时间间隔
            limiter.trySetRate(
                    rateLimit.mode(),
                    rateLimit.rate(),
                    rateLimit.rateInterval(),
                    rateLimit.rateIntervalUnit()
            );
            return limiter;
        });

        // 尝试获取访问令牌
        if (!rateLimiter.tryAcquire()) {
            // 令牌获取失败，抛出限流异常
            throw new RateLimitException(rateLimit.throwsDesc());
        }

        // 令牌获取成功，执行目标方法
        return point.proceed();
    }

    /**
     * 解析 SpEL 表达式生成动态 Key
     * <p>
     * 支持在限流 Key 中使用 SpEL 表达式，根据方法参数值动态生成 Key。
     * <p>
     * <b>解析流程：</b>
     * <ol>
     *   <li>检查 Key 是否包含 SpEL 表达式（# 开头）</li>
     *   <li>如果不包含，直接返回 Key</li>
     *   <li>如果包含，获取方法签名和参数</li>
     *   <li>建立 SpEL 上下文，设置参数变量</li>
     *   <li>解析每个 SpEL 表达式，替换为实际值</li>
     *   <li>返回解析后的 Key</li>
     * </ol>
     * <p>
     * <b>示例：</b>
     * <ul>
     *   <li>输入：{@code "api:user:#userId"}，参数 userId=123</li>
     *   <li>输出：{@code "api:user:123"}</li>
     * </ul>
     *
     * @param joinPoint  连接点，包含方法信息和参数
     * @param rateLimit 限流注解
     * @return 解析后的限流 Key
     */
    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String input = rateLimit.key();

        // 检查是否包含 SpEL 表达式
        if (!input.contains("#")) {
            return input;
        }

        // 获取方法签名和参数
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        // 建立 SpEL 上下文，设置参数变量
        StandardEvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        // 解析 SpEL 表达式并替换
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String group = matcher.group();
            // 解析表达式并获取值
            Expression expression = parser.parseExpression(group.substring(1));
            Object value = expression.getValue(context);
            // 替换为实际值
            matcher.appendReplacement(sb, value != null ? value.toString() : "");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
