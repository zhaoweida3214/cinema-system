package contorller;

import common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pojo.DTO.LoginDTO;
import pojo.VO.TokenVO;
import service.UserService;

@RestController
@RequestMapping("/login")
public class UserController {
    @Autowired
    private UserService userService;
/**
 * 用户登录
 *
 * @param loginDTO 登录信息（username, password）
 * @return Result<TokenVO>
 */
@PostMapping
public Result<TokenVO> login(@RequestBody LoginDTO loginDTO) {
    TokenVO tokenVO = userService.login(loginDTO);
    return Result.success(tokenVO);
}
}
