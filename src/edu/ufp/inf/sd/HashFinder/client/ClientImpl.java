package edu.ufp.inf.sd.HashFinder.client;

import edu.ufp.inf.sd.HashFinder.server.Server;
import edu.ufp.inf.sd.HashFinder.server.ServerRI;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

public class ClientImpl extends UnicastRemoteObject implements ClientRI {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private ServerRI serverRI;

    /**
     * Adiciona o cliente ao servidor
     */
    public ClientImpl(ServerRI serverRI) throws RemoteException {
        this.serverRI = serverRI;
        this.serverRI.attach(this); //attach this client to server
    }

    /**
     * Recebe servidor de backup e torna-o o principal
     */
    @Override
    public void attachNewServer(ServerRI server) throws RemoteException {
        LOGGER.info("Attaching new server!");
        this.serverRI = server;
    }

    /**
     * Verifica se cliente está ok
     */
    @Override
    public void checkIfClientOk() throws RemoteException {
    }

    /**
     * Recebe mensagens do AuthSessionImpl
     */
    @Override
    public void sendMessage(String msg) throws RemoteException {
        LOGGER.info("[SERVER MESSAGE] " + msg);
    }

    /**
     * Devolve interface do server
     */
    public ServerRI getServerRI() {
        return serverRI;
    }
}
