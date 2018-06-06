package multiprocess;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * @author SY1706306 XingRui
 *
 * @date: 18-05-28
 *
 * @version 1.0
 *
 * @description: 节点模型，包括节点的id，port，status，role等信息
 *
 */
public class Node {
    private int uid;
    private int port;
    private String status;
    private String role;

    //进程通讯相关
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    public Node(int uid,int port,String status,String role){
        this.uid=uid;
        this.port=port;
        this.status=status;
        this.role=role;
    }


    /**
     * 与该进程建立连接，侦测进程存活
     * @return true 连接成功
     */
    public boolean connect(){
        try {
            socket=new Socket("localhost",this.getPort());
            socket.setSoTimeout(Timeout.timeout);   //超时认为进程崩溃
            reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer=new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            return true;    //连接成功
        }
        catch(SocketTimeoutException e){
            System.out.println(this.getUid()+" "+"is down.");
            return false;
        }

        /*
        进程已挂
        区分leader进程和follower进程异常
         */
        catch (IOException e) {
            System.out.println(this.getUid()+" "+"is down.");
            //e.printStackTrace();
            return false;
        }
    }

    /**
     * 断开连接，侦测存活
     * @return
     */
    public boolean disconnect(){
        try {
            socket.close();
            reader=null;
            writer=null;
            return true;
        } catch (IOException e) {
            System.out.println(this.getUid()+" "+"is down.");
            return false;
        }
    }


    /**
     * @return true leader存活
     *
     * @// TODO: 18-5-31
     * 如果这个node是leader，监视是否存活，若超时则启动Elect过程
     * 这个方法只有node是leader的时候才会调用
     * 可以整合
     */

    public boolean askAlive(){
        //leader is dead
        if(this.connect()==false){
            disconnect();
            return false;
        }
        else{
            String askAliveMessage=this.getUid()+Message.SPLIT+Message.ASK_ALIVE;
            writer.write(askAliveMessage);
            writer.flush();
            this.disconnect();
            return true;    //leader is alive this round 2333
        }
    }


    /*
    向这个uid发送消息
    发送失败说明对方进程已挂
     */
    public boolean sendMessage(String message){
        //leader进程已挂
        if(this.connect()==false){
            if(this.disconnect()==false) return false;
            return false;
        }
        else{
            writer.write(message);
            writer.flush();
            if(this.disconnect()==false) return false;
            return true;
        }
    }

    public synchronized int getUid() {
        return uid;
    }
    public synchronized int getPort(){
        return port;
    }
    public synchronized String getStatus(){
        return status;
    }
    public synchronized void setStatus(String status){
        this.status=status;
    }
    public synchronized String getRole(){
        return role;
    }
    public synchronized void setRole(String role){
        this.role=role;
    }
}
