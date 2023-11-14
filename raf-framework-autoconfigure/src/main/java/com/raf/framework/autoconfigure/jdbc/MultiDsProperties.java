package com.raf.framework.autoconfigure.jdbc;

import lombok.Data;

import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Jerry
 * @date 2019/01/01
 * https://github.com/brettwooldridge/HikariCP
 */
@Data
public class MultiDsProperties {
    private Map<String, JdbcProperties> dataSource;

    @Data
    public static class JdbcProperties {
        public static final String DEFAULT_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
        public static final Supplier<JdbcPoolProperties> JDBC_POOL = JdbcPoolProperties::new;

        private String username;
        private String password;
        private String url;
        private String driverClassName = DEFAULT_DRIVER_CLASS_NAME;
        private JdbcPoolProperties pool = JDBC_POOL.get();
    }

    @Data
    public static class JdbcPoolProperties {

        public static final boolean DEFAULT_AUTO_COMMIT = true;
        public static final int DEFAULT_CONNECTION_TIMEOUT = 2000;
        public static final int DEFAULT_IDLE_TIMEOUT = 120000;
        public static final int DEFAULT_MAX_LIFETIME = 0;
        public static final int DEFAULT_MAX_POOL_SIZE = 50;
        public static final int DEFAULT_MINIMUM_IDLE = 5;
        public static final int DEFAULT_INITIALIZATION_FAIL_TIMEOUT = 1;
        public static final boolean DEFAULT_ISOLATE_INTERNAL_QUERIES = false;
        public static final boolean DEFAULT_ALLOW_POOL_SUSPENSION = false;
        public static final boolean DEFAULT_READ_ONLY = false;
        public static final boolean DEFAULT_REGISTER_MBEANS = false;
        public static final int DEFAULT_VALIDATION_TIMEOUT = 1500;
        public static final int DEFAULT_LEAK_DETECTION_THRESHOLD = 0;


        private boolean autoCommit = DEFAULT_AUTO_COMMIT;
        private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        private int idleTimeout = DEFAULT_IDLE_TIMEOUT;
        private int maxLifetime = DEFAULT_MAX_LIFETIME;
        private int maximumPoolSize = DEFAULT_MAX_POOL_SIZE;
        private int minimumIdle = DEFAULT_MINIMUM_IDLE;
        private int initializationFailTimeout = DEFAULT_INITIALIZATION_FAIL_TIMEOUT;
        private boolean isolateInternalQueries = DEFAULT_ISOLATE_INTERNAL_QUERIES;
        private boolean allowPoolSuspension = DEFAULT_ALLOW_POOL_SUSPENSION;
        private boolean readOnly = DEFAULT_READ_ONLY;
        private boolean registerMbeans = DEFAULT_REGISTER_MBEANS;
        private int validationTimeout = DEFAULT_VALIDATION_TIMEOUT;
        private int leakDetectionThreshold = DEFAULT_LEAK_DETECTION_THRESHOLD;
    }
}
