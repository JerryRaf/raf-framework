package io.github.jerryraf.examples.shardingsphere.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.jerryraf.examples.shardingsphere.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 按用户ID查询订单（ShardingSphere 自动路由到对应分库）
     */
    @Select("SELECT * FROM t_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Order> selectByUserId(@Param("userId") Long userId);
}
