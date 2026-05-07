package com.raf.framework.core.jackson;

import com.raf.framework.core.util.DesensitizeUtil;

import java.util.List;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 脱敏Bean序列化修改器
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DesensitizeBeanSerializerModifier extends BeanSerializerModifier {

    // 全掩码序列化器
    private JsonSerializer<Object> fullMaskSerializer = new FullMaskSerializer();

    // 自定义策略序列化器
    private JsonSerializer<Object> desensitizeSerializer = new DesensitizeJsonSerializer();

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        // 循环所有的beanPropertyWriter
        for (BeanPropertyWriter writer : beanProperties) {
            String fieldName = writer.getName();

            // 判断是否为敏感字段
            if (DesensitizeUtil.isSensitiveField(fieldName)) {
                // 获取脱敏策略
                DesensitizeUtil.DesensitizeStrategy strategy = DesensitizeUtil.getDesensitizeStrategy(fieldName);

                if (strategy == DesensitizeUtil.DesensitizeStrategy.FULL) {
                    // 全掩码
                    writer.assignSerializer(fullMaskSerializer);
                } else {
                    // 其他策略
                    writer.assignSerializer(desensitizeSerializer);
                }
            }
        }
        return beanProperties;
    }
}
