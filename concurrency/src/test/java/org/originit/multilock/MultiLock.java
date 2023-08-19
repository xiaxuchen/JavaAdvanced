package org.originit.multilock;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * 测试在多重加锁中使用wait进行锁的释放会怎样？
 * 结果: 只会释放调用wait的锁，而其他锁不会释放
 */
@RunWith(Parameterized.class)
public class MultiLock {

    final Object lockA = new Object();

    final Object lockB = new Object();

    // tryLock尝试加锁并修改该状态
    volatile boolean flag = false;

    public static final int LOCK_A = 1;

    public static final int LOCK_B = 2;

    int waitLock = LOCK_A;
    int tryingLock = LOCK_A;
    final boolean expected;

    public MultiLock(int waitLock, int tryingLock, boolean expected) {
        this.waitLock = waitLock;
        this.tryingLock = tryingLock;
        this.expected = expected;
    }

    public void multiLock() {
        synchronized (lockA) {
            synchronized (lockB) {
                try {
                    if (waitLock == LOCK_A) {
                        lockA.wait();
                    } else {
                        lockB.wait();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void tryLock() {
        if (tryingLock == LOCK_A) {
            synchronized (lockA) {
                flag = true;
            }
        } else {
            synchronized (lockB) {
                flag = true;
            }
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        // 通过测试可知道，在多重锁中wait，只会释放wait的锁，然后会阻塞，导致外层的锁释放不掉，因此多重锁不能使用wait
        return Arrays.asList(new Object[][] {
                        { LOCK_A, LOCK_A, true },
                        { LOCK_B, LOCK_B, true },
                        { LOCK_B, LOCK_A, false},
                        { LOCK_A, LOCK_B, false}
                }
        );
    }

    @Test
    public void testMulti() throws InterruptedException {
        Assert.assertFalse(flag);
        new Thread(this::multiLock).start();
        new Thread(this::tryLock).start();
        Thread.sleep(1000);
        Assert.assertEquals(flag, expected);
    }
}

