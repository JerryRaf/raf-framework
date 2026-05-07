package io.github.jerryraf.examples.mybatis.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Pagination request for user list.
 *
 * @author Jerry
 */
@Data
public class UserPageReq {

    @Min(1)
    private int pageNum = 1;

    @Min(1)
    @Max(100)
    private int pageSize = 10;

    /** Optional: filter by username (LIKE) */
    private String username;
}
