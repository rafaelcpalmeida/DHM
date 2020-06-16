package edu.ufp.inf.sd.HashFinder.client;

import com.rabbitmq.client.*;
import edu.ufp.inf.sd.HashFinder.Rabbit;
import edu.ufp.inf.sd.HashFinder.server.AvailableDigestAlgorithms;
import edu.ufp.inf.sd.HashFinder.server.StringGroup;
import edu.ufp.inf.sd.HashFinder.server.User;
import edu.ufp.inf.sd.HashFinder.server.states.GeneralState;
import edu.ufp.inf.sd.HashFinder.server.states.HashSate;
import edu.ufp.inf.sd.HashFinder.server.states.TaskState;
import edu.ufp.inf.sd.HashFinder.server.states.WorkerStatus;
import org.apache.commons.lang3.SerializationUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class Worker extends UnicastRemoteObject implements WorkerRI {
    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());
    private final int id;
    private String recvDirectQueue;
    private String sendQueue;
    private String exchangeName;
    private Channel sendChannel;
    private Channel recvGeralChannel;
    private Channel recvDirectChannel;
    private String generalQueue;
    private StringGroup original;
    private int currentLine = 0;
    private ArrayList<String> hashes = new ArrayList<>();
    private final ArrayList<String> words = new ArrayList<>();
    private AvailableDigestAlgorithms hashType;
    private WorkerThread workerThread;
    private Thread workingThread;
    private boolean stop = false;


    /**
     * Connection Factory
     * Ao criar o Worker criamos 2 channels (recv e send)
     * Callback método com handler para recv and send queues
     *
     * @param id   do Worker
     * @param user Utilizador a que pertence o Worker
     */
    public Worker(int id, User user, String taskOwner) throws IOException, TimeoutException {
        this.id = id;
        this.namingQueues(taskOwner);
        this.createChannels();
        this.declareQueuesAndExchange();

    }

    public void start() {
        this.fannoutQueue();
    }

    /**
     * Adiciona o nome do propritário as queues send, recv e o exchange do fannout
     *
     * @param taskOwner nome do user para nomear as queues
     */
    private void namingQueues(String taskOwner) {
        this.recvDirectQueue = taskOwner + "_task_workers_queue";
        this.sendQueue = taskOwner + "_task_recv_queue";
        this.exchangeName = taskOwner + "_exchange";
    }

    /**
     * Ligação ao RabbitMQ
     * Criação dos canais
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
        this.sendChannel = connection.createChannel();
        this.recvGeralChannel = connection.createChannel();
        this.recvDirectChannel = connection.createChannel();
    }

    /**
     * Declare all the queues for the task and the exchange
     *
     * @throws IOException opening files
     */
    private void declareQueuesAndExchange() throws IOException {
        // declare queues
        this.sendChannel.queueDeclare(this.sendQueue, true, false, false, null);
        this.generalQueue = this.recvGeralChannel.queueDeclare().getQueue();
        recvGeralChannel.exchangeDeclare(this.exchangeName, BuiltinExchangeType.FANOUT);
        recvGeralChannel.queueBind(this.generalQueue, this.exchangeName, "");
        this.recvDirectChannel.queueDeclare(this.recvDirectQueue, true, false, false, null);
        this.recvDirectChannel.basicQos(1);
    }

    /**
     * Função callback de escuta à queue que recebe as Tasks
     */
    private void listenToDirect() {
        try {
            DeliverCallback work = (consumerTag, delivery) -> {
                this.workingThread = Thread.currentThread();
                byte[] bytes = delivery.getBody();
                TaskState taskState = (TaskState) SerializationUtils.deserialize(bytes);
                this.original = taskState.getStringGroup();
                LOGGER.info(taskState.toString());
                this.workerThread = new WorkerThread(taskState, this);
                this.workerThread.setDeliveryTag(delivery.getEnvelope().getDeliveryTag());
                this.workerThread.run();
            };
            this.recvDirectChannel.basicConsume(this.recvDirectQueue, false, work, consumerTag -> {
                LOGGER.warning("Killing thread #" + this.id);
                this.recvGeralChannel.queueDelete(this.generalQueue);
            });
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in worker.listen()");
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Envio de informações (HashState) para a Task
     * Utiliza ENUM
     *
     * @param match se encontrou hash
     * @param done  se terminou o trabalho (chegou ao fim do string group)
     */
    private void sendHashState(boolean match, boolean done, String word, String hash) {
        int originalCeiling = this.original.getCeiling();
        int lastLine = originalCeiling + this.original.getDelta();
        int newDelta = lastLine - this.currentLine;
        StringGroup pending = new StringGroup(this.currentLine, newDelta);
        WorkerStatus workerStatus = WorkerStatus.MATCH;
        if (done) workerStatus = WorkerStatus.DONE;
        if (done && match) workerStatus = WorkerStatus.DONE_AND_MATCH;
        HashSate hashSate = new HashSate(workerStatus, this.original, pending, hash, this.id, word);
        this.sendHashState(hashSate);
    }

    /**
     * Envia para a Task informação que o StringGroup chegou ao final!
     *
     * @param match if has a match
     */
    public void doneWithStringGroup(boolean match, long deliveryTagThread) {
        this.sendHashState(match, true, "", "");
        LOGGER.info("A enviar ACK e HashState com deliveryTag " + deliveryTagThread);
        try {
            this.recvDirectChannel.basicAck(deliveryTagThread, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Envia para a Task informação que foi encontrada uma correspondência!
     *
     * @param word
     */
    public void match(String word, String hash) {
        LOGGER.info("Encontrada correspondência!");
        this.sendHashState(true, false, word, hash);
    }

    /**
     * Função Callback que fica à escuta da Exchange Fannout e atualiza os Workers
     */
    private void fannoutQueue() {
        try {
            DeliverCallback listen = (consumerTag, delivery) -> {
                LOGGER.info("[RECV][W#" + this.id + "][" + this.generalQueue + "]" + " Received General STATE'");
                byte[] bytes = delivery.getBody();
                GeneralState generalState = (GeneralState) SerializationUtils.deserialize(bytes);
                LOGGER.info(generalState.toString());
                if (generalState.getHashes() == null) {
                    // No more hashes to be found equals no more work to be done
                    this.workingThread.interrupt();
                    this.workerThread = null;
                    LOGGER.info("Thread killed, trabalho completo!!!");
                    Thread.currentThread().interrupt();
                    this.stop = true;
                    return;
                }
                if (this.hashes.isEmpty()) {
                    // Se o worker não tem Hashes, é porque é novo na Task
                    // Recebe via fannout a lista de Hashes atualizada
                    this.hashType = generalState.getHashType();
                    this.populateWordsList(generalState.getWordsUrl());
                    this.hashes = generalState.getHashes();
                    // Recebe via Direct o seu StringGroup
                    this.listenToDirect();
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
     * Preenchimento do ArrayList via URL
     *
     * @param wordsUrl URL
     */
    private void populateWordsList(String wordsUrl) {
        URL url;
        LOGGER.info("A descarregar palavras ...");
        try {
            url = new URL(wordsUrl);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                this.words.add(inputLine);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("Palavras prontas!");
    }

    /**
     * Envio de HashState para TasK
     */
    public void sendHashState(HashSate hashSate) {
        try {
            byte[] hashStateBytes = SerializationUtils.serialize(hashSate);
            this.sendChannel.basicPublish("", this.sendQueue, null, hashStateBytes);
            LOGGER.info("[SEND][W#" + this.id + "]" + " HashState enviado'");
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in worker.publish()");
            LOGGER.severe(e.getMessage());
        }
    }

    public String getGeneralQueue() {
        return this.generalQueue;
    }

    public int getId() {
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
}
