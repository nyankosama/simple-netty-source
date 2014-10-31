#Simple Netty Source

本项目是netty.3.9.4.Final源码的精简版本，删除掉netty中不必要的功能，只保留了核心模块，方便进行源码学习。

##Change log

* **0.1.0**
    * 在保证interface的方法完整性的前提下，删除了核心逻辑package下的所有不必要的class
    * 重新组织了channel包下的类，添加了event, exception和future包，使每个package下的类的职能更明确，方便理解

* **0.0.1**
    * 以packge为粒度，删除了所有不必要的包
    * 确保netty的网络部分的核心逻辑的完整性


##模块变更

*以下的表格列出了保留的模块*

| Modules | Function |
|-----------------|-----------------------------------------------------------------------|
| bootstrap | 组织netty模块工作并启动相关模块的工具类 |
| buffer | Buffer的具体相关实现 |
| channel | 对java nio中的channel的重新封装的相关实现 |
| channel-socket | netty的网络核心模块，包括Boss、Worker、Selector、核心主循环等相关实现 |
| logging(不重要) | 相关的log工具类实现 |
| util(不重要) | 相关的工具类实现，包括Timer、StringUtil、ByteBufferUtil等 |   

**注：其中logging和util并不重要，阅读源码时可以略过，这里由于一些依赖关系所以保留**

*以下表格列出了删除掉的模块*

| Modules | Function |
|---------------------|------------------------------------|
| channel-group | Channel Group相关实现 |
| channel-local | local transport的相关支持 |
| channel-socket-http | http handler的相关实现 |
| channel-socket-oio | oio模式下的核心网络模块 |
| container | 一些无关紧要的适配 |
| example | Netty官方自带的一些例子 |
| handler | Netty异常强大功能全面的handler实现 |
| netty-test |  Netty的单元测试集|  

##源码阅读指南

由于netty模块内部的对象协作关系较为复杂，所以这里推荐从最为简单的EchoServer作为入口阅读相关源码。（Example代码已经附在了test中）

###关键源码阅读路径

* **new NioServerSocketChannelFactory()** ====> **new NioWorkerPool()** ====> **AbstractNioWorkerPool.init()** ====> **AbstractNioWorkerPool.newWorker(Executor)** ====> **NioWorkerPool.createWorker(Executor)** ====> **new AbstractNioSelector(Executor, ThreadNameDeterminer)** ====> **AbstractNioSelector.openSelector(ThreadNameDeterminer)**
* **new NioServerSocketChannelFactory()** ====> **new NioServerBossPool()** ====> **NioServerBossPool.init()** ====> **AbstractNioBossPool.newBoss(Executor)** ====> **NioServerBossPool.newBoss(Executor)** ====> **new NioServerBoss(Executor, ThreadNameDeterminer)** ====> **AbstractNioSelector.openSelector(ThreadNameDeterminer)**
* **AbstractNioSelector.run()** ====> **AbstractNioSelector.process()**
* **AbstractNioSelector.run()** ====> **NioServerBoss.process()**
* **AbstractNioSelector.run()** ====> **AbstractNioSelector.processTaskQueue()**

###注意事项

在阅读过程中需要注意以下几点：

* NioWorker和NioServerBoss分别是Worker和Boss的线程runnable实现，Netty的核心nio网络处理代码就在这两个类以及其相关父类中
* 顺着ServerBootstrap的创建，就可以摸清楚NioWorker和NioServerBoss是如何被创建，如何run起来的
* NioWorker和NioServerBoss中的firexxx一系列方法即为触发channelPipeline寻找已添加的handler分发对应的事件的入口方法
* DefaultChannelPipeline实现了ChannelPipeline，其中以链表维护具体的handler列表。具体事件的分发分为两个方向，即upStream和downStream，前者代表read事件的分发，后者代表write事件的分发
* buffer包中为ChannelBuffer的相关实现，在NioWorker中的process方法的read调用里面可以清晰看到ChannelBuffer的创建和分发的upStream的具体过程。
* ChannelBuffer的实现主要去看ChannelBuffer -> AbstractChannelBuffer -> HeapChannelBuffer -> BigEndianHeapChannelBuffer这条线即可
* 善于在关键路径运用断点查看调用栈，一目了然

##License
Apache License v2









