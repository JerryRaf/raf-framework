package com.raf.framework.autoconfigure.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.google.common.collect.Maps;
import com.raf.framework.autoconfigure.spring.ConfigUtil;
import com.raf.framework.autoconfigure.spring.condition.ConditionalOnMapProperty;
import org.springframework.beans.FatalBeanException;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;
import java.util.Map;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@ConditionalOnMapProperty(prefix = "spring.redis")
@EnableConfigurationProperties({RedisProperties.class,CacheProperties.class})
@EnableCaching
public class RedisConfig implements EnvironmentAware {

    private final CacheProperties cacheProperties;

    private ConfigurableEnvironment environment;

    public RedisConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }


    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory,Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisSingleton redisSingleton(RedisTemplate<String, String> redisTemplate) {
        return new RedisSingleton(redisTemplate);
    }

    @Bean
    public Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer() {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance ,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        jackson2JsonRedisSerializer.setObjectMapper(om);
        return jackson2JsonRedisSerializer;
    }

    @Bean
    public RedisCacheManager getRedisCacheManager(RedisConnectionFactory redisConnectionFactory,Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer) {
        CustomRedisCacheWriter customRedisCacheWriter = new CustomRedisCacheWriter(RedisCacheWriter.lockingRedisCacheWriter(redisConnectionFactory));

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.RedisCacheManagerBuilder
                .fromCacheWriter(customRedisCacheWriter)
                .cacheDefaults(determineConfiguration(jackson2JsonRedisSerializer));

        List<String> cacheNames = this.cacheProperties.getCacheNames();
        if (!cacheNames.isEmpty()) {
            try {
                CustomCacheProperties customCacheProperties = ConfigUtil.resolveSetting("raf",CustomCacheProperties.class, this.environment);
                Map<String, RedisCacheConfiguration> map = Maps.newHashMap();
                cacheNames.forEach(name -> {
                    CustomCacheProperties.CacheProperties cacheProperties = customCacheProperties.getCustomCache().get(name);
                    RedisCacheConfiguration redisCacheConfiguration = determineConfiguration(jackson2JsonRedisSerializer);
                    if (cacheProperties.getTimeToLive() != null) {
                        redisCacheConfiguration = redisCacheConfiguration.entryTtl(cacheProperties.getTimeToLive());
                    }
                    if (cacheProperties.getKeyPrefix() != null) {
                        redisCacheConfiguration = redisCacheConfiguration.prefixCacheNameWith(cacheProperties.getKeyPrefix());
                    }
                    if (!cacheProperties.isCacheNullValues()) {
                        redisCacheConfiguration = redisCacheConfiguration.disableCachingNullValues();
                    }
                    if (!cacheProperties.isUseKeyPrefix()) {
                        redisCacheConfiguration = redisCacheConfiguration.disableKeyPrefix();
                    }
                    map.put(name, redisCacheConfiguration);
                });
                builder.withInitialCacheConfigurations(map);
            } catch (FatalBeanException e) {
                //ignore
            }
        }
        return builder.build();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    private RedisCacheConfiguration determineConfiguration(Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer) {
        CacheProperties.Redis redisProperties = this.cacheProperties.getRedis();
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();

        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer));

        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }
}
