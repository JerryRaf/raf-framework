package io.github.jerryraf.examples.openapi.controller;

import com.raf.framework.core.common.result.RafResult;
import io.github.jerryraf.examples.openapi.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/users")
@Tag(name = "User API", description = "User management endpoints")
public class UserApiController {

    private final Map<Long, UserDto> store = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1000);

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new user and returns the created resource")
    public RafResult<UserDto> create(@Valid @RequestBody UserDto req) {
        req.setId(idGen.incrementAndGet());
        store.put(req.getId(), req);
        log.info("User created: id={}", req.getId());
        return RafResult.success(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public RafResult<UserDto> getById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        UserDto user = store.get(id);
        if (user == null) {
            return RafResult.fail("User not found: " + id);
        }
        return RafResult.success(user);
    }

    @GetMapping
    @Operation(summary = "List all users")
    public RafResult<List<UserDto>> listAll() {
        return RafResult.success(List.copyOf(store.values()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user by ID")
    public RafResult<Void> delete(@PathVariable Long id) {
        store.remove(id);
        return RafResult.success();
    }
}
