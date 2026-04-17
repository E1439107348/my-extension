package org.example.myextension.config;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * my-extension 自动日志配置（运行时为 Logback 添加文件输出）
 * <p>
 * 目的：当引用该库的应用没有显式配置文件型日志输出时，提供一个安全的、可配置的默认文件输出，
 * 使得库与引用项目在最小改造下也能将日志写到磁盘并包含 TraceId（MDC key: "TraceId"），便于线下排查。
 * <p>
 * 行为与优先级：
 * 1) 优先读取属性：
 * - myextension.logging.file  （优先，指定完整文件名，例如 /var/logs/app/app.log）
 * - logging.file.name         （兼容 Spring Boot）
 * - logging.file.path         （仅指定目录，则默认为 application.log）
 * 2) 若未配置上述任一属性，则不自动添加文件 appender，保持应用默认日志行为（通常为控制台输出）。
 * 3) 添加时仅在 Root Logger 中不存在名为 LIB_ASYNC 的 appender 时才添加，避免重复注册。
 * <p>
 * 实现细节：
 * - 运行时通过 Logback API 动态创建 RollingFileAppender + SizeAndTimeBasedRollingPolicy，并使用 AsyncAppender 包装以减小 I/O 阻塞对业务线程的影响；
 * - 输出 pattern 默认包含 TraceId："%X{TraceId}"；
 * - 自动尝试创建目标日志文件所在目录（如果无权限或失败会记录 warn，但不会中断应用启动）；
 * - 任何异常均以日志记录方式记录（logger.error/warn），不会抛出阻塞启动。
 * <p>
 * 使用建议与注意事项：
 * - 最佳实践仍然是让应用自行管理 logback-spring.xml（应用级配置优先）；本自动配置用于补充在无文件输出配置时的默认行为；
 * - 若引用项目希望全文控制日志格式与切分策略，应在应用中配置 logback-spring.xml 并可在其中包含 %X{TraceId}；
 * - 若需强制启用库的文件输出，请在应用中设置 myextension.logging.file=/path/to/file.log，并确保进程对该路径有写权限；
 */
@Configuration
public class LoggingAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAutoConfiguration.class);

    /**
     * 优先级配置（请在应用的 application.properties / application.yml 中设置）：
     * <p>
     * myextension.logging.file  —— 优先，完整文件名（推荐用于库用户定制）：
     * myextension.logging.file=/var/logs/myapp/app.log
     * <p>
     * <p>
     * <p>
     * <p>
     * 说明：三者任一被设置时，库会尝试在启动时为 root logger 添加名为 LIB_ASYNC 的异步文件 appender，写入对应路径。
     * 若三者均未设置，则不会自动添加任何文件 appender（保持应用默认日志行为）。
     */
    @Value("${myextension.logging.file:#{null}}")
    private String myextensionLogFile;

    /**
     * logging.file.name        —— Spring Boot 兼容属性，作为次优先级：
     * logging.file.name=/var/logs/myapp/app.log
     */
    @Value("${logging.file.name:#{null}}")
    private String loggingFileName;
    /**
     * logging.file.path        —— 仅指定目录时使用，最终文件名为 {path}/application.log：
     * logging.file.path=/var/logs/myapp
     */
    @Value("${logging.file.path:#{null}}")
    private String loggingFilePath;

    @PostConstruct
    public void configureFileAppender() {
        String targetFile = null;
        try {
            targetFile = resolveTargetFile();
            if (targetFile == null || targetFile.trim().isEmpty()) {
                // 未配置目标文件路径，跳过自动添加
                logger.debug("my-extension：未配置日志文件位置，跳过自动添加文件 appender");
                return;
            }

            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger root = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

            // 如果已经存在名为 LIB_ASYNC 的 appender，则认为已经配置过，不重复添加
            if (root.getAppender("LIB_ASYNC") != null) {
                logger.debug("my-extension：已存在 LIB_ASYNC appender，跳过自动配置");
                return;
            }

            // ensure directory exists
            try {
                Path p = Paths.get(targetFile).toAbsolutePath();
                if (p.getParent() != null) {
                    Files.createDirectories(p.getParent());
                }
            } catch (Exception ex) {
                logger.warn("my-extension：创建日志目录失败 {}: {}", targetFile, ex.getMessage());
            }

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

        } catch (Throwable ex) {
            // 防御性处理：任何异常都不应阻塞应用启动
            if (targetFile != null) {
                logger.error("my-extension：为 {} 配置文件 appender 失败：{}", targetFile, ex.getMessage(), ex);
            } else {
                logger.error("my-extension：配置文件 appender 失败：{}", ex.getMessage(), ex);
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
