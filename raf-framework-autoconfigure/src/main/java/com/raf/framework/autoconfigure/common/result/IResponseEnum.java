package com.raf.framework.autoconfigure.common.result;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public interface IResponseEnum {
    /**
     * 获取返回码
     * @return
     */
    int getCode();

    /**
     *  获取返回消息
     * @return
     */
    String getMsg();

}