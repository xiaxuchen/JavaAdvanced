package org.originit.print;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractNumberPrint implements NumberPrint{

    public static int maxNumber = 100000;

    AtomicInteger atomicInteger = new AtomicInteger(0);

    protected abstract void printOdd(Thread odd, Thread even);

    protected abstract void printEven(Thread odd, Thread even);

    private Thread oddT, evenT;

    private volatile long start;

    private volatile long time;

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    protected int add(int num) {
        try {
//            Thread.sleep(1);
            return num + 1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean isFinish() {
        return atomicInteger.get() > maxNumber;
    }

    protected boolean print(Object o) {
        final int i = atomicInteger.getAndIncrement();
        if ((int)o != i) {
            log.error("num should be {}, but now is {}", i, o);
            countDownLatch.countDown();
            return false;
        }
        if (i == maxNumber) {
            time = (System.currentTimeMillis() - start);
            countDownLatch.countDown();
            return false;
        }

        if (i >= maxNumber) {
            return false;
        }
//        log.info("current i={}",i);
        return true;
    }

    public long run() {
        this.start = System.currentTimeMillis();
        oddT = new Thread(() -> {
            printOdd(oddT, evenT);
        });
        oddT.setName(this.getClass().getSimpleName() + "-odd");

        evenT = new Thread(() -> {
            printEven(oddT, evenT);
        });
        evenT.setName(this.getClass().getSimpleName() + "-even");

        oddT.start();
        evenT.start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return time;
    }
}
