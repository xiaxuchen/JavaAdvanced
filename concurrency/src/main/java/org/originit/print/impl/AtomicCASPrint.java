package org.originit.print.impl;


import org.originit.print.AbstractNumberPrint;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicCASPrint extends AbstractNumberPrint {

    private AtomicInteger atomicInteger = new AtomicInteger(0);

    @Override
    public void printOdd(Thread odd, Thread even) {
        while (!isFinish()) {
            final int i = atomicInteger.get();
            if (i % 2 != 0) {
                final boolean res = print(i);
                atomicInteger.compareAndSet(i, add(i));
                if (!res) {
                    break;
                }
            }
        }

    }

    @Override
    public void printEven(Thread odd, Thread even) {
        while (!isFinish()) {
            final int i = atomicInteger.get();
            if (i % 2 == 0) {
                final boolean res = print(i);
                atomicInteger.compareAndSet(i, add(i));
                if (!res) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        new AtomicCASPrint().run();
    }
}
