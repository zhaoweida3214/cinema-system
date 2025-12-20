package service;

import pojo.DTO.LoginDTO;
import pojo.VO.TokenVO;

public interface UserService {
    /**
     * 用户登录，验证账号密码并生成 JWT Token
     *
     * @param loginDTO
     * @return TokenVO
     */
    TokenVO login(LoginDTO loginDTO);
}
