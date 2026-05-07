package com.raf.framework.openapi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAPI 文档配置属性
 *
 * @author Jerry
 */
@Data
@ConfigurationProperties(prefix = "raf.openapi")
public class OpenApiProperties {

    /**
     * 是否开启 OpenAPI 文档，默认 false
     */
    private boolean enabled = false;

    /**
     * 文档标题
     */
    private String title = "API Document";

    /**
     * 文档描述
     */
    private String description = "";

    /**
     * 文档版本
     */
    private String version = "1.0.0";

    /**
     * 联系人信息
     */
    private Contact contact = new Contact();

    /**
     * 许可证信息
     */
    private License license = new License();

    /**
     * Bearer Token 安全方案，默认开启
     */
    private BearerAuth bearerAuth = new BearerAuth();

    @Data
    public static class Contact {
        private String name = "";
        private String email = "";
        private String url = "";
    }

    @Data
    public static class License {
        private String name = "";
        private String url = "";
    }

    @Data
    public static class BearerAuth {
        /**
         * 是否启用 Bearer Token 安全方案
         */
        private boolean enabled = true;

        /**
         * Header 名称
         */
        private String headerName = "Authorization";
    }
}
