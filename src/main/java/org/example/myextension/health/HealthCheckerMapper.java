package org.example.myextension.health;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 健康检查 Mapper 接口
 * <p>
 * 提供 MySQL 数据库的健康检查功能，通过执行简单的 SQL 查询来验证数据库连接是否正常。
 * <p>
 * <b>设计目的：</b>
 * 专门用于健康检查的轻量级数据库操作，避免依赖业务 Mapper。
 * 如果业务数据库异常，此接口仍可独立执行健康检查。
 * <p>
 * <b>检查原理：</b>
 * 执行简单的 {@code SELECT 1} 查询，这个查询不涉及具体表，
 * 只验证数据库连接和基本查询功能是否正常。
 * <p>
 * <b>执行结果：</b>
 * <ul>
 *   <li>查询成功并返回结果：表示数据库连接正常</li>
 *   <li>查询抛出异常：表示数据库连接失败或查询执行异常</li>
 *   <li>返回值非 1：表示查询结果异常（理论上不应该发生）</li>
 * </ul>
 * <p>
 * <b>使用方式：</b>
 * <pre>
 * // 在 MysqlHealthChecker 中使用
 * &#64;Component
 * public class MysqlHealthChecker implements HealthChecker {
 *
 *     private final HealthCheckerMapper healthCheckerMapper;
 *
 *     &#64;Override
 *     public HealthCheckResult check() {
 *         try {
 *             int result = healthCheckerMapper.healthCheck();
 *             if (result == 1) {
 *                 return HealthCheckResult.up("MySQL", duration, "MySQL 连接正常");
 *             }
 *         } catch (Exception e) {
 *             return HealthCheckResult.down("MySQL", duration, "连接失败: " + e.getMessage());
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>此接口仅用于健康检查，不应包含业务逻辑</li>
 *   <li>查询操作非常轻量，对数据库性能影响极小</li>
 *   <li>依赖 MyBatis 的 Mapper 扫描机制自动注册</li>
 *   <li>确保数据源配置正确，否则无法建立连接</li>
 * </ul>
 * <p>
 * <b>相关配置：</b>
 * <pre>
 * # application.yml
 * spring:
 *   datasource:
 *     url: jdbc:mysql://localhost:3306/your_db
 *     username: your_username
 *     password: your_password
 *     driver-class-name: com.mysql.cj.jdbc.Driver
 * </pre>
 *
 * @see MysqlHealthChecker MySQL 健康检查器
 * @see HealthChecker 健康检查器接口
 */
@Mapper
public interface HealthCheckerMapper {
    /**
     * 数据库健康检查查询
     * <p>
     * 执行简单的 {@code SELECT 1} 查询，用于验证数据库连接和基本查询功能。
     * <p>
     * <b>查询说明：</b>
     * <ul>
     *   <li>不涉及具体表，避免表不存在导致检查失败</li>
     *   <li>执行速度极快，对数据库性能影响小</li>
     *   <li>返回固定值 1，便于验证结果</li>
     * </ul>
     * <p>
     * <b>可能的异常：</b>
     * <ul>
     *   <li>{@code SQLException}：数据库连接失败</li>
     *   <li>{@code CommunicationsException}：网络连接异常</li>
     *   <li>{@code SQLSyntaxErrorException}：SQL 语法错误（本例中不应该发生）</li>
     * </ul>
     *
     * @return 查询结果，固定为 1
     */
    @Select("SELECT 1")
    int healthCheck();
}
