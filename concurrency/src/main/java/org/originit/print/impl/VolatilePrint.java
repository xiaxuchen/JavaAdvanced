package org.originit.analyze.print.impl;

import org.originit.print.AbstractNumberPrint;

public class VolatilePrint extends AbstractNumberPrint {


    // 如果没有volatile提供可见性，那么可能两个线程都会死循环，因为更新不可见导致两个都互相看到的不是自己要的，一直在cpu轮询
    private volatile int num = 0;

    @Override
    public void printOdd(Thread odd, Thread even) {
        while (!isFinish()) {
            if (num % 2 != 0) {
                final boolean res = print(num);
                num = add(num);
                if (!res) {
                    break;
                }
            }
        }

    }

    @Override
    public void printEven(Thread odd, Thread even) {
        while (!isFinish()) {
            if (num % 2 == 0) {
                final boolean res = print(num);
                num = add(num);
                if (!res) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        new VolatilePrint().run();
    }
}
