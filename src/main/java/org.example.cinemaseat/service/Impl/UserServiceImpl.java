package org.example.cinemaseat.service.Impl;

import org.example.cinemaseat.common.BusinessException;
import org.example.cinemaseat.common.Jwt.JwtUtil;
import org.example.cinemaseat.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.cinemaseat.pojo.DTO.LoginDTO;
import org.example.cinemaseat.pojo.VO.TokenVO;
import org.example.cinemaseat.pojo.entity.User;
import org.example.cinemaseat.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public TokenVO login(LoginDTO loginDTO) {
        // 1. 查询用户
        User user = userMapper.getByUsername(loginDTO.getUsername());
        if (user == null || !user.getPassword().equals(loginDTO.getPassword()))
        {
            throw new BusinessException("用户名或密码错误");
        }
        // 2. 生成 JWT Token
        String token = JwtUtil.createToken(user.getId(), user.getUsername());
        // 3. 返回封装对象
        return TokenVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .token(token)
                .build();
    }
}

