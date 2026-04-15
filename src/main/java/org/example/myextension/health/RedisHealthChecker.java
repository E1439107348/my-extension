package org.example.myextension.health;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存健康检查器
 * <p>
 * 实现 {@link HealthChecker} 接口，用于检查 Redis 缓存的连接状态和可用性。
 * 基于 Redisson 客户端实现，通过写入和读取测试数据验证 Redis 功能。
 * <p>
 * <b>检查原理：</b>
 * 通过执行完整的读写删除操作验证：
 * <ul>
 *   <li>Redis 服务是否正常运行</li>
 *   <li>网络连接是否畅通</li>
 *   <li>读写功能是否正常</li>
 *   <li>数据一致性是否正确</li>
 * </ul>
 * <p>
 * <b>检查流程：</b>
 * <ol>
 *   <li>记录开始时间</li>
 *   <li>生成唯一的测试键和值</li>
 *   <li>写入测试数据到 Redis</li>
 *   <li>从 Redis 读取测试数据</li>
 *   <li>验证读取的数据与写入的数据是否一致</li>
 *   <li>删除测试数据（清理）</li>
 *   <li>根据验证结果生成健康检查报告</li>
 *   <li>记录执行耗时</li>
 * </ol>
 * <p>
 * <b>检查结果：</b>
 * <ul>
 *   <li>UP 状态：读写操作成功且数据一致，表示 Redis 连接正常</li>
 *   <li>DOWN 状态：操作抛出异常或数据验证失败，表示 Redis 异常</li>
 * </ul>
 * <p>
 * <b>常见的失败原因：</b>
 * <ul>
 *   <li>Redis 服务未启动</li>
 *   <li>网络连接失败</li>
 *   <li>Redis 地址或端口配置错误</li>
 *   <li>Redis 密码配置错误</li>
 *   <li>Redis 内存不足</li>
 *   <li>Redis 服务负载过高</li>
 * </ul>
 * <p>
 * <b>测试数据说明：</b>
 * <pre>
 * Key: health:check:{UUID}  // 使用 UUID 确保键的唯一性
 * Value: ping               // 固定的测试值
 * TTL: 5 秒                // 自动过期，防止堆积测试数据
 * </pre>
 * <p>
 * <b>日志输出：</b>
 * <pre>
 * // 健康检查成功
 * Redis 健康检查通过, 耗时: 5ms
 *
 * // 健康检查失败
 * Redis 健康检查失败: 无法连接到 Redis
 * </pre>
 * <p>
 * <b>性能考虑：</b>
 * <ul>
 *   <li>操作非常简单，执行时间通常在几毫秒内</li>
 *   <li>使用 Redisson 客户端，支持连接池和自动重连</li>
 *   <li>测试数据设置 5 秒 TTL，自动清理防止堆积</li>
 *   <li>使用 UUID 确保键的唯一性，避免并发冲突</li>
 * </ul>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>此检查器仅验证基本读写功能，不验证高级特性（如集群、持久化）</li>
 *   <li>检查频率由 {@link HealthCheckRunner} 控制，在应用启动时执行一次</li>
 *   <li>失败不会导致应用启动失败，仅记录日志</li>
 *   <li>建议配合外部监控工具定期执行健康检查</li>
 * </ul>
 * <p>
 * <b>相关配置：</b>
 * <pre>
 * # application.yml
 * spring:
 *   redis:
 *     host: localhost
 *     port: 6379
 *     password: your_password
 *     database: 0
 *     timeout: 3000
 *
 * # Redisson 配置
 * spring:
 *   redis:
 *     redisson:
 *       config: classpath:redisson.yaml
 * </pre>
 *
 * @author 系统生成
 * @see HealthChecker 健康检查器接口
 * @see HealthCheckResult 健康检查结果
 * @see org.redisson.api.RedissonClient Redisson 客户端
 */
@Slf4j
@Component
public class RedisHealthChecker implements HealthChecker {

    /**
     * Redisson 客户端
     * <p>
     * 用于与 Redis 服务进行交互，提供同步和异步的 API。
     */
    private final RedissonClient redissonClient;

    /**
     * 构造函数：通过依赖注入获取 Redisson 客户端
     *
     * @param redissonClient Redisson 客户端
     */
    public RedisHealthChecker(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 获取检查器名称
     *
     * @return 检查器名称 "Redis"
     */
    @Override
    public String getName() {
        return "Redis";
    }

    /**
     * 执行 Redis 健康检查
     * <p>
     * 通过完整的读写删除操作验证 Redis 连接状态和数据一致性。
     * <p>
     * <b>检查逻辑：</b>
     * <ol>
     *   <li>记录开始时间</li>
     *   <li>生成唯一的测试键和固定的测试值</li>
     *   <li>写入测试数据，设置 5 秒过期时间</li>
     *   <li>读取测试数据</li>
     *   <li>验证读取的数据与写入的数据是否一致</li>
     *   <li>删除测试数据（清理）</li>
     *   <li>计算执行耗时</li>
     *   <li>生成健康检查结果</li>
     * </ol>
     *
     * @return 健康检查结果，包含状态、消息和耗时
     */
    @Override
    public HealthCheckResult check() {
        long startTime = System.currentTimeMillis();
        try {
            // 生成唯一的测试键和固定的测试值
            String testKey = "health:check:" + UUID.randomUUID();
            String testValue = "ping";

            // 执行完整的读写删除操作
            RBucket<String> bucket = redissonClient.getBucket(testKey);
            // 写入，5 秒后自动过期
            bucket.set(testValue, 5, TimeUnit.SECONDS);
            // 读取
            String result = bucket.get();
            // 删除
            bucket.delete();

            long duration = System.currentTimeMillis() - startTime;

            // 验证数据一致性
            if (testValue.equals(result)) {
                // 读写操作成功且数据一致，Redis 连接正常
                log.info("Redis 健康检查通过, 耗时: {}ms", duration);
                return HealthCheckResult.up(getName(), duration, "Redis 连接正常");
            } else {
                // 数据验证失败，返回值与期望值不一致
                log.warn("Redis 健康检查失败: 数据验证不一致");
                return HealthCheckResult.down(getName(), duration, "数据验证失败");
            }
        } catch (Exception e) {
            // 操作抛出异常，Redis 连接失败
            long duration = System.currentTimeMillis() - startTime;
            log.error("Redis 健康检查失败: {}", e.getMessage());
            return HealthCheckResult.down(getName(), duration, "连接失败: " + e.getMessage());
        }
    }
}
