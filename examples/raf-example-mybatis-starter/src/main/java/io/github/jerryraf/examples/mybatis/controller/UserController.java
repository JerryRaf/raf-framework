package io.github.jerryraf.examples.mybatis.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.jerryraf.examples.mybatis.dto.UserCreateReq;
import io.github.jerryraf.examples.mybatis.dto.UserPageReq;
import io.github.jerryraf.examples.mybatis.dto.UserUpdateReq;
import io.github.jerryraf.examples.mybatis.entity.User;
import io.github.jerryraf.examples.mybatis.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller
 *
 * <p>REST API endpoints demonstrating CRUD, pagination, and transaction propagation.
 *
 * @author Jerry
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Create user */
    @PostMapping
    public Long createUser(@Valid @RequestBody UserCreateReq req) {
        return userService.createUser(req);
    }

    /** Update user */
    @PutMapping("/{id}")
    public void updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateReq req) {
        req.setId(id);
        userService.updateUser(req);
    }

    /** Delete user (logical) */
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    /** Get user by ID */
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    /** Paginate users with optional username filter */
    @GetMapping("/page")
    public Page<User> pageUsers(@Valid UserPageReq req) {
        return userService.pageUsers(req);
    }

    /**
     * Demonstrate transaction propagation (REQUIRES_NEW).
     * outerUser will be rolled back; innerUser will be committed.
     */
    @PostMapping("/tx-demo")
    public void txDemo(@Valid @RequestBody TxDemoReq req) {
        userService.demonstratePropagation(req.getOuterUser(), req.getInnerUser());
    }

    /** Request body for transaction propagation demo */
    @lombok.Data
    public static class TxDemoReq {
        @Valid
        private UserCreateReq outerUser;
        @Valid
        private UserCreateReq innerUser;
    }
}
