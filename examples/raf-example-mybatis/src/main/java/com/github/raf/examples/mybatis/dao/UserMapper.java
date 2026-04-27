package io.github.jerryraf.examples.mybatis.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.jerryraf.examples.mybatis.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * User Mapper
 *
 * <p>Extends MyBatis-Plus BaseMapper for CRUD operations:
 * - insert(entity)
 * - deleteById(id)
 * - updateById(entity)
 * - selectById(id)
 * - selectList(queryWrapper)
 * - selectPage(page, queryWrapper)
 * - ... and many more
 *
 * <p>Custom methods can be defined here and implemented in XML
 *
 * @author RAF Framework Team
 * @since 2026-04-20
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * Custom query: Find users by age range
     *
     * @param minAge Minimum age
     * @param maxAge Maximum age
     * @return User list
     */
    List<User> findByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);

    /**
     * Custom query with pagination: Find active users
     *
     * @param page Page object
     * @param status User status
     * @return Paginated user list
     */
    IPage<User> findActiveUsers(Page<User> page, @Param("status") Integer status);

    /**
     * Custom update: Batch update user status
     *
     * @param userIds User ID list
     * @param status New status
     * @return Updated count
     */
    int batchUpdateStatus(@Param("userIds") List<Long> userIds, @Param("status") Integer status);
}
