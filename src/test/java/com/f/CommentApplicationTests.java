package com.f;

import com.f.service.impl.ShopServiceImpl;
import com.f.utils.RedisConstants;
import com.f.utils.RedisIdWorker;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CommentApplicationTests {
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
}
