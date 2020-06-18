package edu.ufp.inf.sd.dhm.client;

import com.rabbitmq.client.*;
import edu.ufp.inf.sd.dhm.Rabbit;
import edu.ufp.inf.sd.dhm.server.AvailableDigestAlgorithms;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class Worker extends UnicastRemoteObject implements WorkerRI{
    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());
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
    private AvailableDigestAlgorithms hashType;
    private WorkerThread workerThread;
    private Thread workingThread;
    private boolean stop = false;
    private boolean pause = false;


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
                this.workingThread = Thread.currentThread();
                //LOGGER.info("W#" + this.id + " working ...");
                //LOGGER.info("deserializatin taskstate");
                byte[] bytes = delivery.getBody();
                TaskState taskState = (TaskState) SerializationUtils.deserialize(bytes);
                this.original = taskState.getStringGroup();
                //LOGGER.info("I'm currently on thread + " +  Thread.currentThread().getName());
                //LOGGER.info("This is this thread delivery tag " + delivery.getEnvelope().getDeliveryTag());
                LOGGER.info(taskState.toString());
                this.workerThread = new WorkerThread(taskState,this);
                this.workerThread.setDeliveryTag(delivery.getEnvelope().getDeliveryTag());
                this.workerThread.run();
            };
            this.recvDirectChannel.basicConsume(this.recvDirectQueue, false, work, consumerTag -> {
                LOGGER.warning("Killing worker #" + this.id);
                this.recvGeralChannel.queueDelete(this.generalQueue);
            });
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in worker.listen()");
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Send an HashState to the task to inform about something
     * @param match if found a match
     * @param done if done with all the StringGroup
     */
    private void sendHashState(boolean match, boolean done, String word, String hash){
        int originalCeiling = this.original.getCeiling();
        int lastLine = originalCeiling + this.original.getDelta();
        int newDelta = lastLine - this.currentLine;
        StringGroup pending = new StringGroup(this.currentLine, newDelta);
        WorkerStatus workerStatus = WorkerStatus.MATCH;
        if(done) workerStatus = WorkerStatus.DONE;
        if(done && match) workerStatus = WorkerStatus.DONE_AND_MATCH;
        HashSate hashSate = new HashSate(workerStatus, this.original, pending, hash,this.id,word,this.owner.getUsername());
        this.publish(hashSate);
    }

    /**
     * Send a confirmation that the StringGroup is done!
     * @param match if has a match
     */
    public void doneWithStringGroup(boolean match, long deliveryTagThread){
        this.sendHashState(match,true,"","");
        //LOGGER.info("Sending ack and hash state w/ deliveryTag " + deliveryTagThread);
        try {
            this.recvDirectChannel.basicAck(deliveryTagThread,false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void match(String word, String hash, long deliveryTagThread){
        //LOGGER.info("Sending information that we found a match!");
        this.sendHashState(true,false,word,hash);
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
                //LOGGER.info("[RECV][W#" + this.id + "][" + this.generalQueue + "]" + " Received General STATE'");
                byte[] bytes = delivery.getBody();
                GeneralState generalState = (GeneralState) SerializationUtils.deserialize(bytes);
                //LOGGER.info(generalState.toString());
                if(generalState.isResume()){
                    //resuming working after being paused
                    LOGGER.info("Going to resume the work !!!");
                    this.pause=false;
                    //this.workingThread.notify();
                    //Thread.currentThread().interrupt();
                    return;
                }
                if(generalState.isPause()) {
                    LOGGER.info("Paused state received, going to stop work");
                    this.pause = true;
                    //this.workingThread.wait();
                    //Thread.currentThread().interrupt();
                    return;

                }
                if(generalState.getHashes() == null){
                    // No more hashes to be found = no more work to do
                    this.workingThread.interrupt();
                    this.workerThread = null;
                    LOGGER.info("Thread killed because there are no more hashes :(");
                    Thread.currentThread().interrupt();
                    this.stop = true;
                    return;
                }
                if(this.hashes.isEmpty()){
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
            LOGGER.severe("[ERROR] Exception in worker.listen()");
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Populates the words ArrayList with a given URL
     * @param wordsUrl URL that has all the words to hash and compare
     */
    private void populateWordsList(String wordsUrl) {
        URL url = null;
        LOGGER.info("Populating words list array ...");
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
        LOGGER.info("Done populating word lists!");
    }

    /**
     * Sends an HashState to the sending queue
     *
     */
    public void publish(HashSate hashSate) {
        try {
            byte[] hashStateBytes =  SerializationUtils.serialize(hashSate);
            this.sendChannel.basicPublish("", this.sendQueue, null, hashStateBytes);
            LOGGER.info("[SEND][W#" + this.id + "]" + " Sent hashing state'");
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in worker.publish()");
            LOGGER.severe(e.getMessage());
        }
    }

    public String getTaskOwner() {
        return taskOwner;
    }

    public String getGeneralQueue() throws RemoteException {
        return this.generalQueue;
    }

    /**
     * Method to print a message sent by the Task ( server )
     * @param message sent by the Task
     */
    @Override
    public void printServerMessage(String message) throws RemoteException {
        LOGGER.info("[SERVER MESSAGE] " + message);
    }

    @Override
    public String getOwnerName() throws RemoteException {
        return this.owner.getUsername();
    }


    public int getId() throws RemoteException {
        return id;
    }

    public ArrayList<String> getHashes() {
        return hashes;
    }

    public ArrayList<String> getWords() {
        return words;
    }

    public AvailableDigestAlgorithms getHashType() {
        return hashType;
    }

    public void setCurrentLine(int currentLine) {
        this.currentLine = currentLine;
    }

    public boolean isStop() {
        return stop;
    }

    public boolean isPause() {
        return pause;
    }
}