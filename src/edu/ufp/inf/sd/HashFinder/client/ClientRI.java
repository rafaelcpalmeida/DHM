package edu.ufp.inf.sd.HashFinder.client;

import edu.ufp.inf.sd.HashFinder.server.ServerRI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRI extends Remote {
    void attachNewServer(ServerRI server) throws RemoteException;

    void checkIfClientOk() throws RemoteException;

    void sendMessage(String msg) throws RemoteException;
}
