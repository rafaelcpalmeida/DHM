package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Worker;
import edu.ufp.inf.sd.dhm.client.WorkerRI;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public interface AuthSessionRI extends Remote {
    public void joinTaskGroup(String username,String token) throws RemoteException;
    public String printTaskGroups(String token) throws RemoteException;
    public String createTaskGroup(String token) throws IOException, TimeoutException;
    public void logout(String token) throws RemoteException;
    public User getUser() throws RemoteException;
    public void addWorkerToTask(String taskOwner, WorkerRI worker,String token) throws RemoteException;
    public User getUserFromName(String username, String token) throws RemoteException;
    public String getCoins(String token) throws RemoteException;
    public void buyCoins(int amount,String token) throws RemoteException;
    public String pauseTask(String token) throws RemoteException;
    public String resumeTask(String token) throws RemoteException;
    public String deleteTaskGroup(String token) throws RemoteException;
    public void isAlive() throws RemoteException;
}
