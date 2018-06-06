package multiprocess;

/**
 * @author SY1706306 XingRui
 *
 * @date: 18-05-28
 *
 * @description: 网络延时类，模拟网络延时
 *
 * @// TODO: 18-5-31
 * ServerSocket超时和Result发送有潜在的问题，当潜在leader发送消息到其余节点时，
 * 如果还没有发送到某个follower，这个follower可能会宣布“叛乱”为leader
 * 可能的解决方案：排序倒序发送，id越小越容易知道自己不是leader而进入WAIT_FOR_LEADER状态，
 * id大的容易宣布成为leader
 *
 *
 */
public class Timeout {
    public static int timeout=5000;
}
