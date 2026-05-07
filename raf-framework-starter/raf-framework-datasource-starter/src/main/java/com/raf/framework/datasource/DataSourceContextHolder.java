package com.raf.framework.datasource;

/**
 * DataSource Context Holder using ThreadLocal
 *
 * @author Jerry
 * @since 2026-04-20
 */
public class DataSourceContextHolder {

    public static final String DEFAULT_DS = "master";

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * Set current datasource key
     *
     * @param dbType datasource key
     */
    public static void setDB(String dbType) {
        CONTEXT_HOLDER.set(dbType);
    }

    /**
     * Get current datasource key
     *
     * @return datasource key
     */
    public static String getDB() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * Clear current datasource key
     */
    public static void clearDB() {
        CONTEXT_HOLDER.remove();
    }
}
