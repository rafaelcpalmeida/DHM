package edu.ufp.inf.sd.dhm.client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WorkerRI extends Remote {
    public void start() throws RemoteException;
    public int getId() throws RemoteException;
    public String getGeneralQueue() throws RemoteException;
}
