package com.raf.framework.mybatisplus.extension.page;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for PageRequest
 *
 * @author Jerry
 * @since 2026-04-20
 */
class PageRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void testValidPageRequest() {
        PageRequest request = new PageRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        Set<ConstraintViolation<PageRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testPageNumCannotBeNull() {
        PageRequest request = new PageRequest();
        request.setPageSize(10);
        Set<ConstraintViolation<PageRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("不能为null"));
    }

    @Test
    void testPageNumMinValue() {
        PageRequest request = new PageRequest();
        request.setPageNum(0);
        request.setPageSize(10);
        Set<ConstraintViolation<PageRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
    }

    @Test
    void testPageSizeCannotBeNull() {
        PageRequest request = new PageRequest();
        request.setPageNum(1);
        Set<ConstraintViolation<PageRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
    }

    @Test
    void testPageSizeMinValue() {
        PageRequest request = new PageRequest();
        request.setPageNum(1);
        request.setPageSize(0);
        Set<ConstraintViolation<PageRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
    }

    @Test
    void testPageSizeMaxValue() {
        PageRequest request = new PageRequest();
        request.setPageNum(1);
        request.setPageSize(1001);
        Set<ConstraintViolation<PageRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
    }
}
