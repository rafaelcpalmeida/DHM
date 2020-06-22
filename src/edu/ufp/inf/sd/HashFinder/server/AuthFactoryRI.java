package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.User;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthFactoryRI extends Remote {
    boolean register(User guest) throws RemoteException;
    AuthSessionRI login(User guest) throws RemoteException;
}
