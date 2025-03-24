package org.originit.jdk8.str.uppercase;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 场景: 在生产环境中，由于toUpperCase调用频率比较高，导致方法的耗时增加。
 * 因此尝试对比和分析jdk自带的toUpperCase和自定义toUpperCase的性能对比。
 * 看是否是由于toUpperCase的实现问题，还是由于toUpperCase的调用频率问题。
 *
 * 边界: 只考虑常规英文字母的大小写，而不考虑拉丁字母的大小写转换
 *
 * 结论: 实际上，jdk自带的toUpperCase相对于自己实现的性能只有一倍的差距，
 * 具体也可以从源码中看出实现的时间复杂度为O(n)，
 * 调用十万次也只需要几十毫秒的时间,并不会对业务产生影响，
 * 因此在实际的业务场景中，不需要自己实现toUpperCase方法。
 *
 * 场景的问题原因: 由于数据量过大(30w条，50并发左右)，导致缓存中的数据过多，进而遍历ConcurrentSkipListMap
 * 的key时同时调用get方法(1500w次调用)，产生大量的toUpper方法调用，导致明显差异。
 * 具体可以查看{@link MapIgnoreCaseTest}的对比
 *
 */
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
public class UpperCaseTest {

    @Param(value = {"hello world", "abcdefghijklmnopqrstuvwxyz", "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz", "你好hello哈哈哈测试"})
    private String strToUpper;

    @Param(value = {"100,000"})
    private String times;

    @Param(value = {"jdk", "custom"})
    private String realization;

    public int getTimes() {
        return Integer.parseInt(times.replace(",", ""));
    }

    @Benchmark
    public void testToUpper() {
        if (realization.equals("jdk")) {
            for (int i = 0; i < getTimes(); i++) {
                strToUpper.toUpperCase(Locale.ROOT);
            }
        } else {
            for (int i = 0; i < getTimes(); i++) {
                CustomUpperCaseUtil.toUpperCase(strToUpper);
            }
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(UpperCaseTest.class.getSimpleName())
                .result("toUpper.csv")
                .resultFormat(ResultFormatType.CSV)
                .build();
        new Runner(opt).run();
    }
}
