package com.raf.framework.autoconfigure.util;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Jerry
 * @date 2020/11/30
 */
public class DateExtUtil {

    private DateExtUtil(){}

    public static String gmtFormat(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss 'GMT'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date d = sdf.parse(date);
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
        } catch (Exception ex) {
            return StringUtils.EMPTY;
        }
    }
}
