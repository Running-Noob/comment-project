package com.f.service.impl;

import com.f.dto.Result;
import com.f.pojo.VoucherOrder;
import com.f.mapper.VoucherOrderMapper;
import com.f.service.ISeckillVoucherService;
import com.f.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f.utils.RedisIdWorker;
import com.f.utils.UserHolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024); // 创建阻塞队列
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // 1.获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 2.减少库存，创建订单
                    // 2.1库存充足，扣减库存
                    boolean success = seckillVoucherService.update()
                            .setSql("stock = stock -1") // set stock = stock - 1
                            .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0) // where id = ? and stock > 0 添加了乐观锁
                            .update();
                    if (!success) {
                        log.error("库存不足");
                        return;
                    }
                    // 2.2创建订单
                    save(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.执行lua脚本
        Long userId = UserHolder.getUser().getId();
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString());
        // 2.判断结果是否为0
        // 3.不为0则返回错误信息
        if (!result.equals(0L)) {
            return Result.fail(result.equals(1L) ? "库存不足" : "您已经购买过该特价券，每人仅限一张");
        }
        // 4.为0则将下单信息保存到阻塞队列中
        VoucherOrder order = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        order.setId(orderId);
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        orderTasks.add(order);
        // 5.返回订单id
        return Result.ok(orderId);
    }

    //@Override
    //public Result seckillVoucher(Long voucherId) {
    //    // 1.查询优惠券信息
    //    SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
    //    // 2.判断秒杀是否开始或结束
    //    LocalDateTime now = LocalDateTime.now();
    //    if (voucher.getBeginTime().isAfter(now)
    //            || voucher.getEndTime().isBefore(now)) {
    //        // 3.如果秒杀还未开始或者已经结束，返回错误信息
    //        return Result.fail("秒杀还未开始或已经结束");
    //    }
    //    // 4.如果秒杀开始，且没有结束，再判断库存是否充足
    //    if (voucher.getStock() < 1) {
    //        // 5.库存不足，返回错误信息
    //        return Result.fail("库存不足");
    //    }
    //    // 使用Redisson的分布式锁（对同一个用户的id上锁）
    //    RLock lock = redissonClient.getLock("lock:order:" + UserHolder.getUser().getId());
    //    // 获取锁
    //    boolean isLock = lock.tryLock();
    //    if (!isLock) {   // 获取锁失败
    //        return Result.fail("您已经购买过该特价券，每人仅限一张");
    //    }
    //    try {
    //        IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();  // 代理对象才能实现事务
    //        return proxy.createVoucherOrder(voucherId);
    //    } finally {
    //        lock.unlock();  // 释放锁
    //    }
    //}

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 6.判断用户是否已经买过该特价券了
        Long userId = UserHolder.getUser().getId();
        long count = query().eq("user_id", userId).eq("voucher_id", voucherId)
                .count();
        // 7.如果用户已经买过该特价券，就返回错误信息
        if (count > 0) {
            return Result.fail("您已经购买过该特价券，每人仅限一张");
        }
        // 8.库存充足，扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock -1") // set stock = stock - 1
                .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0 添加了乐观锁
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }
        // 9.创建订单
        VoucherOrder order = new VoucherOrder();
        order.setId(redisIdWorker.nextId("order"));
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        save(order);
        // 10.返回订单id
        return Result.ok(order.getId());
    }
}