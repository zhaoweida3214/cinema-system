package service.Impl;

import common.BusinessException;
import common.JwtUtil;
import mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pojo.DTO.LoginDTO;
import pojo.VO.TokenVO;
import pojo.entity.User;
import service.UserService;

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

