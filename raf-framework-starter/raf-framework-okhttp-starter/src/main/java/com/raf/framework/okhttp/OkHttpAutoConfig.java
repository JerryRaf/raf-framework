package com.raf.framework.okhttp;

import com.raf.framework.core.util.BeanRegistrationUtils;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnClass({OkHttpClient.class, RequestBody.class})
@ConditionalOnProperty(prefix = "raf.okhttp", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OkHttpProperties.class)
public class OkHttpAutoConfig {

    @Autowired
    private OkHttpProperties okHttpProperties;

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 确保在Properties属性绑定完成后执行
     */
    @PostConstruct
    public void registerChannelClients() {
        if (okHttpProperties.getChannels().isEmpty()) {
            throw new BeanInitializationException("At least one okhttp channel needs to be configured");
        }

        validateSslConfiguration();

        okHttpProperties.getChannels().forEach((channelName, config) -> {
            String beanName = String.format("%s-OkHttpClient", channelName);
            BeanRegistrationUtils.registerBeanWithQualifier(beanFactory, beanName, OkHttpClient.class, OkHttpProperties.ChannelConfig.class, Collections.singletonMap("value", channelName), () -> createClient(config));
            log.info("-->OkHttpAutoConfig {} beanFactory register success", beanName);

        });
    }

    /**
     * Validate SSL configuration to prevent insecure mode in production
     */
    private void validateSslConfiguration() {
        String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        boolean isDevelopment = activeProfiles.length > 0 &&
                (activeProfiles[0].equalsIgnoreCase("dev") ||
                 activeProfiles[0].equalsIgnoreCase("local") ||
                 activeProfiles[0].equalsIgnoreCase("test"));

        okHttpProperties.getChannels().forEach((channelName, config) -> {
            if (config.getSsl().isInsecure() && !isDevelopment) {
                throw new IllegalStateException(
                    String.format("Insecure SSL mode is only allowed in dev/local/test environment. " +
                                "Current profile: %s, Channel: %s",
                                activeProfiles.length > 0 ? activeProfiles[0] : "default",
                                channelName));
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpExecutor httpExecutor(ApplicationContext context) {
        return new HttpExecutor(context);
    }


    private OkHttpClient createClient(OkHttpProperties.ChannelConfig config) {
        // 连接池配置
        ConnectionPool pool = new ConnectionPool(config.getConnection().getMaxIdleConnections(), config.getConnection()
                .getKeepAliveDuration(), TimeUnit.MILLISECONDS);

        // 构建基础客户端
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectionPool(pool)
                .connectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeout(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(config.isRetryOnConnectionFailure())
                .addInterceptor(createLoggingInterceptor(config.getLevel()));

        // 安全配置
        configureSsl(config.getSsl(), builder);
        // 代理配置
        configureProxy(config.getProxy(), builder);
        // 认证配置
        configureAuth(config.getAuth(), builder);

        return builder.build();
    }

    private void configureSsl(OkHttpProperties.ChannelConfig.SslConfig sslConfig, OkHttpClient.Builder builder) {
        try {
            if (!sslConfig.isEnabled()) {
                return;
            }

            if (sslConfig.isInsecure()) {
                log.warn("Using insecure SSL mode - certificate verification is disabled. This should ONLY be used in development environment!");
                X509TrustManager trustManager = createInsecureTrustManager();
                SSLSocketFactory sslSocketFactory = createInsecureSSLSocketFactory(trustManager);
                builder.sslSocketFactory(sslSocketFactory, trustManager).hostnameVerifier((hostname, session) -> true);
            } else if (sslConfig.getKeyStorePath() != null && sslConfig.getTrustStorePath() != null) {
                SSLContext sslContext = createCustomSslContext(sslConfig);
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                X509TrustManager trustManager = getTrustManager(sslConfig);
                builder.sslSocketFactory(sslSocketFactory, trustManager);
            }
        } catch (Exception e) {
            throw new RuntimeException("SSL configuration failed", e);
        }
    }

    private X509TrustManager createInsecureTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    private SSLSocketFactory createInsecureSSLSocketFactory(X509TrustManager trustManager) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    private SSLContext createCustomSslContext(OkHttpProperties.ChannelConfig.SslConfig sslConfig) throws Exception {
        KeyStore keyStore = loadKeyStore(sslConfig.getKeyStorePath(), sslConfig.getKeyStorePassword());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, sslConfig.getKeyStorePassword().toCharArray());

        KeyStore trustStore = loadKeyStore(sslConfig.getTrustStorePath(), sslConfig.getTrustStorePassword());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private KeyStore loadKeyStore(String path, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream is = new FileInputStream(path)) {
            keyStore.load(is, password.toCharArray());
        }
        return keyStore;
    }

    private X509TrustManager getTrustManager(OkHttpProperties.ChannelConfig.SslConfig sslConfig) throws Exception {
        KeyStore trustStore = loadKeyStore(sslConfig.getTrustStorePath(), sslConfig.getTrustStorePassword());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }
        throw new IllegalStateException("No X509TrustManager found");
    }


    private void configureProxy(OkHttpProperties.ChannelConfig.ProxyConfig proxyConfig, OkHttpClient.Builder builder) {
        if (proxyConfig != null && proxyConfig.getHost() != null) {
            Proxy.Type type = Proxy.Type.valueOf(proxyConfig.getType().toUpperCase());
            Proxy proxy = new Proxy(type, new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort()));
            builder.proxy(proxy);

            if (proxyConfig.getUsername() != null) {
                Authenticator authenticator = (route, response) -> {
                    String credential = Credentials.basic(proxyConfig.getUsername(), proxyConfig.getPassword());
                    return response.request().newBuilder().header("Proxy-Authorization", credential).build();
                };
                builder.proxyAuthenticator(authenticator);
            }
        }
    }

    private void configureAuth(OkHttpProperties.ChannelConfig.AuthConfig authConfig, OkHttpClient.Builder builder) {
        if (authConfig.getType() != null) {
            builder.addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder();

                switch (authConfig.getType().toUpperCase()) {
                    case "BEARER":
                        requestBuilder.header("Authorization", "Bearer " + authConfig.getToken());
                        break;
                    case "BASIC":
                        String credential = Credentials.basic(authConfig.getUsername(), authConfig.getPassword());
                        requestBuilder.header("Authorization", credential);
                        break;
                }
                return chain.proceed(requestBuilder.build());
            });
        }
    }

    private Interceptor createLoggingInterceptor(String level) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.valueOf(level.toUpperCase()));
        return interceptor;
    }
}