package org.example.cinemaseat.service;

import org.example.cinemaseat.pojo.DTO.LoginDTO;
import org.example.cinemaseat.pojo.VO.TokenVO;

public interface UserService {
    /**
     * 用户登录，验证账号密码并生成 JWT Token
     *
     * @param loginDTO
     * @return TokenVO
     */
    TokenVO login(LoginDTO loginDTO);
}
