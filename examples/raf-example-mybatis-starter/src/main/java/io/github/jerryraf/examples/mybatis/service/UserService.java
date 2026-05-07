package io.github.jerryraf.examples.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import io.github.jerryraf.examples.mybatis.dao.UserMapper;
import io.github.jerryraf.examples.mybatis.dto.UserCreateReq;
import io.github.jerryraf.examples.mybatis.dto.UserPageReq;
import io.github.jerryraf.examples.mybatis.dto.UserUpdateReq;
import io.github.jerryraf.examples.mybatis.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * User service demonstrating CRUD, pagination, and transaction propagation.
 *
 * @author Jerry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // ===== CRUD =====

    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateReq req) {
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setAge(req.getAge());
        userMapper.insert(user);
        log.info("User created: id={}", user.getId());
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateReq req) {
        User user = userMapper.selectById(req.getId());
        if (user == null) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR, "User not found: " + req.getId());
        }
        user.setEmail(req.getEmail());
        user.setAge(req.getAge());
        userMapper.updateById(user);
        log.info("User updated: id={}", req.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        int rows = userMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR, "User not found: " + id);
        }
        log.info("User deleted (logical): id={}", id);
    }

    public User getUser(Long id) {
        return userMapper.selectById(id);
    }

    // ===== Pagination =====

    public Page<User> pageUsers(UserPageReq req) {
        Page<User> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(req.getUsername() != null, User::getUsername, req.getUsername())
                .orderByDesc(User::getId);
        return userMapper.selectPage(page, wrapper);
    }

    // ===== Transaction Propagation Demo =====

    /**
     * REQUIRED (default): joins the outer transaction.
     * If this method throws, the outer transaction also rolls back.
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void createUserRequired(UserCreateReq req) {
        createUser(req);
        log.info("createUserRequired: joined outer transaction");
    }

    /**
     * REQUIRES_NEW: suspends the outer transaction and starts a new one.
     * Even if the outer transaction rolls back, this commit is preserved.
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void createUserRequiresNew(UserCreateReq req) {
        createUser(req);
        log.info("createUserRequiresNew: independent transaction committed");
    }

    /**
     * Demonstrates REQUIRES_NEW isolation:
     * - createUserRequiresNew commits independently
     * - then throws to roll back the outer transaction
     * - result: the REQUIRES_NEW user persists, the outer user does not
     */
    @Transactional(rollbackFor = Exception.class)
    public void demonstratePropagation(UserCreateReq outerReq, UserCreateReq innerReq) {
        createUserRequired(outerReq);          // joins this transaction
        createUserRequiresNew(innerReq);       // independent transaction, commits immediately
        log.info("About to throw — outer transaction will roll back, inner already committed");
        throw new BusinessException(RafResponseEnum.SERVER_ERROR, "Simulated failure to demonstrate REQUIRES_NEW");
    }
}
