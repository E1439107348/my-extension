package org.example.myextension.logger.aspect;

import com.alibaba.fastjson2.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.myextension.logger.annotation.OpenApiLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 开放接口日志记录切面
 * <p>
 * 使用 Spring AOP 拦截带有 {@link OpenApiLog} 注解的方法，
 * 在方法执行前后记录开放 API 调用日志，包括接口信息、客户端 IP、请求参数、响应结果等。
 * <p>
 * <b>切面功能：</b>
 * <ul>
 *   <li>记录接口名称、版本和 URL</li>
 *   <li>自动获取客户端真实 IP 地址（支持代理和负载均衡）</li>
 *   <li>记录接口请求参数</li>
 *   <li>记录接口响应结果</li>
 *   <li>自动计算并记录接口执行耗时</li>
 *   <li>捕获接口执行异常并记录错误信息</li>
 * </ul>
 * <p>
 * <b>日志格式：</b>
 * <pre>
 * // 接口调用成功
 * OpenApi
 * API Name: {接口名称}
 * API Version: {接口版本}
 * API Url: {接口URL}
 * ClientIP: {客户端IP}
 * Times: {耗时} ms
 * Args: {请求参数JSON}
 * Result: {响应结果JSON}
 *
 * // 接口调用异常
 * OpenApi Error
 * API Name: {接口名称}
 * API Version: {接口版本}
 * API Url: {接口URL}
 * ClientIP: {客户端IP}
 * Times: {耗时} ms
 * Args: {请求参数JSON}
 * {异常堆栈}
 * </pre>
 * <p>
 * <b>IP 获取策略：</b>
 * 支持多种代理和负载均衡场景的 IP 获取，按优先级依次检查：
 * <ol>
 *   <li>{@code X-Real-IP}：Nginx 等代理通常使用此请求头传递真实 IP</li>
 *   <li>{@code X-Forwarded-For}：支持多级代理，取第一个 IP 作为客户端 IP</li>
 *   <li>{@code request.getRemoteAddr()}：作为备选，获取直接连接的 IP</li>
 * </ol>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>请求参数和响应结果会被序列化为 JSON，如果对象过大可能影响性能</li>
 *   <li>敏感信息（如密码、密钥）不会被自动脱敏，需要手动处理</li>
 *   <li>高频调用的接口建议谨慎使用以避免日志量过大</li>
 *   <li>IP 获取依赖于请求头配置，请确保代理服务器正确传递原始 IP</li>
 * </ul>
 *
 * @see OpenApiLog 开放接口日志注解
 */
@Aspect
@Component
public class OpenApiLogAspect {
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(OpenApiLogAspect.class);

    /**
     * 定义切点：拦截所有带有 {@link OpenApiLog} 注解的方法
     */
    @Pointcut("@annotation(org.example.myextension.logger.annotation.OpenApiLog)")
    public void logPoint() {}

    /**
     * 环绕通知：拦截接口调用并记录日志
     * <p>
     * 此方法在目标方法执行前后都被调用，负责：
     * <ul>
     *   <li>获取客户端 IP 地址</li>
     *   <li>执行目标方法</li>
     *   <li>根据执行结果记录相应的日志</li>
     *   <li>处理接口执行异常</li>
     * </ul>
     *
     * @param point 连接点，包含被拦截方法的信息和调用参数
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("logPoint()")
    public Object before(ProceedingJoinPoint point) throws Throwable {
        // 记录开始时间，用于计算执行耗时
        long start = System.currentTimeMillis();

        // 获取方法签名和注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        OpenApiLog annotation = signature.getMethod().getAnnotation(OpenApiLog.class);

        // 获取客户端 IP
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String ip = (attributes != null) ? getClientIp(attributes.getRequest()) : "*";

        try {
            // 执行目标方法
            Object result = point.proceed(point.getArgs());

            // 记录成功日志（模拟 Kotlin trimIndent 的格式化输出）
            log.info("\n开放接口调用\n接口名称: {}\n接口版本: {}\n接口地址: {}\n客户端IP: {}\n耗时: {} ms\n请求参数: {}\n响应: {}",
                    annotation.name(), annotation.version(), annotation.url(), ip,
                    (System.currentTimeMillis() - start), JSON.toJSONString(point.getArgs()), JSON.toJSONString(result));
            return result;
        } catch (Throwable e) {
            // 记录异常日志
            log.error("\n开放接口异常\n接口名称: {}\n接口版本: {}\n接口地址: {}\n客户端IP: {}\n耗时: {} ms\n请求参数: {}",
                    annotation.name(), annotation.version(), annotation.url(), ip,
                    (System.currentTimeMillis() - start), JSON.toJSONString(point.getArgs()), e);
            throw e;
        }
    }

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 支持多种代理和负载均衡场景的 IP 获取，按优先级依次检查：
     * <ol>
     *   <li>检查 {@code X-Real-IP} 请求头</li>
     *   <li>检查 {@code X-Forwarded-For} 请求头（取第一个 IP）</li>
     *   <li>使用 {@code request.getRemoteAddr()} 作为备选</li>
     * </ol>
     * <p>
     * <b>X-Forwarded-For 说明：</b>
     * 此请求头格式为：{@code client, proxy1, proxy2}
     * 第一个 IP 是客户端真实 IP，后续是代理服务器的 IP。
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 1. 检查 X-Real-IP 请求头（Nginx 等代理通常使用）
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 2. 检查 X-Forwarded-For 请求头（支持多级代理）
        ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            return (index != -1) ? ip.substring(0, index) : ip;
        }

        // 3. 使用 getRemoteAddr 作为备选
        return request.getRemoteAddr();
    }
}
