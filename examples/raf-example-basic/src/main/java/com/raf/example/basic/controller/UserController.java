package com.raf.example.basic.controller;

import com.raf.example.basic.dto.UserCreateReq;
import com.raf.example.basic.dto.UserRes;
import com.raf.example.basic.service.UserService;
import com.raf.framework.autoconfigure.common.result.RafResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * User controller - demonstrates RAF Framework unified response and exception handling.
 *
 * <p>Key features shown:
 * <ul>
 *   <li>RafResult&lt;T&gt; unified response format (code/msg/data/traceId)</li>
 *   <li>@Valid parameter validation → auto returns code=10700 on failure</li>
 *   <li>BusinessException → HTTP 200 with business error code</li>
 *   <li>traceId automatically injected into every response</li>
 * </ul>
 *
 * @author Jerry
 * @since 2026-04-17
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get user by ID.
     *
     * @param id user ID
     * @return user info
     */
    @GetMapping("/{id}")
    public RafResult<UserRes> getUser(@PathVariable Long id) {
        return RafResult.success(userService.getById(id));
    }

    /**
     * Create a new user.
     *
     * @param req creation request (validated)
     * @return created user info
     */
    @PostMapping
    public RafResult<UserRes> createUser(@Valid @RequestBody UserCreateReq req) {
        return RafResult.success(userService.create(req));
    }

    /**
     * Disable a user account.
     *
     * @param id user ID
     * @return empty success response
     */
    @DeleteMapping("/{id}")
    public RafResult<Void> disableUser(@PathVariable Long id) {
        userService.disable(id);
        return RafResult.success();
    }
}
