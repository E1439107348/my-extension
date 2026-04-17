package org.example.myextension.threadPool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 兼容旧版构造签名的适配类：ContextAwaredThreadPoolExecutor
 *
 * 说明：
 *  - 该类继承自 ContextAwareThreadPoolExecutor，保持原有的上下文传播（MDC/RequestAttributes/SecurityContext）行为；
 *  - 提供与第三方或历史项目一致的构造器签名，便于直接替换使用而不改造调用代码；
 *  - 不改变基类的执行/封装逻辑，仅作为构造器便利适配器。
 *
 * 推荐使用 ThreadFactory（可由 Guava ThreadFactoryBuilder 构造）和合适的拒绝策略。
 *
 * 使用示例：
 * new ContextAwaredThreadPoolExecutor(
 *     8, 16, 120L, TimeUnit.SECONDS,
 *     new LinkedBlockingQueue<>(15),
 *     new ThreadFactoryBuilder().setNameFormat("refresh-send-status-%d").build(),
 *     new ThreadPoolExecutor.CallerRunsPolicy()
 * );
 */
public class ContextAwaredThreadPoolExecutor extends ContextAwareThreadPoolExecutor {

    public ContextAwaredThreadPoolExecutor(int corePoolSize,
                                           int maximumPoolSize,
                                           long keepAliveTime,
                                           TimeUnit unit,
                                           BlockingQueue<Runnable> workQueue,
                                           ThreadFactory threadFactory,
                                           RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }
}
