package org.originit.thread;

import org.junit.Assert;
import org.junit.Test;

public class ThreadInterruptTest {

    @Test(timeout = 3000)
    public void testInterrupt() throws InterruptedException {
        Thread thread = new Thread(new ThreadInterrupt.GoodUseRunnable());
        thread.start();
        // 等100ms让thread进入sleep
        Thread.sleep(100);
        thread.interrupt();
        // 活着的时候还能获取到状态
        Assert.assertTrue(thread.isInterrupted());
        thread.join();
        // 线程会结束
        Assert.assertFalse(thread.isAlive());
    }

    @Test
    public void testBadInterrupt() throws InterruptedException {
        Thread thread = new Thread(new ThreadInterrupt.BadUseRunnable());
        thread.start();
        Thread.sleep(1000);
        thread.interrupt();
        Thread.sleep(1000);
        // interrupt之后线程还活着同时没有被打断
        Assert.assertTrue(thread.isAlive() && !thread.isInterrupted());
    }
}