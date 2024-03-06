package com.f.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.f.dto.LoginFormDTO;
import com.f.dto.Result;
import com.f.pojo.User;
import jakarta.servlet.http.HttpSession;

/**
 * <p>
 * 服务类
 * </p>
 */
public interface IUserService extends IService<User> {
    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}
