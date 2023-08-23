package org.originit.base;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public interface ExecutorServiceCapable {

    default ExecutorService getExecutorService(Integer corePoolSize, Integer maximumPoolSize) {
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
}
