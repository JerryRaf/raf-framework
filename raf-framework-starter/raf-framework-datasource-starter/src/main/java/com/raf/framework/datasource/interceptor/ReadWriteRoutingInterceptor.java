package com.raf.framework.datasource.interceptor;

import com.raf.framework.datasource.DataSourceContextHolder;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 读写分离自动路由 MyBatis 拦截器。
 *
 * <p>拦截 MyBatis {@link Executor} 的 query/update 方法，根据 SQL 类型自动路由数据源：
 * <ul>
 *   <li>SELECT → slave（从库）</li>
 *   <li>INSERT / UPDATE / DELETE → master（主库）</li>
 * </ul>
 *
 * <p>以下情况不干预，保持现有路由逻辑：
 * <ul>
 *   <li>已通过 {@code @DsSelector} 或 {@code @ForceMaster} 显式指定数据源</li>
 *   <li>当前处于 Spring 事务中（强制走主库，避免读己写问题）</li>
 * </ul>
 *
 * <p>通过 {@code raf.datasource.read-write-splitting.enabled=true} 开启，默认关闭。
 *
 * @author Jerry
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class ReadWriteRoutingInterceptor implements Interceptor {

    private final String slaveKey;

    public ReadWriteRoutingInterceptor(String slaveKey) {
        this.slaveKey = slaveKey;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 已有显式数据源设置（@DsSelector 或 @ForceMaster）则不干预
        if (DataSourceContextHolder.getDB() != null) {
            return invocation.proceed();
        }

        // 事务中强制走主库，避免读己写问题
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("ReadWriteRouting: in transaction, using master");
            return invocation.proceed();
        }

        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        boolean isSelect = ms.getSqlCommandType() == SqlCommandType.SELECT;
        String targetDs = isSelect ? slaveKey : DataSourceContextHolder.DEFAULT_DS;

        log.debug("ReadWriteRouting: {} → {}", ms.getSqlCommandType(), targetDs);
        DataSourceContextHolder.setDB(targetDs);
        try {
            return invocation.proceed();
        } finally {
            DataSourceContextHolder.clearDB();
        }
    }
}
