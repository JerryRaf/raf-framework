package com.raf.framework.mybatisplus.extension.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Page request parameter with validation
 *
 * @author Jerry
 * @since 2026-04-20
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Page number, starting from 1
     */
    @NotNull(message = "页码不能为null")
    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNum;

    /**
     * Page size, range [1, 1000]
     */
    @NotNull(message = "每页数量不能为null")
    @Min(value = 1, message = "每页数量最小值为1")
    @Max(value = 1000, message = "每页数量最大值为1000")
    private Integer pageSize;
}
