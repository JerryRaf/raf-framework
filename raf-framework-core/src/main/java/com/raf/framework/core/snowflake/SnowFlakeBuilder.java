package com.raf.framework.core.snowflake;

import com.raf.framework.core.spring.bean.SpringContext;

import cn.hutool.core.lang.Snowflake;

/**
 * @author Jerry
 * @date 2021/09/24
 */
public class SnowFlakeBuilder {

    /**
     * 私有构造器,防止实例化工具类
     */
    private SnowFlakeBuilder() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final String SNOWFLAKE = "snowflake";

    public static String generateId() {
        Snowflake snowflake = SpringContext.getBean(SNOWFLAKE, Snowflake.class);
        return snowflake.nextIdStr();
    }
}
