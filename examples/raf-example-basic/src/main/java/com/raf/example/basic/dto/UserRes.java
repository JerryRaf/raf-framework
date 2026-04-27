package com.raf.example.basic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User response DTO.
 *
 * @author Jerry
 * @since 2026-04-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRes {

    /**
     * User ID
     */
    private Long id;

    /**
     * Username
     */
    private String username;

    /**
     * Email address
     */
    private String email;

    /**
     * Account status: 1=active, 0=disabled
     */
    private Integer status;
}
