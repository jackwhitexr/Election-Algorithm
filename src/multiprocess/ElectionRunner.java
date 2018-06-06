package multiprocess;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SY1706306 XingRui
 *
 * @date: 18-05-28
 *
 * @version 1.0
 *
 * @description: 选举算法运行类，包含本进程的信息
 *
 */
public class ElectionRunner {

    private static int selfUid=-1;  //TODO  remove static
    private HashMap<Integer, Node> nodes=null;    //设计成HashMap是为了根据NodeID高效查找Node
    private BufferedReader reader = null;
    private PrintWriter writer = null;

    private int leaderUid = -1;     //-1表示当前没有leader
    private LeaderWatcher leaderWatcher=null;

    public static void main(String args[]) {
        ElectionRunner runner = new ElectionRunner();
        runner.parseArgs(args);
        runner.initLeaderWatcher(); //避免直接在类中声明出现NullPointer问题
        //非Leader时开启监视进程
        if(!runner.getNodeByUid(selfUid).getRole().equals(Role.LEADER)){
            runner.leaderWatcher.start();
        }
        runner.listen();
    }

    /**
     * @param Uid 进程Uid
     *
     * @return Node Uid对应的进程
     */
    Node getNodeByUid(int Uid){
        for(Map.Entry<Integer,Node> entry: nodes.entrySet()){
            if(entry.getKey()==Uid){
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 初始化监视线程
     */
    private void initLeaderWatcher(){
        leaderWatcher=new LeaderWatcher(leaderUid,selfUid,nodes);
        System.out.println(getNodeByUid(selfUid).getStatus());
    }

    private void sleepFew(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析命令行参数
     * @param args 命令行参数 格式：uid config-file-name
     */
    void parseArgs(String[] args) {
        if (args.length != 0) {
            selfUid = Integer.parseInt(args[0]);
            String configFileName = args[1];
            parseConfigFile(configFileName);
        }
    }

    /**
     * 读取配置文件，存储所有的节点信息
     * @param configFileName
     */
    private void parseConfigFile(String configFileName) {
        //读文件
        BufferedReader configReader = null;
        nodes = new HashMap<Integer, Node>();
        nodes.clear();
        try {
            configReader = new BufferedReader(new FileReader(configFileName));
            String line;
            while ((line = configReader.readLine()) != null) {
                if (line.charAt(0) == '#') continue;   //略过注释
                else {
                    String[] splitArgs = line.split(",");
                    int uid = Integer.parseInt(splitArgs[0]);
                    int port = Integer.parseInt(splitArgs[1]);
                    String status = splitArgs[2];
                    String role=splitArgs[3];
                    if(role.equals(Role.LEADER)) leaderUid=uid;
                    Node newNode = new Node(uid, port, status, role);
                    nodes.put(uid,newNode);
                }
            }
            /*
            对nodes按key排序
             */
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //TODO 异常处理
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 监听端口，接收消息
     * 作为follower时候监听elect消息和ok消息
     * 作为leader的时候监听isAlive消息并返回alive消息
     */
    private void listen() {
        boolean isListening = true;
        Socket socket = null;
        ServerSocket ss=null;
        try {
            ss= new ServerSocket(getNodeByUid(selfUid).getPort());
        }catch (IOException e){
            System.out.println("Connection failed on port " + getNodeByUid(selfUid).getPort());
            e.printStackTrace();
        }
        System.out.println("listening on port:"+getNodeByUid(selfUid).getPort());
        while (isListening) {
            try {
                //选举状态下超时，自身就成为leader
                if(getNodeByUid(selfUid).getStatus().equals(Status.ELECTING)) {
                    ss.setSoTimeout(Timeout.timeout);
                }
                else{
                    ss.setSoTimeout(0); //infinite等待时间
                }
                socket = ss.accept();
                receiveMessage(socket);
            }
            //没有接收到消息，成为leader节点
            catch (SocketTimeoutException e){
                System.out.println(getNodeByUid(selfUid).getUid()+" has become a leader!");
                System.out.println("Self status is "+getNodeByUid(selfUid).getStatus());
                System.out.println("Watcher state is "+leaderWatcher.isAlive());
                getNodeByUid(selfUid).setStatus(Status.BECOMING_LEADER);
                //等待发送Result消息完成
                sleepFew();
            }
            catch (IOException e){
                System.out.println("Failed to listen on port " + getNodeByUid(selfUid).getPort());
                e.printStackTrace();
            }
        }
    }


    /**
     * 接受消息并根据消息做不同的处理
     * @param socket    serverSocket监听返回的socket
     * @throws IOException
     */
    private void receiveMessage(Socket socket) throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        String line =reader.readLine();
        String[] messageArray =line.split(",");

        int senderUid = Integer.parseInt(messageArray[0]);    //发送方Node的id
        String message = messageArray[1];
        /*
        收到follower监视消息，发送响应
        角色：leader
         */
        if (message.equals(Message.ASK_ALIVE)) {
            if(getNodeByUid(selfUid).getStatus()!=Status.CRASH){
                //发送响应消息
                Node senderNode=nodes.get(senderUid);
                senderNode.sendMessage(getNodeByUid(selfUid).getUid()+Message.SPLIT+Message.ALIVE_REPLY);
                System.out.println("Receive ASK_ALIVE message from "+senderUid+".Answering");
            }
        }
        else if(message.equals(Message.ALIVE_REPLY)){
            System.out.println("Receive LEADER_ALIVE message from "+senderUid);
        }

        /*
        收到其他follower发起的选举消息，回复ok
        角色：follower
         */
        else if (message.equals(Message.ELECT)) {
            getNodeByUid(selfUid).setStatus(Status.ELECTING);    //进入选举状态并回复
            Node senderNode=nodes.get(senderUid);
            senderNode.sendMessage(getNodeByUid(selfUid).getUid()+Message.SPLIT+Message.OK);

        }

        /*
        得到响应，Watcher线程停止发送Elect消息，进入等待leader状态
        角色：follower
         */
        else if (message.equals(Message.OK)) {
            System.out.println("Receive OK message from "+senderUid);
            getNodeByUid(selfUid).setStatus(Status.WAIT_FOR_LEADER);
            System.out.println(getNodeByUid(selfUid).getUid()+" Status is "+getNodeByUid(selfUid).getStatus());
        }
        /*
        收到结果，可以开始监控新的LEADER
        角色：follower
        */
        else if (message.equals(Message.RESULT)) {
            leaderUid=senderUid;
            leaderWatcher.setLeaderUid(leaderUid);  //告诉watcher线程新的leader Uid
            getNodeByUid(selfUid).setStatus(Status.NORMAL);
        }
        socket.close();
        reader=null;
        writer=null;
    }
}


/**
 * @author XingRui
 *
 * @date: 18-05-28
 *
 * @description:
 *
 *  监视线程，不同角色行为不同
 *  Follower：
 *  非选举状态（存在Leader时）：向Leader发送存活查询消息
 *  选举状态时，ELECTING状态进行选举；WAIT_FOR_LEADER状态等待RESULT消息。
 *  Leader：
 *  非选举状态（本身是Leader时，NORMAL状态）：本监视线程死亡
 *  选举状态时，BECOMING_LEADER状态时，发送RESULT消息。
 *
 */
class LeaderWatcher extends Thread{

    private boolean isAlive=true;
    private int leaderUid=-1;
    private Node leaderNode=null;
    private int selfUid=-1;
    private HashMap<Integer, Node> nodes=null;

    /**
     * @param Uid 进程的Uid
     * @return Node Uid对应的进程
     */
    Node getNodeByUid(int Uid){
        for(Map.Entry<Integer,Node> entry: nodes.entrySet()){
            if(entry.getKey()==Uid){
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 线程构造
     * @param leaderUid
     * @param selfUid
     * @param nodes 所有节点信息
     */
    public LeaderWatcher(int leaderUid,int selfUid,HashMap<Integer, Node> nodes){
        this.leaderUid=leaderUid;
        this.nodes=nodes;
        this.selfUid=selfUid;
        System.out.println("Watcher is activated.");
    }

    private void sleepFew(){
        try {
            Thread.sleep(Timeout.timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /**
     * 设置leader Uid
     * @param leaderUid 新的leaderUid
     */
    public void setLeaderUid(int leaderUid) {
        this.leaderUid = leaderUid;
    }


    /**
     * 线程运行
     */
    public void run() {
        System.out.println(getNodeByUid(selfUid).getStatus());
        /*
        发送isAlive消息给leader节点
        当前状态是isElecting
         */
        while(isAlive){
            if (getNodeByUid(selfUid).getStatus().equals(Status.NORMAL)) {
                //记住，发送的时候发自己的UID。。。。
                String message = getNodeByUid(selfUid).getUid() + Message.SPLIT + Message.ASK_ALIVE;
                leaderNode=nodes.get(leaderUid);
                //leader is dead,start election
                if(!leaderNode.sendMessage(message)){
                    getNodeByUid(selfUid).setStatus(Status.ELECTING);
                    System.out.println("Leader is down.Start electing...");
                }
            }

             /*
             非监视状态，搜索Leader
             向比自己大的UID发送消息
             得到一个OK就不用再发消息，将自己设成NORMAL，等待RESULT消息
             得到一个RESULT消息转入监视模式
             */
            else if(getNodeByUid(selfUid).getStatus().equals(Status.ELECTING)){
                System.out.println(getNodeByUid(selfUid).getUid()+" Status is "+getNodeByUid(selfUid).getStatus());
                for(HashMap.Entry<Integer, Node> entry: nodes.entrySet()){
                    if(getNodeByUid(selfUid).getUid()<entry.getKey()){
                        Node candidateNode=entry.getValue();
                        String message = getNodeByUid(selfUid).getUid() + Message.SPLIT + Message.ELECT;
                        candidateNode.sendMessage(message);
                        //可能此时已经收到了OK信息，检查一下，防止自己不是潜在leader还发送elect消息的情况
                        if(getNodeByUid(selfUid).getStatus().equals(Status.WAIT_FOR_LEADER)) break;
                    }
                }
            }
            //收到OK，等待直到收到Result消息
            else if(getNodeByUid(selfUid).getStatus().equals(Status.WAIT_FOR_LEADER)){
                System.out.println(selfUid +"'s watcher is waiting for leader...");
            }
            //节点成为Leader但是还没有发送Result消息
            else if(getNodeByUid(selfUid).getStatus().equals(Status.BECOMING_LEADER)){
                for(HashMap.Entry<Integer, Node> entry: nodes.entrySet()){
                    if(entry.getKey()==selfUid) continue;
                    Node candidateNode=entry.getValue();
                    String message = getNodeByUid(selfUid).getUid() + Message.SPLIT + Message.RESULT;
                    System.out.println("sending Result message to:"+candidateNode.getUid());
                    candidateNode.sendMessage(message);
                }
                getNodeByUid(selfUid).setStatus(Status.NORMAL);
                getNodeByUid(selfUid).setRole(Role.LEADER);
                isAlive=false;
            }
        }
    }
}