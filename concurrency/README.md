## 交替打印奇偶数(org.originit.print.Entry)
项目在org.originit.print.impl中实现了AbstractNumberPrint去使用不同的同步
方法去交替打印，Entry是项目入口，会自动扫描这个包加载所有的实现类然后调用对应的策略去验证，
并在控制台打印时间
### 等待和唤醒与忙等对比
在org.originit.print.impl.ReentrantLockPrint、org.originit.print.impl.SynchronizedPrint中我们分别通过synchronized与ReentrantLock实现了等待与唤醒机制，
而在其对应的WithoutWait版本中则不使用等待与唤醒而是采用循环忙等，相比之下循环忙等效率大大超过等待与唤醒。对于1000000次的交替打印奇偶数，差距在6倍左右。其原因可能在于**大量
线程切换**导致的耗时。

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
## 限流器实现(org.originit.limiter.RateLimiterTest)
### 计数器(org.originit.limiter.CounterRateLimiter)
通过计数器记录当前请求的数量，如果超过最大请求数量则等待。而下一秒的时候重置请求数量。
如果大量流量在一秒的末端申请，就会产生在一秒内可以有两倍的流量。比如1.99s的时候突然100个请求来了。然后到了2.0s重置流量又有100个请求来了，那么这一段时间就出现了200个请求
### 滑动窗口(org.originit.limiter.SlidingWindowRateLimiter)
解决了计数器在一秒内可能有两倍流量的问题，但是滑动窗口的流量并**不是均匀**放出的。
- 通过窗口解决计数器问题。每次请求都会查看当前请求是否在最新的窗口中，如果在的话直接记录请求数，若流量已满则等待。    
- 而如果不在最新的窗口中则根据是否距离上次请求已经超过一个窗口期，如果超过一个窗口期则可以重置限流器(因为前面的请求已经不重要了，当前剩余的允许流量为maxPermits)。  
- 而如果当前请求时间距离最新的窗口结束时间不超过一个窗口期，则可以添加一个新的窗口进入队列中成为最新的窗口。且若队列满了则将队列头的窗口清除并释放其占用的请求数。

**问题**:滑动窗口限流器把一个时期分成多个窗口，比如一个时期是1s，然后能放行100个请求，
我们将一个时期分成10个子窗口假设，如果窗口队列超过10就会淘汰前面的，
一个窗口有他的开始时间结束时间，比如第一个窗口是0-0.1s，第二个是0.1到0.2秒，
每个窗口期间会记录这个窗口期内放行的请求数量，并累计到全局放行请求数上，
如果全局放行请求数等于最大了，就不放行了。等到时间到达1-1.1s，
就会创建一个新窗口将0-0.1s的窗口挤出去，并释放这个窗口期间放行的请求数。
那么可能我在第一个窗口就占满了，占100个，然后后面9个窗口都是0个，
直到一个新窗口将第一个窗口挤掉释放100个占用。但是如果请求比较秘籍，
就会不断循环。感觉很奇怪。意思就是说前面的请求不是渐进式释放的，
如果请求积压在一个窗口中。本来应该是假设请求在前1s被陆续占用，
那么请求在后面应该陆续释放，而不是一下放出来然后被占掉之后又不释放。而如果子窗口数量比较少，
假设是1，那么该算法会退化为计数器限流器。
### 漏桶算法(org.originit.limiter.LeakyBucketRateLimiter)
解决滑动窗口算法流量不均匀问题。但是该算法存在**无法应对突发流量**问题。
请求到来时进入漏桶排队，漏桶以指定速度从漏桶中流出请求。    
而实现上则通过预占机制进行，即通过记录下一个流量流出的时间，
如果该事件大于当前时间则当前请求等待到该时间点，否则直接放行。
但这样的问题是如果一段时间没有请求流量，
不会累积可放行的请求，仍然是按照固定速率去一个一个放行。
### 令牌桶算法(org.originit.limiter.TokenBucketRateLimiter)
解决滑动窗口算法流量不均匀问题。    
每次申请规则如下:
- 更新当前令牌信息
- 当前桶内有令牌，使用桶内的令牌
- 当前桶内没有令牌，通过预占用的方式来分配令牌，即**预占用**(通过一个next变量指示下一个生成令牌的时间点)未来某个时间点将会产生的令牌，并睡眠等待到该时间
### 漏桶算法的park与unpark实现(org.originit.limiter.DeprecatedLeakyBucketRateLimiter)
从漏桶算法的定义上，按照指定速率放出请求，比较直接的思路是使用一个阻塞队列，请求直接进入队列之后等待，然后由一个线程去按照指定速率从阻塞队列中拿取请求并唤醒。    
**问题**: 这样做能够实现，但是性能非常的差，使用上面的算法能够保证与期望流量差不多，但是使用该实现则会有巨大差距，如15s每秒100个请求实际放行900-1100个左右
### 令牌桶算法与漏桶算法的区别
- 令牌桶能够累计令牌，当请求进入时如果有足够的令牌，这些请求能够同时进行。
- 漏桶则不会累计令牌，桶中的请求按照指定的速率流出，可以看做请求按照时间顺序开始执行，如果有很多请求，每个请求之间至少都会有一个间隔(1秒钟/最大允许一秒的请求数),对于突发流量其无法处理，仍然是按照固定速率放出请求，而如果请求过多超过桶的容量则会直接拒绝。
## park与unpark先后问题(org.originit.park.ParkTest)
### 先park再unpark
正常的情况，park的线程会被unpark唤醒继续执行
### 先unpark再park
线程执行park时会继续执行，这就是信号量机制，unpark应该会对信号量+1,然后park的时候减1，这样即使先unpark再park也能够正常工作。