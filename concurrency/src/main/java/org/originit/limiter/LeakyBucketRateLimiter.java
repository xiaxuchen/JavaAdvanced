package org.originit.limiter;

import lombok.extern.slf4j.Slf4j;
import org.originit.limiter.exception.LeakyBucketFullException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LeakyBucketRateLimiter implements RateLimiter{


    private final long interval;

    private long last = System.nanoTime();

    private final int capacity;

    private AtomicInteger waitingCounts = new AtomicInteger();

    public LeakyBucketRateLimiter(int maxPermits, int capacity) {
        this.capacity = capacity;
        // 多少纳秒产生一个请求
        this.interval = 1000_000_000 / maxPermits;
    }

    /**
     * @throws LeakyBucketFullException 当请求超过桶的容量，将抛出该异常
     */
    @Override
    public void acquire() throws LeakyBucketFullException {
        long now = System.nanoTime();
        synchronized (this) {
            // 满了
            if (capacity == waitingCounts.get()) {
                throw new LeakyBucketFullException();
            }
            if (last > now) {
                // 有请求了
                last += interval;
            } else {
                // 空闲
                last = now + interval;
            }
            waitingCounts.incrementAndGet();
        }
        long waitTime = Math.max(0, last - now);
        try {
            if (waitTime > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(waitTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            waitingCounts.decrementAndGet();
        }

    }


}
