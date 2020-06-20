package edu.ufp.inf.sd.dhm.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerRI extends Remote {

    void setRun(boolean b) throws RemoteException;
}
