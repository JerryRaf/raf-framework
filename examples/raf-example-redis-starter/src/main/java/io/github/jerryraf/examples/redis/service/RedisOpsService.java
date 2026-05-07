package io.github.jerryraf.examples.redis.service;

import com.raf.framework.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates common Redis data structure operations via RedisService.
 *
 * <p>Covered operations:
 * <ul>
 *   <li>String: set/get/incr/expire</li>
 *   <li>Hash: hSet/hGet/hGetAll/hDel</li>
 *   <li>List: lPush/rPush/lRange/lPop</li>
 *   <li>Set: sAdd/sMembers/sIsMember/sRem</li>
 *   <li>ZSet: zAdd/zRange/zRangeByScore/zRem</li>
 * </ul>
 *
 * @author Jerry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisOpsService {

    private final RedisService redisService;
    private final RedisTemplate<String, Object> redisTemplate;

    // ===== String =====

    public void stringDemo(String key, String value) {
        // set with TTL
        redisService.set(key, value, 10, TimeUnit.MINUTES);
        log.info("String set: key={}, value={}", key, value);

        // get
        Object got = redisService.get(key);
        log.info("String get: key={}, value={}", key, got);

        // incr (counter demo)
        String counterKey = key + ":counter";
        redisTemplate.opsForValue().set(counterKey, 0);
        redisTemplate.opsForValue().increment(counterKey);
        redisTemplate.opsForValue().increment(counterKey);
        Object counter = redisTemplate.opsForValue().get(counterKey);
        log.info("String incr: key={}, value={}", counterKey, counter);
    }

    // ===== Hash =====

    public void hashDemo(String hashKey) {
        // hSet
        redisTemplate.opsForHash().put(hashKey, "name", "Jerry");
        redisTemplate.opsForHash().put(hashKey, "age", "30");
        redisTemplate.expire(hashKey, 10, TimeUnit.MINUTES);
        log.info("Hash hSet: key={}", hashKey);

        // hGet
        Object name = redisTemplate.opsForHash().get(hashKey, "name");
        log.info("Hash hGet: name={}", name);

        // hGetAll
        Map<Object, Object> all = redisTemplate.opsForHash().entries(hashKey);
        log.info("Hash hGetAll: {}", all);

        // hDel
        redisTemplate.opsForHash().delete(hashKey, "age");
        log.info("Hash hDel: deleted field 'age'");
    }

    // ===== List =====

    public void listDemo(String listKey) {
        // lPush / rPush
        redisTemplate.opsForList().leftPush(listKey, "first");
        redisTemplate.opsForList().rightPush(listKey, "second");
        redisTemplate.opsForList().rightPush(listKey, "third");
        redisTemplate.expire(listKey, 10, TimeUnit.MINUTES);
        log.info("List push: key={}", listKey);

        // lRange (get all)
        List<Object> range = redisTemplate.opsForList().range(listKey, 0, -1);
        log.info("List lRange: {}", range);

        // lPop
        Object popped = redisTemplate.opsForList().leftPop(listKey);
        log.info("List lPop: popped={}", popped);
    }

    // ===== Set =====

    public void setDemo(String setKey) {
        // sAdd
        redisTemplate.opsForSet().add(setKey, "apple", "banana", "cherry");
        redisTemplate.expire(setKey, 10, TimeUnit.MINUTES);
        log.info("Set sAdd: key={}", setKey);

        // sMembers
        Set<Object> members = redisTemplate.opsForSet().members(setKey);
        log.info("Set sMembers: {}", members);

        // sIsMember
        Boolean isMember = redisTemplate.opsForSet().isMember(setKey, "apple");
        log.info("Set sIsMember 'apple': {}", isMember);

        // sRem
        redisTemplate.opsForSet().remove(setKey, "banana");
        log.info("Set sRem: removed 'banana'");
    }

    // ===== ZSet =====

    public void zsetDemo(String zsetKey) {
        // zAdd
        redisTemplate.opsForZSet().add(zsetKey, "player1", 100.0);
        redisTemplate.opsForZSet().add(zsetKey, "player2", 200.0);
        redisTemplate.opsForZSet().add(zsetKey, "player3", 150.0);
        redisTemplate.expire(zsetKey, 10, TimeUnit.MINUTES);
        log.info("ZSet zAdd: key={}", zsetKey);

        // zRange (ascending by score)
        Set<Object> range = redisTemplate.opsForZSet().range(zsetKey, 0, -1);
        log.info("ZSet zRange (asc): {}", range);

        // zRangeByScore
        Set<Object> byScore = redisTemplate.opsForZSet().rangeByScore(zsetKey, 100.0, 160.0);
        log.info("ZSet zRangeByScore [100,160]: {}", byScore);

        // zRem
        redisTemplate.opsForZSet().remove(zsetKey, "player1");
        log.info("ZSet zRem: removed 'player1'");
    }
}
