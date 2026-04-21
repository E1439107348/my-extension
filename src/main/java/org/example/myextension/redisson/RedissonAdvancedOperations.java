package org.example.myextension.redisson;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBucket;
import org.redisson.api.RMapCache;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 增强操作封装（完整实现基线）。
 * <p>
 * 覆盖常见高频场景：
 * <ul>
 *   <li>分布式计数器（RAtomicLong）</li>
 *   <li>缓存 Map（RMapCache，支持 TTL）</li>
 *   <li>布隆过滤器（RBloomFilter）</li>
 *   <li>发布订阅（RTopic）</li>
 *   <li>带回源能力的 Bucket 读取</li>
 * </ul>
 */
@Component
public class RedissonAdvancedOperations {

    private final RedissonClient redissonClient;

    public RedissonAdvancedOperations(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 原子自增计数器。
     *
     * @param counterKey 计数器 key
     * @return 自增后的值
     */
    public long increment(String counterKey) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(counterKey);
        return atomicLong.incrementAndGet();
    }

    /**
     * 将值写入 RMapCache，并设置 TTL。
     *
     * @param mapKey         map key
     * @param field          字段
     * @param value          值
     * @param expireSeconds  过期秒数
     * @param <T>            值类型
     */
    public <T> void mapPut(String mapKey, String field, T value, long expireSeconds) {
        RMapCache<String, T> mapCache = redissonClient.getMapCache(mapKey);
        mapCache.put(field, value, expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 从 RMapCache 读取字段值。
     *
     * @param mapKey map key
     * @param field  字段
     * @param <T>    值类型
     * @return 字段值
     */
    public <T> T mapGet(String mapKey, String field) {
        RMapCache<String, T> mapCache = redissonClient.getMapCache(mapKey);
        return mapCache.get(field);
    }

    /**
     * 布隆过滤器初始化（幂等调用）。
     *
     * @param bloomKey            布隆过滤器 key
     * @param expectedInsertions  预期插入量
     * @param falseProbability    误判率
     * @return RBloomFilter 实例
     */
    public RBloomFilter<String> initBloomFilter(String bloomKey, long expectedInsertions, double falseProbability) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(bloomKey);
        bloomFilter.tryInit(expectedInsertions, falseProbability);
        return bloomFilter;
    }

    /**
     * 布隆过滤器写入。
     *
     * @param bloomKey 布隆过滤器 key
     * @param value    写入值
     */
    public void bloomAdd(String bloomKey, String value) {
        redissonClient.getBloomFilter(bloomKey).add(value);
    }

    /**
     * 布隆过滤器查询。
     *
     * @param bloomKey 布隆过滤器 key
     * @param value    查询值
     * @return true=可能存在，false=一定不存在
     */
    public boolean bloomContains(String bloomKey, String value) {
        return redissonClient.getBloomFilter(bloomKey).contains(value);
    }

    /**
     * 发布订阅消息。
     *
     * @param topic   topic 名
     * @param payload 消息体
     * @return 订阅者数量
     */
    public long publish(String topic, Object payload) {
        RTopic rTopic = redissonClient.getTopic(topic);
        return rTopic.publish(payload);
    }

    /**
     * Bucket 读取，未命中时回源并写回（带 TTL）。
     *
     * @param key           bucket key
     * @param expireSeconds 过期秒数
     * @param supplier      回源函数
     * @param <T>           值类型
     * @return 读取结果
     */
    public <T> T getOrLoad(String key, long expireSeconds, Supplier<T> supplier) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        T value = bucket.get();
        if (value != null) {
            return value;
        }
        T loaded = supplier.get();
        if (loaded != null) {
            bucket.set(loaded, expireSeconds, TimeUnit.SECONDS);
        }
        return loaded;
    }
}
