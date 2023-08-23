package org.originit.limiter;

import org.originit.limiter.exception.LimiterException;

public interface RateLimiter {

    /**
     * 获取许可
     * @throws LimiterException 当特定算法中存在获取不到的异常情况时将抛出，具体看实现类
     */
    void acquire() throws LimiterException;
}
