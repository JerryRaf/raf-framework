package com.raf.framework.web.servlet.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

/**
 * API version request condition.
 * Matches URI pattern like /v1/** and supports version fallback:
 * a handler with lower version can serve higher version requests.
 *
 * @author Jerry
 * @since 2019-01-01
 */
public class ApiVersionCondition implements RequestCondition<ApiVersionCondition> {

    /**
     * URI version prefix pattern, e.g. /v1/.
     */
    private static final Pattern VERSION_PREFIX_PATTERN = Pattern.compile("v(\\d+)/");

    /**
     * API version declared on handler.
     */
    private final int apiVersion;

    public ApiVersionCondition(int apiVersion) {
        this.apiVersion = apiVersion;
    }

    /**
     * Combine conditions: method-level version overrides type-level version.
     */
    @Override
    public ApiVersionCondition combine(ApiVersionCondition apiVersionCondition) {
        return new ApiVersionCondition(apiVersionCondition.getApiVersion());
    }

    /**
     * Match request URI with version rule.
     */
    @Nullable
    @Override
    public ApiVersionCondition getMatchingCondition(HttpServletRequest httpServletRequest) {
        Matcher matcher = VERSION_PREFIX_PATTERN.matcher(httpServletRequest.getRequestURI());
        if (matcher.find()) {
            int version = Integer.parseInt(matcher.group(1));
            if (version >= this.apiVersion) {
                return this;
            }
        }
        return null;
    }

    /**
     * Sort conditions: higher version has higher priority.
     */
    @Override
    public int compareTo(ApiVersionCondition apiVersionCondition, HttpServletRequest httpServletRequest) {
        return apiVersionCondition.getApiVersion() - this.apiVersion;
    }

    public int getApiVersion() {
        return apiVersion;
    }
}
