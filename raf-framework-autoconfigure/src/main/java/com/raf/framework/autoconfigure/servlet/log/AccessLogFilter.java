package com.raf.framework.autoconfigure.servlet.log;

import com.raf.framework.autoconfigure.jackson.Json;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class AccessLogFilter extends OncePerRequestFilter {

    @Autowired
    private Json json;

    @Autowired
    private AuditProperties auditProperties;

    @Getter
    private Integer payloadMaxLength = 4096;

    private static final String DEFAULT_SKIP_PATTERN =
            "//null/swagger.*|/csrf|/|/v3/api-docs|/v2/api-docs|/api-docs.*|/swagger.*|/webjars.*|.*\\.png|.*\\.css|.*\\.js|.*\\.html|/favicon.ico|/actuator.*|/hystrix.stream";

    private static final Pattern SKIP_PATTERNS = Pattern.compile(DEFAULT_SKIP_PATTERN);

    private boolean unContain(HttpServletRequest request) {
        String path = request.getServletPath();
        return !SKIP_PATTERNS.matcher(path).matches();
    }

    private boolean isNormalRequest(HttpServletRequest request) {
        return !isMultipart(request) && !isBinaryContent(request) && unContain(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        LogHolder.setCurrentLogResponse(false);

        if (auditProperties.getLog().level.equals(AuditProperties.LogLevel.OFF) || !isNormalRequest(request)) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                LogHolder.remove();
            }
            return;
        }

        StopWatch watch = new StopWatch();
        watch.start();
        final boolean isFirstExecution = !isAsyncDispatch(request);
        final boolean isLastExecution = !isAsyncStarted(request);
        HttpServletRequestAdapter requestWrapper = null;
        ContentCachingResponseWrapper responseWrapper = null;
        AccessJsonLogBuilder accessJsonLogBuilder = null;
        try {
            if (isFirstExecution && !(request instanceof HttpServletRequestAdapter)) {
                byte[] bytes = IOUtils.toByteArray(request.getInputStream());
                requestWrapper = new HttpServletRequestAdapter(request, bytes);
                responseWrapper = new ContentCachingResponseWrapper(response);

                String requestPayload = getPayLoad(bytes, request.getCharacterEncoding());
                accessJsonLogBuilder = AccessJsonLogBuilder
                        .accessJsonLogBuilder(json, auditProperties)
                        .put(request)
                        .addRequestBody(requestPayload);
            }
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            try {
                requestWrapper = Optional.ofNullable(requestWrapper).orElseGet(
                        () -> getHttpServletRequestAdapter(request)
                );
                if (requestWrapper != null) {
                    responseWrapper = Optional.ofNullable(responseWrapper)
                            .orElseGet(() -> getContentCachingResponseWrapper(response));

                    boolean flag = isLastExecution && !isBinaryContent(response) && !isMultipart(response)
                            && responseWrapper != null && (LogHolder.currentLogResponse() || (
                            auditProperties.getLog().level.getLevel() >= AuditProperties.LogLevel.RSP_HEADERS.getLevel()));
                    if (flag) {
                        String responsePayload = getPayLoad(responseWrapper.getContentAsByteArray(),
                                response.getCharacterEncoding());
                        responseWrapper.copyBodyToResponse();

                        Optional.ofNullable(accessJsonLogBuilder).ifPresent(c ->
                                c.addResponseBody(responsePayload, response)
                                        .put(response).put("COST", watch.getTime()).log()
                        );

                    } else if (isLastExecution) {
                        responseWrapper.copyBodyToResponse();
                        Optional.ofNullable(accessJsonLogBuilder).ifPresent(c ->
                                c.put(response).put("COST", watch.getTime()).log()
                        );
                    }
                }
            } catch (Exception e) {
                logger.error("accessLogFilter error", e);
            }
            LogHolder.remove();
        }
    }

    private HttpServletRequestAdapter getHttpServletRequestAdapter(HttpServletRequest httpServletRequest) {
        if (httpServletRequest instanceof HttpServletRequestAdapter) {
            return (HttpServletRequestAdapter) httpServletRequest;
        }
        return null;
    }

    private ContentCachingResponseWrapper getContentCachingResponseWrapper(HttpServletResponse httpServletResponse) {
        if (httpServletResponse instanceof ContentCachingResponseWrapper) {
            return (ContentCachingResponseWrapper) httpServletResponse;
        }
        return null;
    }

    private String getPayLoad(byte[] buf, String characterEncoding) {
        String payload = "";
        if (buf == null) {
            return payload;
        }
        if (buf.length > 0) {
            int length = Math.min(buf.length, getPayloadMaxLength());
            try {
                payload = new String(buf, 0, length, characterEncoding);
            } catch (UnsupportedEncodingException ex) {
                payload = "[unknown]";
            }
        }
        return payload;
    }

    private boolean isMultipart(final HttpServletRequest request) {
        return request.getContentType() != null && request.getContentType().startsWith("multipart/form-data");
    }

    private boolean isBinaryContent(final HttpServletRequest request) {
        if (request.getContentType() == null) {
            return false;
        }
        return request.getContentType().startsWith("image") || request.getContentType()
                .startsWith("video") || request.getContentType().startsWith("audio");
    }

    private boolean isBinaryContent(final HttpServletResponse response) {
        return response.getContentType() != null && (response.getContentType().startsWith("image")
                || response.getContentType().startsWith("video") || response.getContentType()
                .startsWith("audio"));
    }

    private boolean isMultipart(final HttpServletResponse response) {
        return response.getContentType() != null && (
                response.getContentType().startsWith("multipart/form-data") || response.getContentType()
                        .startsWith("application/octet-stream"));
    }

    /**
     * query header参数前后空格去除，Xss转义
     */
    public static class HttpServletRequestAdapter extends HttpServletRequestWrapper {

        private InputStream inputStream;

        public HttpServletRequestAdapter(HttpServletRequest request, byte[] payload) {
            super(request);
            inputStream = new ByteArrayInputStream(payload);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return false;
                }

                @Override
                public boolean isReady() {
                    return false;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                }

                @Override
                public int read() throws IOException {
                    return inputStream.read();
                }
            };
        }

        /**
         * 获取多个参数值逐一清洁
         * @param name
         * @return
         */
        @Override
        public String[] getParameterValues(String name) {
            String[] parameterValues = super.getParameterValues(name);
            if (null == parameterValues || parameterValues.length == 0) {
                return new String[0];
            }
            return Arrays.stream(parameterValues).map(this::clean).toArray(String[]::new);
        }

        /**
         *
         * @param name
         * @return
         */
        @Override
        public String getHeader(String name) {
            if (null == name) {
                return null;
            }
            return clean(super.getHeader(name));
        }

        /**
         * 获取单个参数清洁
         * @param parameter
         * @return
         */
        @Override
        public String getParameter(String parameter) {
            if (null == parameter) {
                return null;
            }
            return clean(super.getParameter(parameter));
        }

        private String clean(String value) {
            return StringUtils.isEmpty(value) ? "" : HtmlUtils.htmlEscape(value.trim());
        }
    }
}
