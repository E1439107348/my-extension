package org.example.myextension.health;

/**
 * 健康检查器接口
 * <p>
 * 定义健康检查器的规范，所有具体的健康检查实现都需要实现此接口。
 * 健康检查器用于在应用启动时检查各种依赖服务（如数据库、缓存、消息队列等）的可用性。
 * <p>
 * <b>设计模式：</b>
 * 采用策略模式，每个健康检查器实现自己的检查逻辑。
 * {@link HealthCheckRunner} 通过此接口统一管理所有检查器。
 * <p>
 * <b>工作流程：</b>
 * <ol>
 *   <li>应用启动时，Spring 自动收集所有实现了此接口的 Bean</li>
 *   <li>{@link HealthCheckRunner} 触发 {@link ApplicationReadyEvent} 事件</li>
 *   <li>依次调用每个检查器的 {@code check()} 方法</li>
 *   <li>收集并汇总所有检查结果，输出到日志</li>
 * </ol>
 * <p>
 * <b>实现示例：</b>
 * <pre>
 * &#64;Component
 * public class MyServiceHealthChecker implements HealthChecker {
 *
 *     &#64;Override
 *     public String getName() {
 *         return "MyService";
 *     }
 *
 *     &#64;Override
 *     public HealthCheckResult check() {
 *         long startTime = System.currentTimeMillis();
 *         try {
 *             // 执行具体的检查逻辑
 *             boolean healthy = myService.ping();
 *             long duration = System.currentTimeMillis() - startTime;
 *
 *             if (healthy) {
 *                 return HealthCheckResult.up(getName(), duration, "服务连接正常");
 *             } else {
 *                 return HealthCheckResult.down(getName(), duration, "服务不可用");
 *             }
 *         } catch (Exception e) {
 *             long duration = System.currentTimeMillis() - startTime;
 *             return HealthCheckResult.down(getName(), duration, "检查异常: " + e.getMessage());
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>检查逻辑应该快速响应，避免阻塞应用启动</li>
 *   <li>建议设置超时时间，防止某个检查器长时间阻塞</li>
 *   <li>检查失败不应导致应用启动失败，只记录日志</li>
 *   <li>异常应该被捕获并转换为 DOWN 状态的结果</li>
 * </ul>
 * <p>
 * <b>常见的健康检查实现：</b>
 * <ul>
 *   <li>{@link MysqlHealthChecker}：MySQL 数据库健康检查</li>
 *   <li>{@link RedisHealthChecker}：Redis 缓存健康检查</li>
 *   <li>其他：ElasticSearch、消息队列、第三方 API 等</li>
 * </ul>
 *
 * @see HealthCheckResult 健康检查结果
 * @see HealthCheckRunner 健康检查执行器
 * @see MysqlHealthChecker MySQL 健康检查实现
 * @see RedisHealthChecker Redis 健康检查实现
 */
public interface HealthChecker {
    /**
     * 获取检查器名称
     * <p>
     * 返回此健康检查器的标识名称，用于在日志中区分不同的检查项。
     * <p>
     * <b>命名建议：</b>
     * 使用简短、清晰的服务名称，如 "MySQL"、"Redis"、"ES" 等。
     *
     * @return 检查器名称
     */
    String getName();

    /**
     * 执行健康检查
     * <p>
     * 实现具体的健康检查逻辑，检查所依赖的服务是否可用。
     * <p>
     * <b>实现要求：</b>
     * <ul>
     *   <li>捕获所有异常，不应向外抛出未处理的异常</li>
     *   <li>记录检查的执行耗时</li>
     *   <li>根据检查结果返回 UP 或 DOWN 状态</li>
     *   <li>提供清晰的错误消息，便于排查问题</li>
     * </ul>
     * <p>
     * <b>常见检查方式：</b>
     * <ul>
     *   <li>数据库：执行简单查询（SELECT 1）</li>
     *   <li>缓存：写入和读取测试数据</li>
     *   <li>HTTP API：发送心跳请求</li>
     *   <li>消息队列：发送和接收测试消息</li>
     * </ul>
     *
     * @return 健康检查结果，包含状态、消息和耗时
     */
    HealthCheckResult check();
}
