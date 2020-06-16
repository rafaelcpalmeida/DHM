package edu.ufp.inf.sd.HashFinder.client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WorkerRI extends Remote {
    void start() throws RemoteException;

    int getId() throws RemoteException;

    String getGeneralQueue() throws RemoteException;
}
