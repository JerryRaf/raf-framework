package com.raf.framework.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Dynamic routing datasource that routes to different datasources based on ThreadLocal context
 *
 * @author Jerry
 * @since 2026-04-20
 */
@Slf4j
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    /**
     * Determine the current lookup key for datasource routing
     *
     * @return current datasource key, or null to use default
     */
    @Override
    public Object determineCurrentLookupKey() {
        String dsKey = DataSourceContextHolder.getDB();
        log.debug("Current datasource key: {}", dsKey != null ? dsKey : "default");
        return dsKey;
    }
}
