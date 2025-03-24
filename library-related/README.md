# lib analyze

## 1. antlr4
[antlr quick start](./src/test/java/org/originit/antlr/CalculatorDriver.java)

[ ] auto compile g4 before java compile

### 访问者模式
对于所有的类型，接口存在对应的visit方法，Visitor接口中定义了所有的visit方法。
而对于每种类型， 其自身存在accept方法, 接受一个Visitor对象，自行决定调用哪个visit方法。

- 优点: 当扩展新的操作时(比如从AST中获取特定信息，再比如替换树中的节点等)，只需要实现Visitor接口，而不需要修改原有的实现。
- 缺点: 但是当新增一种类型时，需要修改所有的Visitor实现。(因此像spark等框架会定义各种类型的基类， 从而覆盖各种操作，扩展新的操作只需要继承基类，而在Visitor添加对应的逻辑注册，比如UDF,而不修改Visitor实现)
