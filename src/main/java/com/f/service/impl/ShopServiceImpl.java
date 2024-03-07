package com.f.service.impl;

import cn.hutool.json.JSONUtil;
import com.f.dto.Result;
import com.f.pojo.Shop;
import com.f.mapper.ShopMapper;
import com.f.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f.utils.RedisConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopById(Long id) {
        // 1.从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 2.判断商铺缓存是否存在且不为空
        if (shopJson != null && !shopJson.equals("")) {
            // 3.存在则直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        if (shopJson != null) { // 表示商铺信息是空值，用于解决缓存穿透问题
            return Result.fail("商铺信息不存在");
        }
        // 4.不存在则从数据库中查询商铺信息
        Shop shop = getById(id);
        // 5.判断商铺信息是否存在
        if (shop == null) {
            // 6.不存在则将空值写入redis，并返回错误信息
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,
                    "", Duration.ofMinutes(RedisConstants.CACHE_NULL_TTL));
            return Result.fail("店铺不存在");
        }
        // 7.存在则将商铺信息写入redis，并返回商铺信息
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + shop.getId(),
                JSONUtil.toJsonStr(shop), Duration.ofMinutes(RedisConstants.CACHE_SHOP_TTL));
        return Result.ok(shop);
    }

    @Transactional  // 事务保证原子性操作
    public Result updateShopById(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("商铺不存在，无法更新");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
