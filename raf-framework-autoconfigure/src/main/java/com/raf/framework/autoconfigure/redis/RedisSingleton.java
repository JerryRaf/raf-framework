package com.raf.framework.autoconfigure.redis;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.Optional;
import java.util.Set;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@SuppressWarnings("unchecked")
public class RedisSingleton {

    private RedisTemplate<String, String> redisTemplate;

    public RedisSingleton(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 设置
     */
    public Long set(final String key, final Object value, final long second) {
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
                    RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
                    Jackson2JsonRedisSerializer<Object> vSerializer = (Jackson2JsonRedisSerializer) redisTemplate.getValueSerializer();
                    byte[] bKey = serializer.serialize(key);
                    connection.set(bKey, vSerializer.serialize(value));
                    connection.expire(bKey, second);
                    return 1L;
                }
        );
    }

    /**
     * 设置
     */
    public Long set(final String key, final Object value) {
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
                    RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
                    Jackson2JsonRedisSerializer<Object> vSerializer = (Jackson2JsonRedisSerializer) redisTemplate.getValueSerializer();
                    byte[] bKey = serializer.serialize(key);
                    connection.set(bKey, vSerializer.serialize(value));
                    return 1L;
                }
        );
    }

    /**
     * 设置值，不修改过期时间
     */
    public Long updateValue(final String key, final Object value) {
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
                    RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
                    Jackson2JsonRedisSerializer<Object> vSerializer = (Jackson2JsonRedisSerializer) redisTemplate.getValueSerializer();
                    connection.setRange(serializer.serialize(key), vSerializer.serialize(value), 0);
                    return 1L;
                }
        );
    }

    /**
     * 设置如果不存在
     */
    public Boolean setIfAbsent(final String key, final String object, final long seconds) {
        return redisTemplate.execute(
                (RedisCallback<Boolean>) connection -> {
                    RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
                    Jackson2JsonRedisSerializer<Object> vSerializer = (Jackson2JsonRedisSerializer) redisTemplate.getValueSerializer();
                    byte[] keys = serializer.serialize(key);
                    byte[] values = vSerializer.serialize(object);
                    Boolean result = connection.setNX(keys, values);
                    if (result) {
                        connection.expire(keys, seconds);
                    }
                    return result;
                }
        );
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Boolean setIfAbsent(final String key, final String value) {
        return redisTemplate.execute(
                (RedisCallback<Boolean>) connection -> {
                    RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
                    Jackson2JsonRedisSerializer<Object> vSerializer = (Jackson2JsonRedisSerializer) redisTemplate.getValueSerializer();
                    byte[] keys = serializer.serialize(key);
                    byte[] values = vSerializer.serialize(String.valueOf(value));
                    return connection.setNX(keys, values);
                }
        );
    }

    /**
     * 获取
     */
    public String get(final String key) {
        return redisTemplate.execute(
                (RedisCallback<String>) connection -> {
                    RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
                    Jackson2JsonRedisSerializer<String> vSerializer = (Jackson2JsonRedisSerializer) redisTemplate.getValueSerializer();
                    byte[] bytes = connection.get(serializer.serialize(key));
                    return vSerializer.deserialize(bytes);
                }
        );
    }

    /**
     * 是否存在
     */
    public boolean exist(final String key) {
        return redisTemplate.execute(
                (RedisCallback<Boolean>) connection -> {
                    RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
                    return connection.exists(serializer.serialize(key));
                });
    }

    /**
     * 删除
     */
    public Long del(final String key) {
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
            try {
                RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
                byte[] keys = serializer.serialize(key);
                return connection.del(keys);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * 获取原来key键对应的值并重新赋新值
     */
    public String getAndSet(final String key, final String object, final long seconds) {
        return this.redisTemplate.opsForValue().getAndSet(key, object);
    }

    /**
     *
     * @param key
     * @param values
     * @return
     */
    public Long sadd(final String key, final String... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     *
     * @param key
     * @param value
     * @param score
     * @return
     */
    public Boolean zadd(final String key, final String value, final double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Long zdel(final String key, final String value) {
        return redisTemplate.opsForZSet().remove(key, value);
    }

    /**
     *
     * @param key
     * @return
     */
    public Long zcard(final String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }

    /**
     * 获取有序容器长度
     */
    public Set<String> zRange(final String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 获取指定值得下标
     */
    public Long zRank(final String key, String value) {
        return redisTemplate.opsForZSet().rank(key, value);
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Boolean lpush(final String key, final String value) {
        Long row = redisTemplate.opsForList().leftPush(key, value);
        return row != null && row > 0;
    }

    /**
     *
     * @param key
     * @return
     */
    public String rpop(final String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     *
     * @param key
     * @return
     */
    public String lpop(final String key) {
        return redisTemplate.opsForList().leftPop(key);
    }


    /**
     * Hash
     *
     * @param key   容器名
     * @param field 字段名
     * @param value 字段值
     */
    public Boolean hset(final String key, final String field, final String value) {
        return redisTemplate.opsForHash().putIfAbsent(key, field, value);
    }

    /**
     *
     * @param key
     * @param field
     * @return
     */
    public String hget(final String key, final String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        return Optional.ofNullable(value).orElse("").toString();
    }

    /**
     *
     * @param key
     * @param field
     * @return
     */
    public Long hdel(final String key, final String field) {
        return redisTemplate.opsForHash().delete(key, field);
    }


    /**
     *
     * @param key
     * @return
     */
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key, 1L);
    }


    /**
     *
     * @param key
     * @return
     */
    public Long decr(String key) {
        return redisTemplate.opsForValue().increment(key, -1L);
    }

    /**
     *
     * @param key
     * @param num
     * @return
     */
    public Long incr(String key, Long num) {
        return redisTemplate.opsForValue().increment(key, num);
    }


    /**
     *
     * @param key
     * @param num
     * @return
     */
    public Long decr(String key, Long num) {
        return redisTemplate.opsForValue().increment(key, -num);
    }

}
