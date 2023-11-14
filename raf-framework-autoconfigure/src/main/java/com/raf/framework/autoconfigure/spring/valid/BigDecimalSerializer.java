package com.raf.framework.autoconfigure.spring.valid;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal序列号
 * @author Jerry
 * @date 2019/01/01
 */
public class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (null != value) {
            BigDecimal number = value.setScale(2, RoundingMode.HALF_UP);
            gen.writeNumber(number);
        }
    }
}
