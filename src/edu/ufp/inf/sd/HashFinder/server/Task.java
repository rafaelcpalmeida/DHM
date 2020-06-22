package edu.ufp.inf.sd.HashFinder.server;

import com.rabbitmq.client.*;
import edu.ufp.inf.sd.HashFinder.Rabbit;
import edu.ufp.inf.sd.HashFinder.client.WorkerRI;
import edu.ufp.inf.sd.HashFinder.server.exceptions.TaskOwnerRunOutOfMoney;
import edu.ufp.inf.sd.HashFinder.server.states.GeneralState;
import edu.ufp.inf.sd.HashFinder.server.states.HashSate;
import edu.ufp.inf.sd.HashFinder.server.states.TaskState;
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
    private boolean paused;

    public Task(AvailableDigestAlgorithms hashType, int coins, ArrayList<String> digests, int deltaSize, TaskGroup taskGroup) throws IOException, TimeoutException {
        this.db = taskGroup.getDb();
        this.wordsFound = new HashMap<>();
        this.hashType = hashType;
        this.coins = coins;
        this.digests = digests;
        this.taskGroup = taskGroup;
        populateFreeStringGroup(deltaSize);
        User user = this.taskGroup.getOwner();
        this.createQueueNamesAndExchange(user);
        this.createChannels();
        this.declareQueuesAndExchange();
        this.populateWorkersQueue();
        this.listen();
    }

    /**
     * Envia dados para a queue
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
        this.sendQueue = user.getUsername() + "_toWorkesQueue";
        this.recvQueue = user.getUsername() + "_fromWorkersQueue";
        this.exchangeName = user.getUsername() + "_fannout";
    }

    /**
     * Liga-se oa Rabbit
     * Cria canais
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


    private void declareQueuesAndExchange() throws IOException {
        // declare queues
        this.sendQueueChannel.queueDeclare(this.sendQueue, true, false, false, null);
        this.recvChannel.queueDeclare(this.recvQueue, true, false, false, null);
        // Declare fanout in an exchange
        this.sendGeralChannel.exchangeDeclare(this.exchangeName, BuiltinExchangeType.FANOUT);
    }

    private void populateFreeStringGroup(int deltaSize) {
        this.freeStringGroups = new ArrayList<>();
        this.words = new ArrayList<>();
        URL oracle = null;
        LOGGER.info("Populating string group array...");
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
     * Adiciona worker a BD
     */
    public void addWorker(WorkerRI worker) throws RemoteException {
        LOGGER.info(" Adding worker to queue ...");
        this.db.insert(worker,this.db.getUser(worker.getOwnerName()));
        this.startWorker(worker);
    }

    private void startWorker(WorkerRI worker) throws RemoteException {
        worker.start();
        String workerQueue = worker.getGeneralQueue();
        this.publishToQueue(new GeneralState(this.digests,this.paused,this.hashType,this.url,false),workerQueue);
    }
    /**
     * Método de escuta a informação vinda dos workers
     */
    private void listen() {
        try {
            DeliverCallback listen = (consumerTag, delivery) -> {
                byte[] bytes = delivery.getBody();
                HashSate hashSate = (HashSate) SerializationUtils.deserialize(bytes);
                switch (hashSate.getStatus()){
                    case NEED_HASHES:
                        break;
                    case DONE:
                        LOGGER.info("HashState: Job done");
                        if(this.giveCoins(1,hashSate.getOwnerName()))
                            this.sendMessage(hashSate.getWorkerId(),hashSate.getOwnerName(),"You received 1 coin!");
                        break;
                    case MATCH:
                        LOGGER.info("Received a MATCH for " + hashSate.getHash() + "with word " + hashSate.getWord());
                        this.updateTaskMatches(hashSate.getWord(),hashSate.getHash());
                        if(this.giveCoins(10,hashSate.getOwnerName()))
                            this.sendMessage(hashSate.getWorkerId(),hashSate.getOwnerName(),"You received 10 coins!");
                        break;
                    case DONE_AND_MATCH:
                        break;
                    default:
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
     * Envia mensagem para worker
     */
    private void sendMessage(int workerId, String ownerName, String message) {
        try {
            WorkerRI workerRI = this.db.getWorkerStub(ownerName,workerId);
            if(workerRI != null){
                workerRI.printServerMessage(message);
                return;
            }
            LOGGER.warning("Error sending message to worker");
        } catch (RemoteException e) {
            LOGGER.severe("Error sending message to worker");
        }

    }

    /**
     * Pagamento do trabalho
     * Pausa a task caso não haja coins
     */
    private boolean giveCoins(int amount, String username) {
        User user = this.db.getUser(username);
        User taskOwner = this.taskGroup.getOwner();
        if(user != null){
            try {
                this.removeCoins(amount,taskOwner);
                this.db.giveMoney(user,amount);
            } catch (TaskOwnerRunOutOfMoney taskOwnerRunOutOfMoney) {
                LOGGER.warning("Owner run out of coins!!! Pausing task!!");
                this.taskGroup.getOwnerSession().sendMessage("Buy coins to continue Task");
                this.pauseTask();
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Pausa todas as tasks
     */
    public String pauseAllTask(){
        if(!this.paused){
            this.pauseTask();
            return "Task has been paused!";
        }
        return "Task already paused";
    }

    /**
     * Resume Tasks
     */
    public String resumeAllTask(){
        if(this.paused){
            // not paused yet
            User taskOwner = this.taskGroup.getOwner();
            if(this.taskGroup.getDb().getCoins(taskOwner) == 0){
                return "Insufficient coins do start";
            }
            this.resumeTask();
            return "Task has been resumed!";
        }
        return "Task currently running";
    }

    /**
     * Pausa uma task
     */
    private void pauseTask() {
        GeneralState generalState = new GeneralState(null,true,null,"",false);
        this.publishToAllWorkers(generalState);
        this.paused = true;
    }

    /**
     * Resume uma task
     */
    private void resumeTask(){
        GeneralState generalState = new GeneralState(null,false,null,"",true);
        this.publishToAllWorkers(generalState);
        this.paused = false;
    }


    /**
     * Tird dinheiro
     */
    private void removeCoins(int amount, User user) throws TaskOwnerRunOutOfMoney {
        this.db.takeMoney(user,amount);
    }

    /**
     * Método usado para atulizar ficheiro de Hashes quando encontradas correspondências
     */
    private void updateTaskMatches(String wordFound, String digestFound) throws IOException {
        this.digests.remove(digestFound);
        this.wordsFound.put(digestFound,wordFound);
        GeneralState generalState = new GeneralState(this.digests,false,this.hashType,this.url,false);
        if(this.digests.isEmpty()){
            this.endTask();
            return;
        }
        this.publishToAllWorkers(generalState);
    }

    /**
     * Envia informação aos workes e apaga o canal
     */
    protected void endTask() throws IOException {
        LOGGER.info("Ending task!!");
        GeneralState generalState = new GeneralState(null,false,null,"",false);
        this.publishToAllWorkers(generalState);
        this.sendQueueChannel.queueDelete(this.sendQueue);
        this.recvChannel.queueDelete(this.recvQueue);
    }

    /**
     * Envia informação aos workers
     */
    private void publishToQueue(GeneralState generalState, String workerQueue){
        try {
            byte[] generalStateBytes =  SerializationUtils.serialize(generalState);
            this.sendQueueChannel.basicPublish("", workerQueue, null, generalStateBytes);
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in task.publishToWorkersQueue()");
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Envia TaskState
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
     * Envia fannout
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
