package com.f.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author fzy
 * @date 2024/3/12 21:47
 */
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        // 配置类
        Config config = new Config();
        // 添加Redis地址
        config.useSingleServer().setAddress("redis://192.168.44.132:6379").setPassword("123456");
        // 返回RedissonClient对象
        return Redisson.create(config);
    }
}