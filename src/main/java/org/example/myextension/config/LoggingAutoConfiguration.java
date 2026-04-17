package org.example.myextension.config;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * my-extension 自动日志配置（在运行时为 Logback 添加控制台与文件 appender）
 *
 * 目标：移除库中的 logback-spring.xml，改为在运行时通过代码为引用项目提供一致的日志输出行为，
 * 包括：控制台输出 pattern（包含 TraceId）、可选的文件输出（按属性配置）。
 *
 * 总体原则：尽量不覆盖引用方已有配置；在引用方未配置文件输出或未提供名为 LIB_ASYNC/ LIB_CONSOLE 的 appender 时，
 * 本配置会按需补充控制台与文件 appender，保证 TraceId 能被输出并支持通过属性指定文件路径。
 *
 * 配置优先级（请在应用的 application.properties / application.yml 中设置）：
 *   1) myextension.logging.file = /var/logs/myapp/app.log   （优先）
 *   2) logging.file.name   = /var/logs/myapp/app.log
 *   3) logging.file.path   = /var/logs/myapp  （生成 application.log）
 *
 * 行为说明：
 *  - 当 root logger 上不存在名为 LIB_CONSOLE 的控制台 appender 时，会添加一个默认控制台 appender（包含 TraceId 的 pattern）；
 *  - 当任一 logging.file.* 属性配置存在，并且 root logger 上不存在名为 LIB_ASYNC 的 appender 时，会添加异步文件 appender（LIB_ASYNC），并将滚动策略、大小限制与保留策略应用到该 appender；
 *  - 所有创建/IO 操作异常会以 logger.warn/error 记录，不会阻塞应用启动。
 */
@Configuration
public class LoggingAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAutoConfiguration.class);

    /**
     * 自定义日志文件（完整路径），优先级最高，示例：
     *   myextension.logging.file=/var/logs/myapp/app.log
     */
    @Value("${myextension.logging.file:#{null}}")
    private String myextensionLogFile;

    /**
     * Spring Boot 的 logging.file.name，作为次优先级。
     */
    @Value("${myextension.logging.files.name:#{null}}")
    private String loggingFileName;

    /**
     * Spring Boot 的 logging.file.path，仅指定目录时使用，最终文件名为 {path}/application.log。
     */
    @Value("${myextension.logging.files.path:#{null}}")
    private String loggingFilePath;

    @PostConstruct
    public void configureLogging() {
        final String TRACE_KEY = "TraceId";
        String originalTrace = null;
        boolean injected = false;
        try {
            // 确保在初始化日志组件期间，MDC 中存在 TraceId，以便所有初始化日志记录都带上 TraceId
            originalTrace = MDC.get(TRACE_KEY);
            if (originalTrace == null || originalTrace.trim().isEmpty()) {
                String gen = UUID.randomUUID().toString().replace("-", "");
                MDC.put(TRACE_KEY, gen);
                injected = true;
            }

            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger root = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

            // 1) 确保存在控制台 appender（LIB_CONSOLE），包含 TraceId 的输出 pattern
            if (root.getAppender("LIB_CONSOLE") == null) {
                try {
                    PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
                    consoleEncoder.setContext(ctx);
                    consoleEncoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [TraceId:%X{TraceId}] %logger{36} - %msg%n");
                    consoleEncoder.start();

                    ConsoleAppender<ILoggingEvent> console = new ConsoleAppender<>();
                    console.setContext(ctx);
                    console.setName("LIB_CONSOLE");
                    console.setEncoder(consoleEncoder);
                    console.start();

                    root.addAppender(console);
                    logger.info("my-extension：已添加 LIB_CONSOLE 控制台 appender，输出包含 TraceId");
                } catch (Exception ex) {
                    logger.warn("my-extension：添加控制台 appender 失败：{}", ex.getMessage());
                }
            } else {
                logger.debug("my-extension：存在 LIB_CONSOLE appender，跳过添加控制台");
            }

            // 2) 根据属性决定是否添加文件 appender（LIB_ROLLING + LIB_ASYNC）
            String targetFile = resolveTargetFile();
            if (targetFile == null || targetFile.trim().isEmpty()) {
                logger.debug("my-extension：未配置日志文件位置，跳过添加文件 appender");
                return;
            }

            if (root.getAppender("LIB_ASYNC") != null) {
                logger.debug("my-extension：已存在 LIB_ASYNC appender，跳过添加文件 appender");
                return;
            }

            // 确保父目录存在（不抛出异常）
            try {
                Path p = Paths.get(targetFile).toAbsolutePath();
                if (p.getParent() != null) {
                    Files.createDirectories(p.getParent());
                }
            } catch (Exception ex) {
                logger.warn("my-extension：创建日志目录失败 {}: {}", targetFile, ex.getMessage());
            }

            try {
                // Encoder with pattern including TraceId from MDC
                PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                encoder.setContext(ctx);
                encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [TraceId:%X{TraceId}] %logger{36} - %msg%n");
                encoder.start();

                // Rolling file appender
                RollingFileAppender<ILoggingEvent> rolling = new RollingFileAppender<>();
                rolling.setContext(ctx);
                rolling.setName("LIB_ROLLING");
                rolling.setFile(targetFile);
                rolling.setEncoder(encoder);

                SizeAndTimeBasedRollingPolicy<ILoggingEvent> policy = new SizeAndTimeBasedRollingPolicy<>();
                policy.setContext(ctx);
                policy.setParent(rolling);
                policy.setFileNamePattern(targetFile + ".%d{yyyy-MM-dd}.%i.gz");
                policy.setMaxHistory(30);
                policy.setMaxFileSize(FileSize.valueOf("100MB"));
                policy.setTotalSizeCap(FileSize.valueOf("10GB"));
                policy.start();

                rolling.setRollingPolicy(policy);
                rolling.start();

                // Async wrapper
                AsyncAppender async = new AsyncAppender();
                async.setContext(ctx);
                async.setName("LIB_ASYNC");
                async.setQueueSize(512);
                async.addAppender(rolling);
                async.start();

                // attach to root
                root.addAppender(async);
                logger.info("my-extension：已添加 LIB_ASYNC appender，日志写入 {}", targetFile);
            } catch (Exception ex) {
                logger.error("my-extension：添加文件 appender 失败 {}: {}", targetFile, ex.getMessage(), ex);
            }

            // 注册 TurboFilter：当 MDC 中缺失 TraceId 时自动注入，保证引用项目的日志也能获得 TraceId
            try {
                TraceIdTurboFilter filter = new TraceIdTurboFilter();
                filter.start();
                ctx.addTurboFilter(filter);
                logger.info("my-extension：已注册 TraceIdTurboFilter，用于在缺失时注入 TraceId 到 MDC");
            } catch (Exception ex) {
                logger.warn("my-extension：注册 TraceIdTurboFilter 失败：{}", ex.getMessage());
            }

        } catch (Throwable ex) {
            logger.error("my-extension：初始化日志自动配置失败：{}", ex.getMessage(), ex);
        } finally {
            // 恢复原始 TraceId
            try {
                if (injected) {
                    MDC.remove(TRACE_KEY);
                } else if (originalTrace != null) {
                    MDC.put(TRACE_KEY, originalTrace);
                }
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    private String resolveTargetFile() {
        if (myextensionLogFile != null && !myextensionLogFile.trim().isEmpty()) return myextensionLogFile.trim();
        if (loggingFileName != null && !loggingFileName.trim().isEmpty()) return loggingFileName.trim();
        if (loggingFilePath != null && !loggingFilePath.trim().isEmpty()) {
            return loggingFilePath.trim() + (loggingFilePath.endsWith("/") ? "" : "/") + "application.log";
        }
        return null;
    }
}
