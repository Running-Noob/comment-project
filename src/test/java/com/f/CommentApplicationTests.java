package com.f;

import com.f.service.impl.ShopServiceImpl;
import com.f.utils.RedisConstants;
import com.f.utils.RedisIdWorker;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class CommentApplicationTests {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Test
    void testSaveShop2Redis() {
        shopService.saveShop2Redis(1L, RedisConstants.SHOP_EXPIRE_TTL);
    }

    @Test
    void testRedisIdWorker() {
        for (int i = 0; i < 100; i++) {
            long id = redisIdWorker.nextId("order");
            System.out.println(id);
        }
    }

    @Test
    void testThread() {
        String threadName = Thread.currentThread().getName();
        Long threadId = Thread.currentThread().getId();
        System.out.println(threadName + " - " + threadId);
    }

    @Test
    void testLong() {
        int i = 1;
        long j = 1l;
        System.out.println(i == j);
    }

    @Test
    void testHyperLogLog() {
        String[] ids = new String[1000];
        int j;
        for (int i = 0; i < 1000000; i++) {
            j = i % 1000;
            ids[j] = "user_" + (i);
            if (j == 999) { // 每一千条数据推一次到Redis
                stringRedisTemplate.opsForHyperLogLog().add("hll", ids);
            }
        }
        Long size = stringRedisTemplate.opsForHyperLogLog().size("hll");    // 统计
        System.out.println(size);
    }
}
