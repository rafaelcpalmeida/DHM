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
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class Worker extends UnicastRemoteObject implements WorkerRI {
    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());
    private final int id;
    private final User owner;
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
    private boolean pause = false;
    private final Object lock = new Object();


    /**
     * Cria canais de comunicação com a Task
     */
    public Worker(int id, User user, String taskOwner) throws IOException, TimeoutException {
        this.owner = user;
        this.id = id;
        this.createQueueNamesAndExchange(taskOwner);
        this.createChannels();
        this.declareQueuesAndExchange();
    }

    public void start() throws RemoteException {
        this.listenToGeneral();
    }

    /**
     * Declaração das Queues
     */
    private void createQueueNamesAndExchange(String taskOwner){
        this.recvDirectQueue = taskOwner + "_toWorkersQueue";
        this.sendQueue = taskOwner + "_fromWorkersQueue";
        this.exchangeName = taskOwner + "_fannout";
    }

    /**
     * Criação de canais
     * Ligação ao Rabbit
     */
    private void createChannels() throws IOException, TimeoutException {
        // Ligação ao Rabbit
        Rabbit rabbit = new Rabbit();
        ConnectionFactory factory = rabbit.connect();
        // Criação de canais
        Connection connection = factory.newConnection();
        this.sendChannel = connection.createChannel();
        this.recvGeralChannel = connection.createChannel();
        this.recvDirectChannel = connection.createChannel();
    }

    /**
     * Declaração de Queues e Exchange
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
     * Função para receber informação da Task (Callback)
     */
    private void listenToDirect() {
        try {
            DeliverCallback work = (consumerTag, delivery) -> {
                this.workingThread = Thread.currentThread();
                byte[] bytes = delivery.getBody();
                TaskState taskState = (TaskState) SerializationUtils.deserialize(bytes);
                this.original = taskState.getStringGroup();
                this.workerThread = new WorkerThread(taskState, this);
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
     * Envio de informações para a Task
     * Informa estado atual da tarefa
     */
    private void sendHashState(boolean match, boolean done, String word, String hash) {
        int originalCeiling = this.original.getCeiling();
        int lastLine = originalCeiling + this.original.getDelta();
        int newDelta = lastLine - this.currentLine;
        StringGroup pending = new StringGroup(this.currentLine, newDelta);
        WorkerStatus workerStatus = WorkerStatus.MATCH;
        if (done) workerStatus = WorkerStatus.DONE;
        if (done && match) workerStatus = WorkerStatus.DONE_AND_MATCH;
        HashSate hashSate = new HashSate(workerStatus, this.original, pending, hash, this.id, word, this.owner.getUsername());
        this.publish(hashSate);
    }

    /**
     * Informação de conclusão de tarefa
     */
    public void workerFinished(boolean match, long deliveryTagThread) {
        this.sendHashState(match, true, "", "");
        //LOGGER.info("Sending ack and hash state w/ deliveryTag " + deliveryTagThread);
        try {
            this.recvDirectChannel.basicAck(deliveryTagThread, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void match(String word, String hash, long deliveryTagThread) {
        //LOGGER.info("Sending information that we found a match!");
        this.sendHashState(true, false, word, hash);
    }


    /**
     * Método para receber informações da Exchange (Fannout)
     */
    private void listenToGeneral() {
        try {
            DeliverCallback listen = (consumerTag, delivery) -> {
                byte[] bytes = delivery.getBody();
                GeneralState generalState = (GeneralState) SerializationUtils.deserialize(bytes);
                //Lixo
                //Resume
                if (generalState.isResume()) {
                    LOGGER.info("Going to resume the work !!!");
                    this.pause = false;
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                    return;
                }
                //Pause
                if (generalState.isPause()) {
                    LOGGER.info("Paused state received, going to stop work");
                    this.pause = true;
                    return;
                }
                //Trabalho Concluido
                if (generalState.getHashes() == null) {
                    this.workingThread.interrupt();
                    this.workerThread = null;
                    LOGGER.info("Killing Thread, job done!");
                    Thread.currentThread().interrupt();
                    this.stop = true;
                    return;
                }
                //Quando o worker é novo
                //Recebe dados para trabalhar
                if (this.hashes.isEmpty()) {
                    this.hashType = generalState.getHashType();
                    this.populateWordsList(generalState.getWordsUrl());
                    this.hashes = generalState.getHashes();
                    this.listenToDirect();
                    return;
                }
                this.hashes = generalState.getHashes();
            };
            this.recvGeralChannel.basicConsume(this.generalQueue, true, listen, consumerTag -> {
            });

        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in worker.listenToGeneral()");
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Vai buscar ao URL as palavras e grava num array
     *
     * @param wordsUrl URL that has all the words to hash and compare
     */
    private void populateWordsList(String wordsUrl) {
        URL url = null;
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
        LOGGER.info("StringGroup ready");
    }

    /**
     * Envia HashState pela Queue de retorno à Task
     */
    public void publish(HashSate hashSate) {
        try {
            byte[] hashStateBytes = SerializationUtils.serialize(hashSate);
            this.sendChannel.basicPublish("", this.sendQueue, null, hashStateBytes);
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in worker.publish()");
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Devolve Queue do Fannout
     */
    public String getGeneralQueue() throws RemoteException {
        return this.generalQueue;
    }

    /**
     * Imprime mensagens do servidor
     */
    @Override
    public void printServerMessage(String message) throws RemoteException {
        LOGGER.info("[SERVER MESSAGE] " + message);
    }

    /**
     * Dono do worker
     */
    @Override
    public String getOwnerName() throws RemoteException {
        return this.owner.getUsername();
    }

    /**
     * ID do worker
     */
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

    public Object getLock() {
        return lock;
    }
}
