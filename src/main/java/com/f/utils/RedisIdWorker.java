package com.f.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author fzy
 * @date 2024/3/8 16:11
 */
@Component
public class RedisIdWorker {
    // 定义初始时间戳，以2000年1月1日为基准
    private static final long BEGIN_TIMESTAMP = LocalDateTime
            .of(2000, 1, 1, 0, 0, 0)
            .toEpochSecond(ZoneOffset.UTC);
    // 时间戳左移位数
    private static final int COUNT_BITS = 32;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // keyPrefix用于区分不同的业务
    public long nextId(String keyPrefix) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long timeStamp = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
        // 2.生成序列号（用redis的自增长）
        // 获取当前日期（精确到天）
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long incr = stringRedisTemplate.opsForValue().increment("incr:" + keyPrefix + ":" + date);
        // 3.拼接并返回
        //return timeStamp << COUNT_BITS + incr;
        return timeStamp << COUNT_BITS | incr;  // 用或运算效率更高
    }
}
