package com.f.utils;

import com.f.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author fzy
 * @date 2024/3/6 21:18
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 如果用户不存在，则拦截，返回 401 状态码
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
