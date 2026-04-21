package org.example.myextension.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry 自动配置。
 * <p>
 * 说明：
 * <ul>
 *   <li>当前提供 SDK + Tracer 基础能力，供 HTTP/Feign/SQL 追踪统一使用。</li>
 *   <li>默认采样策略为 alwaysOn，便于快速接入与调试。</li>
 *   <li>如需对接 OTLP Collector，可在业务项目中覆盖 OpenTelemetry Bean。</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(OpenTelemetryProperties.class)
@ConditionalOnProperty(prefix = "extension.observability.otel", name = "enable", havingValue = "true")
public class OpenTelemetryAutoConfiguration {

    /**
     * 创建 OpenTelemetry 实例。
     *
     * @param properties OTel 配置
     * @return OpenTelemetry SDK
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenTelemetry openTelemetry(OpenTelemetryProperties properties) {
        Resource resource = Resource.getDefault();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setSampler(Sampler.alwaysOn())
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
    }

    /**
     * 创建 Tracer。
     *
     * @param openTelemetry OTel 实例
     * @return tracer
     */
    @Bean
    @ConditionalOnMissingBean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("org.example.myextension");
    }
}
