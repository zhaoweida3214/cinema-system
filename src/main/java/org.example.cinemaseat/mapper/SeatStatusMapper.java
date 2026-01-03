package org.example.cinemaseat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.cinemaseat.pojo.entity.SeatStatus;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SeatStatusMapper {
    // 根据排期ID查询座位状态
    @Select("SELECT * FROM seat_status WHERE schedule_id = #{scheduleId}")
    List<SeatStatus> getByScheduleId(Long scheduleId);

    // 根据座位ID查询座位状态（使用悲观锁）
    @Select("<script>" +
            "SELECT * FROM seat_status WHERE id IN " +
            "<foreach collection='seatIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            " FOR UPDATE" +
            "</script>")
    List<SeatStatus> selectByIdsForUpdate(@Param("seatIds") List<Long> seatIds);

    // 锁定座位
    @Update("<script>" +
            "UPDATE seat_status SET status = #{status}, locked_until = #{expireTime}, order_id = #{orderId} WHERE id IN " +
            "<foreach collection='seatIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    void lockSeats(@Param("seatIds") List<Long> seatIds,
                   @Param("status") String status,
                   @Param("expireTime") LocalDateTime expireTime,
                   @Param("orderId") Long orderId);

    // 更新座位状态
    @Update("UPDATE seat_status SET status = #{status} WHERE order_id = #{orderId}")
    void updateStatusByOrderId(@Param("orderId") Long orderId, @Param("status") String status);

    // 获取座位价格
    @Select("<script>" +
            "SELECT price FROM seat_status WHERE id IN " +
            "<foreach collection='seatIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Double> getPricesByIds(@Param("seatIds") List<Long> seatIds);

    // 释放座位（将座位状态改为AVAILABLE，清除锁定信息和订单关联）
    @Update("UPDATE seat_status SET status = 'AVAILABLE', locked_until = NULL, order_id = NULL WHERE order_id = #{orderId}")
    void releaseSeatsByOrderId(@Param("orderId") Long orderId);

}
