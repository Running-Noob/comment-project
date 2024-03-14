package com.f.service;

import com.f.dto.Result;
import com.f.pojo.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, boolean isFollow);

    Result isfollow(Long followUserId);

    Result followCommons(Long followUserId);
}
