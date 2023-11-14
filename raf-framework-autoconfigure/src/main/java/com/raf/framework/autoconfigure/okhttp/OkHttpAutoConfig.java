package com.raf.framework.autoconfigure.okhttp;

import com.raf.framework.autoconfigure.jackson.Json;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.okhttp.LogbookInterceptor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "raf.okhttp.enabled")
@ConditionalOnClass(OkHttpClient.class)
public class OkHttpAutoConfig {

    private final Logbook logbook;

    @Autowired
    private HttpLoggingInterceptor httpLoggingInterceptor;

    public OkHttpAutoConfig(Logbook logbook) {
        this.logbook = logbook;
    }

    private OkHttpClient.Builder createBuilder(OkHttpClientProperties okHttpClientProperties,
                                               ConnectionPool connectionPool) {

        return new OkHttpClient.Builder()
                .readTimeout(okHttpClientProperties.getReadTimeout(), TimeUnit.MILLISECONDS)
                .connectTimeout(okHttpClientProperties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(okHttpClientProperties.getWriteTimeout(), TimeUnit.MILLISECONDS)
                .connectionPool(connectionPool)
                .followRedirects(okHttpClientProperties.isFollowRedirects())
                .retryOnConnectionFailure(okHttpClientProperties.isRetryOnConnectionFailure())
                .addNetworkInterceptor(new LogbookInterceptor(logbook))
                .addInterceptor(httpLoggingInterceptor);
    }


    @Bean
    @ConditionalOnMissingBean
    public ConnectionPool connectionPool(OkHttpClientProperties okHttpClientProperties) {
        OkHttpClientProperties.Connection connection = okHttpClientProperties.getConnection();
        return new ConnectionPool(connection.getMaxIdleConnections(),
                connection.getKeepAliveDuration(), TimeUnit.MILLISECONDS);
    }


    @Bean("rafOkHttpClient")
    @ConditionalOnBean(Json.class)
    public RafOkHttpClient rafOkHttpClient(OkHttpClientProperties okHttpClientProperties,
                                           ConnectionPool connectionPool, Json json) {
        return new RafOkHttpClient(createBuilder(okHttpClientProperties, connectionPool).build(), json);
    }

    @Bean("rafSslOkHttpClient")
    @ConditionalOnBean(Json.class)
    public RafOkHttpClient rafSslOkHttpClient(SslOkHttp sslOkHttp, Json json) {
        return new RafOkHttpClient(sslOkHttp.okHttpClient(), json);
    }

    @Bean
    public SslOkHttp sslOkHttp(OkHttpClientProperties okHttpClientProperties,
                               ConnectionPool connectionPool) throws Exception {
        OkHttpClient.Builder builder = createBuilder(okHttpClientProperties, connectionPool);
        final X509TrustManager trustManager = new X509TrustManagerImpl();
        SSLSocketFactory sslSocketFactory;
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        sslSocketFactory = sslContext.getSocketFactory();
        builder.sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier((hostname, session) -> true);
        return new SslOkHttp(builder.build());
    }

    private static class X509TrustManagerImpl implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            checkTrusted(chain,authType,false);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            checkTrusted(chain,authType,true);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public static List<String> chains = Collections.singletonList("chains");
        public static void checkTrusted(X509Certificate[] chain, String authType, boolean server) {
            if (!chains.contains(authType)) {
            }
        }
    }
}
