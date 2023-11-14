package com.raf.framework.autoconfigure.servlet.log;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.raf.framework.autoconfigure.common.result.RafResult;
import com.raf.framework.autoconfigure.jackson.Json;
import com.raf.framework.autoconfigure.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j(topic = "Raflog")
public class AccessJsonLogBuilder {

    private Json json;
    private AuditProperties auditProperties;

    protected Map<String, Object> data;

    public AccessJsonLogBuilder(Json json, AuditProperties auditProperties) {
        data = Maps.newLinkedHashMap();
        this.json = json;
        this.auditProperties = auditProperties;
        put("TIME", DateUtil.format(DateUtil.date(), DatePattern.NORM_DATETIME_MS_PATTERN));
    }

    public static AccessJsonLogBuilder accessJsonLogBuilder(Json json, AuditProperties auditProperties) {
        return new AccessJsonLogBuilder(json, auditProperties);
    }

    public AccessJsonLogBuilder put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public AccessJsonLogBuilder put(HttpServletRequest request) {
        int level = auditProperties.getLog().level.getLevel();
        if (level >= AuditProperties.LogLevel.BASIC.getLevel()) {
            this.put("Q_URL", AuditLogUtil.getRequestUri(request))
                    .put("Q_METHOD", request.getMethod())
                    .put("Q_PARAMS", AuditLogUtil.getRequestParams(request));
        }

        if (level >= AuditProperties.LogLevel.REQ_HEADERS.getLevel()) {
            this.put("Q_IP", IpUtil.getIpAddr(request))
                    .put("Q_HEADERS", AuditLogUtil.getAllHeaders(request));
        }
        return this;
    }

    public AccessJsonLogBuilder addRequestBody(String requestPayLoad) {
        if (auditProperties.getLog().level.getLevel() >= AuditProperties.LogLevel.REQ_BODY.getLevel()) {
            this.put("Q_BODY", requestPayLoad);
        }
        return this;
    }

    public AccessJsonLogBuilder put(HttpServletResponse response) {
        if (auditProperties.getLog().level.getLevel() >= AuditProperties.LogLevel.RSP_HEADERS.getLevel()) {
            return this.put("R_HEADERS", AuditLogUtil.getAllHeaders(response))
                    .put("R_CODE", response.getStatus());
        }
        return this;
    }

    public AccessJsonLogBuilder addResponseBody(final String responsePayLoad, HttpServletResponse response) {
        String contentType = response.getContentType();
        try {
            Optional.ofNullable(Strings.emptyToNull(contentType))
                    .filter(c -> c.startsWith("application/json"))
                    .ifPresent(c -> {
                        String startStr = "{";
                        String endStr = "}";
                        if (responsePayLoad.startsWith(startStr) && responsePayLoad.endsWith(endStr)) {
                            try {
                                Map<String, Object> map = json.strToObj(responsePayLoad, new TypeReference<Map<String, Object>>() {
                                });
                                String code = String.valueOf(map.get("code"));
                                if (StringUtils.isNotBlank(code)) {
                                    this.put("R_CODE", code);
                                }
                            } catch (Exception e) {
                                log.warn("get result code: {}", e.getMessage());
                                this.put("R_CODE", String.valueOf(RafResult.success().getCode()));
                            }
                        } else {
                            this.put("R_CODE", String.valueOf(RafResult.success().getCode()));
                        }

                    });
        } catch (Exception ex) {
            log.error("日志-获取responseBody失败:", ex);
        }
        if (auditProperties.getLog().level.getLevel() >= AuditProperties.LogLevel.RSP_BODY.getLevel()) {
            this.put("R_BODY", responsePayLoad);
        }
        return this;
    }

    public AccessJsonLogBuilder put(Throwable throwable) {
        return this.put("E_TRACE", AuditLogUtil.getTrace(throwable));
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void log() {
        log.info(json.objToString(getData()));
    }
}
