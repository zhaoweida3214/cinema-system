package org.example.cinemaseat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.cinemaseat.pojo.VO.ScheduleVO;

import java.util.List;

@Mapper
public interface ScheduleMapper {
    List<ScheduleVO> selectByCinemaAndDate(@Param("cinemaId") Long cinemaId, @Param("date") String date);
}
