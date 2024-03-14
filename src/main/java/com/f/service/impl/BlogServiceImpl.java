package com.f.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.f.dto.Result;
import com.f.dto.ScrollResult;
import com.f.dto.UserDTO;
import com.f.pojo.Blog;
import com.f.mapper.BlogMapper;
import com.f.pojo.Follow;
import com.f.pojo.User;
import com.f.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f.service.IFollowService;
import com.f.service.IUserService;
import com.f.utils.RedisConstants;
import com.f.utils.SystemConstants;
import com.f.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private IFollowService followService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result saveBlog(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 2.保存探店笔记
        boolean isSuccess = save(blog);
        if (!isSuccess) {
            return Result.fail("保存笔记失败");
        }
        // 3.查找所有关注了作者的粉丝
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        // 4.将笔记的id推送到粉丝的收件箱
        for (Follow follow : follows) {
            Long userId = follow.getUserId();   // 粉丝id
            stringRedisTemplate.opsForZSet().add(RedisConstants.FEED_KEY + userId,
                    blog.getId().toString(),
                    System.currentTimeMillis());
        }
        // 5.返回笔记id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogById(Long id) {
        //1.查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在!");
        }
        //2.查询blog相关用户
        queryBlogUser(blog);
        return Result.ok(blog);
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(this::queryBlogUser);
        return Result.ok(records);
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2.根据当前用户找到对应的收件箱，并根据max和offset找到收件箱对应的片段
        String key = RedisConstants.FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 3);
        if (tuples == null || tuples.isEmpty()) {   // 非空判断
            return Result.ok();
        }
        // 3.解析数据：blogId、minTime、offset
        List<String> blogIds = new ArrayList<>();
        long minTime = 0;
        int count = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            // 获取blogId
            String blogId = tuple.getValue();
            blogIds.add(blogId);
            // 获取时间戳和offset
            long time = tuple.getScore().longValue();
            if (minTime == time) {
                count++;
            } else {
                minTime = time;
                count = 1;
            }
        }
        // 4.根据blogId查询对应的博客并封装
        List<Blog> blogs = new ArrayList<>();
        for (String blogId : blogIds) {
            Blog blog = query().eq("id", blogId).one();
            queryBlogUser(blog);
            blogs.add(blog);
        }
        // 5.返回结果
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setMinTime(minTime);
        scrollResult.setOffset(count);
        return Result.ok(scrollResult);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
