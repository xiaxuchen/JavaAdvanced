package org.originit.limiter;

import org.originit.limiter.exception.LimiterException;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

public class SlidingWindowRateLimiter implements RateLimiter{

    /**
     * 限流窗口时间长度
     */
    private long periodTime;
    /**
     * 限流窗口访问量上限
     */
    private long maxPermits;
    /**
     * 限流窗口分割成子窗口数
     */
    private long partSize;
    /**
     * 子窗口时间长度
     */
    private long subPeriodTime;
    /**
     * 最新子窗口起始时间(用于判断是否生产子窗口及计算子窗口起始结束时间)
     */
    private long currentTimeStart;
    /**
     * 最新子窗口结束时间(用于判断是否生产子窗口及计算子窗口起始结束时间)
     */
    private long currentTimeEnd;
    /**
     * 双向队列
     * 1.存放子窗口
     * 2.长度为子窗口数
     * 3.最新子窗口尾部添加，最老子窗口头部删除，实现滑动
     */
    private Deque<SubCounterLimiter> deque;
    /**
     * 滑动窗口总访问数
     * 1.记录滑动窗口目前访问总量
     * 2.窗口滑动时，减去最老子窗口的访问量
     * 3.访问成功 + 1
     */
    private long acceptTimes;

    // 如果窗口数量过小，比如是1，就会退化成计数器限流器
    public static final int MIN_WINDOW = 10;

    public SlidingWindowRateLimiter(long periodTime, long maxPermits) {
        this.periodTime = periodTime;
        this.maxPermits = maxPermits;
        this.partSize = Math.max(maxPermits, MIN_WINDOW);
        this.subPeriodTime = periodTime / partSize;
    }

    /**
     * 步骤1.区分第一次请求和后续请求，第一次请求执行初始化方法
     * 步骤2.后续请求，分三种情况：
     *   <1>.请求时间与最新一次子窗口的结束时间相差超过一个periodTime，执行初始化方法
     *       作为一次新的开始
     *   <2>.请求时间与最新一次子窗口的结束时间相差少于一个periodTime且不在最新子窗口
     *       的时间段内，则创建新的子窗口且加入队列，并且队列已满时挤出最老的子窗口和acceptTimes减去挤出子窗口的访问量，直到请求时间在最新的子窗口时间段内
     *   <3>.请求时间在最新一次子窗口内
     * 步骤3.步骤1和步骤2保证了当前请求在最新子窗口时间段内
     * 步骤4.访问次数进行判断，是否可以访问，成功访问则总访问量+1，子窗口访问量+1
     */
    @Override
    public synchronized void acquire() throws LimiterException {
        while(true) {
            long requestTime = System.nanoTime();
            //第一次请求
            if(deque == null){
                init(requestTime);
            } else {
                // 请求时间已经超过一个period了，就清空后添加一个窗口
                if(requestTime >= periodTime + deque.getLast().getSubTimeEnd()){
                    init(requestTime);
                }else if(requestTime >= deque.getLast().getSubTimeEnd() && requestTime < periodTime + deque.getLast().getSubTimeEnd()){
                    // 在一个period之中，没有超过一period没有请求,但是超出了最新的子窗口
                    recur(requestTime);
                }
            }

            if(acceptTimes < maxPermits){
                acceptTimes ++;
                deque.getLast().setSubAcceptTimes(deque.getLast().getSubAcceptTimes() + 1);
                break;
            }else{
                Thread.yield();
            }
        }
    }

    /**
     * 子窗口类
     */
    private class SubCounterLimiter{
        /**
         * 子窗口起始时间
         */
        private long subTimeStart;
        /**
         * 子窗口结束时间
         */
        private long subTimeEnd;
        /**
         * 子窗口接受访问数
         */
        private long subAcceptTimes;

        public SubCounterLimiter(long subTimeStart, long subTimeEnd) {
            this.subTimeStart = subTimeStart;
            this.subTimeEnd = subTimeEnd;
            this.subAcceptTimes = 0L;
        }

        public long getSubTimeStart() {
            return subTimeStart;
        }

        public long getSubTimeEnd() {
            return subTimeEnd;
        }

        public long getSubAcceptTimes() {
            return subAcceptTimes;
        }

        public void setSubTimeStart(long subTimeStart) {
            this.subTimeStart = subTimeStart;
        }

        public void setSubTimeEnd(long subTimeEnd) {
            this.subTimeEnd = subTimeEnd;
        }

        public void setSubAcceptTimes(long subAcceptTimes) {
            this.subAcceptTimes = subAcceptTimes;
        }
    }

    /**
     * 1.访问量初始化
     * 2.队列初始化
     * 3.创建第一个子窗口
     * 4.队列加入子窗口
     */
    private synchronized void init(long requestTime){
        acceptTimes = 0L;
        if (deque == null) {
            deque = new LinkedBlockingDeque<>((int) partSize);
        }
        deque.clear();
        currentTimeStart = requestTime;
        currentTimeEnd = currentTimeStart + subPeriodTime;
        SubCounterLimiter initFirst = createSubCounterLimiter(currentTimeStart,currentTimeEnd);
        deque.add(initFirst);
    }

    /**
     * 1.递归获得子窗口，并更新入队列
     * 2.直到请求时刻在获得子窗口时间区间内，该子窗口作为最新子窗口，递归创建子窗口停止
     */
    private synchronized void recur(long requestTime){
        do {
            currentTimeStart = deque.getLast().getSubTimeEnd();
            currentTimeEnd = currentTimeStart + subPeriodTime;
            // 以最后一个窗口的结束开始
            SubCounterLimiter last = createSubCounterLimiter(currentTimeStart, currentTimeEnd);
            if (!deque.offerLast(last)) {
                // 如果子窗口队列满了,就移除队头窗口再添加进去
                acceptTimes = acceptTimes - deque.getFirst().getSubAcceptTimes();
                deque.removeFirst();
                deque.offerLast(last);
            }
            // 当前请求不在最新的窗口中，就继续排除掉窗口
        } while (requestTime >= deque.getLast().getSubTimeEnd());
    }

    /**
     * 创建子窗口
     */
    private SubCounterLimiter createSubCounterLimiter(long timeStart,long timeEnd) {
        return new SubCounterLimiter(timeStart, timeEnd);
    }
}
