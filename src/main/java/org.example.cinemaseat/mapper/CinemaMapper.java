package org.example.cinemaseat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.cinemaseat.pojo.entity.Cinema;

import java.util.List;

@Mapper
public interface CinemaMapper {
    @Select("SELECT * FROM cinema")
    List<Cinema> selectAll();
}
