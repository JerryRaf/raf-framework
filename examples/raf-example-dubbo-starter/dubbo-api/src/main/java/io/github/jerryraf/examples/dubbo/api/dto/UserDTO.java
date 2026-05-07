package io.github.jerryraf.examples.dubbo.api.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * User data transfer object for Dubbo RPC.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@Data
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_INACTIVE = 0;

    private Long id;
    private String username;
    private String phone;
    /** 1: active, 0: inactive */
    private Integer status;
}
