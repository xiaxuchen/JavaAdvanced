package org.originit.deadlock;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
public class JmhDeadLock {

    private Integer taskCount = 5000;

    @Benchmark
    public void run50Threads() throws InterruptedException {
        DeadLockTest deadLockTest = new DeadLockTest(50);
        deadLockTest.setTaskCount(taskCount);
        deadLockTest.testMultiThreadsV1();
    }

    @Benchmark
    public void run20Threads() throws InterruptedException {
        DeadLockTest deadLockTest = new DeadLockTest(20);
        deadLockTest.setTaskCount(taskCount);
        deadLockTest.testMultiThreadsV1();
    }

    @Benchmark
    public void run100Threads() throws InterruptedException {
        DeadLockTest deadLockTest = new DeadLockTest(100);
        deadLockTest.setTaskCount(taskCount);
        deadLockTest.testMultiThreadsV1();
    }

    @Benchmark
    public void run200Threads() throws InterruptedException {
        DeadLockTest deadLockTest = new DeadLockTest(200);
        deadLockTest.setTaskCount(taskCount);
        deadLockTest.testMultiThreadsV1();
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(JmhDeadLock.class.getSimpleName())
            .build();
        new Runner(opt).run();
    }
}