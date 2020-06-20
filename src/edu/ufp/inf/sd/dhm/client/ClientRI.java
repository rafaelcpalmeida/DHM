package edu.ufp.inf.sd.dhm.client;

import edu.ufp.inf.sd.dhm.server.ServerImpl;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRI extends Remote {
    void attachNewServer(ServerImpl server) throws RemoteException;

    void isAlive() throws RemoteException;
}
