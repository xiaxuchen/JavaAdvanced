package org.originit.limiter;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.originit.base.BatchRunner;
import org.originit.base.ExecutorServiceCapable;
import org.originit.limiter.exception.LimiterException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RateLimiterTest implements ExecutorServiceCapable, BatchRunner {

    // 测试五秒钟的限流
    private final int TEST_SECOND = 15;

    /**
     * 一秒钟允许的最大量
     */
    private final int MAX_PERMITS = 100;

    private void testCommon(RateLimiter rateLimiter,int maxPermits) {
        // 首先有很多个线程
        ExecutorService executor = getExecutorService((int) (maxPermits * 1.5), maxPermits * 3);
        AtomicInteger atomicInteger = new AtomicInteger();
        long finishTime = System.currentTimeMillis() + TEST_SECOND * 1000;
        for (int i = 0; i < maxPermits * 100; i++) {
            executor.submit(() -> {
                long now = System.currentTimeMillis();
                if (now > finishTime) {
                    return;
                }
                try {
                    rateLimiter.acquire();
                } catch (LimiterException e) {
                    // 获取不到请求略过
                    return;
                }
                now = System.currentTimeMillis();
                if (now > finishTime) {
                    return;
                }
                atomicInteger.incrementAndGet();
            });
        }
        try {
            Thread.sleep(TEST_SECOND * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Algorithm 【{}】 permitted {} requests in {} s", rateLimiter.getClass().getSimpleName(), atomicInteger.get(), TEST_SECOND);
        Assert.assertTrue(atomicInteger.get() <= (MAX_PERMITS * (TEST_SECOND + 1)));
    }

    @Test
    public void testTokenBucket() {
        testCommon(new TokenBucketRateLimiter(MAX_PERMITS), MAX_PERMITS);
    }

    @Test
    public void testLeakyBucket() {
        testCommon(new LeakyBucketRateLimiter(MAX_PERMITS, MAX_PERMITS * 10), MAX_PERMITS);
    }

    @Test
    public void testCounter() {
        // 如果大量流量在一秒的末端申请，就会产生在一秒内可以有两倍的流量。
        // 比如1.99s的时候突然100个请求来了。然后到了2.0s重置流量又有100个请求来了，那么这一段时间就出现了200个请求
        testCommon(new CounterRateLimiter(MAX_PERMITS), MAX_PERMITS);
    }

    @Test
    public void testSlidingWindow() {
        testCommon(new SlidingWindowRateLimiter(1000_000_000, MAX_PERMITS), MAX_PERMITS);
    }

    @Override
    public ExecutorService getExecutorService() {
        return getExecutorService(MAX_PERMITS * 2, MAX_PERMITS * 3);
    }
}