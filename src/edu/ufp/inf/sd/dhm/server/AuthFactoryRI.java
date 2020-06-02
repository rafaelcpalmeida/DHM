package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Guest;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthFactoryRI extends Remote {
    public boolean register(Guest guest) throws RemoteException;
    public AuthSessionRI login(Guest guest) throws RemoteException;

}
