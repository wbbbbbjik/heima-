package com.hmdp.utils;

public interface ILock {

    /**
     * 尝试获取锁
     * @param timeoutSec
     * @return
     */

    boolean tryLock(long timeoutSec);
    void  unlock();
}
