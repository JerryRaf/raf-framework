package com.raf.framework.okhttp;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * OkHttp 客户端执行器
 * <p>
 * 提供了常用的 HTTP 请求方法封装，支持多个渠道配置不同的 OkHttpClient
 * 支持 GET、POST、PUT、DELETE 等常见HTTP方法
 * </p>
 *
 * @author Jerry
 */
@Slf4j
@ConditionalOnClass({
        OkHttpClient.class,
        RequestBody.class,
        FormBody.class,
        HttpUrl.class
})
public class HttpExecutor {
    private final ApplicationContext applicationContext;

    public HttpExecutor(ApplicationContext applicationContext) {
        Assert.notNull(applicationContext, "ApplicationContext must not be null");
        this.applicationContext = applicationContext;
    }

    /**
     * Execute GET request and return response body as string.
     * Response body is automatically closed after reading.
     */
    public String get(String channel, String url) throws IOException {
        return executeToString(channel, buildRequest(url, null, null, "GET", null));
    }

    /**
     * Execute GET request with headers and query params, return response body as string.
     */
    public String get(String channel, String url, Map<String, String> headers, Map<String, String> queryParams) throws IOException {
        return executeToString(channel, buildRequest(url, headers, null, "GET", queryParams));
    }

    /**
     * Execute POST JSON request and return response body as string.
     */
    public String postJson(String channel, String url, String json) throws IOException {
        return executeToString(channel, buildJsonRequest(url, null, json, "POST"));
    }

    /**
     * Execute POST JSON request with headers, return response body as string.
     */
    public String postJson(String channel, String url, String json, Map<String, String> headers) throws IOException {
        return executeToString(channel, buildJsonRequest(url, headers, json, "POST"));
    }

    /**
     * Execute POST form request and return response body as string.
     */
    public String postForm(String channel, String url, Map<String, String> formData) throws IOException {
        return postForm(channel, url, formData, null);
    }

    /**
     * Execute POST form request with headers, return response body as string.
     */
    public String postForm(String channel, String url, Map<String, String> formData, Map<String, String> headers) throws IOException {
        FormBody body = buildFormBody(formData);
        return executeToString(channel, buildRequest(url, headers, body, "POST", null));
    }

    /**
     * Execute PUT JSON request and return response body as string.
     */
    public String putJson(String channel, String url, String json) throws IOException {
        return executeToString(channel, buildJsonRequest(url, null, json, "PUT"));
    }

    /**
     * Execute PUT JSON request with headers, return response body as string.
     */
    public String putJson(String channel, String url, String json, Map<String, String> headers) throws IOException {
        return executeToString(channel, buildJsonRequest(url, headers, json, "PUT"));
    }

    /**
     * Execute PUT form request and return response body as string.
     */
    public String putForm(String channel, String url, Map<String, String> formData) throws IOException {
        return putForm(channel, url, formData, null);
    }

    /**
     * Execute PUT form request with headers, return response body as string.
     */
    public String putForm(String channel, String url, Map<String, String> formData, Map<String, String> headers) throws IOException {
        FormBody body = buildFormBody(formData);
        return executeToString(channel, buildRequest(url, headers, body, "PUT", null));
    }

    /**
     * Execute DELETE request and return response body as string.
     */
    public String delete(String channel, String url, Map<String, String> headers) throws IOException {
        return executeToString(channel, buildRequest(url, headers, null, "DELETE", null));
    }

    /**
     * Execute request and return response body as string.
     * Response body is automatically closed after reading to prevent connection leaks.
     */
    private String executeToString(String channel, Request request) throws IOException {
        try (Response response = execute(channel, request)) {
            ResponseBody body = response.body();
            return body != null ? body.string() : "";
        }
    }

    /**
     * Execute request and return raw Response.
     * <p>
     * <b>Warning:</b> Caller MUST close the response body to avoid connection leaks.
     * Prefer using the string-returning methods instead.
     * </p>
     */
    private Response execute(String channel, Request request) throws IOException {
        OkHttpClient client = getChannelClient(channel);
        return client.newCall(request).execute();
    }

    private OkHttpClient getChannelClient(String channel) {
        return applicationContext.getBean(channel + "-OkHttpClient", OkHttpClient.class);
    }

    private Request buildRequest(String url, Map<String, String> headers, RequestBody body, String method, Map<String, String> queryParams) {
        String finalUrl = buildUrl(url, queryParams);
        Request.Builder builder = new Request.Builder().url(finalUrl).method(method, body);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        return builder.build();
    }

    private Request buildJsonRequest(String url, Map<String, String> headers, String json, String method) {
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        return buildRequest(url, headers, body, method, null);
    }

    private FormBody buildFormBody(Map<String, String> formData) {
        FormBody.Builder builder = new FormBody.Builder();
        if (formData != null) {
            formData.forEach(builder::add);
        }
        return builder.build();
    }

    private String buildUrl(String baseUrl, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return baseUrl;
        }

        HttpUrl parsedUrl = HttpUrl.parse(baseUrl);
        if (parsedUrl == null) {
            throw new IllegalArgumentException("Invalid URL: " + baseUrl);
        }

        HttpUrl.Builder urlBuilder = parsedUrl.newBuilder();
        queryParams.forEach(urlBuilder::addQueryParameter);
        return urlBuilder.build().toString();
    }
}