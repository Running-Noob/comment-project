package com.f.service.impl;

import com.f.dto.Result;
import com.f.pojo.SeckillVoucher;
import com.f.pojo.VoucherOrder;
import com.f.mapper.VoucherOrderMapper;
import com.f.service.ISeckillVoucherService;
import com.f.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f.utils.RedisIdWorker;
import com.f.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始或结束
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getBeginTime().isAfter(now)
                || voucher.getEndTime().isBefore(now)) {
            // 3.如果秒杀还未开始或者已经结束，返回错误信息
            return Result.fail("秒杀还未开始或已经结束");
        }
        // 4.如果秒杀开始，且没有结束，再判断库存是否充足
        if (voucher.getStock() < 1) {
            // 5.库存不足，返回错误信息
            return Result.fail("库存不足");
        }
        return createVoucherOrder(voucherId);
    }

    @Transactional
    public synchronized Result createVoucherOrder(Long voucherId) { // 使用悲观锁
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