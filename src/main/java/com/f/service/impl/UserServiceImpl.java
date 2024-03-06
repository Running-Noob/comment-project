package com.f.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f.dto.LoginFormDTO;
import com.f.dto.Result;
import com.f.dto.UserDTO;
import com.f.pojo.User;
import com.f.mapper.UserMapper;
import com.f.service.IUserService;
import com.f.utils.RegexUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import com.f.utils.SystemConstants;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 手机号不符合，返回错误信息
            return Result.fail("手机号有误，请重试");
        }
        // 2.生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 3.将手机号验证码保存到session
        session.setAttribute("phone", phone);
        session.setAttribute("code", code);
        log.info("手机号验证码为：{}", code);
        // 4.发送验证码
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号，防止用户在得到短信验证码后修改手机号
        String phone = loginForm.getPhone();
        Object cachePhone = session.getAttribute("phone");
        if (cachePhone == null) {
            return Result.fail("手机号过期，请重试");
        }
        if (!phone.equals(cachePhone.toString())) {
            // 前后手机号不一致
            return Result.fail("获取验证码的手机号和当前手机号不一致");
        }
        // 2.校验验证码
        String code = loginForm.getCode();
        Object cacheCode = session.getAttribute("code");
        if (cacheCode == null) {
            return Result.fail("验证码过期，请重试");
        }
        if (!code.equals(cacheCode.toString())) {
            // 验证码有误
            return Result.fail("验证码有误，请重试");
        }
        // 3.根据手机号查询用户
        User user = query().eq("phone", cachePhone.toString()).one();
        if (user == null) {
            // 用户不存在则保存新用户数据到数据库中
            user = createUserWithPhone(cachePhone.toString());
            save(user);
        }
        // 4.保存用户信息到session中
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10));
        return user;
    }
}
