package io.github.jerryraf.examples.redis.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.redis.service.RedisOpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST interface for Redis data structure operation demos.
 *
 * @author Jerry
 */
@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisOpsController {

    private final RedisOpsService redisOpsService;

    @PostMapping("/string/{key}")
    public RafResult<Void> stringDemo(@PathVariable String key, @RequestParam String value) {
        redisOpsService.stringDemo(key, value);
        return RafResult.success();
    }

    @PostMapping("/hash/{key}")
    public RafResult<Void> hashDemo(@PathVariable String key) {
        redisOpsService.hashDemo(key);
        return RafResult.success();
    }

    @PostMapping("/list/{key}")
    public RafResult<Void> listDemo(@PathVariable String key) {
        redisOpsService.listDemo(key);
        return RafResult.success();
    }

    @PostMapping("/set/{key}")
    public RafResult<Void> setDemo(@PathVariable String key) {
        redisOpsService.setDemo(key);
        return RafResult.success();
    }

    @PostMapping("/zset/{key}")
    public RafResult<Void> zsetDemo(@PathVariable String key) {
        redisOpsService.zsetDemo(key);
        return RafResult.success();
    }
}
