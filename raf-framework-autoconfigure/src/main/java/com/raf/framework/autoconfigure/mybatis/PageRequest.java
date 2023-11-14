package com.raf.framework.autoconfigure.mybatis;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 通用分页输入
 * @author Jerry
 * @date 2019/01/01
 */
@Data
@ToString(includeFieldNames = false, doNotUseGetters = true)
public class PageRequest {
    /**
     * 第几页，1-100
     */
    @NotNull(message = "请选择页数")
    @Min(value = 1, message = "当前页数不存在")
    @Max(value = 100, message = "当前页数不存在")
    public Integer pageNum=1;

    /**
     * 每页大小，最大200
     */
    @NotNull(message = "请选择每页大小")
    @Min(value = 1, message = "请选择每页大小")
    @Max(value = 200, message = "每页大小超过了最大值")
    public Integer pageSize=10;
}
