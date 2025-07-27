package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    private  final ExecutorService executorService= Executors.newFixedThreadPool(500);


    @Autowired
private RedisIdWorker redisIdWorker;

    @Test
    public  void  test() throws Exception{

        CountDownLatch countDownLatch = new CountDownLatch(300);
        Runnable task=()-> {
            for (int i = 0; i < 100; i++) {
                long cart = redisIdWorker.nexId("cart");
                System.out.println(ThreadLocal.class.getTypeName()+"id="+cart);
            };
         countDownLatch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            executorService.submit(task);
        }


        countDownLatch.await();
        long end = System.currentTimeMillis();

        System.out.println(end -begin);
    }



}
