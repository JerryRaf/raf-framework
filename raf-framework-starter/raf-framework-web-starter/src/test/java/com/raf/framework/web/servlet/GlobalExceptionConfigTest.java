package com.raf.framework.web.servlet;

import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import com.raf.framework.core.common.result.RafResult;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for GlobalExceptionConfig.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class GlobalExceptionConfigTest {

    @Test
    void shouldReturnBusinessFailureResult() {
        GlobalExceptionConfig config = new GlobalExceptionConfig();
        BusinessException exception = new BusinessException(RafResponseEnum.PARAM_ERROR);

        RafResult<?> result = config.handleBusinessException(exception);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.PARAM_ERROR.getCode());
        assertThat(result.getMsg()).isEqualTo(RafResponseEnum.PARAM_ERROR.getMsg());
    }

    @Test
    void shouldReturnNotFoundFailureResult() {
        GlobalExceptionConfig config = new GlobalExceptionConfig();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/not-exist");

        RafResult<?> result = config.handleNotFoundException(request);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(RafResponseEnum.NOT_FOUND.getCode());
        assertThat(result.getMsg()).isEqualTo(RafResponseEnum.NOT_FOUND.getMsg());
    }
}
