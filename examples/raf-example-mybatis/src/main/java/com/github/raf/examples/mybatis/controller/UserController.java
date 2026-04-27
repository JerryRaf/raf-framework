package io.github.jerryraf.examples.mybatis.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.jerryraf.examples.mybatis.entity.User;
import io.github.jerryraf.examples.mybatis.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Controller
 *
 * <p>REST API endpoints for user management
 *
 * @author RAF Framework Team
 * @since 2026-04-20
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Create user
     *
     * @param user User entity
     * @return Created user
     */
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    /**
     * Update user
     *
     * @param id User ID
     * @param user User entity
     * @return Updated user
     */
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return userService.updateUser(user);
    }

    /**
     * Delete user
     *
     * @param id User ID
     */
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    /**
     * Get user by ID
     *
     * @param id User ID
     * @return User entity
     */
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    /**
     * List all users
     *
     * @return User list
     */
    @GetMapping
    public List<User> listAllUsers() {
        return userService.listAllUsers();
    }

    /**
     * Find users by username
     *
     * @param username Username
     * @return User list
     */
    @GetMapping("/search")
    public List<User> findByUsername(@RequestParam String username) {
        return userService.findByUsername(username);
    }

    /**
     * Find users by age range
     *
     * @param minAge Minimum age
     * @param maxAge Maximum age
     * @return User list
     */
    @GetMapping("/age-range")
    public List<User> findByAgeRange(@RequestParam Integer minAge, @RequestParam Integer maxAge) {
        return userService.findByAgeRange(minAge, maxAge);
    }

    /**
     * Paginate users
     *
     * @param pageNum Page number (default: 1)
     * @param pageSize Page size (default: 10)
     * @return Paginated user list
     */
    @GetMapping("/page")
    public IPage<User> paginateUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return userService.paginateUsers(pageNum, pageSize);
    }

    /**
     * Find active users with pagination
     *
     * @param pageNum Page number
     * @param pageSize Page size
     * @param status User status
     * @return Paginated user list
     */
    @GetMapping("/active")
    public IPage<User> findActiveUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam Integer status) {
        return userService.findActiveUsers(pageNum, pageSize, status);
    }

    /**
     * Batch update user status
     *
     * @param userIds User ID list
     * @param status New status
     * @return Updated count
     */
    @PutMapping("/batch-status")
    public int batchUpdateStatus(@RequestParam List<Long> userIds, @RequestParam Integer status) {
        return userService.batchUpdateStatus(userIds, status);
    }
}
