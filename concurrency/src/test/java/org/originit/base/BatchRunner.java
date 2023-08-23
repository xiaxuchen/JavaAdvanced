package org.originit.base;

import java.util.concurrent.ExecutorService;

public interface BatchRunner{

    ExecutorService getExecutorService();

    default void runBatch(int count, Runnable runnable) {
        ExecutorService executorService = getExecutorService();
        for (int i = 0; i < count; i++) {
            executorService.submit(runnable);
        }
    }
}
