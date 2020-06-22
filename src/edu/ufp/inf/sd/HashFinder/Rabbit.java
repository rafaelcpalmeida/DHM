package edu.ufp.inf.sd.HashFinder;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.logging.Logger;

/**
 * Generic class to make connection to rabbitmq queues
 */
public class Rabbit {
    private static final Logger LOGGER = Logger.getLogger(Rabbit.class.getName());


    public Rabbit() {
    }

    //TODO is it needed 2 connections for diff. queues??

    /**
     * @return ConnectionFactory that is needed to the channel creation.
     */
    public ConnectionFactory connect() {
        // TODO use env variable
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rmi_rabbit_mq_server");
        factory.setUsername("User");
        factory.setPassword("User");
        return factory;
    }

    /**
     * Creates a connection to receive messages.
     * If doesn't exists , than a queue is created, if not , joins
     *
     * @param factory   to the channel creation
     * @param recvQueue queue being added
     * @param who       Task or Worker id or name
     */
    public Channel channelRecv(ConnectionFactory factory, String recvQueue, String who) {
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(recvQueue, false, false, false, null);
            LOGGER.info(who + " connected to " + recvQueue);
            return channel;
        } catch (Exception e) {
            LOGGER.info("[ERROR] Exception in rabbit.channelRecv()");
            LOGGER.info(e.getMessage());
            return null;
        }
    }

    /**
     * Creates a connection to send messages.
     * If doesn't exists , than a queue is created, if not , joins
     *
     * @param factory   to the channel creation
     * @param sendQueue send queue being added
     * @param who       Task or Worker id or name
     */
    public Channel channelSend(ConnectionFactory factory, String sendQueue, String who) {
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(sendQueue, false, false, false, null);
            LOGGER.info(who + " connected to " + sendQueue);
            return channel;
        } catch (Exception e) {
            LOGGER.severe("[ERROR] Exception in rabbit.channelSend()");
            LOGGER.severe(e.getMessage());
            return null;
        }
    }
}
