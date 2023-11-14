package com.raf.framework.autoconfigure.okhttp;

import lombok.Data;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
@ConfigurationProperties(prefix = "raf.okhttp")
public class OkHttpClientProperties {
  private boolean enabled;
  private int connectTimeout = 2000;
  private int readTimeout = 5000;
  private int writeTimeout = 5000;
  private boolean retryOnConnectionFailure = true;
  private boolean followRedirects = true;
  private boolean followSslRedirects = true;
  private List<String> interceptors = new ArrayList<>();
  private List<String> networkInterceptors = new ArrayList<>();
  private Connection connection = new Connection();
  private HttpLoggingInterceptor.Level level;

  @Data
  public static class Connection {
    private int maxIdleConnections = 5;
    private long keepAliveDuration = 60_000;
  }
}
