package com.raf.framework.autoconfigure.util;

import cn.hutool.core.util.ReUtil;

/**
 * @author Jerry
 * @date 2020/06/05
 */
public class RegUtil {
    private RegUtil(){}

    /**
     * 6-20位，并且包含数字，字母，符号中的两项,除空格
     * @param content
     * @return
     */
    public static boolean isStrongPwd(String content) {
        return ReUtil.isMatch("(?!^\\d+$)(?!^[A-Za-z]+$)(?!^[^A-Za-z0-9]+$)(?!^.*[\\u4E00-\\u9FA5].*$)^\\S{6,20}$", content);
    }
}
