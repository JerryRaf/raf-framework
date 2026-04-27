package com.raf.example.basic.service;

import com.raf.example.basic.common.UserErrorCode;
import com.raf.example.basic.dto.UserCreateReq;
import com.raf.example.basic.dto.UserRes;
import com.raf.framework.autoconfigure.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User service - demonstrates exception hierarchy and business logic patterns.
 * Uses in-memory storage for simplicity (no database required).
 *
 * @author Jerry
 * @since 2026-04-17
 */
@Slf4j
@Service
public class UserService {

    private final Map<Long, UserRes> userStore = new ConcurrentHashMap<>();
    private final Map<String, Long> usernameIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * Get user by ID.
     *
     * @param id user ID
     * @return user response
     * @throws BusinessException if user not found
     */
    public UserRes getById(Long id) {
        log.info("Fetching user: id={}", id);
        UserRes user = userStore.get(id);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * Create a new user.
     *
     * @param req creation request
     * @return created user response
     * @throws BusinessException if username already exists
     */
    public UserRes create(UserCreateReq req) {
        log.info("Creating user: username={}", req.getUsername());

        if (usernameIndex.containsKey(req.getUsername())) {
            throw new BusinessException(UserErrorCode.USER_ALREADY_EXISTS);
        }

        Long id = idGenerator.getAndIncrement();
        UserRes user = new UserRes(id, req.getUsername(), req.getEmail(), 1);
        userStore.put(id, user);
        usernameIndex.put(req.getUsername(), id);

        log.info("User created successfully: id={}, username={}", id, req.getUsername());
        return user;
    }

    /**
     * Disable a user account.
     *
     * @param id user ID
     * @throws BusinessException if user not found
     */
    public void disable(Long id) {
        log.info("Disabling user: id={}", id);
        UserRes user = userStore.get(id);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(0);
        log.info("User disabled: id={}", id);
    }
}
