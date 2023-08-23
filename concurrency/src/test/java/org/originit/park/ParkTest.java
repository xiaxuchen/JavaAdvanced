package org.originit.park;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class ParkTest {

    @Test
    public void testParkBeforeUnPark() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final Thread thread = new Thread(() -> {
            LockSupport.park();
            countDownLatch.countDown();
        });
        Thread control = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            LockSupport.unpark(thread);
        });
        thread.start();
        control.start();
        countDownLatch.await(2, TimeUnit.SECONDS);
        Assert.assertEquals(0, countDownLatch.getCount());
    }

    @Test
    public void testParkAfterUnPark() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final Thread thread = new Thread(() -> {
            // 等待control线程先unpark
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("parking");
            LockSupport.park();
            log.info("park over");
            countDownLatch.countDown();
        });
        Thread control = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            LockSupport.unpark(thread);
            log.info("unpacked");
        });
        thread.start();
        control.start();
        countDownLatch.await(3, TimeUnit.SECONDS);
        Assert.assertEquals(0, countDownLatch.getCount());
    }


}
