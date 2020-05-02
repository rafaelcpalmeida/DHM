package edu.ufp.inf.sd.dhm.server;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import edu.ufp.inf.sd.dhm.Rabbit;
import edu.ufp.inf.sd.dhm.client.Worker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class Task {
    private final TaskGroup taskGroup;                 // TaskGroup created
    private final File passwFile;                     // File with all the passwords
    private final String hashType;                    // hash type ex. SHA1 , MD5 ...
    private int coins;                          // coins remaining
    private ArrayList<User> users;             // users registered in this task
    private final ArrayList<String> digests;        // hashes wanted to be mined
    private ArrayList<StringGroup> freeStringGroups;        // Available string groups for workers to use
    private HashMap<Worker,StringGroup> busyStringGroups;   // String groups that are already being used by workers
    private String recvQueue;       // name of the receiving channel (hash states)
    private String sendQueue;       // name of the sending channel  (task states)
    private Channel recvChannel;    // channel used to receive info
    private Channel sendChannel;    // channel used to send info

    /**
     * @param hashType ex. MD5 , SHA1 , etc ...
     * @param coins remaining
     * @param digests array with the hashes
     * @param file with all the passwords
     * @param deltaSize amount of lines a single worker need to make in each StringGroup
     */
    public Task(String hashType, int coins, ArrayList<String> digests, File file, int deltaSize,TaskGroup taskGroup) {
        // TODO change hashType to enum
        this.hashType = hashType;
        this.coins = coins;
        this.digests = digests;
        this.passwFile = file;
        this.taskGroup = taskGroup;
        // TODO break me
        // Need to populate the free string group
        populateFreeStringGroup(deltaSize);
        // Create a connection to rabbitmq
        Rabbit rabbit = new Rabbit();
        ConnectionFactory factory = rabbit.connect();
        // Create the recv and send queues
        User user = this.taskGroup.getOwner();
        this.recvQueue = user.getUsername() + "_task_recv";
        this.sendQueue = user.getUsername() + "_task_send";
        this.recvChannel = rabbit.channelRecv(factory,this.recvQueue,"task_"+this.recvQueue);
        this.sendChannel = rabbit.channelSend(factory,this.sendQueue,"task_"+this.sendQueue);
        this.listen(this.recvChannel);
    }

    /**
     * Populates the FreeStringGroup ArrayList with
     * len = (Number of lines of the file) / deltaSize
     * @param deltaSize number of lines for each StringGroup
     */
    private void populateFreeStringGroup(int deltaSize) {
    }


    /**
     * Create a callback function that listens to the task queue
     * and processes that info.
     */
    private void listen(Channel channel) {
        try{
            DeliverCallback listen = (consumerTag, delivery) -> {
                // TODO make the callback to the received message from the worker queue
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("[RECV][TASK]["+this.recvQueue+"]"+" Received '" + message + "'");
            };
            channel.basicConsume(this.recvQueue, true, listen, consumerTag -> { });
        } catch (Exception e){
            System.out.println("[ERROR] Exception in worker.listen()");
            System.out.println(e.getMessage());
        }
    }

    /**
     * Sends a message to the sending queue
     * @param channel send channel
     * @param message being sent to the send queue
     */
    public void publish(Channel channel, String message){
        try{
            //channel.exchangeDeclare("teste","fanout");
            //channel.queueBind(this.sendQueue,"teste","");
            channel.basicPublish("", this.sendQueue, null, message.getBytes("UTF-8"));
            System.out.println("[SENT] Message from task : " + message);
        } catch (Exception e){
            System.out.println("[ERROR] Exception in task.publish()");
            System.out.println(e.getMessage());
        }
    }

    public Channel getSendChannel() {
        return sendChannel;
    }

    public String getRecvQueue() {
        return recvQueue;
    }

    public String getSendQueue() {
        return sendQueue;
    }
}
