package org.originit.print.impl;

import org.originit.print.AbstractNumberPrint;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockPrint extends AbstractNumberPrint {

    private volatile int num = 0;

    private ReentrantLock reentrantLock = new ReentrantLock();

    private Condition evenCondition = reentrantLock.newCondition();

    private Condition oddCondition = reentrantLock.newCondition();

    @Override
    public void printOdd(Thread odd, Thread even) {
            reentrantLock.lock();
            try {
                while(true) {
                    if (num % 2 != 0) {
                        final boolean res = print(num);
                        num = add(num);
                        evenCondition.signalAll();
                        if (!res) {
                            break;
                        }
                    } else {
                        evenCondition.signalAll();
                    }
                    try {
                        oddCondition.await();
                    } catch (InterruptedException ignored) {
                    }
                }
            } finally {
                reentrantLock.unlock();
            }
    }

    @Override
    public void printEven(Thread odd, Thread even) {
        reentrantLock.lock();
        try {
            while(true) {
                if (num % 2 == 0) {
                    final boolean res = print(num);
                    num = add(num);
                    oddCondition.signalAll();
                    if (!res) {
                        break;
                    }
                } else {
                    oddCondition.signalAll();
                }
                try {
                    evenCondition.await();
                } catch (InterruptedException ignored) {
                }
            }
        } finally {
            reentrantLock.unlock();
        }
    }
}
