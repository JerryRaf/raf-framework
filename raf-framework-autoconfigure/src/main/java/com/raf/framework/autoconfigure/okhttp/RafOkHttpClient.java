package com.raf.framework.autoconfigure.okhttp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.raf.framework.autoconfigure.common.exception.ComponentException;
import com.raf.framework.autoconfigure.common.result.RafResponseEnum;
import com.raf.framework.autoconfigure.common.result.RafResult;
import com.raf.framework.autoconfigure.jackson.Json;
import io.netty.channel.ConnectTimeoutException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.Request.Builder;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
public class RafOkHttpClient {

    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient okHttpClient;
    private final Json json;

    public RafOkHttpClient(OkHttpClient okHttpClient, Json json) {
        this.okHttpClient = okHttpClient;
        this.json = json;
    }

    public <T> RafResult<T> get(OkHttpClientBuilder okHttpClientBuilder) {
        checkBasicParam(okHttpClientBuilder);
        Builder builder = createRequestBuilder(createGetUrl(okHttpClientBuilder.getUrl(), okHttpClientBuilder.getParams()), okHttpClientBuilder.getHeaders());
        return returnResult(builder.get().build(), okHttpClientBuilder.getTypeReference());
    }

    public <T> RafResult<T> put(OkHttpClientBuilder okHttpClientBuilder) {
        checkBasicParam(okHttpClientBuilder);
        Builder builder = createRequestBuilder(okHttpClientBuilder.getUrl(),
                okHttpClientBuilder.getHeaders());

        FormBody.Builder formBuilder = new FormBody.Builder();
        Optional.ofNullable(okHttpClientBuilder.getParams()).ifPresent(c -> c.forEach(
                (key, value) -> {
                    if (value != null) {
                        formBuilder.add(key, value);
                    }
                }
        ));

        return returnResult(builder.put(formBuilder.build()).build(),
                okHttpClientBuilder.getTypeReference()
        );
    }

    public <T> RafResult<T> post(OkHttpClientBuilder okHttpClientBuilder) {
        checkBasicParam(okHttpClientBuilder);
        Builder builder = createRequestBuilder(okHttpClientBuilder.getUrl(),
                okHttpClientBuilder.getHeaders());

        FormBody.Builder formBuilder = new FormBody.Builder();
        Optional.ofNullable(okHttpClientBuilder.getParams()).ifPresent(c -> c.forEach(
                (key, value) -> {
                    if (value != null) {
                        formBuilder.add(key, value);
                    }
                }
        ));

        return returnResult(builder.post(formBuilder.build()).build(),
                okHttpClientBuilder.getTypeReference()
        );
    }

    public <T> RafResult<T> postEncoded(OkHttpClientBuilder okHttpClientBuilder, List<String> excludeEncode) {
        checkBasicParam(okHttpClientBuilder);
        Builder builder = createRequestBuilder(okHttpClientBuilder.getUrl(), okHttpClientBuilder.getHeaders());

        FormBody.Builder formBuilder = new FormBody.Builder();
        Optional.ofNullable(okHttpClientBuilder.getParams()).ifPresent(c -> c.forEach(
                (key, value) -> {
                    if (value != null) {
                        if (!excludeEncode.contains(key)) {
                            formBuilder.add(key, value);
                        } else {
                            formBuilder.addEncoded(key, value);
                        }
                    }
                }
        ));

        return returnResult(builder.post(formBuilder.build()).build(),
                okHttpClientBuilder.getTypeReference()
        );
    }

    public <T> RafResult<T> postJson(OkHttpClientBuilder okHttpClientBuilder) {
        checkBasicParam(okHttpClientBuilder);
        Builder builder = createRequestBuilder(okHttpClientBuilder.getUrl(),
                okHttpClientBuilder.getHeaders());
        checkBody(okHttpClientBuilder.getBody());
        RequestBody body = RequestBody.create(MEDIA_TYPE, okHttpClientBuilder.getBody());

        return returnResult(builder.post(body)
                .build(), okHttpClientBuilder.getTypeReference());
    }

    @SuppressWarnings("unchecked")
    private <T> RafResult<T> returnResult(Request request, TypeReference<T> typeReference) {
        try {
            Response response = this.okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                String result = Objects.requireNonNull(response.body()).string();
                checkResult(result);
                Type type = typeReference.getType();
                if (isPrimitiveType(type)) {
                    return (RafResult<T>) RafResult.success(result);
                }

                T t = json.strToObj(result, typeReference);
                if (t instanceof RafResult) {
                    return (RafResult<T>) t;
                } else {
                    return RafResult.success(t);
                }
            } else {
                log.error("http request server error,url:{},code:{}", request.url(), response.code());
                return (RafResult<T>) RafResult.fail(RafResponseEnum.HTTP_ERROR_800, Objects.requireNonNull(response.body()).string());
            }
        } catch (ConnectTimeoutException ex) {
            log.error("http connectTimeout,url:{},ex:{}", request.url(), ex.getMessage());
            throw new ComponentException(RafResponseEnum.HTTP_ERROR_800.getCode(), ex.getMessage());
        } catch (SocketTimeoutException ex) {
            log.error("http socketTimeout,url:{},ex:{}", request.url(), ex.getMessage());
            throw new ComponentException(RafResponseEnum.HTTP_ERROR_800.getCode(), ex.getMessage());
        } catch (IOException ex) {
            log.error("http IOException,url:{},ex:{}", request.url(), ex.getMessage());
            throw new ComponentException(RafResponseEnum.HTTP_ERROR_800.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("http other ex,url:{},ex:", request.url(), ex);
            throw new ComponentException(RafResponseEnum.HTTP_ERROR_800.getCode(), ex.getMessage());
        }
    }

    private boolean isPrimitiveType(Type type) {
        if (type instanceof Class) {
            Class clazz = (Class) type;
            return ClassUtils.isPrimitiveOrWrapper(clazz) || ClassUtils
                    .isAssignable(clazz, String.class);
        }
        return false;
    }

    private void checkBody(String body) {
        if (StringUtils.isBlank(body)) {
            throw new RuntimeException("http请求body不能为空!");
        }
    }

    private void checkResult(String result) {
        if (StringUtils.isBlank(result)) {
            throw new RuntimeException("http请求返回值为空!");
        }
    }

    private Builder createRequestBuilder(String url, Map<String, String> headers) {
        Builder builder = new Builder();
        builder.url(url);
        if (null != headers) {
            headers.forEach(builder::addHeader);
        }
        return builder;
    }

    private void checkBasicParam(OkHttpClientBuilder okHttpClientBuilder) {
        if (StringUtils.isBlank(okHttpClientBuilder.getUrl())) {
            throw new RuntimeException("http url cannot be empty!");
        }

        if (okHttpClientBuilder.getTypeReference() == null) {
            throw new RuntimeException("http return type cannot be empty!");
        }
    }

    private String createGetUrl(String url, Map<String, String> params) {
        if (CollectionUtils.isEmpty(params)) {
            return url;
        }
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (sb.length() == 0) {
                sb.append("?");
            } else if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(k);
            sb.append("=").append(v);
        });
        return url + sb.toString();
    }

}
