package org.example.myextension.mybatis;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Properties;

/**
 * SQL 执行链路追踪拦截器。
 * <p>
 * 追踪目标：
 * <ul>
 *   <li>记录 SQL 执行耗时</li>
 *   <li>记录 SQL 文本与参数</li>
 *   <li>在启用 OTel 时创建 db span</li>
 * </ul>
 * <p>
 * 注意：
 * <ul>
 *   <li>该拦截器对所有 SQL 生效，生产环境建议配合日志采样。</li>
 *   <li>参数打印可能包含敏感信息，业务方可按需改造脱敏策略。</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "extension.mybatis.sql-trace", name = "enable", havingValue = "true")
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class SqlTraceMybatisInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(SqlTraceMybatisInterceptor.class);

    private final ObjectProvider<Tracer> tracerProvider;

    public SqlTraceMybatisInterceptor(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = normalizeSql(boundSql.getSql());
        Object param = boundSql.getParameterObject();
        long start = System.currentTimeMillis();

        Tracer tracer = tracerProvider.getIfAvailable();
        Span span = null;
        Scope scope = null;
        if (tracer != null) {
            span = tracer.spanBuilder("MyBatis SQL").startSpan();
            scope = span.makeCurrent();
            span.setAttribute("db.system", "mysql");
            span.setAttribute("db.statement", sql);
        }

        try {
            Object result = invocation.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("SQL执行完成, cost={}ms, sql={}, param={}", cost, sql, param);
            if (span != null) {
                span.setAttribute("db.cost.ms", cost);
            }
            return result;
        } catch (Throwable ex) {
            log.error("SQL执行异常, sql={}, param={}", sql, param, ex);
            if (span != null) {
                span.recordException(ex);
                span.setStatus(StatusCode.ERROR, ex.getMessage());
            }
            throw ex;
        } finally {
            if (scope != null) {
                scope.close();
            }
            if (span != null) {
                span.end();
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 当前无自定义参数，保留扩展点
    }

    /**
     * 压缩 SQL 空白字符，便于日志阅读。
     */
    private String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.replaceAll("\\s+", " ").trim();
    }
}
