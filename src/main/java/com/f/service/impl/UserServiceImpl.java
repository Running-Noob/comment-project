package com.f.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f.dto.LoginFormDTO;
import com.f.dto.Result;
import com.f.dto.UserDTO;
import com.f.mapper.UserMapper;
import com.f.pojo.User;
import com.f.service.IUserService;
import com.f.utils.RedisConstants;
import com.f.utils.RegexUtils;
import com.f.utils.SystemConstants;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 手机号不符合，返回错误信息
            return Result.fail("手机号有误，请重试");
        }
        // 2.生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 3.将手机号验证码保存到redis，key是手机号，value是验证码，并设置有效期为两分钟
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, Duration.ofMinutes(RedisConstants.LOGIN_CODE_TTL));
        log.info("手机号验证码为：{}", code);
        // 4.发送验证码
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验验证码
        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if (cacheCode == null) {
            // 验证码过期
            return Result.fail("验证码过期，请重试");
        }
        if (!code.equals(cacheCode)) {
            // 验证码有误
            return Result.fail("验证码有误，请重试");
        }
        // 2.根据手机号查询用户
        User user = query().eq("phone", phone).one();
        if (user == null) {
            // 用户不存在则保存新用户数据到数据库中
            user = createUserWithPhone(phone);
            save(user);
        }
        // 3.保存用户信息到redis中
        //      3.1 随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //      3.2 将用户信息用Hash类型存储在redis中，key是token，value是用户信息，并设置有效期为30分钟
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token, BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString())));
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, Duration.ofMinutes(RedisConstants.LOGIN_USER_TTL));
        // 4.返回token给前端
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10));
        return user;
    }
}
