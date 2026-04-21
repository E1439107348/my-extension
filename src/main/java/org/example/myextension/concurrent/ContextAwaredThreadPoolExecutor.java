package org.example.myextension.concurrent;

import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 带上下文传递能力的 ThreadPoolExecutor：自动传递 MDC（TraceId）、RequestAttributes、Spring Security Context
 *
 * 特性：
 *  - 在提交任务时捕获提交线程的 MDC、SecurityContext、RequestAttributes，并在异步线程中恢复；
 *  - 任务执行结束后恢复异步线程原有的上下文，避免污染线程池中的后续任务环境；
 *  - 覆盖 execute/submit 等方法，保证主流提交方式均被包装；
 *  - 提供 shutdownGracefully 方法用于优雅关闭线程池。
 *
 * 使用建议：
 *  - 将线程池交由 Spring 管理（作为 bean）并包装为 TaskExecutor 让 @Async 使用；
 *  - 合理配置 core/max/queue，避免过大导致 OOM，必要时使用监控告警限流；
 *  - 在高并发场景，优先考虑合适的拒绝策略并结合限流策略。
 */
public class ContextAwaredThreadPoolExecutor extends ThreadPoolExecutor {

    public ContextAwaredThreadPoolExecutor(int corePoolSize,
                                           int maximumPoolSize,
                                           long keepAliveTime,
                                           TimeUnit unit,
                                           BlockingQueue<Runnable> workQueue,
                                           ThreadFactory threadFactory,
                                           RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    public ContextAwaredThreadPoolExecutor(int corePoolSize,
                                           int maximumPoolSize,
                                           long keepAliveTime,
                                           TimeUnit unit,
                                           BlockingQueue<Runnable> workQueue,
                                           String threadNamePrefix) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, createThreadFactory(threadNamePrefix), new ThreadPoolExecutor.AbortPolicy());
    }

    private static ThreadFactory createThreadFactory(String prefix) {
        final AtomicInteger idx = new AtomicInteger(1);
        final String name = (prefix == null || prefix.isEmpty()) ? "ctx-pool" : prefix;
        return r -> {
            Thread t = new Thread(r, name + "-" + idx.getAndIncrement());
            t.setDaemon(false);
            return t;
        };
    }

    private Runnable wrap(Runnable task) {
        if (task == null) {return null;}
        final Map<String, String> mdc = MDC.getCopyOfContextMap();
        final SecurityContext securityContext = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext();
        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        return () -> {
            Map<String, String> originalMdc = MDC.getCopyOfContextMap();
            SecurityContext originalSecurity = SecurityContextHolder.getContext();
            RequestAttributes originalRequest = RequestContextHolder.getRequestAttributes();

            try {
                if (mdc != null) {
                    MDC.setContextMap(mdc);
                } else {
                    MDC.clear();
                }

                if (securityContext != null) {
                    SecurityContextHolder.setContext(securityContext);
                } else {
                    SecurityContextHolder.clearContext();
                }

                if (requestAttributes != null) {
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                }

                task.run();
            } finally {
                if (originalMdc != null) {
                    MDC.setContextMap(originalMdc);
                } else {
                    MDC.clear();
                }

                if (originalSecurity != null) {
                    SecurityContextHolder.setContext(originalSecurity);
                } else {
                    SecurityContextHolder.clearContext();
                }

                if (originalRequest != null) {
                    RequestContextHolder.setRequestAttributes(originalRequest);
                } else {
                    RequestContextHolder.resetRequestAttributes();
                }
            }
        };
    }

    private <T> Callable<T> wrap(Callable<T> task) {
        if (task == null) {return null;}
        final Map<String, String> mdc = MDC.getCopyOfContextMap();
        final SecurityContext securityContext = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext();
        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        return () -> {
            Map<String, String> originalMdc = MDC.getCopyOfContextMap();
            SecurityContext originalSecurity = SecurityContextHolder.getContext();
            RequestAttributes originalRequest = RequestContextHolder.getRequestAttributes();

            try {
                if (mdc != null) {
                    MDC.setContextMap(mdc);
                } else {
                    MDC.clear();
                }

                if (securityContext != null) {
                    SecurityContextHolder.setContext(securityContext);
                } else {
                    SecurityContextHolder.clearContext();
                }

                if (requestAttributes != null) {
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                }

                return task.call();
            } finally {
                if (originalMdc != null) {
                    MDC.setContextMap(originalMdc);
                } else {
                    MDC.clear();
                }

                if (originalSecurity != null) {
                    SecurityContextHolder.setContext(originalSecurity);
                } else {
                    SecurityContextHolder.clearContext();
                }

                if (originalRequest != null) {
                    RequestContextHolder.setRequestAttributes(originalRequest);
                } else {
                    RequestContextHolder.resetRequestAttributes();
                }
            }
        };
    }

    @Override
    public void execute(Runnable command) {
        super.execute(wrap(command));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(wrap(task), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(wrap(task));
    }

    // 方便的优雅关闭方法
    public void shutdownGracefully(long awaitSeconds) {
        try {
            this.shutdown();
            if (!this.awaitTermination(awaitSeconds, TimeUnit.SECONDS)) {
                this.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.shutdownNow();
        }
    }
}
