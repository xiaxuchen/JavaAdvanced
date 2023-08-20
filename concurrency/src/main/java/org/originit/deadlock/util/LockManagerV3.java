package org.originit.deadlock.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class LockManagerV3 {

    private Set<Integer> lockedAccounts = new HashSet<>();

    private static volatile LockManagerV3 instance;

    private final ThreadLocal<Set<Integer>> acquiredLocks = ThreadLocal.withInitial(HashSet::new);

    private final ConcurrentHashMap<Integer, List<Thread>> waitThreads = new ConcurrentHashMap<>();

    private LockManagerV3() {}

    public static LockManagerV3 getInstance() {
        if (LockManagerV3.instance == null) {
            synchronized (LockManagerV3.class) {
                if (LockManagerV3.instance == null) {
                    LockManagerV3.instance = new LockManagerV3();
                }
            }
        }
        return LockManagerV3.instance;
    }

    public void apply(Integer from, Integer to) {
        while (true) {
            synchronized (this) {
                if (from == null || to == null) {
                    throw new IllegalArgumentException("用户id不能为空");
                }
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
                        waitThreads.computeIfAbsent(from, k -> new ArrayList<>());
                        waitThreads.get(from).add(Thread.currentThread());
                    }
                    else if (lockedAccounts.contains(to)) {
                        waitThreads.computeIfAbsent(to, k -> new ArrayList<>());
                        waitThreads.get(to).add(Thread.currentThread());
                    }
                }
            }
            LockSupport.park();
        }

    }

    public synchronized void release(Integer from, Integer to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("用户id不能为空");
        }
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
    }

    private void unpark(Integer accountId) {
        Optional<Thread> optional = waitThreads.getOrDefault(accountId, (List<Thread>) Collections.EMPTY_LIST).stream().findFirst();
        optional.ifPresent(thread -> {
            waitThreads.get(accountId).remove(thread);
            LockSupport.unpark(thread);
        });
    }



}
