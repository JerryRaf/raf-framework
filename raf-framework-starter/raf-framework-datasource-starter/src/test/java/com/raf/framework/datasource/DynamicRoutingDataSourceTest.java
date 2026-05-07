package com.raf.framework.datasource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * DynamicRoutingDataSource Test
 *
 * @author Jerry
 * @since 2026-04-20
 */
class DynamicRoutingDataSourceTest {

    private DynamicRoutingDataSource routingDataSource;
    private DataSource masterDs;
    private DataSource slaveDs;

    @BeforeEach
    void setUp() {
        masterDs = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("masterDb")
                .build();
        slaveDs = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("slaveDb")
                .build();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDs);
        targetDataSources.put("slave", slaveDs);

        routingDataSource = new DynamicRoutingDataSource();
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDs);
        routingDataSource.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clearDB();
    }

    @Test
    void testReturnsNullKeyWhenContextNotSet() {
        assertNull(DataSourceContextHolder.getDB());
    }

    @Test
    void testDetermineCurrentLookupKeyReturnsMasterKey() {
        DataSourceContextHolder.setDB("master");
        assertEquals("master", routingDataSource.determineCurrentLookupKey());
    }

    @Test
    void testDetermineCurrentLookupKeyReturnsSlaveKey() {
        DataSourceContextHolder.setDB("slave");
        assertEquals("slave", routingDataSource.determineCurrentLookupKey());
    }

    @Test
    void testDetermineCurrentLookupKeyReturnsNullWhenNotSet() {
        assertNull(routingDataSource.determineCurrentLookupKey());
    }
}
