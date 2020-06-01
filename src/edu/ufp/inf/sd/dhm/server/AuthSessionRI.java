package edu.ufp.inf.sd.dhm.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface AuthSessionRI extends Remote {
    public void joinTaskGroup(String username) throws RemoteException;
    public ArrayList<TaskGroup> listTaskGroups() throws RemoteException;
    public String printTaskGroups() throws RemoteException;
    public String createTaskGroup() throws RemoteException;
    public void logout() throws RemoteException;
}
