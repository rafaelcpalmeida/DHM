package edu.ufp.inf.sd.dhm.server;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

public class AuthSessionImpl extends UnicastRemoteObject implements AuthSessionRI{
    private DBMockup db;
    private User user;
    private ArrayList<TaskGroup> taskGroups;

    public AuthSessionImpl(DBMockup db, User user) throws RemoteException{
        super();
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
     * @param username the user we want to join the taskgroup
     * @throws RemoteException if remote error
     */
    @Override
    public void joinTaskGroup(String username) throws RemoteException {
        User taskOwner = this.db.getUser(username);
        if(taskOwner == null){
            System.out.println("Owner not found ...");
            return;
        }
        TaskGroup taskGroup = this.getTaskGroupFrom(taskOwner);
        taskGroup.addUser(this.user);
        this.db.insert(taskGroup,this.user);
    }

    /**
     * @return ArrayList<TaskGroup>
     * @throws RemoteException if remote error
     */
    @Override
    public ArrayList<TaskGroup> listTaskGroups() throws RemoteException {
        return this.db.getTaskGroups();
    }

    /**
     * Prints all the task groups
     */
    @Override
    public String printTaskGroups() throws RemoteException {
        ArrayList<TaskGroup> taskGroups = this.listTaskGroups();
        System.out.println("Printing available task groups ...");
        StringBuilder builder = new StringBuilder();
        if(!taskGroups.isEmpty()){
            for(TaskGroup taskGroup : taskGroups){
                builder.append(taskGroup.toString());
                builder.append("\n");
            }
            return builder.toString();
        }
        return "Cannot print taskGroups because there aren't any.";
    }

    /**
     * @param user who owns taskGroups
     * @return TaskGroup owned by user or null if none own
     */
    private TaskGroup getTaskGroupFrom(User user){
        if(!this.db.getTaskGroups().isEmpty()){
            for(TaskGroup taskGroup : this.db.getTaskGroups()){
                if(taskGroup.getOwner().getUsername().compareTo(user.getUsername()) == 0) return taskGroup;
            }
        }
        return null;
    }

    /**
     * @return TaskGroup created
     * @throws RemoteException if remote error
     */
    @Override
    public String createTaskGroup() throws RemoteException {
        TaskGroup taskGroup = new TaskGroup(this.user.getCoins(),this.user,this.db);
        taskGroup.addUser(this.user);
        this.db.insert(taskGroup,user);     // inserting in db
        System.out.println("task group added for owner " + this.user.getUsername());
        return "Task Group created!";
    }

    /**
     * @throws RemoteException if remote error
     */
    @Override
    public void logout() throws RemoteException {
        this.db.removeSession(this.user);
        //System.exit(1);
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
