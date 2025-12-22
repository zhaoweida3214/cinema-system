package org.example.cinemaseat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.cinemaseat.pojo.entity.User;

@Mapper
public interface UserMapper {
    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User getByUsername(String username);
}
