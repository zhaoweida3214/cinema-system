package org.example.cinemaseat.common.Jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.cinemaseat.common.BaseContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头中获取token
        String token = request.getHeader("Authorization");

        // 如果没有token，放行（可能有一些不需要认证的接口）
        if (token == null || token.isEmpty()) {
            return true;
        }

        try {
            // 使用现有的JwtUtil工具类验证token
            DecodedJWT jwt = JwtUtil.verify(token);
            Long userId = jwt.getClaim("userId").asLong();
            String username = jwt.getClaim("username").asString();

            // 将用户信息存入BaseContext
            BaseContext.setCurrentUserId(userId);

            return true;
        } catch (Exception e) {
            // token无效，返回错误
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\": 401, \"msg\": \"Invalid token\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理ThreadLocal中的数据
        BaseContext.clear();
    }
}