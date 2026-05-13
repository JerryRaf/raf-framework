package com.raf.framework.mybatisplus.extension.interceptor;

import com.raf.framework.mybatisplus.extension.autoconfigure.SlowSqlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import java.sql.Statement;

/**
 * 慢 SQL 监控拦截器。
 *
 * <p>拦截 {@link StatementHandler#query} 和 {@link StatementHandler#update}，
 * 在 SQL 执行层面计时，超过阈值时打印 WARN 日志。
 *
 * <p>设计要点：
 * <ul>
 *   <li>拦截 StatementHandler 而非 Executor，避免连接获取等待时间干扰计时</li>
 *   <li>{@code logFullSql=false} 时只打印 SQL 模板，防止生产环境敏感数据泄露</li>
 *   <li>{@code logStackTrace=true} 时打印调用栈，便于快速定位慢 SQL 来源代码</li>
 * </ul>
 *
 * @author Jerry
 */
@Slf4j
@RequiredArgsConstructor
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query",
                args = {Statement.class, org.apache.ibatis.session.ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update",
                args = {Statement.class})
})
public class SlowSqlInterceptor implements Interceptor {

    private final SlowSqlProperties properties;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= properties.getThresholdMs()) {
                StatementHandler handler = (StatementHandler) invocation.getTarget();
                BoundSql boundSql = handler.getBoundSql();
                String sql = normalizeSql(boundSql.getSql());

                if (properties.isLogFullSql()) {
                    log.warn("[SlowSQL] elapsed={}ms threshold={}ms sql={} | params={}",
                            elapsed, properties.getThresholdMs(), sql,
                            boundSql.getParameterObject());
                } else {
                    log.warn("[SlowSQL] elapsed={}ms threshold={}ms sql={}",
                            elapsed, properties.getThresholdMs(), sql);
                }

                if (properties.isLogStackTrace()) {
                    log.warn("[SlowSQL] stacktrace", new RuntimeException("SlowSQL stacktrace"));
                }
            }
        }
    }

    private String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.replaceAll("\\s+", " ").trim();
    }
}
