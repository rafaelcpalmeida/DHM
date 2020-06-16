package edu.ufp.inf.sd.HashFinder.server;

import com.rabbitmq.client.*;
import edu.ufp.inf.sd.HashFinder.Rabbit;
import edu.ufp.inf.sd.HashFinder.client.WorkerRI;
import edu.ufp.inf.sd.HashFinder.server.states.GeneralState;
import edu.ufp.inf.sd.HashFinder.server.states.HashSate;
import edu.ufp.inf.sd.HashFinder.server.states.TaskState;
import org.apache.commons.lang3.SerializationUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class Task {
    private static final Logger LOGGER = Logger.getLogger(Task.class.getName());
    private final String url = "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/darkc0de.txt";
    private final AvailableDigestAlgorithms hashType;                    // hash type ex. SHA1 , MD5 ...
    private final ArrayList<String> digests;        // hashes wanted to be mined
    private ArrayList<StringGroup> wordsToHashStringGroups;        // Available string groups for workers to use
    private final HashMap<Integer, WorkerRI> workers = new HashMap<>();                // all workers from this task
    private final HashMap<String, String> wordsFound;
    private String recvQueue;       // name of the receiving channel (hash states)
    private String sendQueue;       // name of the sending channel  (task states)
    private Channel recvChannel;    // channel used to receive info from workers
    private Channel sendQueueChannel;    // channel used to send info to work queues
    private Channel sendGeralChannel; // channel used to send info to all workers
    private String exchangeName;

    /**
     * @param hashType  ex. MD5 , SHA1 , etc ...
     * @param digests   array with the hashes
     * @param deltaSize amount of lines a single worker need to make in each StringGroup
     */
    public Task(AvailableDigestAlgorithms hashType, ArrayList<String> digests, int deltaSize, TaskGroup taskGroup) throws IOException, TimeoutException {
        this.wordsFound = new HashMap<>();
        this.hashType = hashType;
        // coins remaining
        this.digests = digests;
        // TaskGroup created
        // Need to populate the free string group
        populateFreeStringGroup(deltaSize);
        User user = taskGroup.getOwner();
        // Create the recv and send queues names
        this.namingQueues(user);
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
     * Popula a Queue com frações do StringGroup
     */
    private void populateWorkersQueue() {
        LOGGER.info("A enviar StringGroups para a queue ...");
        for (StringGroup stringGroup : this.wordsToHashStringGroups) {
            TaskState taskState = new TaskState(stringGroup);
            byte[] taskStateBytes = SerializationUtils.serialize(taskState);
            this.sendHashStateToWorkersQueue(taskStateBytes);
        }
        LOGGER.info("Ficheiro de palavras finalizado!");
    }

    /**
     * Nomeia as Queues
     *
     * @param user for queue's names
     */
    private void namingQueues(User user) {
        this.sendQueue = user.getUsername() + "_task_to_workers-queue";
        this.recvQueue = user.getUsername() + "_workers_to_task_queue";
        this.exchangeName = user.getUsername() + "broadcast_exchange";
    }

    /**
     * Create the connection to rabbitmq and create channels
     *
     * @throws IOException      files
     * @throws TimeoutException timeout
     */
    private void createChannels() throws IOException, TimeoutException {
        Rabbit rabbit = new Rabbit();
        ConnectionFactory factory = rabbit.connect();
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
        this.sendQueueChannel.queueDeclare(this.sendQueue, true, false, false, null);
        this.recvChannel.queueDeclare(this.recvQueue, true, false, false, null);
        this.sendGeralChannel.exchangeDeclare(this.exchangeName, BuiltinExchangeType.FANOUT);
    }

    /**
     * Populates the FreeStringGroup ArrayList with
     * len = (Number of lines of the file) / deltaSize
     *
     * @param deltaSize number of lines for each StringGroup
     */
    private void populateFreeStringGroup(int deltaSize) {
        this.wordsToHashStringGroups = new ArrayList<>();
        ArrayList<String> wordsToFindHash = new ArrayList<>();
        URL url;
        LOGGER.info("Populating string group array ...");
        try {
            url = new URL(this.url);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            int count = 0;
            int totalLines = 0;
            while ((inputLine = in.readLine()) != null) {
                if (count == deltaSize || count == 0) {
                    this.wordsToHashStringGroups.add(new StringGroup(totalLines, deltaSize));
                    count = 0;
                }
                count++;
                wordsToFindHash.add(inputLine);
                totalLines++;
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("String group populated!");
    }

    /**
     * Pendura um Worker na queue das tarefas
     *
     * @param worker added
     */
    public void addWorker(WorkerRI worker) throws RemoteException {
        LOGGER.info("Adding worker to queue");
        this.workers.put(worker.getId(), worker);
        this.startWorker(worker);
    }

    /**
     * Inicia o worker
     *
     * @param worker who is starting the work
     */
    private void startWorker(WorkerRI worker) throws RemoteException {
        worker.start();
        String workerQueue = worker.getGeneralQueue();
        this.sendHashStateToQueue(new GeneralState(this.digests, false, this.hashType, this.url), workerQueue);
    }

    /**
     * Função de callback para receber HashStates
     * Caso seja um worker novo e precise de Hashes
     * Caso tenha um Match
     */
    private void listen() {
        try {
            DeliverCallback listen = (consumerTag, delivery) -> {
                byte[] bytes = delivery.getBody();
                HashSate hashSate = (HashSate) SerializationUtils.deserialize(bytes);
                switch (hashSate.getStatus()) {
                    case NEED_HASHES:
                        break;
                    case MATCH:
                        LOGGER.info("Foi encontrada correspondência para  Hash -> " + hashSate.getHash());
                        this.updateTaskMatches(hashSate.getWord(), hashSate.getHash());
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
     * Envia um fannout para atualizar todos os ArrayLists de Hashes sendo encontrada uma correspondência.
     *
     * @param digestFound hash found
     * @param wordFound   found that is the plain text of @digest
     */
    private void updateTaskMatches(String wordFound, String digestFound) throws IOException {
        this.digests.remove(digestFound);
        this.wordsFound.put(digestFound, wordFound);
        GeneralState generalState = new GeneralState(this.digests, false, this.hashType, this.url);
        if (this.digests.isEmpty()) {
            this.endTask();
            return;
        }
        this.sendHashStateToAllWorkers(generalState);
    }

    /**
     * Envia fannout a informar o termino
     *
     *
     *
     *
     *
     *
     * 
     * Elimina as queues
     */
    private void endTask() throws IOException {
        LOGGER.info("TODAS AS HASHES ENCONTRADAS!!!");
        GeneralState generalState = new GeneralState(null, false, null, "");
        this.sendHashStateToAllWorkers(generalState);
        this.sendQueueChannel.queueDelete(this.sendQueue);
        this.recvChannel.queueDelete(this.recvQueue);
    }

    /**
     * Send a GeneralState to a specific worker queue
     *
     *
     *
     *
     * Ver com RAFA!!!!
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     * @param generalState
     * @param workerQueue
     */
    private void sendHashStateToQueue(GeneralState generalState, String workerQueue) {
        try {
            byte[] generalStateBytes = SerializationUtils.serialize(generalState);
            this.sendQueueChannel.basicPublish("", workerQueue, null, generalStateBytes);
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in task.sendHashStateToWorkersQueue()");
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Sends a TaskState to workers queue
     *
     * @param message being sent to the workers queue
     */
    private void sendHashStateToWorkersQueue(byte[] message) {
        try {
            this.sendQueueChannel.basicPublish("", this.sendQueue, null, message);
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in task.sendHashStateToWorkersQueue()");
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Sends a GeneralState to the all the workers via fanout
     *
     * @param generalState new generalState of the task
     */
    public void sendHashStateToAllWorkers(GeneralState generalState) {
        try {
            byte[] generalStateBytes = SerializationUtils.serialize(generalState);
            this.sendGeralChannel.basicPublish(this.exchangeName, "", null, generalStateBytes);
            LOGGER.info("[SENT] Message from task to all workers!");
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in task.sendHashStateToAllWorkers()");
            LOGGER.severe(e.getMessage());
        }
    }


}
