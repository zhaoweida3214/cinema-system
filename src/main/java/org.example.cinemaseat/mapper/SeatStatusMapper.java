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
    @Select("SELECT * FROM seat_status WHERE schedule_id = #{scheduleId}")
    List<SeatStatus> getByScheduleId(Long scheduleId);

    @Select("<script>" +
            "SELECT * FROM seat_status WHERE id IN " +
            "<foreach collection='seatIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<SeatStatus> selectByIds(@Param("seatIds") List<Long> seatIds);

    @Update("<script>" +
            "UPDATE seat_status SET status = #{status}, locked_until = #{expireTime}" + "WHERE id IN " +
            "<foreach collection='seatIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    void lockSeats(@Param("seatIds") List<Long> seatIds,
                   @Param("status") String status,
                   @Param("expireTime") LocalDateTime expireTime);
}
