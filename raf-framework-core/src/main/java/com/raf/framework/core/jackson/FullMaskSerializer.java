package com.raf.framework.core.jackson;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * 全掩码序列化器
 * 将任何值都序列化为"***"
 */
class FullMaskSerializer extends StdSerializer<Object> {

    protected FullMaskSerializer() {
        super(Object.class);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // 全掩码，无论什么类型都返回"***"
        gen.writeString("***");
    }
}
