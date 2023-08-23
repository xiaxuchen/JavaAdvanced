package org.originit.forkjoin;

import lombok.extern.slf4j.Slf4j;
import org.originit.util.TimeUtil;

import java.math.BigDecimal;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class Accumlater {

    private final ForkJoinPool forkJoinPool;

    private int exclusiveEnd = 1000000;

    public Accumlater(int threadCount) {
        this.forkJoinPool = new ForkJoinPool(threadCount);
    }

    public Accumlater(int threadCount, int exclusiveEnd) {
        this.forkJoinPool = new ForkJoinPool(threadCount);
        this.exclusiveEnd = exclusiveEnd;
    }

    public BigDecimal getSum() {
        return forkJoinPool.invoke(new SumTask(1, exclusiveEnd));
    }

    public BigDecimal getSumSingleThread() {
        BigDecimal sum = new BigDecimal(0);
        int i = 1;
        while (i < this.exclusiveEnd) {
            sum = sum.add(BigDecimal.valueOf(i));
            i++;
        }
        return sum;
    }

    public static class SumTask extends RecursiveTask<BigDecimal> {

        private int start;

        private int end;

        private static final int MAX_NUM_COUNT = 10000;
        /**
         * 左闭右开求和
         */
        public SumTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected BigDecimal compute() {
            // 如果大于10000就拆成2个
            if (this.end - this.start > 10000) {
                int mid = (this.end - this.start)/2 + this.start;
                SumTask left = new SumTask(this.start, mid - 1);
                SumTask right = new SumTask(mid, end);
                right.fork();
                return left.compute().add(right.join());
            } else {
                BigDecimal sum = new BigDecimal(0);
                int i = this.start;
                while (i < this.end) {
                    sum = sum.add(BigDecimal.valueOf(i));
                    i++;
                }
                return sum;
            }
        }
    }

    public static void main(String[] args) {
        TimeUtil.TimeResult<BigDecimal> bigDecimalTimeResult = TimeUtil.timeIt(() -> new Accumlater(1, 20000001).getSum());
        log.info("result is {}, and it takes {} ms", bigDecimalTimeResult.res, bigDecimalTimeResult.time);
        TimeUtil.TimeResult<BigDecimal> singleThreadResult = TimeUtil.timeIt(() -> new Accumlater(50, 20000001).getSum());
        log.info("single thread result is {}, and it takes {} ms", singleThreadResult.res, singleThreadResult.time);
    }
}
