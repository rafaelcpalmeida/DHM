package edu.ufp.inf.sd.dhm.client;

import edu.ufp.inf.sd.dhm.server.ServerImpl;
import edu.ufp.inf.sd.dhm.server.ServerRI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRI extends Remote {
    void attachNewServer(ServerRI server) throws RemoteException;
    void isAlive() throws RemoteException;
    void sendToken(String token) throws RemoteException;
    void sendMessage(String msg) throws RemoteException;
}
