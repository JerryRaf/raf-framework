package com.raf.framework.autoconfigure.common;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public interface RafConstant {
    String AMPERSAND = "&";
    String AND = "and";
    String AT = "@";
    String ASTERISK = "*";
    String STAR = "*";
    String BACK_SLASH = "\\";
    String COLON = ":";
    String COMMA = ",";
    String DASH = "-";
    String DOLLAR = "$";
    String DOT = ".";
    String DOTDOT = "..";
    String DOT_CLASS = ".class";
    String DOT_JAVA = ".java";
    String DOT_XML = ".xml";
    String EMPTY = "";
    String EQUALS = "=";
    String FALSE = "false";
    String SLASH = "/";
    String HASH = "#";
    String HAT = "^";
    String LEFT_BRACE = "{";
    String LEFT_BRACKET = "(";
    String LEFT_CHEV = "<";
    String NEWLINE = "\n";
    String N = "n";
    String NO = "no";
    String NULL = "null";
    String OFF = "off";
    String ON = "on";
    String PERCENT = "%";
    String PIPE = "|";
    String PLUS = "+";
    String QUESTION_MARK = "?";
    String EXCLAMATION_MARK = "!";
    String QUOTE = "\"";
    String RETURN = "\r";
    String TAB = "\t";
    String RIGHT_BRACE = "}";
    String RIGHT_BRACKET = ")";
    String RIGHT_CHEV = ">";
    String SEMICOLON = ";";
    String SINGLE_QUOTE = "'";
    String BACKTICK = "`";
    String SPACE = " ";
    String TILDA = "~";
    String LEFT_SQ_BRACKET = "[";
    String RIGHT_SQ_BRACKET = "]";
    String TRUE = "true";
    String UNDERSCORE = "_";
    String UTF_8 = "UTF-8";
    String US_ASCII = "US-ASCII";
    String ISO_8859_1 = "ISO-8859-1";
    String Y = "y";
    String YES = "yes";
    String ONE = "1";
    String ZERO = "0";
    String DOLLAR_LEFT_BRACE = "${";
    String HASH_LEFT_BRACE = "#{";
    String CRLF = "\r\n";
    String HTML_NBSP = "&nbsp;";
    String HTML_AMP = "&amp";
    String HTML_QUOTE = "&quot;";
    String HTML_LT = "&lt;";
    String HTML_GT = "&gt;";
    String NODATA = "NODATA";
    String DEF_ROOT_PATH = ",";
    Long DEF_PARENT_ID = 0L;
    String EXCEPTION = "Exception";

    String ZH = "zh";
    String EN = "en";


    String CONNECTION_REFUSED = "Connection refused";
    String READ_TIMEDOUT = "Read timed out";


    /**
     * env
     */
    String LOCAL = "local";
    String DEV = "dev";
    String TEST = "test";
    String UAT = "uat";
    String GRAY = "gray";
    String PROD = "prod";


    /**
     * AUTHORIZATION
     */
    String AUTHORIZATION = "Authorization";

    /**
     * 用户id
     */
    String USER_ID_HEADER = "x-user-id";


    /**
     * 追踪id
     */
    String TRACE_ID = "traceId";

    String ASYNC_POOL = "raf-async-pool-%d";
    String ASYNC_CUST_POOL="raf-async-cust-pool-%d";


}
