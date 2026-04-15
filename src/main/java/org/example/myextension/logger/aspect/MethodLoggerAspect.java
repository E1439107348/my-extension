package org.example.myextension.logger.aspect;

import com.alibaba.fastjson2.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.myextension.logger.annotation.MethodLogger;
import org.example.myextension.logger.enums.LogOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 方法日志记录切面
 * <p>
 * 使用 Spring AOP 拦截带有 {@link MethodLogger} 注解的方法，
 * 在方法执行前后记录日志，包括入参、返回值、执行耗时等信息。
 * <p>
 * <b>切面功能：</b>
 * <ul>
 *   <li>方法调用开始时记录入参（可选）</li>
 *   <li>方法执行成功时记录返回值（可选）</li>
 *   <li>自动计算并记录方法执行耗时</li>
 *   <li>方法执行异常时记录错误堆栈</li>
 * </ul>
 * <p>
 * <b>日志格式：</b>
 * <pre>
 * // 方法调用开始
 * Invoke method start, name: {标题}[{方法签名}], args: {入参JSON}
 *
 * // 方法执行成功
 * Invoke method end, name: {标题}, times: {耗时} ms, result: {返回值JSON}
 *
 * // 方法执行异常
 * Invoke method end with exception, name: {标题}[{方法签名}], times: {耗时} ms
 * {异常堆栈}
 * </pre>
 * <p>
 * <b>工作流程：</b>
 * <ol>
 *   <li>拦截带有 {@code MethodLogger} 注解的方法</li>
 *   <li>解析注解配置，确定需要记录的日志内容</li>
   *   <li>记录方法调用开始日志（包含入参）</li>
 *   <li>执行目标方法</li>
 *   <li>根据执行结果记录相应日志（成功记录返回值，失败记录异常）</li>
 *   <li>返回方法执行结果或抛出原始异常</li>
 * </ol>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>入参和返回值会被序列化为 JSON，如果对象过大可能影响性能</li>
 *   <li>对于循环引用的对象，序列化可能失败</li>
 *   <li>敏感信息（如密码、密钥）不会被自动脱敏</li>
 *   <li>高频调用方法建议谨慎使用</li>
 * </ul>
 *
 * @see MethodLogger 方法日志注解
 * @see LogOutput 日志输出范围枚举
 */
@Component
@Aspect
public class MethodLoggerAspect {
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(MethodLoggerAspect.class);

    /**
     * 定义切点：拦截所有带有 {@link MethodLogger} 注解的方法
     */
    @Pointcut("@annotation(org.example.myextension.logger.annotation.MethodLogger)")
    public void logPoint() {}

    /**
     * 环绕通知：拦截方法执行并记录日志
     * <p>
     * 此方法在目标方法执行前后都被调用，负责：
     * <ul>
     *   <li>记录方法调用开始日志</li>
     *   <li>执行目标方法</li>
     *   <li>记录方法执行结果日志</li>
     *   <li>处理方法执行异常</li>
     * </ul>
     *
     * @param point 连接点，包含被拦截方法的信息和调用参数
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("logPoint()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 获取方法签名，用于日志记录
        String methodName = point.getSignature().toShortString();

        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        MethodLogger annotation = signature.getMethod().getAnnotation(MethodLogger.class);

        // 获取日志输出配置
        List<LogOutput> output = Arrays.asList(annotation.output());

        // 记录开始时间，用于计算执行耗时
        long startTime = System.currentTimeMillis();

        try {
            // 记录入参日志（如果配置了输出参数）
            String argsJson = output.contains(LogOutput.PARAMS) ? JSON.toJSONString(point.getArgs()) : "-";
            log.info("方法调用开始，名称：{}[{}]，入参：{}", annotation.title(), methodName, argsJson);

            // 执行目标方法
            Object proceed = point.proceed(point.getArgs());

            // 记录返回值日志（如果配置了输出结果）
            String resultJson = output.contains(LogOutput.RESULT) ? JSON.toJSONString(proceed) : "-";
            log.info("方法执行结束，名称：{}，耗时：{} ms，返回：{}",
                    annotation.title(), (System.currentTimeMillis() - startTime), resultJson);

            return proceed;
        } catch (Throwable throwable) {
            // 记录异常日志
            log.error("方法执行异常结束，名称：{}[{}]，耗时：{} ms",
                    annotation.title(), methodName, (System.currentTimeMillis() - startTime), throwable);
            throw throwable;
        }
    }
}
