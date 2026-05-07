package io.github.jerryraf.examples.dubbo.api;

import io.github.jerryraf.examples.dubbo.api.dto.UserDTO;

/**
 * User service facade for Dubbo RPC.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
public interface UserFacade {

    /**
     * Get user by id.
     *
     * @param id user id
     * @return user DTO, null if not found
     */
    UserDTO getById(Long id);

    /**
     * Check if user exists and is active.
     *
     * @param id user id
     * @return true if user exists and is active
     */
    boolean exists(Long id);
}
