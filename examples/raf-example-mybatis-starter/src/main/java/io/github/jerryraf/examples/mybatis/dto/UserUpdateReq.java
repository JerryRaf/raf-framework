package io.github.jerryraf.examples.mybatis.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for updating a user.
 *
 * @author Jerry
 */
@Data
public class UserUpdateReq {

    @NotNull
    private Long id;

    @NotBlank
    private String email;

    @Min(0)
    @Max(150)
    private int age;
}
