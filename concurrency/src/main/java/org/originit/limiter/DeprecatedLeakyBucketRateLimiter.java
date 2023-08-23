package org.originit.limiter;

import lombok.extern.slf4j.Slf4j;
import org.originit.limiter.exception.LeakyBucketFullException;
import org.originit.limiter.exception.LimiterException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class DeprecatedLeakyBucketRateLimiter implements RateLimiter, Runnable{


    private final long interval;

    private final ArrayBlockingQueue<Thread> waitingThreads;

    public DeprecatedLeakyBucketRateLimiter(int maxPermits, int capacity) {
        // 多少纳秒产生一个请求
        this.interval = 1000_000_000 / maxPermits;
        waitingThreads = new ArrayBlockingQueue<>(capacity);
        // 启动一个线程进行漏桶
         new Thread(this).start();
    }

    /**
     * @throws LeakyBucketFullException 当请求超过桶的容量，将抛出该异常
     */
    @Override
    public void acquire() throws LeakyBucketFullException {
        try {
            // 每个condition对应一个请求
            waitingThreads.add(Thread.currentThread());
            LockSupport.park();
        } catch (IllegalStateException e) {
            throw new LeakyBucketFullException(e);
        }
    }

    /**
     * 原本让线程acquire时park，然后后台开一个线程按指定速率漏出请求,但是性能不理想
     */
    @Override
    @Deprecated
    public void run() {
        while (true) {
            // 按指定速率漏
            try {
                TimeUnit.NANOSECONDS.sleep(interval);
            } catch (InterruptedException e) {
                throw new LimiterException("漏桶被非法打断", e);
            }
            try {
                LockSupport.unpark(waitingThreads.take());
            } catch (InterruptedException e) {
                throw new LimiterException("漏桶被非法打断", e);
            }
        }
    }
}
