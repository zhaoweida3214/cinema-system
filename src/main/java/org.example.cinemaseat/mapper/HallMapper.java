package org.example.cinemaseat.mapper;

import org.apache.ibatis.annotations.Select;
import org.example.cinemaseat.pojo.entity.Hall;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HallMapper {
    @Select("SELECT h.* FROM hall h JOIN schedule s ON h.id = s.hall_id WHERE s.id = #{scheduleId}")
    Hall getByScheduleId(Long scheduleId);
}
