package com.f.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f.dto.Result;
import com.f.dto.UserDTO;
import com.f.pojo.Follow;
import com.f.mapper.FollowMapper;
import com.f.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f.service.IUserService;
import com.f.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, boolean isFollow) {
        //1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        String followKey = "follows:" + userId;
        if (isFollow) {
            //关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            follow.setCreateTime(LocalDateTime.now());
            boolean isSave = save(follow);
            if (isSave) {
                //把被关注用户id放入Redis sadd follows:userId（key） followerId（value）
                stringRedisTemplate.opsForSet().add(followKey, followUserId.toString());
            }
        } else {
            //取关
            QueryWrapper<Follow> queryWrapper = new QueryWrapper();
            queryWrapper.eq("user_id", userId).eq("follow_user_id", followUserId);
            boolean isRemove = remove(queryWrapper);
            if (isRemove) {
                //把被关注用户id从Redis移除
                stringRedisTemplate.opsForSet().remove(followKey, followUserId.toString());
            }
        }
        return Result.ok();
    }


    @Override
    public Result isfollow(Long followUserId) {
        //1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        //2.查询是否已关注 select count(*) from tb_follow where user_id = #{userId} and follow_user_id = #{followUserId};
        Long count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long followUserId) {
        //1.先获取当前用户
        Long userId = UserHolder.getUser().getId();
        String followKey1 = "follows:" + userId;
        String followKey2 = "follows:" + followUserId;

        //2.求交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(followKey1, followKey2);
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        //3.解析出id数组
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        //4.根据ids查询用户数组 List<User> ---> List<UserDTO>
        List<UserDTO> userDTOS = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);
    }
}
