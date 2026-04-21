package org.example.myextension.http;

import io.opentelemetry.api.trace.Tracer;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * HTTP Client 增强自动配置。
 * <p>
 * 开启后提供：
 * <ul>
 *   <li>基于 Apache HttpClient 的连接池 RestTemplate</li>
 *   <li>超时配置（连接超时、读取超时）</li>
 *   <li>可选追踪拦截器（Tracer 存在时自动启用）</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(RestTemplate.class)
@EnableConfigurationProperties(HttpClientEnhancementProperties.class)
@ConditionalOnProperty(prefix = "extension.http.client", name = "enable", havingValue = "true")
public class HttpClientEnhancementAutoConfiguration {

    /**
     * 创建连接池 HttpClient。
     */
    @Bean
    @ConditionalOnMissingBean
    public CloseableHttpClient enhancedHttpClient(HttpClientEnhancementProperties properties) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(properties.getMaxTotal());
        connectionManager.setDefaultMaxPerRoute(properties.getMaxPerRoute());

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(properties.getConnectTimeoutMs())
                .setSocketTimeout(properties.getReadTimeoutMs())
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .build();
    }

    /**
     * 创建增强版 RestTemplate。
     * <p>
     * 说明：仅在业务方未提供 RestTemplate Bean 时生效，避免覆盖业务自定义实现。
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate(CloseableHttpClient httpClient, HttpClientEnhancementProperties properties) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(Collections.emptyList());
        return restTemplate;
    }

    /**
     * 在存在 Tracer 时，为 RestTemplate 注入出站追踪拦截器。
     *
     * @param restTemplate RestTemplate
     * @param tracer       OTel Tracer
     * @return 追踪拦截器
     */
    @Bean
    @ConditionalOnClass(Tracer.class)
    @ConditionalOnProperty(prefix = "extension.observability.otel", name = "enable", havingValue = "true")
    public TracingClientHttpRequestInterceptor tracingClientHttpRequestInterceptor(RestTemplate restTemplate, Tracer tracer) {
        TracingClientHttpRequestInterceptor interceptor = new TracingClientHttpRequestInterceptor(tracer);
        restTemplate.getInterceptors().add(interceptor);
        return interceptor;
    }
}
