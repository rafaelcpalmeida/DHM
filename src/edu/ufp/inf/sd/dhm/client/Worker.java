package edu.ufp.inf.sd.dhm.client;

import com.rabbitmq.client.*;
import edu.ufp.inf.sd.dhm.Rabbit;
import edu.ufp.inf.sd.dhm.server.StringGroup;
import edu.ufp.inf.sd.dhm.server.User;
import edu.ufp.inf.sd.dhm.server.states.GeneralState;
import edu.ufp.inf.sd.dhm.server.states.HashSate;
import edu.ufp.inf.sd.dhm.server.states.TaskState;
import edu.ufp.inf.sd.dhm.server.states.WorkerStatus;
import org.apache.commons.lang3.SerializationUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public class Worker extends UnicastRemoteObject implements WorkerRI{
    private final int id;
    private int coinsEarnt;
    private User owner;
    private String taskOwner;
    private String recvDirectQueue;
    private String sendQueue;
    private String exchangeName;
    private Channel sendChannel;
    private Channel recvGeralChannel;
    private Channel recvDirectChannel;
    private String generalQueue;
    private StringGroup original;
    private int currentLine  = 0;
    private ArrayList<String> hashes = new ArrayList<>();
    private ArrayList<String> words = new ArrayList<>();
    private long deliveryTag;
    private String hashType;
    private WorkerThread workerThread;
    private Thread thread;


    /**
     * Create a factory, the opens 2 channels (recv and send),
     * then calls the callback to handle recv and send methods.
     *
     * @param id        of this worker
     * @param user      that this worker belongs
     */
    public Worker(int id, User user, String taskOwner) throws IOException, TimeoutException {
        this.taskOwner = taskOwner;
        this.owner = user;
        this.id = id;
        this.coinsEarnt = 0;
        this.createQueueNamesAndExchange(taskOwner);
        this.createChannels();
        this.declareQueuesAndExchange();
    }

    public void start() throws RemoteException{
        this.listenToGeneral();
    }
    /**
     * Creates the name of the send , recv and exchange names
     * @param taskOwner for queue's names
     */
    private void createQueueNamesAndExchange(String taskOwner){
        this.recvDirectQueue = taskOwner + "_task_workers_queue";
        this.sendQueue = taskOwner + "_task_recv_queue";
        this.exchangeName = taskOwner + "_exchange";
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
        this.sendChannel = connection.createChannel();
        this.recvGeralChannel = connection.createChannel();
        this.recvDirectChannel = connection.createChannel();
    }
    /**
     * Declare all the queues for the task and the exchange
     * @throws IOException opening files
     */
    private void declareQueuesAndExchange() throws IOException {
        // declare queues
        this.sendChannel.queueDeclare(this.sendQueue,true,false,false,null);
        this.generalQueue = this.recvGeralChannel.queueDeclare().getQueue();
        recvGeralChannel.exchangeDeclare(this.exchangeName,BuiltinExchangeType.FANOUT);
        recvGeralChannel.queueBind(this.generalQueue, this.exchangeName, "");
        this.recvDirectChannel.queueDeclare(this.recvDirectQueue,true,false,false,null);
        this.recvDirectChannel.basicQos(1);
    }

    /**
     * Create a callback function that listens to the task queue
     * and processes that info.
     */
    private void listenToDirect() {
        try {
            DeliverCallback work = (consumerTag, delivery) -> {
                // TODO make the callback to the received message from the task queue
                this.deliveryTag = delivery.getEnvelope().getDeliveryTag();
                System.out.println("W#" + this.id + " working ...");
                System.out.println("deserializatin taskstate");
                byte[] bytes = delivery.getBody();
                TaskState taskState = (TaskState) SerializationUtils.deserialize(bytes);
                System.out.println(taskState.toString());
                if(this.workerThread == null) this.workerThread = new WorkerThread(taskState);
                this.workerThread.setTaskState(taskState);
                if(this.thread != null){
                    thread.interrupt();
                }
                this.thread = new Thread(this.workerThread);
                this.thread.start();
            };
            this.recvDirectChannel.basicConsume(this.recvDirectQueue, false, work, consumerTag -> {
                System.out.println("canceling");
            });
        } catch (Exception e) {
            System.out.println("[ERROR] Exception in worker.listen()");
            System.out.println(e.getMessage());
        }
    }

    /**
     * The working from the worker
     * Mines all the passwords
     * @param taskState to mine
     */
    private void work(TaskState taskState){
        // TODO work method
        System.out.println("im working bitch ...");
    }

    /**
     * Send an HashState to the task to inform about something
     * @param match if found a match
     * @param done if done with all the StringGroup
     */
    private void sendHashState(boolean match, boolean done){
        int originalCeiling = this.original.getCeiling();
        int lastLine = originalCeiling + this.original.getDelta();
        int newDelta = lastLine - this.currentLine;
        StringGroup pending = new StringGroup(this.currentLine, newDelta);
        ArrayList<String> matches = new ArrayList<>();
        if(match){
            // TODO if has match add to the matches array
        }
        WorkerStatus workerStatus = WorkerStatus.MATCH;
        if(done) workerStatus = WorkerStatus.DONE;
        if(done && match) workerStatus = WorkerStatus.DONE_AND_MATCH;
        HashSate hashSate = new HashSate(workerStatus, this.original, pending, matches,this.id);
        this.publish(hashSate);
    }

    /**
     * Send a confirmation that the StringGroup is done!
     * @param match if has a match
     */
    private void doneWithStringGroup(boolean match){
        System.out.println("Sending ack and hash state ...");
        this.sendHashState(match,true);
        try {
            this.recvDirectChannel.basicAck(this.deliveryTag,false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When the worker is new and has now Hashes to compare
     */
    private void needHashes(){
        HashSate hashSate = new HashSate(WorkerStatus.NEED_HASHES,this.id);
        this.publish(hashSate);
    }

    /**
     * Create a callback function that listens to the general fanout
     * and processes that info.
     */
    private void listenToGeneral() {
        try {
            DeliverCallback listen = (consumerTag, delivery) -> {
                System.out.println("[RECV][W#" + this.id + "][" + this.generalQueue + "]" + " Received General STATE'");
                byte[] bytes = delivery.getBody();
                GeneralState generalState = (GeneralState) SerializationUtils.deserialize(bytes);
                if(generalState.isPause()) {
                    // TODO STOP WORKING!!!!
                }
                if(hashes.isEmpty()){
                    // If the hashes arraylist is empty , then the worker just got join
                    // Starts listen to the direct queue
                    this.hashType = generalState.getHashType();
                    this.populateWordsList(generalState.getWordsUrl());     // populates the words ArrayList
                    this.hashes = generalState.getHashes();
                    this.listenToDirect();                  // when it updates the hash list , then the worker can work
                    return;
                }
                this.hashes = generalState.getHashes();
            };
            this.recvGeralChannel.basicConsume(this.generalQueue, true, listen, consumerTag -> {
            });

        } catch (Exception e) {
            System.out.println("[ERROR] Exception in worker.listen()");
            System.out.println(e.getMessage());
        }
    }

    /**
     * Populates the words ArrayList with a given URL
     * @param wordsUrl URL that has all the words to hash and compare
     */
    private void populateWordsList(String wordsUrl) {
        URL url = null;
        System.out.println("Populating words list array ...");
        try {
            url = new URL(wordsUrl);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                this.words.add(inputLine);
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Done populating word lists!");
    }

    /**
     * Sends an HashState to the sending queue
     *
     */
    public void publish(HashSate hashSate) {
        try {
            byte[] hashStateBytes =  SerializationUtils.serialize(hashSate);
            this.sendChannel.basicPublish("", this.sendQueue, null, hashStateBytes);
            System.out.println("[SEND][W#" + this.id + "]" + " Sent hashing state'");
        } catch (Exception e) {
            System.out.println("[ERROR] Exception in worker.publish()");
            System.out.println(e.getMessage());
        }
    }

    public String getTaskOwner() {
        return taskOwner;
    }

    public String getGeneralQueue() throws RemoteException {
        return this.generalQueue;
    }

    public int getId() throws RemoteException {
        return id;
    }


}
