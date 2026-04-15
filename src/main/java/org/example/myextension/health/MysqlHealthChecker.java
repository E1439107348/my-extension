package org.example.myextension.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MySQL 数据库健康检查器
 * <p>
 * 实现 {@link HealthChecker} 接口，用于检查 MySQL 数据库的连接状态和可用性。
 * <p>
 * <b>检查原理：</b>
 * 通过 MyBatis 执行简单的 {@code SELECT 1} 查询，验证：
 * <ul>
 *   <li>数据库连接池是否可用</li>
 *   <li>数据库服务是否正常运行</li>
 *   <li>网络连接是否畅通</li>
 *   <li>基本查询功能是否正常</li>
 * </ul>
 * <p>
 * <b>检查流程：</b>
 * <ol>
 *   <li>记录开始时间</li>
 *   <li>调用 {@link HealthCheckerMapper#healthCheck()} 执行查询</li>
 *   <li>检查返回值是否为 1</li>
 *   <li>根据结果生成健康检查报告</li>
 *   <li>记录执行耗时</li>
 * </ol>
 * <p>
 * <b>检查结果：</b>
 * <ul>
 *   <li>UP 状态：查询成功且返回值为 1，表示数据库连接正常</li>
 *   <li>DOWN 状态：查询抛出异常或返回值不为 1，表示数据库异常</li>
 * </ul>
 * <p>
 * <b>常见的失败原因：</b>
 * <ul>
 *   <li>数据库服务未启动</li>
 *   <li>网络连接失败</li>
 *   <li>数据库地址或端口配置错误</li>
 *   <li>用户名或密码错误</li>
 *   <li>数据库连接池耗尽</li>
 *   <li>数据库服务负载过高</li>
 * </ul>
 * <p>
 * <b>日志输出：</b>
 * <pre>
 * // 健康检查成功
 * MySQL 健康检查通过, 耗时: 10ms
 *
 * // 健康检查失败
 * MySQL 健康检查失败: Communications link failed
 * </pre>
 * <p>
 * <b>性能考虑：</b>
 * <ul>
 *   <li>查询非常简单，执行时间通常在几毫秒内</li>
 *   <li>使用连接池，避免了频繁创建/销毁连接的开销</li>
 *   <li>不涉及实际业务表，不影响业务数据</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>此检查器仅验证连接可用性，不验证表结构或数据</li>
 *   <li>检查频率由 {@link HealthCheckRunner} 控制，在应用启动时执行一次</li>
 *   <li>失败不会导致应用启动失败，仅记录日志</li>
 *   <li>建议配合外部监控工具定期执行健康检查</li>
 * </ul>
 * <p>
 * <b>相关配置：</b>
 * <pre>
 * # application.yml
 * spring:
 *   datasource:
 *     url: jdbc:mysql://localhost:3306/your_db?useSSL=false
 *     username: your_username
 *     password: your_password
 *     driver-class-name: com.mysql.cj.jdbc.Driver
 *     hikari:
 *       maximum-pool-size: 10
 *       connection-timeout: 30000
 * </pre>
 *
 * @see HealthChecker 健康检查器接口
 * @see HealthCheckResult 健康检查结果
 * @see HealthCheckerMapper 健康检查 Mapper
 */
@Slf4j
@Component
public class MysqlHealthChecker implements HealthChecker {

    /**
     * 健康检查 Mapper
     * <p>
     * 用于执行数据库查询，验证数据库连接状态。
     */
    private final HealthCheckerMapper healthCheckerMapper;

    /**
     * 构造函数：通过依赖注入获取 Mapper
     *
     * @param healthCheckerMapper 健康检查 Mapper
     */
    public MysqlHealthChecker(HealthCheckerMapper healthCheckerMapper) {
        this.healthCheckerMapper = healthCheckerMapper;
    }

    @Override
    /**
     * 获取检查器名称
     *
     * @return 检查器名称 "MySQL"
     */
    public String getName() {
        return "MySQL";
    }

    @Override
    /**
     * 执行 MySQL 健康检查
     * <p>
     * 通过执行简单的 SQL 查询验证数据库连接状态。
     * <p>
     * <b>检查逻辑：</b>
     * <ol>
     *   <li>记录开始时间</li>
     *   <li>执行 {@code SELECT 1} 查询</li>
     *   <li>检查返回值是否为 1</li>
     *   <li>计算执行耗时</li>
     *   <li>生成健康检查结果</li>
     * </ol>
     *
     * @return 健康检查结果，包含状态、消息和耗时
     */
    public HealthCheckResult check() {
        long startTime = System.currentTimeMillis();
        try {
            // 执行健康检查查询
            int result = healthCheckerMapper.healthCheck();
            long duration = System.currentTimeMillis() - startTime;

            if (result == 1) {
                // 查询成功，数据库连接正常
                log.info("MySQL 健康检查通过, 耗时: {}ms", duration);
                return HealthCheckResult.up(getName(), duration, "MySQL 连接正常");
            } else {
                // 查询返回值异常（理论上不应该发生）
                log.warn("MySQL 健康检查失败: 查询结果异常");
                return HealthCheckResult.down(getName(), duration, "查询结果异常");
            }
        } catch (Exception e) {
            // 查询抛出异常，数据库连接失败
            long duration = System.currentTimeMillis() - startTime;
            log.error("MySQL 健康检查失败: {}", e.getMessage());
            return HealthCheckResult.down(getName(), duration, "连接失败: " + e.getMessage());
        }
    }
}
