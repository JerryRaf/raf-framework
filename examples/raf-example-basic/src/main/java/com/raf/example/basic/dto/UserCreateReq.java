package com.raf.example.basic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * User creation request DTO.
 *
 * @author Jerry
 * @since 2026-04-17
 */
@Data
public class UserCreateReq {

    /**
     * Username (3-50 characters)
     */
    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * Email address
     */
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email format is invalid")
    private String email;

    /**
     * Password (8-100 characters)
     */
    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;
}
