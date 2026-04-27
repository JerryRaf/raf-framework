package io.github.jerryraf.examples.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.jerryraf.examples.mybatis.config.DataSourceConfig;
import io.github.jerryraf.examples.mybatis.dao.UserMapper;
import io.github.jerryraf.examples.mybatis.entity.User;
import com.raf.framework.datasource.annotation.DsSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User Service
 *
 * <p>Demonstrates:
 * 1. Read-Write Splitting with @DsSelector
 * 2. MyBatis-Plus CRUD operations
 * 3. Pagination
 * 4. Transaction management
 *
 * @author RAF Framework Team
 * @since 2026-04-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * Create user (Write operation - uses Master datasource)
     *
     * @param user User entity
     * @return Created user
     */
    @DsSelector(DataSourceConfig.DS_MASTER)
    @Transactional(rollbackFor = Exception.class)
    public User createUser(User user) {
        log.info("Creating user: {}", user.getUsername());
        userMapper.insert(user);
        return user;
    }

    /**
     * Update user (Write operation - uses Master datasource)
     *
     * @param user User entity
     * @return Updated user
     */
    @DsSelector(DataSourceConfig.DS_MASTER)
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(User user) {
        log.info("Updating user: {}", user.getId());
        userMapper.updateById(user);
        return user;
    }

    /**
     * Delete user (Write operation - uses Master datasource)
     * <p>
     * Note: This is a logic delete due to @TableLogic annotation
     *
     * @param id User ID
     */
    @DsSelector(DataSourceConfig.DS_MASTER)
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        userMapper.deleteById(id);
    }

    /**
     * Get user by ID (Read operation - uses Slave datasource)
     *
     * @param id User ID
     * @return User entity
     */
    @DsSelector(DataSourceConfig.DS_SLAVE)
    public User getUserById(Long id) {
        log.info("Querying user by ID: {}", id);
        return userMapper.selectById(id);
    }

    /**
     * List all users (Read operation - uses Slave datasource)
     *
     * @return User list
     */
    @DsSelector(DataSourceConfig.DS_SLAVE)
    public List<User> listAllUsers() {
        log.info("Querying all users");
        return userMapper.selectList(null);
    }

    /**
     * Find users by username (Read operation - uses Slave datasource)
     * <p>
     * Demonstrates LambdaQueryWrapper usage
     *
     * @param username Username (supports fuzzy matching)
     * @return User list
     */
    @DsSelector(DataSourceConfig.DS_SLAVE)
    public List<User> findByUsername(String username) {
        log.info("Querying users by username: {}", username);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(User::getUsername, username);
        return userMapper.selectList(wrapper);
    }

    /**
     * Find users by age range (Read operation - uses Slave datasource)
     * <p>
     * Demonstrates custom SQL query
     *
     * @param minAge Minimum age
     * @param maxAge Maximum age
     * @return User list
     */
    @DsSelector(DataSourceConfig.DS_SLAVE)
    public List<User> findByAgeRange(Integer minAge, Integer maxAge) {
        log.info("Querying users by age range: {} - {}", minAge, maxAge);
        return userMapper.findByAgeRange(minAge, maxAge);
    }

    /**
     * Paginate users (Read operation - uses Slave datasource)
     * <p>
     * Demonstrates MyBatis-Plus pagination
     *
     * @param pageNum Page number (starts from 1)
     * @param pageSize Page size
     * @return Paginated user list
     */
    @DsSelector(DataSourceConfig.DS_SLAVE)
    public IPage<User> paginateUsers(int pageNum, int pageSize) {
        log.info("Paginating users: page={}, size={}", pageNum, pageSize);
        Page<User> page = new Page<>(pageNum, pageSize);
        return userMapper.selectPage(page, null);
    }

    /**
     * Find active users with pagination (Read operation - uses Slave datasource)
     * <p>
     * Demonstrates custom pagination query
     *
     * @param pageNum Page number
     * @param pageSize Page size
     * @param status User status
     * @return Paginated user list
     */
    @DsSelector(DataSourceConfig.DS_SLAVE)
    public IPage<User> findActiveUsers(int pageNum, int pageSize, Integer status) {
        log.info("Querying active users: page={}, size={}, status={}", pageNum, pageSize, status);
        Page<User> page = new Page<>(pageNum, pageSize);
        return userMapper.findActiveUsers(page, status);
    }

    /**
     * Batch update user status (Write operation - uses Master datasource)
     * <p>
     * Demonstrates batch update
     *
     * @param userIds User ID list
     * @param status New status
     * @return Updated count
     */
    @DsSelector(DataSourceConfig.DS_MASTER)
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStatus(List<Long> userIds, Integer status) {
        log.info("Batch updating user status: ids={}, status={}", userIds, status);
        return userMapper.batchUpdateStatus(userIds, status);
    }

    /**
     * Complex query with multiple conditions (Read operation - uses Slave datasource)
     * <p>
     * Demonstrates complex QueryWrapper usage
     *
     * @param username Username (optional)
     * @param minAge Minimum age (optional)
     * @param maxAge Maximum age (optional)
     * @param status Status (optional)
     * @return User list
     */
    @DsSelector(DataSourceConfig.DS_SLAVE)
    public List<User> complexQuery(String username, Integer minAge, Integer maxAge, Integer status) {
        log.info("Complex query: username={}, age={}-{}, status={}", username, minAge, maxAge, status);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // Conditional query
        wrapper.like(username != null, User::getUsername, username)
                .ge(minAge != null, User::getAge, minAge)
                .le(maxAge != null, User::getAge, maxAge)
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getCreateTime);

        return userMapper.selectList(wrapper);
    }
}
