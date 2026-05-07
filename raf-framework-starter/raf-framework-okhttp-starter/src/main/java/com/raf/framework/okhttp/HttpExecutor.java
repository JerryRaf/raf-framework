package com.raf.framework.okhttp;

import java.io.IOException;
import java.util.Map;
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

    public Response get(String channel, String url) throws IOException {
        return execute(channel, buildRequest(url, null, null, "GET", null));
    }

    public Response get(String channel, String url, Map<String, String> headers, Map<String, String> queryParams) throws IOException {
        return execute(channel, buildRequest(url, headers, null, "GET", queryParams));
    }

    public Response postJson(String channel, String url, String json) throws IOException {
        return execute(channel, buildJsonRequest(url, null, json, "POST"));
    }

    public Response postJson(String channel, String url, String json, Map<String, String> headers) throws IOException {
        return execute(channel, buildJsonRequest(url, headers, json, "POST"));
    }

    public Response postForm(String channel, String url, Map<String, String> formData) throws IOException {
        return postForm(channel, url, formData, null);
    }

    public Response postForm(String channel, String url, Map<String, String> formData, Map<String, String> headers) throws IOException {
        FormBody body = buildFormBody(formData);
        return execute(channel, buildRequest(url, headers, body, "POST", null));
    }

    public Response putJson(String channel, String url, String json) throws IOException {
        return execute(channel, buildJsonRequest(url, null, json, "PUT"));
    }

    public Response putJson(String channel, String url, String json, Map<String, String> headers) throws IOException {
        return execute(channel, buildJsonRequest(url, headers, json, "PUT"));
    }

    public Response putForm(String channel, String url, Map<String, String> formData) throws IOException {
        return putForm(channel, url, formData, null);
    }

    public Response putForm(String channel, String url, Map<String, String> formData, Map<String, String> headers) throws IOException {
        FormBody body = buildFormBody(formData);
        return execute(channel, buildRequest(url, headers, body, "PUT", null));
    }

    public Response delete(String channel, String url, Map<String, String> headers) throws IOException {
        return execute(channel, buildRequest(url, headers, null, "DELETE", null));
    }

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