package com.raf.framework.okhttp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "raf.okhttp")
public class OkHttpProperties {
    private boolean enabled = false;
    private Map<String, ChannelConfig> channels = new ConcurrentHashMap<>();

    @Data
    public static class ChannelConfig {
        private int connectTimeout = 2000;
        private int readTimeout = 5000;
        private int writeTimeout = 5000;
        private boolean retryOnConnectionFailure = true;
        private boolean followRedirects = true;
        private boolean followSslRedirects = true;
        private int maxRetries = 0;
        private String level = "BASIC";

        private ConnectionConfig connection = new ConnectionConfig();
        private AuthConfig auth = new AuthConfig();
        private SslConfig ssl = new SslConfig();
        private ProxyConfig proxy;

        @Data
        public static class ConnectionConfig {
            private int maxIdleConnections = 10;
            private long keepAliveDuration = 30000;
        }

        @Data
        public static class SslConfig {
            private boolean enabled = false;
            private boolean insecure = false;
            private String keyStorePath;
            private String keyStorePassword;
            private String trustStorePath;
            private String trustStorePassword;
        }

        @Data
        public static class ProxyConfig {
            private String type = "HTTP";
            private String host;
            private int port;
            private String username;
            private String password;
        }

        @Data
        public static class AuthConfig {
            private String type;
            private String token;
            private String username;
            private String password;
        }
    }
}
