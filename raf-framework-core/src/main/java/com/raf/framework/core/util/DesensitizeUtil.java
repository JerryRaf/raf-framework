package com.raf.framework.core.util;


import com.raf.framework.core.jackson.DesensitizeBeanSerializerModifier;
import com.raf.framework.core.jackson.DesensitizeMapFilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于Jackson SerializerFactory的敏感信息脱敏工具类
 * 支持数值类型（如Integer mobile）的脱敏
 * 纯静态工具类，不依赖Spring容器
 */
@Slf4j
public class DesensitizeUtil {

    private DesensitizeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    // 敏感字段配置（转小写全字匹配）
    private static final Set<String> SENSITIVE_FIELDS = ConcurrentHashMap.newKeySet();

    static {
        SENSITIVE_FIELDS.addAll(Arrays.asList(
                "password", "pwd", "passwd", "secret",
                "token", "authtoken", "accesstoken", "refreshtoken",
                "idcard", "idno", "idnumber", "identitycard",
                "mobile", "phone", "telephone", "cellphone",
                "bankcard", "bankaccount", "creditcard", "cardno", "card", "cardnumber", "accountno",
                "cvv", "securitycode", "cvc",
                "realname", "username", "name", "fullname", "nickname",
                "email", "mail", "emailaddress"
        ));
    }

    /**
     * 脱敏策略枚举
     */
    public enum DesensitizeStrategy {
        PASSWORD(value -> "***"),

        MOBILE(value -> {
            if (value == null) {
                return "***";
            }
            String strValue = value.toString();
            if (strValue.length() != 11) {
                return "***";
            }
            return strValue.substring(0, 3) + "****" + strValue.substring(7);
        }),

        ID_CARD(value -> {
            if (value == null) {
                return "***";
            }
            String strValue = value.toString();
            if (strValue.length() == 15) {
                return strValue.substring(0, 6) + "****" + strValue.substring(12);
            } else if (strValue.length() == 18) {
                return strValue.substring(0, 6) + "****" + strValue.substring(14);
            } else if (strValue.length() < 8) {
                return "***";
            } else {
                return strValue.substring(0, 6) + "****" + strValue.substring(strValue.length() - 4);
            }
        }),

        BANK_CARD(value -> {
            if (value == null) {
                return "***";
            }
            String strValue = value.toString();
            if (strValue.length() < 8) {
                return "***";
            }
            int len = strValue.length();
            return strValue.substring(0, 6) + "****" + strValue.substring(len - 4);
        }),

        TOKEN(value -> {
            if (value == null) {
                return "***";
            }
            String strValue = value.toString();
            if (strValue.length() <= 8) {
                return "***";
            }
            int len = strValue.length();
            return strValue.substring(0, 4) + "****" + strValue.substring(len - 4);
        }),

        EMAIL(value -> {
            if (value == null) {
                return "***";
            }
            String strValue = value.toString();
            if (!strValue.contains("@")) {
                return "***";
            }
            int atIndex = strValue.indexOf("@");
            if (atIndex <= 2) {
                return "***" + strValue.substring(atIndex);
            }
            return strValue.substring(0, 2) + "***" + strValue.substring(atIndex);
        }),

        NAME(value -> {
            if (value == null) {
                return "";
            }
            String strValue = value.toString();
            if (strValue.length() == 0) {
                return "";
            }
            if (strValue.length() == 1) {
                return "*";
            }
            if (strValue.length() == 2) {
                return strValue.charAt(0) + "*";
            }
            return strValue.charAt(0) + "**" + strValue.charAt(strValue.length() - 1);
        }),

        PARTIAL(value -> {
            if (value == null) {
                return null;
            }
            String strValue = value.toString();
            int length = strValue.length();
            if (length > 6) {
                return "***" + strValue.substring(length - 4);
            } else if (length > 4) {
                return "***" + strValue.substring(length - 1);
            } else {
                return "***";
            }
        }),

        FULL(value -> "***");

        private final Function<Object, String> desensitizer;

        DesensitizeStrategy(Function<Object, String> desensitizer) {
            this.desensitizer = desensitizer;
        }

        public String apply(Object value) {
            return desensitizer.apply(value);
        }
    }

    private static final Map<String, DesensitizeStrategy> FIELD_STRATEGY_MAPPING = new ConcurrentHashMap<>();
    private static final AtomicReference<ObjectMapper> DESENSITIZE_OBJECT_MAPPER = new AtomicReference<>();
    private static final Object MAPPER_INIT_LOCK = new Object();

    static {
        initDefaultMapping();
    }

    private static void initDefaultMapping() {
        addMapping("password", DesensitizeStrategy.FULL);
        addMapping("pwd", DesensitizeStrategy.FULL);
        addMapping("passwd", DesensitizeStrategy.FULL);
        addMapping("secret", DesensitizeStrategy.FULL);
        addMapping("cvv", DesensitizeStrategy.FULL);
        addMapping("securitycode", DesensitizeStrategy.FULL);
        addMapping("cvc", DesensitizeStrategy.FULL);

        addMapping("token", DesensitizeStrategy.PARTIAL);
        addMapping("authtoken", DesensitizeStrategy.PARTIAL);
        addMapping("accesstoken", DesensitizeStrategy.PARTIAL);
        addMapping("refreshtoken", DesensitizeStrategy.PARTIAL);

        addMapping("mobile", DesensitizeStrategy.MOBILE);
        addMapping("phone", DesensitizeStrategy.MOBILE);
        addMapping("telephone", DesensitizeStrategy.MOBILE);
        addMapping("cellphone", DesensitizeStrategy.MOBILE);

        addMapping("idcard", DesensitizeStrategy.ID_CARD);
        addMapping("idno", DesensitizeStrategy.ID_CARD);
        addMapping("idnumber", DesensitizeStrategy.ID_CARD);
        addMapping("identitycard", DesensitizeStrategy.ID_CARD);

        addMapping("bankcard", DesensitizeStrategy.BANK_CARD);
        addMapping("bankaccount", DesensitizeStrategy.BANK_CARD);
        addMapping("creditcard", DesensitizeStrategy.BANK_CARD);
        addMapping("cardno", DesensitizeStrategy.BANK_CARD);
        addMapping("cardnumber", DesensitizeStrategy.BANK_CARD);
        addMapping("accountno", DesensitizeStrategy.BANK_CARD);

        addMapping("email", DesensitizeStrategy.EMAIL);
        addMapping("mail", DesensitizeStrategy.EMAIL);
        addMapping("emailaddress", DesensitizeStrategy.EMAIL);

        addMapping("realname", DesensitizeStrategy.NAME);
        addMapping("username", DesensitizeStrategy.NAME);
        addMapping("name", DesensitizeStrategy.NAME);
        addMapping("fullname", DesensitizeStrategy.NAME);
        addMapping("nickname", DesensitizeStrategy.NAME);
    }

    public static boolean isSensitiveField(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return false;
        }
        return SENSITIVE_FIELDS.contains(fieldName.toLowerCase());
    }

    public static DesensitizeStrategy getDesensitizeStrategy(String fieldName) {
        if (fieldName == null) {
            return DesensitizeStrategy.PARTIAL;
        }
        return FIELD_STRATEGY_MAPPING.getOrDefault(fieldName.toLowerCase(), DesensitizeStrategy.PARTIAL);
    }

    public static String toJSONString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return getDesensitizeObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            log.error("脱敏JSON序列化失败", e);
            try {
                ObjectMapper defaultMapper = new ObjectMapper();
                return defaultMapper.writeValueAsString(object);
            } catch (Exception ex) {
                throw new RuntimeException("JSON序列化异常", ex);
            }
        }
    }

    public static Map<String, Object> desensitizeMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        try {
            String json = toJSONString(map);
            return getDesensitizeObjectMapper().readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            log.error("Map脱敏处理失败", e);
            return map;
        }
    }

    public static void addCustomStrategy(String fieldName, DesensitizeStrategy strategy) {
        if (fieldName != null && strategy != null) {
            FIELD_STRATEGY_MAPPING.put(fieldName.toLowerCase(), strategy);
            SENSITIVE_FIELDS.add(fieldName.toLowerCase());
        }
    }

    public static void addSensitiveField(String fieldName) {
        if (fieldName != null && !fieldName.trim().isEmpty()) {
            SENSITIVE_FIELDS.add(fieldName.toLowerCase());
        }
    }

    public static void setCustomObjectMapper(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper不能为null");
        }
        synchronized (MAPPER_INIT_LOCK) {
            ObjectMapper mapper = objectMapper.copy();

            DesensitizeBeanSerializerModifier serializerModifier = new DesensitizeBeanSerializerModifier();
            mapper.setSerializerFactory(
                    mapper.getSerializerFactory().withSerializerModifier(serializerModifier)
            );

            DesensitizeMapFilter mapFilter = new DesensitizeMapFilter();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("desensitizeMapFilter", mapFilter);

            mapper.addMixIn(Map.class, DesensitizeMapFilter.class);
            mapper.setFilterProvider(filterProvider);

            DESENSITIZE_OBJECT_MAPPER.set(mapper);
            log.info("已设置自定义的脱敏ObjectMapper");
        }
    }

    public static Set<String> getSensitiveFields() {
        return new HashSet<>(SENSITIVE_FIELDS);
    }

    public static Map<String, DesensitizeStrategy> getFieldStrategyMapping() {
        return new ConcurrentHashMap<>(FIELD_STRATEGY_MAPPING);
    }

    public static void resetToDefault() {
        SENSITIVE_FIELDS.clear();
        SENSITIVE_FIELDS.addAll(Arrays.asList(
                "password", "pwd", "passwd", "secret",
                "token", "authtoken", "accesstoken", "refreshtoken",
                "idcard", "idno", "idnumber", "identitycard",
                "mobile", "phone", "telephone", "cellphone",
                "bankcard", "bankaccount", "creditcard", "cardno", "card", "cardnumber", "accountno",
                "cvv", "securitycode", "cvc",
                "realname", "username", "name", "fullname", "nickname",
                "email", "mail", "emailaddress"
        ));

        FIELD_STRATEGY_MAPPING.clear();
        initDefaultMapping();

        synchronized (MAPPER_INIT_LOCK) {
            DESENSITIZE_OBJECT_MAPPER.set(null);
        }

        log.info("脱敏工具已重置为默认配置");
    }

    private static ObjectMapper getDesensitizeObjectMapper() {
        ObjectMapper mapper = DESENSITIZE_OBJECT_MAPPER.get();
        if (mapper == null) {
            synchronized (MAPPER_INIT_LOCK) {
                mapper = DESENSITIZE_OBJECT_MAPPER.get();
                if (mapper == null) {
                    mapper = createDesensitizeObjectMapper();
                    DESENSITIZE_OBJECT_MAPPER.set(mapper);
                }
            }
        }
        return mapper;
    }

    private static ObjectMapper createDesensitizeObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        DesensitizeBeanSerializerModifier serializerModifier = new DesensitizeBeanSerializerModifier();
        mapper.setSerializerFactory(
                mapper.getSerializerFactory().withSerializerModifier(serializerModifier)
        );

        DesensitizeMapFilter mapFilter = new DesensitizeMapFilter();
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter("desensitizeMapFilter", mapFilter);

        mapper.addMixIn(Map.class, DesensitizeMapFilter.class);
        mapper.setFilterProvider(filterProvider);

        log.debug("脱敏专用的ObjectMapper创建完成");
        return mapper;
    }

    private static void addMapping(String fieldName, DesensitizeStrategy strategy) {
        FIELD_STRATEGY_MAPPING.put(fieldName.toLowerCase(), strategy);
    }
}
