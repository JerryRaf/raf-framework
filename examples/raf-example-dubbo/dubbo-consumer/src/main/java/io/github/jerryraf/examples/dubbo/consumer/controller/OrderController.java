package io.github.jerryraf.examples.dubbo.consumer.controller;

import io.github.jerryraf.examples.dubbo.api.UserFacade;
import io.github.jerryraf.examples.dubbo.api.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order controller demonstrating Dubbo consumer pattern.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @DubboReference
    private UserFacade userFacade;

    /**
     * Check if user is eligible to place an order.
     *
     * @param userId user id
     * @return user info if eligible, 404 if not found
     */
    @GetMapping("/check-user/{userId}")
    public UserDTO checkUser(@PathVariable Long userId) {
        log.info("Checking user eligibility: {}", userId);
        UserDTO user = userFacade.getById(userId);
        if (user == null || user.getStatus() != UserDTO.STATUS_ACTIVE) {
            return null;
        }
        return user;
    }
}
