package org.example.myextension.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 双缓存自动配置。
 * <p>
 * 使用条件：
 * <ul>
 *   <li>{@code extension.cache.two-level.enable=true}</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(TwoLevelCacheProperties.class)
@ConditionalOnProperty(prefix = "extension.cache.two-level", name = "enable", havingValue = "true")
public class TwoLevelCacheAutoConfiguration {

    /**
     * 创建本地 Caffeine 缓存实例。
     *
     * @param properties 双缓存配置
     * @return 本地缓存对象
     */
    @Bean
    @ConditionalOnMissingBean(name = "twoLevelLocalCache")
    public Cache<String, String> twoLevelLocalCache(TwoLevelCacheProperties properties) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getLocalMaximumSize())
                .expireAfterWrite(properties.getLocalExpireSeconds(), TimeUnit.SECONDS)
                .recordStats()
                .build();
    }

    /**
     * 创建双缓存服务，封装双层读取与回填逻辑。
     *
     * @param localCache          本地缓存
     * @param stringRedisTemplate Redis 模板
     * @param properties          双缓存配置
     * @return 双缓存服务
     */
    @Bean
    @ConditionalOnMissingBean
    public TwoLevelCacheService twoLevelCacheService(Cache<String, String> localCache,
                                                     StringRedisTemplate stringRedisTemplate,
                                                     TwoLevelCacheProperties properties) {
        return new TwoLevelCacheService(localCache, stringRedisTemplate, properties);
    }
}
