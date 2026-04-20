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
 * my-extension 自动日志配置（在运行时为 Logback 增强控制台与文件输出）
 *
 * Purpose
 *   在运行时以编程方式为引用项目补充日志输出能力，确保日志记录包含统一的 TraceId 并支持可选的文件输出。
 *
 * Design goals
 *   1) 不覆盖引用项目已有的自定义 Logback 配置；仅在缺失特定 appender（LIB_CONSOLE / LIB_ASYNC）时补充。
 *   2) 保证日志链路完整：尽可能确保应用任何阶段（包括类初始化期间、主动 logger.info 调用）都有可用的 TraceId。
 *   3) 控制台输出对人类友好：对日志级别进行颜色高亮，便于快速区分 INFO/WARN/ERROR；文件输出保持无颜色以利于长期存储和解析。
 *
 * Features
 *   - 在 root logger 上按需添加命名的 ConsoleAppender（LIB_CONSOLE），默认 pattern 包含 TraceId（%X{TraceId}）。
 *   - 按配置（myextension.logging.file / logging.file.name / logging.file.path）按需添加异步滚动文件写入（LIB_ROLLING + LIB_ASYNC），包含大小与历史保留策略。
 *   - 早期注册 TraceIdTurboFilter（在创建 appenders 之前），确保在日志调用前若 MDC 中缺少 TraceId 时自动注入一个随机 TraceId，从而连带初始化阶段/主动日志调用也能输出 TraceId。
 *
 * Configuration properties (优先级自上而下)
 *   - myextension.logging.file     (最高优先级，完整文件路径，例如 /var/logs/myapp/app.log)
 *   - logging.file.name            (spring-boot 标准属性)
 *   - logging.file.path            (仅目录时，生成 {path}/application.log)
 *
 * Console coloring behavior
 *   - 控制台 pattern 使用 Logback 的 %highlight(...) 对日志级别进行上色（仅 level 文字），例如：%highlight(%-5level)
 *   - 文件 appender 与写入到磁盘的日志不包含 ANSI 颜色码，保证可被文本处理工具 / 收集系统正确消费。
 *   - 颜色显示依赖终端对 ANSI 转义码的支持；若需要在 Windows 旧终端上支持，可集成 JAnsi（非本配置的默认行为）。
 *
 * TraceId guarantees & MDC handling
 *   - 为了尽量保证引用项目的所有日志都携带 TraceId，库在初始化阶段会：
 *       a) 尝试在 configureLogging 的早期为当前线程 MDC 写入一个临时 TraceId（仅在没有时）。
 *       b) 早期注册 TraceIdTurboFilter，该 TurboFilter 在每次日志事件被评估时检查 MDC，若缺失则填充一个随机 TraceId（无短横线）。
 *   - 该策略的权衡：TurboFilter 的填充会持久写入当前线程的 MDC，可能影响后续业务对 MDC 的预期值；若希望仅对单次日志事件临时注入而不持久写入，需要更复杂的事件层面实现。
 *
 * Usage notes
 *   - 若引用方已有完整的 logback-spring.xml 配置且包含自定义的控制台/文件 appender（且命名为 LIB_CONSOLE / LIB_ASYNC），本配置不会覆盖。
 *   - 若希望控制台不使用颜色或改变上色策略，可在应用中自定义 logback-spring.xml 或通过属性禁用本库的行为（当前未提供开关，可根据需要扩展）。
 *
 * Examples (application.properties)
 *   # 优先指定完整文件
 *   myextension.logging.file=/var/logs/myapp/app.log
 *
 *   # 或使用 spring-boot 标准
 *   logging.file.path=/var/logs/myapp
 *
 * Caveats
 *   - 早期在 MDC 中注入 TraceId 是一个可见的副作用；在严格要求 MDC 不被库层改变的场景中，需谨慎使用。
 *   - 控制台颜色依赖环境；日志收集系统在采集控制台输出时若未剥离 ANSI 码，可能将颜色码一并收集至存储层。
 */
@Configuration
public class LoggingAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAutoConfiguration.class);

    /**
     * 自定义日志文件（完整路径），优先级最高，示例：
     *   myextension.logging.file=/var/logs/myapp/app.log
     */
    @Value("${myextension.logging.files.url:#{null}}")
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

            // 注册 TurboFilter：当 MDC 中缺失 TraceId 时自动注入，保证引用项目的日志也能获得 TraceId
            try {
                TraceIdTurboFilter earlyFilter = new TraceIdTurboFilter();
                earlyFilter.start();
                ctx.addTurboFilter(earlyFilter);
                logger.debug("my-extension：已早期注册 TraceIdTurboFilter，用于在缺失时注入 TraceId 到 MDC");
            } catch (Exception ex) {
                logger.warn("my-extension：早期注册 TraceIdTurboFilter 失败：{}", ex.getMessage());
            }

            // 1) 优先尝试更新引用项目已有的 ConsoleAppender，将 TraceId 注入其输出 pattern，避免产生重复日志
            boolean consoleFound = false;
            java.util.Iterator<ch.qos.logback.core.Appender<ILoggingEvent>> it = root.iteratorForAppenders();
            while (it != null && it.hasNext()) {
                ch.qos.logback.core.Appender<ILoggingEvent> app = it.next();
                if (app instanceof ConsoleAppender) {
                    consoleFound = true;
                    try {
                        ConsoleAppender<ILoggingEvent> existing = (ConsoleAppender<ILoggingEvent>) app;
                        Object enc = existing.getEncoder();
                        if (enc instanceof PatternLayoutEncoder) {
                            PatternLayoutEncoder ple = (PatternLayoutEncoder) enc;
                            String pattern = ple.getPattern();
                            if (pattern == null) pattern = "";
                            if (!pattern.contains("%X{TraceId}")) {
                                // 将 TraceId 前置到 pattern 开头，确保日志行以 TraceId 开始
                                String tracePrefix = "TraceId:%X{TraceId}  ";
                                String newPattern = tracePrefix + pattern;

                                // 创建新的 encoder 并替换（需要停止/启动 appender）
                                PatternLayoutEncoder newEnc = new PatternLayoutEncoder();
                                newEnc.setContext(ctx);
                                newEnc.setPattern(newPattern);
                                newEnc.start();

                                existing.stop();
                                existing.setEncoder(newEnc);
                                existing.start();

                                logger.info("my-extension：已更新现有 ConsoleAppender '{}' 的 pattern，注入 TraceId", existing.getName());
                            } else {
                                logger.debug("my-extension：现有 ConsoleAppender '{}' 已包含 TraceId，跳过修改", existing.getName());
                            }
                        } else {
                            logger.debug("my-extension：发现非 PatternLayoutEncoder 的 ConsoleAppender '{}', 跳过修改", existing.getName());
                        }
                    } catch (Exception ex) {
                        logger.warn("my-extension：尝试更新现有 ConsoleAppender 失败：{}", ex.getMessage());
                    }
                }
            }

            // 若确实没有任何 ConsoleAppender（引用项目没有控制台输出），才添加库侧的 LIB_CONSOLE
            if (!consoleFound) {
                try {
                    PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
                    consoleEncoder.setContext(ctx);
                    // Use %highlight to enable colored level output on console
                    consoleEncoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [%thread] [TraceId:%X{TraceId}] %logger{36} - %msg%n");
                    consoleEncoder.start();

                    ConsoleAppender<ILoggingEvent> console = new ConsoleAppender<>();
                    console.setContext(ctx);
                    console.setName("LIB_CONSOLE");
                    console.setEncoder(consoleEncoder);
                    console.start();

                    root.addAppender(console);
                    logger.info("my-extension：未检测到 ConsoleAppender，已添加 LIB_CONSOLE 控制台 appender，输出包含 TraceId");
                } catch (Exception ex) {
                    logger.warn("my-extension：添加控制台 appender 失败：{}", ex.getMessage());
                }
            } else {
                logger.debug("my-extension：检测到现有 ConsoleAppender，已尝试注入 TraceId，未添加额外的控制台 appender");
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

            // TraceIdTurboFilter 已在上文早期注册，故此处跳过重复注册以避免覆盖
            logger.debug("my-extension：TraceIdTurboFilter 已注册，跳过重复注册");

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
