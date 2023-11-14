package com.raf.framework.autoconfigure.spring.valid;

import com.raf.framework.autoconfigure.common.exception.BusinessException;
import com.raf.framework.autoconfigure.common.result.IResponseEnum;

import javax.annotation.Nullable;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class Assert {
    public static void notNull(@Nullable Object object, IResponseEnum iResponseEnum) {
        if (null == object) {
            throw new BusinessException(iResponseEnum);
        }
    }

}