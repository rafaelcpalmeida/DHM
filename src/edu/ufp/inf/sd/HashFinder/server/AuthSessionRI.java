package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.WorkerRI;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.TimeoutException;

public interface AuthSessionRI extends Remote {
    void joinTaskGroup(String username) throws RemoteException;

    String printTaskGroups() throws RemoteException;

    String createTaskGroup() throws IOException, TimeoutException;

    void logout() throws RemoteException;

    User getUser() throws RemoteException;

    void addWorkerToTask(String taskOwner, WorkerRI worker) throws RemoteException;

    User getUserFromName(String username) throws RemoteException;

    String getCoins() throws RemoteException;

    void buyCoins(int amount) throws RemoteException;

    String pauseTask() throws RemoteException;

    String resumeTask() throws RemoteException;

    String deleteTaskGroup() throws RemoteException;

    void checkIfClientOk() throws RemoteException;
}
