package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Guest;

import java.rmi.RemoteException;

public interface AuthFactoryRI {
    public boolean register(Guest guest) throws RemoteException;
    public AuthSessionRI login(Guest guest) throws RemoteException;
}
