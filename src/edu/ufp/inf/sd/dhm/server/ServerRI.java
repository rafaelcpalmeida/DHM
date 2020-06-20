package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Client;
import edu.ufp.inf.sd.dhm.client.ClientRI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

public interface ServerRI extends Remote {

    void setRun(boolean b) throws RemoteException;

    void isAlive() throws RemoteException;

    void removeBackupServer(int id) throws RemoteException;

    int attachBackupServer(ServerRI backupServerRI) throws RemoteException;

    void setServerRI(ServerRI serverRI) throws RemoteException;

    void copyInfo(HashMap<ClientRI, String> clientRIS) throws RemoteException;

    void copyBackupServers(ArrayList<ServerRI> backupServersRIS) throws RemoteException;

    void changeId(int indexOf) throws RemoteException;

    void detach(ClientRI clientRI) throws RemoteException;

    void attach(ClientRI clientRI) throws RemoteException;
}
