package org.example.myextension.http;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.example.myextension.utils.TraceUtil;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * RestTemplate 出站调用追踪拦截器。
 * <p>
 * 主要能力：
 * <ul>
 *   <li>创建 CLIENT Span 记录外部 HTTP 调用。</li>
 *   <li>注入 TraceId 请求头（X-Trace-Id）。</li>
 *   <li>记录状态码与异常信息。</li>
 * </ul>
 */
public class TracingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final Tracer tracer;

    public TracingClientHttpRequestInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        Span span = tracer.spanBuilder("HTTP " + request.getMethod() + " " + request.getURI().getHost())
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            String traceId = TraceUtil.getTraceId();
            if (traceId != null) {
                request.getHeaders().add("X-Trace-Id", traceId);
            }
            ClientHttpResponse response = execution.execute(request, body);
            span.setAttribute("http.url", request.getURI().toString());
            span.setAttribute("http.status_code", response.getRawStatusCode());
            return response;
        } catch (Exception ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }
}
