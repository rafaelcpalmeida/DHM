package edu.ufp.inf.sd.dhm.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import edu.ufp.inf.sd.dhm.Rabbit;
import edu.ufp.inf.sd.dhm.server.User;

public class Worker {
    private final int id;
    private int coinsEarnt;
    private final User user;
    private final String sendQueue;
    private final String recvQueue;
    private final Channel recvChannel;
    private final Channel sendChannel;

    public Channel getSendChannel() {
        return this.sendChannel;
    }

    /**
     * Create a factory, the opens 2 channels (recv and send),
     * then calls the callback to handle recv and send methods.
     *
     * @param id        of this worker
     * @param user      that this worker belongs
     * @param sendQueue that this worker is sending hashing states
     * @param recvQueue that this worker is receiving task states
     */
    public Worker(int id, User user, String sendQueue, String recvQueue) {
        this.id = id;
        this.user = user;
        this.coinsEarnt = 0;
        this.sendQueue = sendQueue;
        this.recvQueue = recvQueue + "_worker_" + this.id;
        // Get the connect factory
        Rabbit rabbit = new Rabbit();
        ConnectionFactory factory = rabbit.connect();
        // Get the channels
        this.recvChannel = rabbit.channelRecv(factory, this.recvQueue, Integer.toString(this.id));
        this.sendChannel = rabbit.channelSend(factory, this.sendQueue, Integer.toString(this.id));
        // Receiving message
        listen(this.recvChannel);
        // Sending message
        //this.publish(sendChannel,"sending msg");
    }


    /**
     * Create a callback function that listens to the task queue
     * and processes that info.
     */
    private void listen(Channel channel) {
        try {
            DeliverCallback listen = (consumerTag, delivery) -> {
                // TODO make the callback to the received message from the task queue
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("[RECV][W#" + this.id + "][" + this.recvQueue + "]" + " Received '" + message + "'");
                this.publish(sendChannel,"message received");
            };
            channel.exchangeDeclare("teste","fanout");
            channel.queueBind(this.recvQueue,"teste","");
            channel.basicConsume(this.recvQueue, true, listen, consumerTag -> {
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
