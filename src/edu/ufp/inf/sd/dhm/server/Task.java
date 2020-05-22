package edu.ufp.inf.sd.dhm.server;

import com.rabbitmq.client.*;
import edu.ufp.inf.sd.dhm.Rabbit;
import edu.ufp.inf.sd.dhm.client.Worker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

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
    private Channel recvChannel;    // channel used to receive info from workers
    private Channel sendQueueChannel;    // channel used to send info to work queues
    private Channel sendGeralChannel; // channel used to send info to all workers
    private String exchangeName;

    /**
     * @param hashType ex. MD5 , SHA1 , etc ...
     * @param coins remaining
     * @param digests array with the hashes
     * @param file with all the passwords
     * @param deltaSize amount of lines a single worker need to make in each StringGroup
     */
    public Task(String hashType, int coins, ArrayList<String> digests, File file, int deltaSize,TaskGroup taskGroup) throws IOException, TimeoutException {
        // TODO change hashType to enum
        this.hashType = hashType;
        this.coins = coins;
        this.digests = digests;
        this.passwFile = file;
        this.taskGroup = taskGroup;
        // TODO break me
        // Need to populate the free string group
        populateFreeStringGroup(deltaSize);
        User user = this.taskGroup.getOwner();
        // Create the recv and send queues names
        this.createQueueNamesAndExchange(user);
        // Creates all the channels to use
        this.createChannels();
        // Declare queues and exchange names
        this.declareQueuesAndExchange();
        // Listen to workers from workers queue
        this.listen();
    }

    /**
     * Creates the name of the send , recv and exchange names
     * @param user for queue's names
     */
    private void createQueueNamesAndExchange(User user){
        this.sendQueue = user.getUsername() + "_task_workers_queue";
        this.recvQueue = user.getUsername() + "_task_recv_queue";
        this.exchangeName = user.getUsername() + "_exchange";
    }

    /**
     * Create the connection to rabbitmq and create channels
     * @throws IOException files
     * @throws TimeoutException timeout
     */
    private void createChannels() throws IOException, TimeoutException {
        // Create a connection to rabbitmq
        Rabbit rabbit = new Rabbit();
        ConnectionFactory factory = rabbit.connect();
        // Create the connections
        Connection connection=factory.newConnection();
        this.sendQueueChannel = connection.createChannel();
        this.recvChannel = connection.createChannel();
        this.sendGeralChannel = connection.createChannel();
    }
    /**
     * Declare all the queues for the task and the exchange
     * @throws IOException opening files
     */
    private void declareQueuesAndExchange() throws IOException {
        // declare queues
        this.sendQueueChannel.queueDeclare(this.sendQueue,false,false,false,null);
        this.recvChannel.queueDeclare(this.recvQueue,false,false,false,null);
        // Declare fanout in an exchange
        this.sendGeralChannel.exchangeDeclare(this.exchangeName,BuiltinExchangeType.FANOUT);
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
    private void listen() {
        try{
            DeliverCallback listen = (consumerTag, delivery) -> {
                // TODO make the callback to the received message from the worker queue
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("[RECV][TASK]"+" Received '" + message + "'");
            };
            this.recvChannel.basicConsume(this.recvQueue, true, listen, consumerTag -> { });
        } catch (Exception e){
            System.out.println("[ERROR] Exception in worker.listen()");
            System.out.println(e.getMessage());
        }
    }

    /**
     * Sends a message to the workers
     * @param message being sent to the workers queue
     */
    public void publishToWorkersQueue(String message){
        try{
            this.sendQueueChannel.basicPublish("", this.sendQueue, null, message.getBytes("UTF-8"));
            System.out.println("[SENT] Message from task to workers queue: " + message);
        } catch (Exception e){
            System.out.println("[ERROR] Exception in task.publishToWorkersQueue()");
            System.out.println(e.getMessage());
        }
    }

    /**
     * Sends a message to the all the workers via fanout
     * @param message being sent
     */
    public void publishToAllWorkers(String message){
        try{
            this.sendGeralChannel.basicPublish(this.exchangeName, "", null, message.getBytes("UTF-8"));
            System.out.println("[SENT] Message from task to all workers: " + message);
        } catch (Exception e){
            System.out.println("[ERROR] Exception in task.publishToAllWorkers()");
            System.out.println(e.getMessage());
        }
    }



    public Channel getSendQueueChannel() {
        return sendQueueChannel;
    }

    public String getRecvQueue() {
        return recvQueue;
    }

    public String getSendQueue() {
        return sendQueue;
    }
}
