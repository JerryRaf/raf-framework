package com.raf.framework.core.common;

import java.io.Serializable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author Jerry
 */
@Data
public class AppRequest<T> implements Serializable {
    /**
     * 1. 当前登录用户UserId
     */
    private String userId;

    /**
     * 2. 客户端环境信息
     */
    @Valid
    @NotNull(message = "客户端环境信息(client)不能为空")
    private ClientInfo client;

    /**
     * 3. 业务数据
     */
    @Valid
    @NotNull(message = "业务数据(data)不能为空")
    private T data;

    /**
     * 客户端环境信息内部类
     */
    @Data
    public static class ClientInfo implements Serializable {
        /**
         * 语言 (e.g., "en_US")
         */
        private String lang;

        /**
         * App版本 (e.g., "3.10.0")
         */
        private String version;

        /**
         * 操作系统类型 (e.g., "ios")
         */
        private String osType;

        /**
         * 操作系统版本 (e.g., "18.0.1")
         */
        private String osVersion;

        /**
         * 内部型号 (e.g., "iPhone16,1")
         */
        private String model;

        /**
         * 市场型号 (e.g., "iPhone 15 Pro")
         */
        private String marketModel;

        /**
         * 设备ID (e.g., "F7801...")
         */
        private String deviceId;

        /**
         * 推送Token (e.g., "bd76...")
         */
        private String pushToken;
    }
}