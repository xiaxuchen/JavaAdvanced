package org.originit.deadlock.util;

import java.util.HashSet;
import java.util.Set;

public class LockManagerV1 {

    private Set<Integer> lockedAccounts = new HashSet<>();

    private static volatile LockManagerV1 instance;

    //
    private final ThreadLocal<Set<Integer>> acquiredLocks = ThreadLocal.withInitial(HashSet::new);

    private LockManagerV1() {}

    public static LockManagerV1 getInstance() {
        if (LockManagerV1.instance == null) {
            synchronized (LockManagerV1.class) {
                if (LockManagerV1.instance == null) {
                    LockManagerV1.instance = new LockManagerV1();
                }
            }
        }
        return LockManagerV1.instance;
    }

    public synchronized boolean apply(Integer from, Integer to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("用户id不能为空");
        }
        if (!lockedAccounts.contains(from) && !lockedAccounts.contains(to)) {
            lockedAccounts.add(from);
            lockedAccounts.add(to);
            Set<Integer> curAcquired = acquiredLocks.get();
            curAcquired.add(from);
            curAcquired.add(to);
            return true;
        }
        return false;
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
        } else {
            throw new IllegalStateException(String.format("current Thread didn't acquire all id include 【%d,%d】", from, to));
        }
    }




}
