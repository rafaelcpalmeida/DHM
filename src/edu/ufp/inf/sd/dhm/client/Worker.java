package edu.ufp.inf.sd.dhm.client;

import com.rabbitmq.client.*;
import edu.ufp.inf.sd.dhm.Rabbit;
import edu.ufp.inf.sd.dhm.server.User;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Worker {
    private final int id;
    private int coinsEarnt;
    private User user;
    private String recvDirectQueue;
    private String sendQueue;
    private String exchangeName;
    private final Channel channel;
    private Channel sendChannel;
    private Channel recvGeralChannel;
    private Channel recvDirectChannel;
    private String generalQueue;


    /**
     * Create a factory, the opens 2 channels (recv and send),
     * then calls the callback to handle recv and send methods.
     *
     * @param id        of this worker
     * @param user      that this worker belongs
     */
    public Worker(int id, User user) throws IOException, TimeoutException {
        this.id = id;
        this.coinsEarnt = 0;
        Rabbit rabbit = new Rabbit();
        ConnectionFactory factory = rabbit.connect();
        Connection connection=factory.newConnection();
        this.channel = connection.createChannel();
        String queueName=channel.queueDeclare().getQueue();
        this.createQueueNamesAndExchange(user);
        this.createChannels();
        this.declareQueuesAndExchange();
        this.listen(this.recvDirectChannel,this.recvDirectQueue);
        this.listen(this.recvGeralChannel,this.generalQueue);
    }

    /**
     * Creates the name of the send , recv and exchange names
     * @param user for queue's names
     */
    private void createQueueNamesAndExchange(User user){
        this.recvDirectQueue = user.getUsername() + "_task_workers_queue";
        this.sendQueue = user.getUsername() + "_task_recv_queue";
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
        this.sendChannel.queueDeclare(this.sendQueue,false,false,false,null);
        this.generalQueue = this.recvGeralChannel.queueDeclare().getQueue();
        recvGeralChannel.exchangeDeclare(this.exchangeName,BuiltinExchangeType.FANOUT);
        recvGeralChannel.queueBind(this.generalQueue, this.exchangeName, "");
        this.recvDirectChannel.queueDeclare(this.recvDirectQueue,false,false,false,null);
    }

    /**
     * Create a callback function that listens to the task queue
     * and processes that info.
     */
    private void listen(Channel channel, String queue) {
        try {
            DeliverCallback listen = (consumerTag, delivery) -> {
                // TODO make the callback to the received message from the task queue
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("[RECV][W#" + this.id + "][" + queue + "]" + " Received '" + message + "'");
                this.publish(this.sendChannel,"message received");
            };
            channel.basicConsume(queue, true, listen, consumerTag -> {
            });
        } catch (Exception e) {
            System.out.println("[ERROR] Exception in worker.listen()");
            System.out.println(e.getMessage());
        }
    }

    /**
     * Sends a message to the sending queue
     *
     * @param channel send channel
     * @param message being sent to the send queue
     */
    public void publish(Channel channel, String message) {
        try {
            channel.basicPublish("", this.sendQueue, null, message.getBytes("UTF-8"));
            System.out.println("[SEND][W#" + this.id + "]" + " Sent '" + message + "'");
        } catch (Exception e) {
            System.out.println("[ERROR] Exception in worker.publish()");
            System.out.println(e.getMessage());
        }
    }
}
