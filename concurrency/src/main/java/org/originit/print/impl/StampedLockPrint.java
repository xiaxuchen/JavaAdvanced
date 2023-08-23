package org.originit.print.impl;

import lombok.extern.slf4j.Slf4j;
import org.originit.print.AbstractAggNumberPrint;

import java.util.concurrent.locks.StampedLock;
import java.util.function.Predicate;

@Slf4j
public class StampedLockPrint extends AbstractAggNumberPrint {

    private volatile int i = 0;

    StampedLock stampedLock = new StampedLock();


    @Override
    protected void printNum(Thread odd, Thread even, Predicate<Integer> predicate, boolean isOdd) {
        while (!isFinish()) {
            long stamp = stampedLock.writeLock();
            try {
                if (predicate.test(i)) {
                    boolean next = print(i);
                    if (!next) {
                        break;
                    }
                    i++;
                }
            } finally {
                stampedLock.unlockWrite(stamp);
            }
        }
    }

    public static void main(String[] args) {
        new StampedLockPrint().run();
    }
}
