package com.f;

import com.f.service.impl.ShopServiceImpl;
import com.f.utils.RedisConstants;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CommentApplicationTests {
    @Resource
    private ShopServiceImpl shopService;

    @Test
    void testSaveShop2Redis() {
        shopService.saveShop2Redis(1L, RedisConstants.SHOP_EXPIRE_TTL);
    }
}
