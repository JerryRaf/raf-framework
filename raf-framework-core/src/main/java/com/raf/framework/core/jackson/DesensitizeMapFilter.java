package com.raf.framework.core.jackson;

import com.raf.framework.core.util.DesensitizeUtil;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

/**
 * Map脱敏过滤器
 */
@JsonFilter("desensitizeMapFilter")
public class DesensitizeMapFilter implements PropertyFilter {

    @Override
    public void serializeAsField(Object pojo, JsonGenerator gen, SerializerProvider provider,
                                 PropertyWriter writer) throws Exception {

        if (pojo instanceof Map) {
            // 处理Map类型
            Map<?, ?> map = (Map<?, ?>) pojo;
            String fieldName = writer.getName();
            Object value = map.get(fieldName);

            // 判断是否为敏感字段
            if (DesensitizeUtil.isSensitiveField(fieldName) && value != null) {
                // 获取脱敏策略
                DesensitizeUtil.DesensitizeStrategy strategy = DesensitizeUtil.getDesensitizeStrategy(fieldName);

                // 应用脱敏策略
                String desensitizedValue = strategy.apply(value);

                // 使用JsonGenerator写入字段，而不是调用writer.serializeAsField
                gen.writeFieldName(fieldName);
                gen.writeString(desensitizedValue);
            } else {
                // 非敏感字段，正常序列化
                writer.serializeAsField(pojo, gen, provider);
            }
        } else {
            // 非Map类型，正常序列化
            writer.serializeAsField(pojo, gen, provider);
        }
    }

    @Override
    public void serializeAsElement(Object element, JsonGenerator gen, SerializerProvider provider,
                                   PropertyWriter writer) throws Exception {
        writer.serializeAsElement(element, gen, provider);
    }

    @Override
    public void depositSchemaProperty(PropertyWriter writer, ObjectNode objectNode,
                                      SerializerProvider provider) throws JsonMappingException {
        // 无需实现
    }

    @Override
    public void depositSchemaProperty(PropertyWriter writer, JsonObjectFormatVisitor visitor,
                                      SerializerProvider provider) throws JsonMappingException {
        // 无需实现
    }
}
