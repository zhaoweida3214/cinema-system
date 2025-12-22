package org.example.cinemaseat.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.example.cinemaseat.pojo.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {
    @Insert("INSERT INTO `order` (user_id, schedule_id, status, created_at, expires_at) " +
            "VALUES (#{userId}, #{scheduleId}, #{status}, #{createdAt}, #{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Order order);
}
