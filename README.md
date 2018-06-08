# Election Algorithm
Java实现的一种选举算法——欺负算法（Bully Algorithm），模拟了多进程Follower之间选举出Leader的过程。

# System Requirements
1. Windows,Linux流行版本
2. JDK1.8及以上
3. 不需要其他第三方库

# Quick Start
请在项目目录下建立config.conf文件作为选举算法的配置文件。配置文件指明了每个进程的uid,port,status,role。请参考config.conf.example文件的格式要求。

在项目目录\out\artifacts打开命令行，按照先运行leader后运行follower的顺序（follower之间不区分顺序）运行进程。

运行一个进程的命令如下：
`java -jar Election_Algorithm.jar uid config.conf`

手动终止leader进程，follower会选出下一个leader。可以看到follower在发送确认leader的存活信息，leader接收follower的查询存活信息并返回结果。

# Note
注意进程数量以及信息需要和配置文件中一致。

# TODO
某些异常被捕获处理，但不太友好地显示出来，需要进一步处理；
可以设计Result消息的回复消息。

# Link
https://github.com/jackwhitexr/Election-Algorithm