package org.originit.deadlock.dao;

import org.originit.deadlock.pojo.Account;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

public class AccountDao {

    private final ConcurrentHashMap<Integer, Account> accounts = new ConcurrentHashMap<>();

    public Account getById(Integer id) {
        if (accounts.get(id) == null) {
            synchronized (accounts) {
                if (accounts.get(id) == null) {
                    // 模拟从数据库中获取，默认大家都有1000块钱
                    accounts.put(id, new Account(id, new BigDecimal("1000")));
                }
            }
        }
        return accounts.get(id);
    }


}
