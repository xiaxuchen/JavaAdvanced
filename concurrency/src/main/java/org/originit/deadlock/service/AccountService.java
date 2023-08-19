package org.originit.deadlock.service;

import lombok.extern.slf4j.Slf4j;
import org.originit.deadlock.dao.AccountDao;
import org.originit.deadlock.exception.AccountSurplusNotEnoughException;
import org.originit.deadlock.pojo.Account;
import org.originit.deadlock.util.LockManagerV1;
import org.originit.deadlock.util.LockManagerV2;

import java.math.BigDecimal;

@Slf4j
public class AccountService {

    private static final int SLEEP_TIME = 100;

    AccountDao accountDao = new AccountDao();

    /**
     * 一次性分配所有资源，破坏占有并等待
     */
    public void transferV1(Integer from, Integer to, BigDecimal amount) {
        // 不断尝试获取锁
        while (!LockManagerV1.getInstance().apply(from, to)) {}
        try {
            doTransfer(from, to, amount);
        } catch (InterruptedException e) {
            // ignore
            throw new RuntimeException(e);
        } finally {
            LockManagerV1.getInstance().release(from, to);
        }
    }


    public void transferV2(Integer from, Integer to, BigDecimal amount) {
        LockManagerV2.getInstance().apply(from, to);
        try {
            doTransfer(from, to, amount);
        } catch (InterruptedException e) {
            // ignore
            throw new RuntimeException(e);
        } finally {
            LockManagerV2.getInstance().release(from, to);
        }
    }

    /**
     * 按账户id排序加锁，破坏循环等待条件
     */
    public void transferV3(Integer from, Integer to, BigDecimal amount) {
        // 不断尝试获取锁
        try {
            Account fromAccount = accountDao.getById(from);
            Account toAccount = accountDao.getById(to);
            Account outAccount = from > to? fromAccount : toAccount;
            Account innerAccount = from <= to? fromAccount : toAccount;
            synchronized (outAccount) {
                synchronized (innerAccount) {
                    Thread.sleep(SLEEP_TIME);
                    if (fromAccount.getSurplus().compareTo(amount) < 0) {
                        throw new AccountSurplusNotEnoughException("余额不足");
                    }
                    // 暂时不理会事务问题
                    fromAccount.setSurplus(fromAccount.getSurplus().subtract(amount));
                    toAccount.setSurplus(toAccount.getSurplus().add(amount));
                    log.debug("转账成功，转账金额【{}】，转账后账户信息【from:{},to:{}】%n", amount, fromAccount, toAccount);
                }
            }
        } catch (InterruptedException e) {
            // ignore
            throw new RuntimeException(e);
        }
    }

    /**
     * 全局锁
     */
    public void transferV4(Integer from, Integer to, BigDecimal amount) {
        synchronized (AccountService.class) {
            try {
                doTransfer(from, to, amount);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }



    private void doTransfer(Integer from, Integer to, BigDecimal amount) throws InterruptedException {
        Account fromAccount = accountDao.getById(from);
        Account toAccount = accountDao.getById(to);
        Thread.sleep(SLEEP_TIME);
        if (fromAccount.getSurplus().compareTo(amount) < 0) {
            throw new AccountSurplusNotEnoughException("余额不足");
        }
        // 暂时不理会事务问题
        fromAccount.setSurplus(fromAccount.getSurplus().subtract(amount));
        toAccount.setSurplus(toAccount.getSurplus().add(amount));
        log.debug("转账成功，转账金额【{}】，转账后账户信息【from:{},to:{}】", amount, fromAccount, toAccount);
    }

    public Account getAccountById(Integer id) {
        return accountDao.getById(id);
    }

}
