## 交替打印奇偶数
项目在org.originit.print.impl中实现了AbstractNumberPrint去使用不同的同步
方法去交替打印，Entry是项目入口，会自动扫描这个包加载所有的实现类然后调用对应的策略去验证，
并在控制台打印时间
## 解决死锁问题(org.originit.deadlock.service.AccountService)
模拟转账的场景，进行转账，同时要避免死锁
### 基础版本,全局锁
#### transferV4
通过使用AccountService.class作为全局锁，这样能保证不出现死锁，但是所有业务都会变成串行，实际业务中不可行
### 破坏死锁形成的占有并等待条件
#### transferV1
通过全局的LockManagerV1进行多个账户的锁的一次性获取和回收。该版本采用while循环的方式不断尝试获取锁，有一定的性能问题。
#### transferV2
通过全局的LockManagerV2进行多个账户的锁的一次性获取和回收。该版本采用park与unpark来对线程进行睡眠与唤醒，避免cpu空转，当然也因此存在大量的线程切换。
### 破坏循环等待
#### transferV3
在转账之前先根据用户的id进行排序，按照从大到小(从小到大也行)的顺序对账户进行加锁。

例如用户3向用户4转100，同时用户4也向用户3转钱    
- 那么两个线程会在加锁用户3的时候冲突，谁抢到用户3的锁才能抢用户4的锁。    
- 如果不采用顺序加锁，那么可能一个线程持有用户3的锁并申请用户4的锁，而另一个线程持有用户4的锁申请用户3的锁，从而导致死锁。
### 单元测试
在test目录org.originit.deadlock.DeadLockTest中对不同的版本以及不同线程数编写了单元测试，直接运行即可测试。
#### 配置
- 参数化配置线程数  
当前测试用例中线程数有20,50,80,200,如下:  
```
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                        { 20},
                        { 50},
                        { 80},
                        { 200}
                }
        );
    }

```
- 配置用户数量    
`private Integer accountNumber = 100;`更改DeadLockTest的该变量即可
- 配置任务数量  
`private Integer taskCount = 5000;`更改DeadLockTest的该变量即可
