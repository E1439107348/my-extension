package org.example.myextension.otel;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.example.myextension.utils.TraceUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * OpenTelemetry HTTP 入口追踪过滤器。
 * <p>
 * 行为说明：
 * <ul>
 *   <li>为每个 HTTP 请求创建 SERVER Span。</li>
 *   <li>将 Span 的 traceId 写入 MDC（TraceId）并透传到响应头。</li>
 *   <li>请求结束后结束 Span，并恢复 MDC 上下文。</li>
 * </ul>
 */
@Component
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(prefix = "extension.observability.otel", name = "enable", havingValue = "true")
public class OpenTelemetryTraceFilter implements Filter {

    private final Tracer tracer;
    private final OpenTelemetryProperties properties;

    public OpenTelemetryTraceFilter(Tracer tracer, OpenTelemetryProperties properties) {
        this.tracer = tracer;
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        Span span = tracer.spanBuilder(req.getMethod() + " " + req.getRequestURI())
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        String previousTraceId = TraceUtil.getTraceId();
        try (Scope ignored = span.makeCurrent()) {
            String traceId = span.getSpanContext().getTraceId();
            TraceUtil.setTraceId(traceId);
            resp.setHeader(properties.getResponseHeader(), traceId);

            chain.doFilter(request, response);
            span.setAttribute("http.method", req.getMethod());
            span.setAttribute("http.target", req.getRequestURI());
            span.setAttribute("http.status_code", resp.getStatus());
        } catch (Throwable ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw new IllegalStateException(ex);
        } finally {
            span.end();
            TraceUtil.setTraceId(previousTraceId);
        }
    }
}
