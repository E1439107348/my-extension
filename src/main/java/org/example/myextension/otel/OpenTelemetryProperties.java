package org.example.myextension.otel;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenTelemetry 配置。
 */
@ConfigurationProperties(prefix = "extension.observability.otel")
public class OpenTelemetryProperties {

    /**
     * OTel 开关。
     */
    private boolean enable = false;

    /**
     * 服务名，用于资源标识。
     */
    private String serviceName = "my-extension-app";

    /**
     * traceId 响应头名称。
     */
    private String responseHeader = "X-Trace-Id";

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(String responseHeader) {
        this.responseHeader = responseHeader;
    }
}
