package org.example.myextension.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 健康检查执行器
 * <p>
 * 负责在应用启动完成后自动执行所有注册的健康检查器，
 * 收集检查结果并输出详细的日志报告。
 * <p>
 * <b>主要职责：</b>
 * <ul>
 *   <li>自动收集所有实现了 {@link HealthChecker} 接口的 Spring Bean</li>
 *   <li>监听 {@link ApplicationReadyEvent} 事件，在应用就绪时触发检查</li>
 *   <li>依次执行所有检查器，收集检查结果</li>
 *   <li>格式化输出检查结果日志，使用符号标识健康状态</li>
 *   <li>统计检查通过/失败的数量</li>
 * </ul>
 * <p>
 * <b>执行时机：</b>
 * 在 Spring Boot 应用的 {@code ApplicationReadyEvent} 事件触发时执行，
 * 即应用初始化完成、所有 Bean 已准备就绪之后。
 * <p>
 * <b>日志格式：</b>
 * <pre>
 * ========== 开始执行健康检查 ==========
 * [✓] MySQL - UP (10ms) MySQL 连接正常
 * [✗] Redis - DOWN (5000ms) 连接超时
 * ========== 健康检查结束: 通过 1/2 ==========
 * </pre>
 * <p>
 * <b>日志说明：</b>
 * <ul>
 *   <li>✓ 表示检查通过（UP）</li>
 *   <li>✗ 表示检查失败（DOWN）</li>
 *   <li>显示检查项名称、状态、耗时和详细消息</li>
 *   <li>最后汇总通过数量和总数量</li>
 * </ul>
 * <p>
 * <b>异常处理：</b>
 * <ul>
 *   <li>单个检查器抛出异常不会中断整体检查流程</li>
 *   <li>异常被捕获并转换为 DOWN 状态的检查结果</li>
 *   <li>异常信息会记录到日志中，便于排查问题</li>
 * </ul>
 * <p>
 * <b>使用方式：</b>
 * <pre>
 * // 1. 实现健康检查器
 * &#64;Component
 * public class MyServiceHealthChecker implements HealthChecker {
 *     &#64;Override
 *     public String getName() {
 *         return "MyService";
 *     }
 *
 *     &#64;Override
 *     public HealthCheckResult check() {
 *         // 检查逻辑
 *         return HealthCheckResult.up(getName(), 10, "服务连接正常");
 *     }
 * }
 *
 * // 2. 应用启动后自动执行
 * // 无需手动调用，HealthCheckRunner 会在应用就绪时自动执行所有检查器
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>健康检查失败不会阻止应用启动，仅记录日志</li>
 *   <li>检查器按 Spring Bean 的注册顺序依次执行</li>
 *   <li>建议为关键服务实现健康检查，便于监控和告警</li>
 *   <li>检查器应该快速响应，避免阻塞应用启动</li>
 * </ul>
 *
 * @see HealthChecker 健康检查器接口
 * @see HealthCheckResult 健康检查结果
 */
@Slf4j
@Component
public class HealthCheckRunner {

    /**
     * 所有已注册的健康检查器列表
     * <p>
     * 通过构造函数依赖注入自动收集所有实现了 {@link HealthChecker} 接口的 Spring Bean。
     */
    private final List<HealthChecker> healthCheckers;

    /**
     * 构造函数：通过依赖注入获取所有健康检查器
     * <p>
     * Spring 自动收集所有实现了 {@link HealthChecker} 接口的 Bean，
     * 并以列表形式注入到此构造函数中。
     *
     * @param healthCheckers 所有已注册的健康检查器
     */
    public HealthCheckRunner(List<HealthChecker> healthCheckers) {
        this.healthCheckers = healthCheckers;
    }

    /**
     * 应用就绪事件监听器
     * <p>
     * 当 Spring Boot 应用完成初始化、所有 Bean 准备就绪后触发此方法。
     * 此时执行所有健康检查器，检查依赖服务的可用性。
     *
     * @param event 应用就绪事件
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 检查是否有注册的健康检查器
        if (healthCheckers == null || healthCheckers.isEmpty()) {
            log.info("未发现任何健康检查器");
            return;
        }

        log.info("========== 开始执行健康检查 ==========");

        // 依次执行所有健康检查器，并捕获异常
        List<HealthCheckResult> results = healthCheckers.stream().map(checker -> {
            try {
                return checker.check();
            } catch (Exception e) {
                // 捕获异常，转换为 DOWN 状态的结果
                log.error("执行健康检查 [{}] 时发生异常: {}", checker.getName(), e.getMessage());
                return HealthCheckResult.down(checker.getName(), 0, "检查异常: " + e.getMessage());
            }
        }).collect(Collectors.toList());

        // 输出每个检查器的结果
        results.forEach(result -> {
            String statusIcon = result.isHealthy() ? "✓" : "✗";
            String statusText = result.isHealthy() ? "UP" : "DOWN";
            log.info("[{}] {} - {} ({}ms) {}",
                    statusIcon, result.getName(), statusText, result.getDuration(),
                    result.getMessage() != null ? result.getMessage() : "");
        });

        // 统计并输出汇总信息
        long passed = results.stream().filter(HealthCheckResult::isHealthy).count();
        log.info("========== 健康检查结束: 通过 {}/{} ==========", passed, results.size());
    }
}
