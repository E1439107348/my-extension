package org.example.myextension.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 双缓存（Caffeine + Redis）配置。
 * <p>
 * 设计目标：
 * <ul>
 *   <li>默认关闭：避免对既有项目产生行为变更。</li>
 *   <li>分层 TTL：本地缓存短 TTL，Redis 缓存长 TTL，以平衡命中率与一致性。</li>
 *   <li>可控容量：避免本地缓存过大导致内存风险。</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "extension.cache.two-level")
public class TwoLevelCacheProperties {

    /**
     * 功能开关。
     */
    private boolean enable = false;

    /**
     * 本地 Caffeine 最大条目数。
     */
    private long localMaximumSize = 10_000L;

    /**
     * 本地 Caffeine 过期时间（秒）。
     */
    private long localExpireSeconds = 60L;

    /**
     * Redis 过期时间（秒）。
     */
    private long redisExpireSeconds = 300L;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public long getLocalMaximumSize() {
        return localMaximumSize;
    }

    public void setLocalMaximumSize(long localMaximumSize) {
        this.localMaximumSize = localMaximumSize;
    }

    public long getLocalExpireSeconds() {
        return localExpireSeconds;
    }

    public void setLocalExpireSeconds(long localExpireSeconds) {
        this.localExpireSeconds = localExpireSeconds;
    }

    public long getRedisExpireSeconds() {
        return redisExpireSeconds;
    }

    public void setRedisExpireSeconds(long redisExpireSeconds) {
        this.redisExpireSeconds = redisExpireSeconds;
    }
}
