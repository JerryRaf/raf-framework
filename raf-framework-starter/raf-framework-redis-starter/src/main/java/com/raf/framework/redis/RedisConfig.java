package com.raf.framework.redis;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
@EnableCaching
@AutoConfigureAfter(JacksonAutoConfiguration.class)
@EnableConfigurationProperties(RedisEnhanceProperties.class)
@ConditionalOnProperty(prefix = "raf.redis", name = "enabled", havingValue = "true")
public class RedisConfig implements CachingConfigurer {

    private final RedisEnhanceProperties properties;
    private CacheManager cacheManager;

    public RedisConfig(RedisEnhanceProperties properties) {
        this.properties = properties;
    }

    @Bean
    public GenericJackson2JsonRedisSerializer jsonRedisSerializer(ObjectMapper objectMapper) {
        ObjectMapper redisMapper = objectMapper.copy();
        redisMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        redisMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        BasicPolymorphicTypeValidator.Builder validatorBuilder = BasicPolymorphicTypeValidator.builder();
        properties.getTrustedPackages().forEach(validatorBuilder::allowIfSubType);
        PolymorphicTypeValidator ptv = validatorBuilder.build();
        redisMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        return new GenericJackson2JsonRedisSerializer(redisMapper);
    }

    @Bean(name = "rafRedisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory,
                                                        GenericJackson2JsonRedisSerializer serializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisService redisHelper(RedisTemplate<String, Object> rafRedisTemplate) {
        return new RedisService(rafRedisTemplate);
    }

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory factory, GenericJackson2JsonRedisSerializer serializer) {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> customCacheMap = new HashMap<>();
        if (properties.getCustomCache() != null) {
            properties.getCustomCache().forEach((key, config) -> {
                RedisCacheConfiguration cfg = defaultCacheConfig;
                if (config.getTimeToLive() != null) {
                    cfg = cfg.entryTtl(config.getTimeToLive());
                }
                if (!config.isCacheNullValues()) {
                    cfg = cfg.disableCachingNullValues();
                }
                if (!config.isUseKeyPrefix()) {
                    cfg = cfg.disableKeyPrefix();
                }
                if (config.getKeyPrefix() != null) {
                    cfg = cfg.computePrefixWith(cacheName -> config.getKeyPrefix() + ":" + cacheName);
                }
                customCacheMap.put(key, cfg);
            });
        }

        this.cacheManager = RedisCacheManager.builder(factory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(customCacheMap)
                .build();
        return this.cacheManager;
    }

    @Override
    public CacheManager cacheManager() {
        return cacheManager;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("Redis 缓存获取失败, key: {}, 降级查询数据库", key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("Redis 缓存写入失败, key: {}, 为保证一致性抛出异常", key, exception);
                throw exception;
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("Redis 缓存删除失败, key: {}, 为保证一致性抛出异常", key, exception);
                throw exception;
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("Redis 缓存清理失败", exception);
                throw exception;
            }
        };
    }
}
