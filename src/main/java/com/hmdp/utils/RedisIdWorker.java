package com.hmdp.utils;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Component

public class RedisIdWorker {
    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    /**
     * 序列号的位数
     */
    private static final int COUNT_BITS = 32;
    private StringRedisTemplate stringRedisTemplate;


    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public   long nexId(String KrePrefix){
        //生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond- BEGIN_TIMESTAMP;
        //获取当前时间chuo
       //资质呢公司咋好难过
        String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //自增长
        Long increment = stringRedisTemplate.opsForValue().increment("ice:" + KrePrefix + ":" + data);
        return  timestamp<<COUNT_BITS | increment;
    }
}
