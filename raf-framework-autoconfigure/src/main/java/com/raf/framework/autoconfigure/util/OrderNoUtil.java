package com.raf.framework.autoconfigure.util;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;

/**
 * @author Jerry
 * @date 2020/10/27
 */
public class OrderNoUtil {
    public static String generateOrderNumber() {
        String timeStr = DateUtil.format(DateUtil.date(), DatePattern.PURE_DATETIME_MS_PATTERN);
        return timeStr + RandomUtil.randomNumbers(6);
    }
}
