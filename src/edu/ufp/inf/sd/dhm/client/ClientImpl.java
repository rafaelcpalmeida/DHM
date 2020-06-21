package edu.ufp.inf.sd.dhm.client;

import edu.ufp.inf.sd.dhm.server.Server;
import edu.ufp.inf.sd.dhm.server.ServerImpl;
import edu.ufp.inf.sd.dhm.server.ServerRI;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

public class ClientImpl extends UnicastRemoteObject implements ClientRI{
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private ServerRI serverRI;
    //private Client client;

    public ClientImpl( ServerRI serverRI) throws RemoteException{
        this.serverRI = serverRI;
        //this.client = client;
        this.serverRI.attach(this); //attach this client to server
    }

    @Override
    public void attachNewServer(ServerRI server) throws RemoteException {
        LOGGER.info("Attaching new server!");
        this.serverRI = server;
    }

    /**
     * Check if client  is online
     * @throws RemoteException if is not alive
     */
    @Override
    public void isAlive() throws RemoteException {
        // do nothing
    }

    public ServerRI getServerRI() {
        return serverRI;
    }
}
