package io.github.jerryraf.examples.mybatis.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.jerryraf.examples.mybatis.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * User Mapper
 *
 * <p>Extends MyBatis-Plus BaseMapper for standard CRUD operations.
 * Custom queries are defined here and implemented in UserMapper.xml.
 *
 * @author Jerry
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * Custom pagination query with dynamic conditions.
     * Implemented in UserMapper.xml.
     *
     * @param page     MyBatis-Plus page object
     * @param username optional username filter (LIKE)
     * @return paginated user list
     */
    Page<User> selectPageByCondition(Page<User> page, @Param("username") String username);
}
