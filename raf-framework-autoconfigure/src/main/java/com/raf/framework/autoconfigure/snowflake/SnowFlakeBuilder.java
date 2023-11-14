package com.raf.framework.autoconfigure.snowflake;

import cn.hutool.core.lang.Snowflake;
import com.raf.framework.autoconfigure.spring.bean.SpringContext;

/**
 * @author Jerry
 * @date 2021/09/24
 */
public class SnowFlakeBuilder {
    private static final String SNOWFLAKE = "snowflake";

    public static String generateId() {
        Snowflake snowflake = SpringContext.getBean(SNOWFLAKE, Snowflake.class);
        return snowflake.nextIdStr();
    }
}
