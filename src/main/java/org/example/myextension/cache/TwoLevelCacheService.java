package org.example.myextension.cache;

import com.alibaba.fastjson2.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 双缓存服务：L1 Caffeine + L2 Redis。
 * <p>
 * 读取路径：
 * <ol>
 *   <li>先读本地缓存（L1）</li>
 *   <li>L1 未命中时读 Redis（L2）并回填 L1</li>
 *   <li>L2 仍未命中时执行 Loader，结果写入 L1 + L2</li>
 * </ol>
 * <p>
 * 一致性策略：
 * <ul>
 *   <li>采用“最终一致”策略，不实现分布式强一致。</li>
 *   <li>写操作建议通过 {@link #evict(String)} 主动失效双层缓存。</li>
 * </ul>
 */
public class TwoLevelCacheService {

    private static final Logger log = LoggerFactory.getLogger(TwoLevelCacheService.class);

    private final Cache<String, String> localCache;
    private final StringRedisTemplate stringRedisTemplate;
    private final TwoLevelCacheProperties properties;

    public TwoLevelCacheService(Cache<String, String> localCache,
                                StringRedisTemplate stringRedisTemplate,
                                TwoLevelCacheProperties properties) {
        this.localCache = localCache;
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    /**
     * 获取缓存值，支持未命中时回源。
     *
     * @param key    缓存键
     * @param type   返回类型
     * @param loader 回源逻辑（缓存未命中时执行）
     * @param <T>    泛型类型
     * @return 缓存值或回源值
     */
    public <T> T get(String key, Class<T> type, Callable<T> loader) {
        // 1) L1 命中直接返回
        String localJson = localCache.getIfPresent(key);
        if (localJson != null) {
            return JSON.parseObject(localJson, type);
        }

        // 2) L2 命中后回填 L1
        String redisJson = stringRedisTemplate.opsForValue().get(key);
        if (redisJson != null) {
            localCache.put(key, redisJson);
            return JSON.parseObject(redisJson, type);
        }

        // 3) 双层都未命中，回源并写入缓存
        try {
            T loaded = loader.call();
            if (loaded == null) {
                return null;
            }
            String json = JSON.toJSONString(loaded);
            localCache.put(key, json);
            stringRedisTemplate.opsForValue().set(key, json, properties.getRedisExpireSeconds(), TimeUnit.SECONDS);
            return loaded;
        } catch (Exception ex) {
            log.error("two-level cache loader 执行异常, key={}", key, ex);
            throw new IllegalStateException("缓存回源失败, key=" + key, ex);
        }
    }

    /**
     * 主动设置缓存。
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void put(String key, Object value) {
        if (value == null) {
            return;
        }
        String json = JSON.toJSONString(value);
        localCache.put(key, json);
        stringRedisTemplate.opsForValue().set(key, json, properties.getRedisExpireSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 主动失效缓存（L1 + L2）。
     *
     * @param key 缓存键
     */
    public void evict(String key) {
        localCache.invalidate(key);
        stringRedisTemplate.delete(key);
    }
}
