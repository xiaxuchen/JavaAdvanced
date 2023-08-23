package org.originit.limiter;

import java.util.concurrent.TimeUnit;

public class TokenBucketRateLimiter implements RateLimiter{

    private final int maxPermits;

    private volatile long storePermits = 0;

    private volatile long next = System.nanoTime();

    /**
     * 令牌发放间隔
     */
    private volatile long interval = 1000_000_000;

    public TokenBucketRateLimiter(int maxPermits) {
        this.maxPermits = maxPermits;
        // 多少纳秒产生一个请求
        interval = 1000_000_000 / maxPermits;
    }

    /**
     * 根据时间重新计算剩余令牌
     * @param now 当前时间
     */
    private synchronized void reSync(long now) {
        // 发放令牌
        if (now > next) {
            // 新产生的令牌数量
            long newPermits = (now - next) / interval;
            storePermits = Math.min(maxPermits, storePermits + newPermits);
            next = now;
        }
    }

    /**
     * 预占令牌，当令牌桶中还有令牌直接扣减，若已经没有了则预占掉下一个时间产生的令牌
     */
    private synchronized long reserve(long now) {
        reSync(now);
        // 如果当前还有令牌
        if (storePermits > 0) {
            storePermits -= 1;
            // 立刻就能放行
            return now;
        } else {
            // 能够放行的时间
            long at = next;
            // 相当于预先占据了next位置的令牌
            next += interval;
            return at;
        }
    }

    @Override
    public void acquire() {
        long now = System.nanoTime();
        long at = reserve(now);
        long waitTime = Math.max(at - now, 0);
        // 等待到达预占时间
        if (waitTime > 0) {
            try {
                TimeUnit.NANOSECONDS.sleep(waitTime);
            } catch (InterruptedException e) {
                throw new RuntimeException("限流时线程被非法唤醒!", e);
            }
        }
    }
}
