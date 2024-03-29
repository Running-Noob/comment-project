package com.f.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;

/**
 * @author fzy
 * @date 2024/3/9 9:44
 */
public class SimpleRedisLock implements ILock {
    private String key;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    private static final String UUID_PREFIX = UUID.randomUUID().toString(true);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String key, StringRedisTemplate stringRedisTemplate) {
        this.key = key;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取当前线程标识
        String threadId = UUID_PREFIX + "-" + Thread.currentThread().getId();
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + key, threadId, Duration.ofSeconds(timeoutSec)));
    }

    @Override
    public void unlock() {
        // 调用Lua脚本
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + key),
                UUID_PREFIX + "-" + Thread.currentThread().getId());
    }

    //@Override
    //public void unlock() {
    //    // 获取线程标识
    //    String threadId = UUID_PREFIX + "-" + Thread.currentThread().getId();
    //    // 获取锁中标识
    //    String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + key);
    //    // 判断线程标识和锁中标识是否一致
    //    if (threadId.equals(value)) {
    //        // 一致则释放锁
    //        stringRedisTemplate.delete(KEY_PREFIX + key);
    //    }
    //}
}
