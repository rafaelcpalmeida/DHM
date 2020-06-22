package edu.ufp.inf.sd.HashFinder.client;

import edu.ufp.inf.sd.HashFinder.server.Server;
import edu.ufp.inf.sd.HashFinder.server.ServerRI;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

public class ClientImpl extends UnicastRemoteObject implements ClientRI {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private ServerRI serverRI;

    public ClientImpl(ServerRI serverRI) throws RemoteException {
        this.serverRI = serverRI;
        LOGGER.info("ServerRI");
        LOGGER.info(serverRI.toString());
        this.serverRI.attach(this); //attach this client to server
    }

    @Override
    public void attachNewServer(ServerRI server) throws RemoteException {
        LOGGER.info("Attaching new server!");
        this.serverRI = server;
    }

    /**
     * Check if client  is online
     */
    @Override
    public void isAlive() {
    }

    public ServerRI getServerRI() {
        return serverRI;
    }
}