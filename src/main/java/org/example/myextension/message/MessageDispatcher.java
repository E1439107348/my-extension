package org.example.myextension.message;


import org.example.myextension.ExtensionEnv;
import org.example.myextension.exception.BizException;
import org.example.myextension.utils.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class MessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MessageDispatcher.class);

    private final StringRedisTemplate redisTemplate;
    private final ExtensionEnv env;

    public MessageDispatcher(StringRedisTemplate redisTemplate, ExtensionEnv env) {
        this.redisTemplate = redisTemplate;
        this.env = env;
    }

    /**
     * 普通处理：根据 Handler 类型分发消息
     */
    public <H extends MessageDispatchHandler<D>, D> void handle(Class<H> handlerClass, String topic, String msgId, D msgData) {
        Collection<H> handlers = SpringContextUtil.getApplicationContext().getBeansOfType(handlerClass).values();
        handleInternal(handlers, topic, msgId, msgData);
    }

    /**
     * 顺序处理：支持 @Order 注解排序
     */
    public <H extends MessageDispatchHandler<D>, D> void handleOrderly(Class<H> handlerClass, String topic, String msgId, D msgData) {
        List<H> handlers = SpringContextUtil.getApplicationContext().getBeansOfType(handlerClass).values()
                .stream()
                .sorted(Comparator.comparingInt(h -> {
                    org.springframework.core.annotation.Order order = h.getClass().getAnnotation(org.springframework.core.annotation.Order.class);
                    return order != null ? order.value() : Integer.MAX_VALUE;
                }))
                .collect(Collectors.toList());
        handleInternal(handlers, topic, msgId, msgData);
    }

    /**
     * 内部统一分发逻辑
     */
    private <H extends MessageDispatchHandler<D>, D> void handleInternal(Collection<H> handlers, String topic, String msgId, D msgData) {
        boolean allSuccessful = true;
        for (H handler : handlers) {
            boolean success = dispatch(handler, topic, msgId, msgData);
            if (!success) {
                allSuccessful = false;
            }
        }
        if (!allSuccessful) {
            throw new BizException("消息处理失败。");
        }
    }

    /**
     * 核心执行方法：包含 Redis 幂等校验（防重复消费）
     */
    private <H extends MessageDispatchHandler<D>, D> boolean dispatch(H handler, String topic, String msgId, D msgData) {
        boolean successful = true;
        String handlerName = handler.getClass().getSimpleName();
        String key = String.format("MSG_DISPATCH::%s::%s::%s", topic, handlerName, msgId);

        // 使用 Redis setIfAbsent 实现简单的分布式锁/幂等控制
        Boolean isAbsent = redisTemplate.opsForValue().setIfAbsent(key, "1", Long.parseLong(env.getDispatchHandlerLifeTime()), TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(isAbsent)) {
            long startTime = System.currentTimeMillis();
            try {
                if (handler.condition(msgData)) {
                    handler.handle(msgData);
                    log.info("消息处理器 {} 处理成功，耗时: {} ms。", handlerName, (System.currentTimeMillis() - startTime));
                } else {
                    log.info("消息处理器 {} 条件不满足，跳过处理。", handlerName);
                }
            } catch (Throwable e) {
                log.error("消息处理器 {} 处理失败。", handlerName, e);
                redisTemplate.delete(key); // 处理失败则删除 key，允许重试
                successful = false;
            }
        } else {
            log.info("消息处理器 {} 已处理或正在处理。", handlerName);
        }
        return successful;
    }
}