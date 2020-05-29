package edu.ufp.inf.sd.dhm.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface AuthSessionRI extends Remote {
    public void joinTaskGroup(TaskGroup taskGroup) throws RemoteException;
    public ArrayList<TaskGroup> listTaskGroups() throws RemoteException;
    public TaskGroup createTaskGroup(User user) throws RemoteException;
    public void logout() throws RemoteException;
}
