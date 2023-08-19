package org.originit.deadlock.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

public class LockManagerV2 {

    private Set<Integer> lockedAccounts = new HashSet<>();

    private static volatile LockManagerV2 instance;

    //
    private final ThreadLocal<Set<Integer>> acquiredLocks = ThreadLocal.withInitial(HashSet::new);

    private final ConcurrentHashMap<Integer, List<Thread>> waitThreads = new ConcurrentHashMap<>();

    private LockManagerV2() {}

    public static LockManagerV2 getInstance() {
        if (LockManagerV2.instance == null) {
            synchronized (LockManagerV2.class) {
                if (LockManagerV2.instance == null) {
                    LockManagerV2.instance = new LockManagerV2();
                }
            }
        }
        return LockManagerV2.instance;
    }

    public void apply(Integer from, Integer to) {
        while (true) {
            synchronized (this) {
                if (from == null || to == null) {
                    throw new IllegalArgumentException("用户id不能为空");
                }
                // ensure no repeat wait thread
                waitThreads.getOrDefault(from, (List<Thread>) Collections.EMPTY_LIST).removeIf(item -> item == Thread.currentThread());
                waitThreads.getOrDefault(to, (List<Thread>) Collections.EMPTY_LIST).removeIf(item -> item == Thread.currentThread());
                if (!lockedAccounts.contains(from) && !lockedAccounts.contains(to)) {
                    lockedAccounts.add(from);
                    lockedAccounts.add(to);
                    Set<Integer> curAcquired = acquiredLocks.get();
                    curAcquired.add(from);
                    curAcquired.add(to);
                    break;
                } else {
                    waitThreads.computeIfAbsent(from, k -> new ArrayList<>()).add(Thread.currentThread());
                    waitThreads.computeIfAbsent(to, k -> new ArrayList<>()).add(Thread.currentThread());
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
            // 释放掉所有等待线程
            waitThreads.getOrDefault(from, (List<Thread>) Collections.EMPTY_LIST).forEach(LockSupport::unpark);
            waitThreads.getOrDefault(to,  (List<Thread>)Collections.EMPTY_LIST).forEach(LockSupport::unpark);
        } else {
            throw new IllegalStateException(String.format("current Thread didn't acquire all id include 【%d,%d】", from, to));
        }
    }




}
