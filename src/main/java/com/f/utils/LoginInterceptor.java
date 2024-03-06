package com.f.utils;

import com.f.dto.UserDTO;
import com.f.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
        // 1.获取session
        HttpSession session = request.getSession();
        // 2.获取session中的用户
        UserDTO user = (UserDTO) session.getAttribute("user");
        // 3.判断用户是否存在
        if (user == null) {
            // 如果用户不存在，则拦截，返回 401 状态码
            response.setStatus(401);
            return false;
        }
        // 如果用户存在，就将用户保存在ThreadLocal中
        UserHolder.saveUser(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        // 移除用户，避免内存泄漏
        UserHolder.removeUser();
    }
}
