package com.f.service;

import com.f.dto.Result;
import com.f.dto.ScrollResult;
import com.f.pojo.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IBlogService extends IService<Blog> {
    Result saveBlog(Blog blog);

    Result queryBlogById(Long id);

    Result queryHotBlog(Integer current);

    Result queryBlogOfFollow(Long max, Integer offset);
}
