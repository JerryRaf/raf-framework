package com.raf.framework.mongodb.context;

/**
 * MongoDB 数据源上下文
 * <p>
 * 用于多数据源场景下动态切换数据源
 *
 * @author Jerry
 * @since 3.0.1
 */
public class MongoContext {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 私有构造器，防止实例化工具类
     */
    private MongoContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 设置当前线程的数据源
     *
     * @param dataSource 数据源名称
     */
    public static void setDataSource(String dataSource) {
        CONTEXT_HOLDER.set(dataSource);
    }

    /**
     * 获取当前线程的数据源
     *
     * @return 数据源名称，如果未设置则返回 "primary"
     */
    public static String currentDataSource() {
        String dataSource = CONTEXT_HOLDER.get();
        return dataSource != null ? dataSource : "primary";
    }

    /**
     * 清除当前线程的数据源设置
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
