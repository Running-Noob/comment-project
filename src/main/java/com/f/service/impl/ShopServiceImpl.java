package com.f.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.f.dto.Result;
import com.f.pojo.Shop;
import com.f.mapper.ShopMapper;
import com.f.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f.utils.RedisConstants;
import com.f.utils.RedisData;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        Shop shop;
        //// 缓存穿透
        //shop = queryShopWithPassThrough(id);
        // 缓存击穿（互斥锁解决）
        shop = queryShopWithMutex(id);
        //// 缓存击穿（逻辑过期解决）
        //shop = queryShopWithLogicalExpire(id);
        if (shop == null) {
            return Result.fail("店铺信息不存在");
        }
        return Result.ok(shop);
    }

    // 线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    // 用逻辑过期解决缓存击穿问题（不考虑缓存穿透）
    public Shop queryShopWithLogicalExpire(Long id) {
        // 1.从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 2.判断商铺缓存是否命中
        if (shopJson == null) {
            // 3.未命中则返回空
            return null;
        }
        // 4.命中则再判断商铺缓存是否过期
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断商铺缓存是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 6.商铺缓存未过期，直接返回
            return shop;
        }
        // 7.商铺缓存过期，尝试获取互斥锁
        boolean isLock = tryLock(RedisConstants.LOCK_SHOP_KEY + id);
        // 8.判断是否获取互斥锁
        if (isLock) {
            // 9.如果获取互斥锁，新开一个线程用于更新商铺缓存
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    saveShop2Redis(id, RedisConstants.SHOP_EXPIRE_TTL);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 释放互斥锁
                    unlock(RedisConstants.LOCK_SHOP_KEY + id);
                }
            });
        }
        // 10.不论是否获取互斥锁，都返回过时的商铺信息
        return shop;
    }

    // 将店铺数据预设到缓存中
    public void saveShop2Redis(Long id, Long expireTime) {
        // 1.查询店铺数据
        Shop shop = getById(id);
        // 2.封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusDays(expireTime));
        // 3.写入redis缓存，没有设置redis中的ttl
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,
                JSONUtil.toJsonStr(redisData));
    }

    // 在缓存穿透的基础上用互斥锁解决缓存击穿问题
    public Shop queryShopWithMutex(Long id) {
        // 1.从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 2.判断商铺缓存是否存在且不为空
        if (shopJson != null && !shopJson.equals("")) {
            // 3.存在则直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        if (shopJson != null) { // 表示商铺信息是空值，用于解决缓存穿透问题
            return null;
        }
        Shop shop = null;
        try {
            // 4.不存在则尝试获取互斥锁
            boolean isLock = tryLock(RedisConstants.LOCK_SHOP_KEY + id);
            // 5.判断是否获取互斥锁
            if (!isLock) {
                // 6.获取互斥锁失败，休眠并重试
                Thread.sleep(50);
                return queryShopWithMutex(id);
            }
            // 7.获取互斥锁成功，去数据库查询商铺信息
            shop = getById(id);
            // 模拟缓存重建的延时
            Thread.sleep(200);
            // 8.判断商铺信息是否存在
            if (shop == null) {
                // 9.不存在则将空值写入redis，用于解决缓存穿透问题
                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,
                        "", Duration.ofMinutes(RedisConstants.CACHE_NULL_TTL));
                return null;
            }
            // 9.存在则将商铺信息写入redis，并在finally中释放互斥锁
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + shop.getId(),
                    JSONUtil.toJsonStr(shop), Duration.ofMinutes(RedisConstants.CACHE_SHOP_TTL));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            stringRedisTemplate.delete(RedisConstants.LOCK_SHOP_KEY + id);
        }
        return shop;
    }

    // 尝试获取锁，通过是否成功插入key来判断是否获取锁
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1",
                Duration.ofSeconds(RedisConstants.LOCK_SHOP_TTL));
        return flag;
    }

    // 删除锁
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    // 解决缓存穿透问题
    public Shop queryShopWithPassThrough(Long id) {
        // 1.从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 2.判断商铺缓存是否存在且不为空
        if (shopJson != null && !shopJson.equals("")) {
            // 3.存在则直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        if (shopJson != null) { // 表示商铺信息是空值，用于解决缓存穿透问题
            return null;
        }
        // 4.不存在则从数据库中查询商铺信息
        Shop shop = getById(id);
        // 5.判断商铺信息是否存在
        if (shop == null) {
            // 6.不存在则将空值写入redis，并返回错误信息
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,
                    "", Duration.ofMinutes(RedisConstants.CACHE_NULL_TTL));
            return null;
        }
        // 7.存在则将商铺信息写入redis，并返回商铺信息
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + shop.getId(),
                JSONUtil.toJsonStr(shop), Duration.ofMinutes(RedisConstants.CACHE_SHOP_TTL));
        return shop;
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
