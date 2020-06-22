package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.ClientRI;
import edu.ufp.inf.sd.HashFinder.client.Guest;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthFactoryRI extends Remote {
    public boolean register(Guest guest) throws RemoteException;
    public AuthSessionRI login(Guest guest, ClientRI clientRI) throws RemoteException;

}
