package edu.ufp.inf.sd.dhm.server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

public class AuthSessionImpl implements AuthSessionRI{
    private DBMockup db;
    private User user;
    private ArrayList<TaskGroup> taskGroups;

    public AuthSessionImpl(DBMockup db, User user) {
        this.db = db;
        this.user = user;
        taskGroups = this.fetchTaskGroups();
    }

    /**
     * The user buys coins , calls the method giveMoney() in @DBMockup
     * @param amount of coins being purchased
     */
    public void buyCoins(int amount){

    }
    /**
     * User wants to join a task group
     * @param taskGroup the user wants to join
     * @throws RemoteException if remote error
     */
    @Override
    public void joinTaskGroup(TaskGroup taskGroup) throws RemoteException {

    }

    /**
     * @return ArrayList<TaskGroup>
     * @throws RemoteException if remote error
     */
    @Override
    public ArrayList<TaskGroup> listTaskGroups() throws RemoteException {
        return null;
    }

    /**
     * @param user who wants to create the task group
     * @return TaskGroup created
     * @throws RemoteException if remote error
     */
    @Override
    public TaskGroup createTaskGroup(User user) throws RemoteException {
        return null;
    }

    /**
     * @param user User logging out
     * @throws RemoteException if remote error
     */
    @Override
    public void logout(User user) throws RemoteException {

    }



    /**
     * Returns all the taskgroup of the user from the bd
     * Goes to db.taskgroups to fetch the info.
     * @return ArrayList<TaskGroup> of this user
     */
    private ArrayList<TaskGroup> fetchTaskGroups() {
        return null;
    }
}
