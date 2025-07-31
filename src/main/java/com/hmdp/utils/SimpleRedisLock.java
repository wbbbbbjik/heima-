package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private StringRedisTemplate stringRedisTemplate;
    private  String name;
    private final  String KEY_PREFIX="lock:";
    private final  String UUID_PREFIX= UUID.randomUUID().toString(true)+"-";

    public SimpleRedisLock(String s, StringRedisTemplate stringRedisTemplate) {

        this.name=s;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }






    @Override
    public boolean tryLock(long timeoutSec) {

        //获取线程标示
        String id=UUID_PREFIX+ Thread.currentThread().getId();

        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, id+"", timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(b);
    }

    @Override
    public void unlock() {
        stringRedisTemplate
                .execute(UNLOCK_SCRIPT, Collections.singletonList(KEY_PREFIX+name),
                        UUID_PREFIX+Thread.currentThread().getId());
    }


//    @Override
//    public void unlock() {
//
//
//        //获取线程标示
//        String id=UUID_PREFIX+ Thread.currentThread().getId();
//        String s = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        if (s.equals(id)){
//            stringRedisTemplate.delete(KEY_PREFIX+name);
//        }
//
//
//
//    }
}
