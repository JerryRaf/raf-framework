package io.github.jerryraf.examples.mybatis.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for creating a user.
 *
 * @author Jerry
 */
@Data
public class UserCreateReq {

    @NotBlank
    private String username;

    @NotBlank
    private String email;

    @Min(0)
    @Max(150)
    private int age;
}
