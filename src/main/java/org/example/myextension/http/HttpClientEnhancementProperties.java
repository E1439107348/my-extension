package org.example.myextension.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HTTP Client 增强配置。
 */
@ConfigurationProperties(prefix = "extension.http.client")
public class HttpClientEnhancementProperties {

    /**
     * 开关。
     */
    private boolean enable = false;

    /**
     * 连接超时（毫秒）。
     */
    private int connectTimeoutMs = 3000;

    /**
     * 读超时（毫秒）。
     */
    private int readTimeoutMs = 5000;

    /**
     * 连接池总连接数。
     */
    private int maxTotal = 200;

    /**
     * 每路由最大连接数。
     */
    private int maxPerRoute = 50;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getMaxPerRoute() {
        return maxPerRoute;
    }

    public void setMaxPerRoute(int maxPerRoute) {
        this.maxPerRoute = maxPerRoute;
    }
}
