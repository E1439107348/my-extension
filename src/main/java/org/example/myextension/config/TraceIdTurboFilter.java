package org.example.myextension.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * TurboFilter：当 MDC 中不存在 TraceId 时，自动注入一个随机 TraceId（不带短横线）。
 *
 * 说明：此过滤器在每次日志记录时判断当前线程 MDC，若缺失则填充。填充会保留在当前线程的 MDC 中，
 * 因为 TurboFilter 在日志调用前执行；这样后续同一线程的日志也会携带该 TraceId（符合“缺失时补全”的场景）。
 *
 * 风险与权衡：填充后会改变线程的 MDC 状态，可能影响后续业务逻辑的 MDC 语义。如果不希望持久化该值，
 * 可改为在日志事件层面临时添加（复杂实现）。当前方案为实现简单且能保证日志链路完整。
 */
public class TraceIdTurboFilter extends TurboFilter {

    private static final String TRACE_KEY = "TraceId";

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public FilterReply decide(org.slf4j.Marker marker, ch.qos.logback.classic.Logger logger, Level level, String format, Object[] params, Throwable t) {
        try {
            String existing = MDC.get(TRACE_KEY);
            if (existing == null || existing.trim().isEmpty()) {
                String gen = UUID.randomUUID().toString().replace("-", "");
                MDC.put(TRACE_KEY, gen);
            }
        } catch (Exception ignored) {
            // ignore errors to avoid breaking logging
        }
        return FilterReply.NEUTRAL;
    }
}
