package io.github.jerryraf.examples.openapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "User data transfer object")
public class UserDto {

    @Schema(description = "User ID", example = "1001")
    private Long id;

    @NotBlank
    @Size(min = 2, max = 64)
    @Schema(description = "Username", example = "alice", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank
    @Email
    @Schema(description = "Email address", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Min(1) @Max(150)
    @Schema(description = "Age", example = "28")
    private Integer age;
}
