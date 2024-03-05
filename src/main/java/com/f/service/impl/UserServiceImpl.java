package com.f.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f.pojo.User;
import com.f.mapper.UserMapper;
import com.f.service.IUserService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
