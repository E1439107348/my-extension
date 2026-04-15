package org.example.myextension.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 健康检查结果模型
 * <p>
 * 封装单个健康检查器的检查结果，包括检查项名称、状态、消息和执行耗时。
 * <p>
 * <b>状态说明：</b>
 * <ul>
 *   <li>{@link Status#UP}：表示检查通过，服务健康</li>
 *   <li>{@link Status#DOWN}：表示检查失败，服务不可用</li>
 * </ul>
 * <p>
 * <b>使用方式：</b>
 * <pre>
 * // 创建健康结果（UP）
 * HealthCheckResult result = HealthCheckResult.up("MySQL", 10, "MySQL 连接正常");
 *
 * // 创建健康结果（DOWN）
 * HealthCheckResult result = HealthCheckResult.down("Redis", 5000, "连接超时");
 *
 * // 判断是否健康
 * if (result.isHealthy()) {
 *     // 处理健康状态
 * }
 * </pre>
 * <p>
 * <b>设计说明：</b>
 * 使用静态工厂方法创建实例，避免直接使用构造函数，确保结果对象的一致性。
 *
 * @see HealthChecker 健康检查器接口
 * @see HealthCheckRunner 健康检查执行器
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HealthCheckResult {
    /**
     * 检查项名称
     * <p>
     * 标识本次健康检查的目标，如 "MySQL"、"Redis"、"ElasticSearch" 等。
     */
    private String name;

    /**
     * 检查状态
     * <p>
     * 表示健康检查的结果状态，UP 表示健康，DOWN 表示异常。
     */
    private Status status;

    /**
     * 检查消息
     * <p>
     * 描述健康检查的详细信息或失败原因。
     * 成功时通常为 "连接正常"，失败时描述具体的错误信息。
     */
    private String message;

    /**
     * 检查耗时（毫秒）
     * <p>
     * 表示本次健康检查的执行时间，用于监控检查器的性能。
     */
    private long duration;

    /**
     * 健康状态枚举
     * <p>
     * 定义健康检查的两种状态：UP 和 DOWN。
     */
    public enum Status {
        /**
         * 健康/上线
         * <p>
         * 表示服务运行正常，健康检查通过。
         */
        UP,

        /**
         * 不健康/下线
         * <p>
         * 表示服务异常或不可用，健康检查失败。
         */
        DOWN
    }

    /**
     * 判断是否健康
     * <p>
     * 简便的方法，用于快速判断检查结果是否为 UP 状态。
     *
     * @return true 表示健康（UP），false 表示不健康（DOWN）
     */
    public boolean isHealthy() {
        return this.status == Status.UP;
    }

    /**
     * 创建健康的结果（UP）
     * <p>
     * 静态工厂方法，用于创建表示健康状态的检查结果。
     *
     * @param name     检查项名称
     * @param duration  检查耗时（毫秒）
     * @param message   检查消息，通常为"连接正常"
     * @return UP 状态的健康检查结果
     */
    public static HealthCheckResult up(String name, long duration, String message) {
        return new HealthCheckResult(name, Status.UP, message, duration);
    }

    /**
     * 创建不健康的结果（DOWN）
     * <p>
     * 静态工厂方法，用于创建表示异常状态的检查结果。
     *
     * @param name     检查项名称
     * @param duration  检查耗时（毫秒）
     * @param message   检查消息，描述失败原因
     * @return DOWN 状态的健康检查结果
     */
    public static HealthCheckResult down(String name, long duration, String message) {
        return new HealthCheckResult(name, Status.DOWN, message, duration);
    }
}
