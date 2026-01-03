package org.example.cinemaseat.mapper;

import org.apache.ibatis.annotations.*;
import org.example.cinemaseat.pojo.VO.UserOrderVO;
import org.example.cinemaseat.pojo.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    // 创建订单
    @Insert("INSERT INTO orders (user_id, schedule_id, status,total_amount, created_at, expires_at) " +
            "VALUES (#{userId}, #{scheduleId}, #{status}, #{totalAmount}, #{createdAt}, #{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Order order);
    // 更新订单状态
    @Update("UPDATE orders SET status = #{status} WHERE id = #{orderId}")
    void updateStatus(@Param("orderId") Long orderId, @Param("status") String status);
    // 根据订单ID查询订单
    @Select("SELECT * FROM orders WHERE id = #{orderId}")
    Order getById(Long orderId);
    // 查询用户订单列表
    List<UserOrderVO> selectUserOrders(@Param("userId") Long userId);
    // 查询过期订单
    @Select("SELECT * FROM orders WHERE status = 'PENDING' AND expires_at < #{now}")
    List<Order> selectExpiredOrders(@Param("now") LocalDateTime now);

}
