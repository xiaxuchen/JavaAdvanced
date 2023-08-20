## 交替打印奇偶数(org.originit.print.Entry)
项目在org.originit.print.impl中实现了AbstractNumberPrint去使用不同的同步
方法去交替打印，Entry是项目入口，会自动扫描这个包加载所有的实现类然后调用对应的策略去验证，
并在控制台打印时间
## 解决死锁问题(org.originit.deadlock.service.AccountService)
模拟转账的场景，进行转账，同时要避免死锁
### 基础版本,全局锁
#### transferGlobalLock
通过使用AccountService.class作为全局锁，这样能保证不出现死锁，但是所有业务都会变成串行，实际业务中不可行
### 破坏死锁形成的占有并等待条件(一次性分配资源)
#### transferManagerV1
通过全局的LockManagerV1进行多个账户的锁的一次性获取和回收。该版本采用while循环的方式不断尝试获取锁，有一定的性能问题。
#### transferManagerV2
通过全局的LockManagerV2进行多个账户的锁的一次性获取和回收。该版本采用park与unpark来对线程进行睡眠与唤醒，避免cpu空转，当然也因此存在大量的线程切换。
#### transferManagerV3
对V2版本的改进，V2是唤醒所有等待对应资源的线程，而该版本则只唤醒一个线程，并且如果加锁失败(因为需要等待两个资源，所以可能另一个资源被占用了)就会唤醒一个其他的线程。
一直遇到一个问题，最终解决了
```java
public void apply(Integer from, Integer to) {
    while (true) {
        synchronized (this) {
            if (两个资源不同时满足) {
                if (lockedAccounts.contains(from)) {
                    waitThreads.computeIfAbsent(from, k -> new ArrayList<>());
                    waitThreads.get(from).add(Thread.currentThread());\
                }
                // 刚开始这里没有else,会导致可能一次失败的资源分配该线程等待两个资源的唤醒
                // unpark方法会保证在唤醒的时候将线程对应的等待移除
                // 而如果唤醒的资源存在，而另一个资源不存在则又会添加一个到waitThreads中，就产生重复的等待。
                if (lockedAccounts.contains(to)) {
                    waitThreads.computeIfAbsent(to, k -> new ArrayList<>());
                    waitThreads.get(to).add(Thread.currentThread());
                }
           }
            
        }
        LockSupport.park();
    }
}
```
#### transferManagerV4
这里采用ReentrantLock与Condition来实现唤醒和等待
### 破坏循环等待(按顺序分配资源)
#### transferLockOrdered
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
## 多重加锁中使用wait方法的探究
具体可以看test目录下org.originit.multilock.MultiLock的单元测试，具体结论就是调用哪个锁对象的wait就释放哪个锁，而其他的锁不释放同时阻塞。
这其实很好理解，**wait操作相当于释放锁 + park**
## 终止线程(org.originit.thread.ThreadInterruptTest)
测试使用Thread实例的interrupt方法打断线程执行。
### 问题
- 通过try catch捕获InterruptedException会清除线程interrupted标志，因此在捕获异常中需要去调用`Thread.currentThread().interrupt();`
- 如果线程执行结束进入销毁状态，其isInterrupted会为false，可以通过isAlive来判断线程是否存活。