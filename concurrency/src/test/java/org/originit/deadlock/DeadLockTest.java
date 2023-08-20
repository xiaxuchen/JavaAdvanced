package org.originit.deadlock;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.originit.deadlock.exception.AccountSurplusNotEnoughException;
import org.originit.deadlock.service.AccountService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

@RunWith(Parameterized.class)
@Slf4j
public class DeadLockTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                        { 20},
                        { 50},
                        { 80},
                        { 200}
                }
        );
    }

    private Integer accountNumber = 100;

    private Integer threadCount;

    private Integer taskCount = 5000;

    public void setTaskCount(Integer taskCount) {
        this.taskCount = taskCount;
    }

    public DeadLockTest(Integer threadCount) {
        this.threadCount = threadCount;
    }

    private final AccountService accountService = new AccountService();

    private ExecutorService getExecutorService(Integer corePoolSize, Integer maximumPoolSize) {
        AtomicInteger idGenerator = new AtomicInteger();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                1L, TimeUnit.MINUTES, new ArrayBlockingQueue<>(20000), r -> {
            Thread thread = new Thread(r);
            thread.setName("threadDeadTest-" + idGenerator.incrementAndGet());
            return thread;
        });
        threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return threadPoolExecutor;
    }

    private void testCommon(BiConsumer<Integer,Integer> runnable) throws InterruptedException {
        if (runnable == null) {
            throw new IllegalArgumentException("业务方法不能为空");
        }
        ExecutorService executorService = getExecutorService(threadCount, threadCount);
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(taskCount);
        long start = System.currentTimeMillis();
        for (int i = 0; i < taskCount; ) {
            // 随机生成1-100之间的两个数，作为转账的账户
            int from = (int) (Math.random() * accountNumber) + 1;
            int to = (int) (Math.random() * accountNumber) + 1;
            if (from == to) {
                continue;
            }
            try {
                executorService.submit(() -> {
                    try {
                        runnable.accept(from, to);
                    }catch (AccountSurplusNotEnoughException ignored) {

                    } finally {
                        count.incrementAndGet();
                        log.info("count: " + count.get());
                        countDownLatch.countDown();
                    }
                });
            }catch (Exception e) {
                // 队列满了抛出异常，忽略，继续提交任务
                continue;
            }
            i++;
        }
        countDownLatch.await();
        BigDecimal result = IntStream.range(1, accountNumber + 1)
                .mapToObj(id -> accountService.getAccountById(id).getSurplus())
                .reduce(new BigDecimal("0"), BigDecimal::add);
        Assert.assertEquals( new BigDecimal("1000").multiply(new BigDecimal(accountNumber)), result);
        long end = System.currentTimeMillis();
        if (end - start < 1000) {
            log.info("execution is so quick in 1s, {} tasks executed", taskCount);
            return;
        }
        // 计算tps
        log.info("current tps: " + (taskCount / ((end - start) / 1000)));
    }

    @Test
    public void testMultiThreadsV1() throws InterruptedException {
       testCommon((from, to) -> {
           accountService.transferManagerV1(from, to, new BigDecimal("100"));
       });
    }


    @Test
    public void testMultiThreadsV2() throws InterruptedException {
        testCommon((from, to) -> {
            accountService.transferManagerV2(from, to, new BigDecimal("100"));
        });
    }

    @Test
    public void testMultiThreadsV3() throws InterruptedException {
        testCommon((from, to) -> {
            accountService.transferManagerV3(from, to, new BigDecimal("100"));
        });
    }

    @Test
    public void testMultiThreadsOrderedLock() throws InterruptedException {
        testCommon((from, to) -> {
            accountService.transferLockOrdered(from, to, new BigDecimal("100"));
        });
    }

    @Test
    public void testMultiThreadsV4() throws InterruptedException {
        testCommon((from, to) -> {
            accountService.transferManagerV4(from, to, new BigDecimal("100"));
        });
    }

    @Test
    public void testMultiThreadsGlobalLock() throws InterruptedException {
        testCommon((from, to) -> {
            accountService.transferGlobalLock(from, to, new BigDecimal("100"));
        });
    }
}
