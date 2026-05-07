package io.github.jerryraf.examples.dubbo.provider.service;

import io.github.jerryraf.examples.dubbo.api.UserFacade;
import io.github.jerryraf.examples.dubbo.api.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * User facade implementation for Dubbo provider.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@Slf4j
@DubboService
public class UserFacadeImpl implements UserFacade {

    @Override
    public UserDTO getById(Long id) {
        log.info("Getting user by id: {}", id);
        if (id == null || id <= 0) {
            return null;
        }
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setUsername("user_" + id);
        user.setPhone("138" + String.format("%08d", id));
        user.setStatus(1);
        return user;
    }

    @Override
    public boolean exists(Long id) {
        log.info("Checking user existence: {}", id);
        return id != null && id > 0;
    }
}
