## 交替打印奇偶数
项目在org.originit.print.impl中实现了AbstractNumberPrint去使用不同的同步
方法去交替打印，Entry是项目入口，会自动扫描这个包加载所有的实现类然后调用对应的策略去验证，
并在控制台打印时间
