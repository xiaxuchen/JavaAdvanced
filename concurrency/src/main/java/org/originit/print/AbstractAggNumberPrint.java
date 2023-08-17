package org.originit.print;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public abstract class AbstractAggNumberPrint extends AbstractNumberPrint{

    @Override
    protected void printOdd(Thread odd, Thread even) {
        printNum(odd, even, integer -> integer % 2 != 0, true);
    }

    @Override
    protected void printEven(Thread odd, Thread even) {
        printNum(odd, even, integer -> integer % 2 == 0, false);
    }

    protected abstract void printNum(Thread odd, Thread even, Predicate<Integer> predicate, boolean isOdd);
}
