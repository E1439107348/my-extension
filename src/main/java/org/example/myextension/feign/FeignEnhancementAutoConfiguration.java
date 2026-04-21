package org.example.myextension.feign;

import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.example.myextension.utils.TraceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 增强自动配置。
 * <p>
 * 提供三类能力：
 * <ul>
 *   <li>请求拦截：透传 TraceId 头</li>
 *   <li>重试策略：默认 100ms 起步、最多 3 次</li>
 *   <li>错误解码：统一日志出口</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(RequestInterceptor.class)
@ConditionalOnProperty(prefix = "extension.feign", name = "enable", havingValue = "true")
public class FeignEnhancementAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FeignEnhancementAutoConfiguration.class);

    /**
     * 注入 TraceId 请求头。
     */
    @Bean
    @ConditionalOnMissingBean(name = "traceFeignRequestInterceptor")
    public RequestInterceptor traceFeignRequestInterceptor() {
        return template -> {
            String traceId = TraceUtil.getTraceId();
            if (traceId != null && !traceId.isEmpty()) {
                template.header("X-Trace-Id", traceId);
            }
        };
    }

    /**
     * Feign 重试策略。
     */
    @Bean
    @ConditionalOnMissingBean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, 1000, 3);
    }

    /**
     * Feign 错误解码器，统一记录下游调用异常。
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorDecoder feignErrorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign 调用失败, methodKey={}, status={}", methodKey, response.status());
            return new RuntimeException("Feign 调用失败: " + methodKey + ", status=" + response.status());
        };
    }

    /**
     * OTel 场景下，为 Feign 请求创建轻量 Span（通过 RequestInterceptor 实现）。
     * <p>
     * 注意：这里做的是“发起前打点”，并不会自动收集完整响应耗时。
     * 如果业务需要完整链路（含响应结果），建议结合 Feign Client 自定义实现扩展。
     */
    @Bean
    @ConditionalOnClass(Tracer.class)
    @ConditionalOnProperty(prefix = "extension.observability.otel", name = "enable", havingValue = "true")
    public RequestInterceptor otelFeignRequestInterceptor(Tracer tracer) {
        return template -> {
            Span span = tracer.spanBuilder("Feign " + template.method() + " " + template.url()).startSpan();
            try (Scope ignored = span.makeCurrent()) {
                template.header("X-Trace-Id", TraceUtil.getTraceId());
            } finally {
                span.end();
            }
        };
    }
}
