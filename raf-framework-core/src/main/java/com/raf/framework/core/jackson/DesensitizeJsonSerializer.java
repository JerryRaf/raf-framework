package com.raf.framework.core.jackson;

import com.raf.framework.core.util.DesensitizeUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * 脱敏JSON序列化器
 * 根据字段名和脱敏策略进行序列化，支持数值类型
 */
class DesensitizeJsonSerializer extends StdSerializer<Object> {

    protected DesensitizeJsonSerializer() {
        super(Object.class);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        // 获取当前字段名
        String fieldName = gen.getOutputContext().getCurrentName();

        // 判断是否为敏感字段
        if (DesensitizeUtil.isSensitiveField(fieldName)) {
            // 获取脱敏策略
            DesensitizeUtil.DesensitizeStrategy strategy = DesensitizeUtil.getDesensitizeStrategy(fieldName);

            // 应用脱敏策略
            String desensitizedValue = strategy.apply(value);
            gen.writeString(desensitizedValue);
        } else {
            // 非敏感字段，按照原类型序列化
            if (value instanceof String) {
                gen.writeString((String) value);
            } else if (value instanceof Integer) {
                gen.writeNumber((Integer) value);
            } else if (value instanceof Long) {
                gen.writeNumber((Long) value);
            } else if (value instanceof Double) {
                gen.writeNumber((Double) value);
            } else if (value instanceof Float) {
                gen.writeNumber((Float) value);
            } else if (value instanceof BigDecimal) {
                gen.writeNumber((BigDecimal) value);
            } else if (value instanceof BigInteger) {
                gen.writeNumber((BigInteger) value);
            } else if (value instanceof Boolean) {
                gen.writeBoolean((Boolean) value);
            } else {
                provider.defaultSerializeValue(value, gen);
            }
        }
    }

}
