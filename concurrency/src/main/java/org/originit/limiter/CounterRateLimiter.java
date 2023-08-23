package org.originit.limiter;

import org.originit.limiter.exception.LimiterException;

// 每秒钟重置请求
public class CounterRateLimiter implements RateLimiter{

    private final int maxPermits;

    private volatile int available;

    private static final int INTERVAL = 1000_000_000;

    /**
     * 下一次生成的时间
     */
    private volatile long next = System.nanoTime() + INTERVAL;

    public CounterRateLimiter(int maxPermits) {
        this.maxPermits = maxPermits;
        available = maxPermits;
    }
    @Override
    public synchronized void acquire() throws LimiterException {
        while (needContinue()) {
            long now = System.nanoTime();
            if (now > next) {
                // 重置流量
                available = maxPermits;
                long delta = (now - next) % INTERVAL;
                next = now + INTERVAL - delta;
            }
            // 获取到了
            if (available > 0) {
                available -= 1;
                break;
            }
            try {
                // 当前没有流量就等到重置的时候再说
                this.wait((next - now) / 1000_000);
            } catch (InterruptedException e) {
                throw new LimiterException("限流器异常打断", e);
            }
        }
    }

    protected boolean needContinue() {
        return true;
    }
}
