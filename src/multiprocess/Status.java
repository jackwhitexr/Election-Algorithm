package multiprocess;

/**
 * @author SY1706306 XingRui
 *
 * @date: 18-05-28
 *
 * @description: 进程状态类
 *
 */
public class Status {
    public static String NORMAL="NORMAL";
    public static String CRASH="CRASH";

    public static String ELECTING="ELECTING";
    public static String WAIT_FOR_LEADER="WAIT_FOR_LEADER";     //收到了OK消息
    public static String BECOMING_LEADER="BECOMING_LEADER";     //成为LEADER，但未发送消息
}
