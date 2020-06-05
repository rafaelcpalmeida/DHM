package edu.ufp.inf.sd.dhm.server;

import com.rabbitmq.client.*;
import edu.ufp.inf.sd.dhm.Rabbit;
import edu.ufp.inf.sd.dhm.client.Worker;
import edu.ufp.inf.sd.dhm.client.WorkerRI;
import edu.ufp.inf.sd.dhm.server.states.GeneralState;
import edu.ufp.inf.sd.dhm.server.states.HashSate;
import edu.ufp.inf.sd.dhm.server.states.TaskState;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.net.*;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class Task {
    private static final Logger LOGGER = Logger.getLogger(Task.class.getName());
    private String url = "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/darkc0de.txt";
    private final TaskGroup taskGroup;                 // TaskGroup created
    private final AvailableDigestAlgorithms hashType;                    // hash type ex. SHA1 , MD5 ...
    private int coins;                          // coins remaining
    private ArrayList<String> words;                // has all the words to mine
    private ArrayList<String> digests = new ArrayList<>();        // hashes wanted to be mined
    private ArrayList<StringGroup> freeStringGroups;        // Available string groups for workers to use
    private HashMap<Integer,WorkerRI> workers = new HashMap<>();                // all workers from this task
    private HashMap<String,String> wordsFound;
    private String recvQueue;       // name of the receiving channel (hash states)
    private String sendQueue;       // name of the sending channel  (task states)
    private Channel recvChannel;    // channel used to receive info from workers
    private Channel sendQueueChannel;    // channel used to send info to work queues
    private Channel sendGeralChannel; // channel used to send info to all workers
    private String exchangeName;
    private DBMockup db;

    /**
     * @param hashType  ex. MD5 , SHA1 , etc ...
     * @param coins     remaining
     * @param digests   array with the hashes
     * @param deltaSize amount of lines a single worker need to make in each StringGroup
     */
    public Task(AvailableDigestAlgorithms hashType, int coins, ArrayList<String> digests, int deltaSize, TaskGroup taskGroup) throws IOException, TimeoutException {
        // TODO change hashType to enum
        this.db = taskGroup.getDb();
        this.wordsFound = new HashMap<>();
        this.hashType = hashType;
        this.coins = coins;
        this.digests = digests;
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
        // Populate the workers queues with TaskSates
        this.populateWorkersQueue();
        // Listen to workers from workers queue
        this.listen();
    }

    /**
     * Populates the worker's queue with @TaskState instances
     */
    private void populateWorkersQueue() {
        LOGGER.info("Populating workers queue ...");
        for (StringGroup stringGroup : this.freeStringGroups) {
            TaskState taskState = new TaskState(stringGroup);
            byte[] taskStateBytes =  SerializationUtils.serialize(taskState);
            this.publishToWorkersQueue(taskStateBytes);
        }
        LOGGER.info("Finished populate workers queue !");
    }

    /**
     * Creates the name of the send , recv and exchange names
     *
     * @param user for queue's names
     */
    private void createQueueNamesAndExchange(User user) {
        this.sendQueue = user.getUsername() + "_task_workers_queue";
        this.recvQueue = user.getUsername() + "_task_recv_queue";
        this.exchangeName = user.getUsername() + "_exchange";
    }

    /**
     * Create the connection to rabbitmq and create channels
     *
     * @throws IOException      files
     * @throws TimeoutException timeout
     */
    private void createChannels() throws IOException, TimeoutException {
        // Create a connection to rabbitmq
        Rabbit rabbit = new Rabbit();
        ConnectionFactory factory = rabbit.connect();
        // Create the connections
        Connection connection = factory.newConnection();
        this.sendQueueChannel = connection.createChannel();
        this.recvChannel = connection.createChannel();
        this.sendGeralChannel = connection.createChannel();
    }

    /**
     * Declare all the queues for the task and the exchange
     *
     * @throws IOException opening files
     */
    private void declareQueuesAndExchange() throws IOException {
        // declare queues
        this.sendQueueChannel.queueDeclare(this.sendQueue, true, false, false, null);
        this.recvChannel.queueDeclare(this.recvQueue, true, false, false, null);
        // Declare fanout in an exchange
        this.sendGeralChannel.exchangeDeclare(this.exchangeName, BuiltinExchangeType.FANOUT);
    }

    /**
     * Populates the FreeStringGroup ArrayList with
     * len = (Number of lines of the file) / deltaSize
     *
     * @param deltaSize number of lines for each StringGroup
     */
    private void populateFreeStringGroup(int deltaSize) {
        this.freeStringGroups = new ArrayList<>();
        this.words = new ArrayList<>();
        URL oracle = null;
        LOGGER.info("Populating string group array ...");
        try {
            oracle = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(oracle.openStream()));
            String inputLine;
            int count = 0;
            int totalLines = 0;
            while ((inputLine = in.readLine()) != null) {
                if(count == deltaSize || count == 0){
                    this.freeStringGroups.add(new StringGroup(totalLines,deltaSize));
                    count = 0;
                }
                count++;
                //LOGGER.info(inputLine);
                this.words.add(inputLine);
                totalLines++;
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("Done populating free string group!");
    }

    public ArrayList<String> getWords() {
        return words;
    }

    /**
     * Adds a worker to workers HashMap and
     * @param worker added
     */
    public void addWorker(WorkerRI worker) throws RemoteException {
        LOGGER.info(" Adding worker to queue ...");
        this.workers.put(worker.getId(),worker);
        this.startWorker(worker);
    }

    /**
     *
     * @param worker who is starting the work
     */
    private void startWorker(WorkerRI worker) throws RemoteException {
        worker.start();
        String workerQueue = worker.getGeneralQueue();
        this.publishToQueue(new GeneralState(this.digests,false,this.hashType,this.url),workerQueue);
    }
    /**
     * Create a callback function that listens to the task queue
     * and processes that info.
     */
    private void listen() {
        try {
            DeliverCallback listen = (consumerTag, delivery) -> {
                // TODO make the callback to the received message from the worker queue
                //String message = new String(delivery.getBody(), "UTF-8");
                //LOGGER.info("[RECV][TASK]" + " Received message from worker");
                byte[] bytes = delivery.getBody();
                HashSate hashSate = (HashSate) SerializationUtils.deserialize(bytes);
                //LOGGER.info(hashSate.toString());
                switch (hashSate.getStatus()){
                    case NEED_HASHES:
                        break;
                    case DONE:
                        //TODO DONE!
                        //LOGGER.info("received a dont w/ string group  ");
                        break;
                    case MATCH:
                        LOGGER.info("received a match for " + hashSate.getHash() + "w/ word " + hashSate.getWord());
                        this.updateTaskMatches(hashSate.getWord(),hashSate.getHash());
                        break;
                    case DONE_AND_MATCH:
                        // TODO DONE_AND_MATCH
                        break;
                    default:
                        // TODO default
                        break;
                }
            };
            this.recvChannel.basicConsume(this.recvQueue, true, listen, consumerTag -> {
            });
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in worker.listen()");
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Updates all the arraylists when a new match is received
     * and notifies all workers
     * @param digestFound hash found
     * @param wordFound found that is the plain text of @digest
     */
    private void updateTaskMatches(String wordFound, String digestFound) throws IOException {
        this.digests.remove(digestFound);
        this.wordsFound.put(digestFound,wordFound);
        GeneralState generalState = new GeneralState(this.digests,false,this.hashType,this.url);
        if(this.digests.isEmpty()){
            // All hashes found!
            this.endTask();
            return;
        }
        //LOGGER.info(generalState.toString());
        this.publishToAllWorkers(generalState);
    }

    /**
     * Send to all workers a GeneralState and deletes the channel
     */
    private void endTask() throws IOException {
        LOGGER.info("All matches found , stop!");
        GeneralState generalState = new GeneralState(null,false,null,"");
        this.publishToAllWorkers(generalState);
        this.sendQueueChannel.queueDelete(this.sendQueue);
        this.recvChannel.queueDelete(this.recvQueue);
    }

    /**
     * Send a GeneralState to a specific worker queue
     * @param generalState
     * @param workerQueue
     */
    private void publishToQueue(GeneralState generalState, String workerQueue){
        try {
            byte[] generalStateBytes =  SerializationUtils.serialize(generalState);
            this.sendQueueChannel.basicPublish("", workerQueue, null, generalStateBytes);
            //LOGGER.info("[SENT] New TaskState to workers queues ");
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in task.publishToWorkersQueue()");
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Sends a TaskState to workers queue
     *
     * @param message being sent to the workers queue
     */
    private void publishToWorkersQueue(byte[] message) {
        try {
            this.sendQueueChannel.basicPublish("", this.sendQueue, null, message);
            //LOGGER.info("[SENT] New TaskState to workers queues ");
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in task.publishToWorkersQueue()");
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Sends a GeneralState to the all the workers via fanout
     *
     * @param generalState new generalState of the task
     */
    public void publishToAllWorkers(GeneralState generalState) {
        try {
            byte[] generalStateBytes =  SerializationUtils.serialize(generalState);
            this.sendGeralChannel.basicPublish(this.exchangeName, "", null, generalStateBytes);
            LOGGER.info("[SENT] Message from task to all workers!");
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in task.publishToAllWorkers()");
            LOGGER.severe(e.getMessage());
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
