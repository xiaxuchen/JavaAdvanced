package org.originit.util;

import java.util.function.Supplier;

public class TimeUtil {

    public static class TimeResult<T> {

        public T res;

        public long time;

        public TimeResult(T res, long time) {
            this.res = res;
            this.time = time;
        }
    }

    public static <T> TimeResult<T> timeIt(Supplier<T> supplier) {
        long start = System.currentTimeMillis();
        T res = supplier.get();
        long end = System.currentTimeMillis();
        return new TimeResult<>(res, end - start);
    }
}
