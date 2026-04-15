package org.example.myextension.utils;


import org.slf4j.MDC;
import java.util.function.Supplier;

/**
 * TraceId 工具类
 *
 * 提供对 SLF4J MDC 中 TraceId 的读写封装，以及在指定 TraceId 环境下执行代码块的能力。
 * 这在异步任务、线程池或需要跨服务链路追踪日志时非常有用。
 */
public class TraceUtil {

    private static final String TRACE_KEY = "TraceId";

    /**
     * 获取当前线程绑定的 TraceId
     * @return TraceId 字符串，若未设置返回 null
     */
    public static String getTraceId() {
        return MDC.get(TRACE_KEY);
    }

    /**
     * 在当前线程的 MDC 中设置 TraceId。若传入 null，则会移除该键（MDC 实现可能以不同方式处理 null）。
     *
     * @param traceId TraceId 值
     */
    public static void setTraceId(String traceId) {
        if (traceId == null) {
            MDC.remove(TRACE_KEY);
        } else {
            MDC.put(TRACE_KEY, traceId);
        }
    }

    /**
     * 在指定 TraceId 的上下文中执行给定代码块，并在执行结束后恢复原有的 TraceId。
     *
     * 典型用例：从请求线程提交异步任务到线程池时，希望在异步执行中保留相同的 TraceId。
     *
     * @param traceId 指定的 TraceId（若为 null 则直接执行 block，不做任何变更）
     * @param block   需要在该 TraceId 环境中执行的逻辑
     * @param <T>     返回类型
     * @return block 的执行结果
     */
    public static <T> T runWithTrace(String traceId, Supplier<T> block) {
        if (traceId == null) {
            return block.get();
        }
        String originalTraceId = getTraceId();
        try {
            setTraceId(traceId);
            return block.get();
        } finally {
            setTraceId(originalTraceId);
        }
    }
}