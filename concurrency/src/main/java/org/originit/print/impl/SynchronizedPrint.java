package org.originit.print.impl;

import lombok.extern.slf4j.Slf4j;
import org.originit.print.AbstractNumberPrint;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class SynchronizedPrint extends AbstractNumberPrint {


    // 如果没有volatile提供可见性，那么可能两个线程都会死循环，因为更新不可见导致两个都互相看到的不是自己要的，一直在cpu轮询
    private volatile Integer num = new Integer(0);

    private Object lock = new Object();

    @Override
    public void printOdd(Thread odd, Thread even) {
        while (true) {
            synchronized (lock) {
                if (num % 2 != 0) {
                    final boolean res = print(num);
                    num = add(num);
                    lock.notifyAll();
                    if (!res) {
                        break;
                    }
                }else {
                    lock.notifyAll();
                }
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                   log.info("interrupt");
                }
            }
        }

    }

    @Override
    public void printEven(Thread odd, Thread even) {
        while (true) {
            synchronized (lock) {
                if (num % 2 == 0) {
                    final boolean res = print(num);
                    num = add(num);
                    lock.notifyAll();
                    if (!res) {
                        break;
                    }
                } else {
                    lock.notifyAll();
                }
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    System.out.println("interrupt");
                }
            }

        }
    }

    public static void main(String[] args) {
        new SynchronizedPrint().run();
    }
}
