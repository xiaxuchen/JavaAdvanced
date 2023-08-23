package org.originit.print.impl;

import org.originit.print.AbstractNumberPrint;

import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockPrintWithoutWait extends AbstractNumberPrint {

    private volatile int num = 0;

    private ReentrantLock reentrantLock = new ReentrantLock();

    @Override
    public void printOdd(Thread odd, Thread even) {
        while(!isFinish()) {
            reentrantLock.lock();
            try {
                if (num % 2 != 0) {
                    final boolean res = print(num);
                    num = add(num);
                    if (!res) {
                        break;
                    }
                }
            } finally {
                reentrantLock.unlock();
            }
        }

    }

    @Override
    public void printEven(Thread odd, Thread even) {
            while(!isFinish()) {
                reentrantLock.lock();
                try {
                    if (num % 2 == 0) {
                        final boolean res = print(num);
                        num = add(num);
                        if (!res) {
                            break;
                        }
                    }
                } finally {
                    reentrantLock.unlock();
                }
            }

    }

}
