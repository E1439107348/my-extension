package org.example.myextension.threadPool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * ThreadPool 工具类 — 提供带上下文传播能力的线程池（ContextAwaredThreadPoolExecutor）。
 *
 * 功能亮点：
 *  - 自动传递 MDC（例如 traceId）到异步线程，保证日志链路不丢失；
 *  - 自动传递 RequestAttributes（Request 请求上下文），使异步任务中能访问 RequestContextHolder；
 *  - 自动传递 Spring Security 的 SecurityContext（用户信息），使异步线程中可读取 SecurityContextHolder.getContext();
 *  - 与 @Transactional 一起使用时，异步方法在 Spring 管理的线程池中能生效（前提：异步方法使用 @Async，并且线程池由 Spring 管理作为 bean）；
 *  - 提供优雅关闭方法，便于在 Spring 容器关闭时正确释放线程池，防止 OOM 或任务中断导致资源泄漏。
 *
 * 完整使用 Demo（与您提供的示例一致，可直接拷贝到业务模块）：
 *
 * <pre>
 * // 1) 依赖（pom.xml）示例：已在本模块添加 Guava，用于 ThreadFactoryBuilder
 * //    <dependency>
 * //      <groupId>com.google.guava</groupId>
 * //      <artifactId>guava</artifactId>
 * //      <version>31.1-jre</version>
 * //    </dependency>
 *
 * // 2) 直接在引用项目中按示例初始化（静态场景，零改造）：
 * import com.google.common.util.concurrent.ThreadFactoryBuilder;
 * import java.util.concurrent.LinkedBlockingQueue;
 * import java.util.concurrent.ThreadPoolExecutor;
 * import java.util.concurrent.TimeUnit;
 *
 * public class ExampleUsage {
 *
 *     /**
 *      * 创建一个自定义的线程池执行器，用于处理特定任务
 *      * 线程池的配置参数根据任务需求精心选择，以确保高效和稳定地执行任务
 *      *//*
 *     private static final ContextAwaredThreadPoolExecutor executor = new ContextAwaredThreadPoolExecutor(
 *             8, // 核心线程数：保持常驻线程池的线程数量
 *             16, // 最大线程数：高负载时允许的最大并发线程
 *             2 * 60L, // 空闲线程存活时间（单位由 TimeUnit 参数指定）
 *             TimeUnit.SECONDS, // 时间单位：秒
 *             new LinkedBlockingQueue<>(3 * 5), // 任务队列：容量为 15
 *             new ThreadFactoryBuilder().setNameFormat("refresh-send-status-%d").build(), // 命名线程工厂
 *             new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用线程执行，确保不丢失任务
 *     );
 *
 *     public static void executor(Runnable task) {
 *         executor.submit(task);
 *     }
 * }
 *
 * // 3) 建议：将线程池交由 Spring 管理（可选替代上面静态做法），以便于监控与优雅关闭：
 * // @Configuration
 * // public class AsyncConfig {
 * //     @Bean(destroyMethod = "shutdownGracefully")
 * //     public ContextAwaredThreadPoolExecutor taskExecutor() {
 * //         ContextAwaredThreadPoolExecutor pool = ThreadPoolUtils.contextAwarePool(8, 16, 120, 15, "refresh-send-status");
 * //         pool.allowCoreThreadTimeOut(true);
 * //         return pool;
 * //     }
 * //     @Bean
 * //     public org.springframework.core.task.TaskExecutor springAsyncExecutor(ContextAwaredThreadPoolExecutor pool) {
 * //         return new org.springframework.scheduling.concurrent.ConcurrentTaskExecutor(pool);
 * //     }
 * // }
 *
 * 使用注意（重要）：
 *  - 若使用 @Async + @Transactional，务必让 @Async 使用 Spring 管理的 TaskExecutor（即把线程池作为 bean 并包装为 TaskExecutor），并确保异步方法位于 Spring 管理的 bean 上（AOP 代理生效）；
 *  - 线程池会复制并恢复提交时的 MDC/RequestAttributes/SecurityContext，但仍需确保在提交任务前这些上下文已被正确设置（例如在 Controller 层或被拦截后的业务线程）；
 *  - CallerRunsPolicy 会使调用线程阻塞执行任务，可能影响吞吐量；在高并发场景需结合限流或降级策略。
 *
打印线程池中任务日志时，请确保已设置 TraceId，否则日志中将缺少 TraceId。
 TraceUtil.setTraceId("T123");
   pool.submit(() -> log.info("task traceId={}", TraceUtil.getTraceId()));

 --------------------------------------------------------------------------------
  日志（TraceId）与日志文件路径配置说明：
  - 本模块提供了一个 sample logback-spring.xml（位于 resources/），包含日志 pattern 中的 TraceId 输出（%X{TraceId}）。
  - 推荐做法：将该 logback-spring.xml 拷贝到应用项目的 resources/ 下并按需修改；应用会优先使用应用级的 logback-spring.xml。
  - 配置日志文件路径：可在应用的 application.properties/application.yml 中设置：
      logging.file.name=/var/logs/myapp/app.log    # 指定完整日志文件名（优先）
      logging.file.path=/var/logs/myapp            # 指定日志目录（当未指定 name 时使用）
  - 若不复制配置文件到应用：当应用没有自定义 logback 配置且类路径中可见时，库内的 sample 可能被采用，但此行为不可靠，不建议依赖。
 --------------------------------------------------------------------------------
 * </pre>
 */
public class ThreadPoolUtils {

    /**
     * 创建一个带上下文传递能力的固定大小线程池（ContextAwaredThreadPoolExecutor）。
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveSeconds 非核心线程空闲存活时间（秒）
     * @param queueCapacity 队列容量
     * @param threadNamePrefix 线程名前缀
     * @return ContextAwaredThreadPoolExecutor
     */
    public static ContextAwaredThreadPoolExecutor contextAwarePool(int corePoolSize,
                                                                   int maximumPoolSize,
                                                                   long keepAliveSeconds,
                                                                   int queueCapacity,
                                                                   String threadNamePrefix) {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(Math.max(1, queueCapacity));
        ContextAwaredThreadPoolExecutor pool = new ContextAwaredThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                queue,
                threadNamePrefix
        );
        // 建议：允许在工厂外部设置 allowCoreThreadTimeOut(true) 根据场景选择
        pool.allowCoreThreadTimeOut(false);
        return pool;
    }

    /**
     * 创建一个带上下文传递能力的线程池，支持传入自定义 ThreadFactory 与 RejectedExecutionHandler（与示例兼容）
     *
     * 示例调用（与项目示例一致）：
     * <pre>
     * private static final ContextAwaredThreadPoolExecutor executor = new ContextAwaredThreadPoolExecutor(
     *         8, // 核心线程数
     *         16, // 最大线程数
     *         2 * 60L, // 空闲线程存活时间（秒）
     *         TimeUnit.SECONDS,
     *         new LinkedBlockingQueue<>(3 * 5), // 队列容量 15
     *         new com.google.common.util.concurrent.ThreadFactoryBuilder().setNameFormat("refresh-send-status-%d").build(),
     *         new ThreadPoolExecutor.CallerRunsPolicy()
     * );
     *
     * public static void executor(Runnable task) {
     *     executor.submit(task);
     * }
     * </pre>
     *
     * 注意事项：
     *  - 该构造器与 ContextAwaredThreadPoolExecutor 保持一致，适用于直接替换历史调用；
     *  - 当传入自定义 ThreadFactory 时，建议设置有意义的线程名前缀以便排查；
     *  - 对于拒绝策略，请根据业务选择：CallerRunsPolicy 可以在队列饱和时由调用线程执行，避免丢失任务，但可能会影响调用线程响应。
     *
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveSeconds 非核心线程空闲存活时间（与 unit 配合）
     * @param unit 时间单位
     * @param workQueue 任务队列
     * @param threadFactory 线程工厂
     * @param handler 拒绝策略
     * @return ContextAwaredThreadPoolExecutor 已包装的线程池实例
     */
    public static ContextAwaredThreadPoolExecutor contextAwarePool(int corePoolSize,
                                                                   int maximumPoolSize,
                                                                   long keepAliveSeconds,
                                                                   TimeUnit unit,
                                                                   BlockingQueue<Runnable> workQueue,
                                                                   ThreadFactory threadFactory,
                                                                   java.util.concurrent.RejectedExecutionHandler handler) {
        ContextAwaredThreadPoolExecutor pool = new ContextAwaredThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveSeconds,
                unit,
                workQueue,
                threadFactory,
                handler
        );
        return pool;
    }

    /**
     * 优雅关闭工具方法
     * @param pool 可为空
     * @param awaitSeconds 等待秒数
     */
    public static void shutdownGracefully(ContextAwaredThreadPoolExecutor pool, long awaitSeconds) {
        if (pool == null) {return;}
        pool.shutdownGracefully(awaitSeconds);
    }






}
