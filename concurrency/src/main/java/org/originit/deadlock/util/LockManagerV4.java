package org.originit.deadlock.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class LockManagerV4 {

    private Set<Integer> lockedAccounts = new HashSet<>();

    private static volatile LockManagerV4 instance;

    private final ThreadLocal<Set<Integer>> acquiredLocks = ThreadLocal.withInitial(HashSet::new);

    private final ReentrantLock reentrantLock = new ReentrantLock();

    private final Map<Integer, Condition> conditions = new HashMap<>();

    private LockManagerV4() {
    }

    public static LockManagerV4 getInstance() {
        if (LockManagerV4.instance == null) {
            synchronized (LockManagerV4.class) {
                if (LockManagerV4.instance == null) {
                    LockManagerV4.instance = new LockManagerV4();
                }
            }
        }
        return LockManagerV4.instance;
    }

    public void apply(Integer from, Integer to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("用户id不能为空");
        }
        reentrantLock.lock();
        try {
            while(true) {
                if (!lockedAccounts.contains(from) && !lockedAccounts.contains(to)) {
                    lockedAccounts.add(from);
                    lockedAccounts.add(to);
                    Set<Integer> curAcquired = acquiredLocks.get();
                    curAcquired.add(from);
                    curAcquired.add(to);
                    break;
                } else {
                    // 如果两个锁只有一个存在，那么就可以唤醒其他等待这个锁的线程
                    if (!lockedAccounts.contains(from)) {
                        unpark(from);
                    }
                    if (!lockedAccounts.contains(to)) {
                        unpark(to);
                    }
                    if (lockedAccounts.contains(from)) {
                        conditions.computeIfAbsent(from, (key) -> reentrantLock.newCondition()).await();
                    }
                    if (lockedAccounts.contains(to)) {
                        conditions.computeIfAbsent(to, (key) -> reentrantLock.newCondition()).await();
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            reentrantLock.unlock();
        }


    }

    public void release(Integer from, Integer to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("用户id不能为空");
        }
        reentrantLock.lock();
        try {
            Set<Integer> curAcquired = acquiredLocks.get();
            if (curAcquired.contains(from) && curAcquired.contains(to)) {
                curAcquired.remove(from);
                curAcquired.remove(to);
                lockedAccounts.remove(from);
                lockedAccounts.remove(to);
                // 唤醒一个
                unpark(from);
                unpark(to);
            } else {
                throw new IllegalStateException(String.format("current Thread didn't acquire all id include 【%d,%d】", from, to));
            }
        } finally {
            reentrantLock.unlock();
        }

    }

    private void unpark(Integer accountId) {
        Condition condition = conditions.get(accountId);
        if (condition != null) {
            condition.signal();
        }
    }



}
