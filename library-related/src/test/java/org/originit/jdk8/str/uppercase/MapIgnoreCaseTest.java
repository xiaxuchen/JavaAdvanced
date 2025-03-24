package org.originit.jdk8.str.uppercase;

import org.junit.Test;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
public class MapIgnoreCaseTest {

    // 30w条数据
    public static final int DATA_SIZE = 300000;

    // 50个线程
    public static final int THREAD_COUNT = 50;

    public void testMapIgnoreCaseInternal(Map<String, String> map, CountDownLatch latch, AtomicLong time, boolean keyToUpper) {
        long l = System.currentTimeMillis();
        if (keyToUpper) {
            for (String key : map.keySet()) {
                map.get(key.toUpperCase(Locale.ROOT));
            }
        } else {
            for (String key : map.keySet()) {
                map.get(key);
            }
        }

        latch.countDown();
        time.addAndGet(System.currentTimeMillis() - l);
    }

    public void multiThreadTestMapIgnoreCase(Supplier<Map<String, String>> supplier, String name, boolean keyToUpper) throws InterruptedException {
        AtomicLong time = new AtomicLong();
        Map<String, String> map = supplier.get();
        for (int i = 0; i < DATA_SIZE; i++) {
            map.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        }
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(() -> {
                testMapIgnoreCaseInternal(map, latch, time, keyToUpper);
            }).start();
        }
        latch.await();
        System.out.println(name + ":" + time.get() / THREAD_COUNT + "ms");
    }

    @Test
    public void testSkipCaseSensitive() throws InterruptedException {
        multiThreadTestMapIgnoreCase(ConcurrentSkipListMap::new, "testSkipCaseSensitive", true);
    }

    @Test
    public void testSkipCaseInSensitive() throws InterruptedException {
        multiThreadTestMapIgnoreCase(() -> new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER), "testSkipCaseInSensitive", false);
    }

    @Test
    public void testHash() throws InterruptedException {
        multiThreadTestMapIgnoreCase(ConcurrentHashMap::new, "testHash", true);
    }


}
