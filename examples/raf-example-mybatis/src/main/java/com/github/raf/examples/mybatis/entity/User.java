package io.github.jerryraf.examples.mybatis.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User Entity
 *
 * <p>Demonstrates MyBatis-Plus annotations:
 * - @TableName: Map to database table
 * - @TableId: Primary key configuration
 * - @TableField: Field mapping and auto-fill
 * - @TableLogic: Logic delete support
 *
 * @author RAF Framework Team
 * @since 2026-04-20
 */
@Data
@TableName("t_user")
public class User {

    /**
     * Primary Key (Auto Increment)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Username
     */
    @TableField("username")
    private String username;

    /**
     * Email
     */
    @TableField("email")
    private String email;

    /**
     * Age
     */
    @TableField("age")
    private Integer age;

    /**
     * Status (1: Active, 0: Inactive)
     */
    @TableField("status")
    private Integer status;

    /**
     * Logic Delete Flag (1: Deleted, 0: Not Deleted)
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * Create Time (Auto-fill on insert)
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * Update Time (Auto-fill on insert and update)
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * Creator
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * Updater
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * Version (for optimistic locking)
     */
    @Version
    @TableField("version")
    private Integer version;
}
