package com.f.utils;

/**
 * @author fzy
 * @date 2024/3/9 9:41
 */
public interface ILock {
    /**
     * 获取锁
     *
     * @param timeoutSec 锁的超时时间，过期后自动释放
     * @return true代表获取锁成功，false代表失败
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
