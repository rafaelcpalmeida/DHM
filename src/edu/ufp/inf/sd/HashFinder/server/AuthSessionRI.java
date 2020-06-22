package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.WorkerRI;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public interface AuthSessionRI extends Remote {
    public void joinTaskGroup(String username) throws RemoteException;

    public ArrayList<TaskGroup> listTaskGroups() throws RemoteException;

    public String printTaskGroups() throws RemoteException;

    public String createTaskGroup() throws IOException, TimeoutException;

    public void logout() throws RemoteException;

    public User getUser() throws RemoteException;

    public void addWorkerToTask(String taskOwner, WorkerRI worker) throws RemoteException;

    public User getUserFromName(String username) throws RemoteException;

    public String getCoins() throws RemoteException;

    public void buyCoins(int amount) throws RemoteException;

    public String pauseTask() throws RemoteException;

    public String resumeTask() throws RemoteException;

    public String deleteTaskGroup() throws RemoteException;

    public void isAlive() throws RemoteException;
}
