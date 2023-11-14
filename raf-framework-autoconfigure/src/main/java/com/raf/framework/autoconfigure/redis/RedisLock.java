package com.raf.framework.autoconfigure.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
public class RedisLock {

    /**
     * 锁key
     */
    private String lockKey;

    /**
     * 默认超时时间 毫秒
     */
    private long timeoutMillis = 5000;

    /**
     * 重试次数
     */
    private int retryCount = 20;

    /**
     * 每次重试后等待的时间
     */
    private long sleepMillis = 100;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private ThreadLocal<String> lockFlag = new ThreadLocal<>();

    private static final String UNLOCK_LUA;

    /*
     * 通过lua脚本释放锁,来达到释放锁的原子操作
     */
    static {
        UNLOCK_LUA = "if redis.call(\"get\",KEYS[1]) == ARGV[1] " +
                "then " +
                "    return redis.call(\"del\",KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end ";
    }

    public RedisLock(RedisTemplate<String, String> redisTemplate,String key) {
        this.redisTemplate=redisTemplate;
        this.lockKey=key;
    }

    public RedisLock(RedisTemplate<String, String> redisTemplate,String key, int retryCount) {
        this.redisTemplate=redisTemplate;
        this.lockKey=key;
        this.retryCount=retryCount;
    }

    public RedisLock(RedisTemplate<String, String> redisTemplate,String key, int retryCount, int sleepMillis) {
        this.redisTemplate=redisTemplate;
        this.lockKey=key;
        this.retryCount=retryCount;
        this.sleepMillis=sleepMillis;
    }

    public RedisLock(RedisTemplate<String, String> redisTemplate,String key, long timeoutMillis) {
        this.redisTemplate=redisTemplate;
        this.lockKey=key;
        this.timeoutMillis=timeoutMillis;
    }

    public RedisLock(RedisTemplate<String, String> redisTemplate,String key, int retryCount, int sleepMillis, long timeoutMillis) {
        this.redisTemplate=redisTemplate;
        this.lockKey=key;
        this.retryCount=retryCount;
        this.sleepMillis=sleepMillis;
        this.timeoutMillis=timeoutMillis;
    }


    public boolean lock() {
        boolean result = setRedis(lockKey, timeoutMillis);
        // 如果获取锁失败，按照传入的重试次数进行重试
        while ((!result) && retryCount-- > 0) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("get redisDistributeLock failed, retrying..." + retryCount);
                }
                log.error("get redisDistributeLock failed, retrying..." + retryCount);
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                log.warn("Interrupted!", e);
                Thread.currentThread().interrupt();
            }
            result = setRedis(lockKey, timeoutMillis);
        }
        return result;
    }

    private boolean setRedis(final String key, final long expire) {
        try {
            return redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                String uuid = UUID.randomUUID().toString();
                lockFlag.set(uuid);
                byte[] keyByte = redisTemplate.getStringSerializer().serialize(key);
                byte[] uuidByte = redisTemplate.getStringSerializer().serialize(uuid);
                boolean result = connection.set(keyByte, uuidByte, Expiration.from(expire, TimeUnit.MILLISECONDS), RedisStringCommands.SetOption.ifAbsent());
                return result;
            });
        } catch (Exception ex) {
            log.error("redisDistributeLock设置异常", ex);
        }
        return false;
    }

    public boolean releaseLock(String key) {
        // 释放锁的时候，有可能因为持锁之后方法执行时间大于锁的有效期，此时有可能已经被另外一个线程持有锁，所以不能直接删除
        try {
            // 使用lua脚本删除redis中匹配value的key，可以避免由于方法执行时间过长而redis锁自动过期失效的时候误删其他线程的锁
            // spring自带的执行脚本方法中，集群模式直接抛出不支持执行脚本的异常，所以只能拿到原redis的connection来执行脚本
            return redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                byte[] scriptByte = redisTemplate.getStringSerializer().serialize(UNLOCK_LUA);
                return connection.eval(scriptByte, ReturnType.BOOLEAN, 1
                        , redisTemplate.getStringSerializer().serialize(key)
                        , redisTemplate.getStringSerializer().serialize(lockFlag.get()));
            });
        } catch (Exception ex) {
            log.error("redisDistributeLock释放异常", ex);
        } finally {
            lockFlag.remove();
        }
        return false;
    }
}
