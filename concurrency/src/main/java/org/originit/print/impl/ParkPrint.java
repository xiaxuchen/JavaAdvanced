package org.originit.print.impl;


import org.originit.print.AbstractAggNumberPrint;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

public class ParkPrint extends AbstractAggNumberPrint {


    private volatile int num = 0;


    private Object lock = new Object();

    @Override
    protected void printNum(Thread odd, Thread even, Predicate<Integer> predicate, boolean isOdd) {
        while(!isFinish()) {
            synchronized (lock) {
                if (predicate.test(num)) {
                    final boolean res = print(num);
                    num = add(num);
                    if (isOdd) {
                        even.interrupt();
                    } else {
                        odd.interrupt();
                    }
                    if (!res) {
                        break;
                    }
                }
            }
            LockSupport.park();
        }
    }

    public static void main(String[] args) {
        new ParkPrint().run();
    }

}
