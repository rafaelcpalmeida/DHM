package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Guest;
import edu.ufp.inf.sd.dhm.client.Worker;
import edu.ufp.inf.sd.dhm.client.WorkerRI;
import edu.ufp.inf.sd.dhm.server.exceptions.TaskOwnerRunOutOfMoney;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simulates a BD , with singleton pattern
 * Only 1 instance of DBMockup exists and it's
 * static.
 */
public class DBMockup implements Serializable {
    private static DBMockup dbMockup = null;
    private HashMap<User, AuthSessionRI> sessions;   // User -> session
    private HashMap<User, String> users;             // User -> passw
    private HashMap<User, TaskGroup> taskgroups;     // User -> taskgroup
    private HashMap<Task, ArrayList<Worker>> tasks;             // Task -> user.worker
    private HashMap<User,Integer> userCoins;
    private HashMap<User, ArrayList<WorkerRI>> userWorkers;

    /**
     * constructor , private , only getInstance()
     * method can access it.
     */
    private DBMockup() {
        sessions = new HashMap<User, AuthSessionRI>();
        users = new HashMap<>();
        taskgroups = new HashMap<User, TaskGroup>();
        tasks = new HashMap<Task, ArrayList<Worker>>();
        userWorkers = new HashMap<User,ArrayList<WorkerRI>>();
        userCoins = new HashMap<>();
    }

    /**
     * @return DBMockup instance
     */
    public synchronized static DBMockup getInstance() {
        if (dbMockup == null)
            dbMockup = new DBMockup();
        return dbMockup;
    }

    /**
     * @param user being added to users
     * @param passwd password
     */
    public synchronized void insert(User user, String passwd) {
        if(!this.users.containsKey(user)){
            this.users.put(user,passwd);
            this.userCoins.put(user,0); // adds 0 coins to user
        }
    }

    /**
     * @param task being added to tasks
     */
    public void insert(Task task) {
        if(!this.tasks.containsKey(task)) this.tasks.put(task,new ArrayList<Worker>());
    }

    /**
     * @param worker being added to tasks
     * @param task key
     */
    public void insert(Worker worker, Task task) {
        ArrayList<Worker> workers=this.tasks.get(task);
        if(!workers.contains(worker)) workers.add(worker);
    }

    /**
     * @param sessionRI being added to sessions
     * @param user key
     */
    public void insert(AuthSessionRI sessionRI, User user) {
        if(!this.sessions.containsKey(user)) this.sessions.put(user,sessionRI);
    }

    public void update(AuthSessionRI sessionRI, User user){
        if(this.sessions.containsKey(user)) this.sessions.put(user,sessionRI);
    }

    /**
     * @param taskGroup being added to taskgroups
     * @param user key
     */
    public synchronized void insert(TaskGroup taskGroup, User user) {
        if(!this.taskgroups.containsKey(user)) this.taskgroups.put(user,taskGroup);
    }


    /**
     * Gives money to user
     * @param user giving the money
     * @param quantity amount of money
     */
    public void giveMoney(User user, int quantity){
        if(this.users.containsKey(user)){
            // The user exists
            this.setUserCoins(user,this.getCoins(user) + quantity);
        }
    }

    public void setUserCoins(User user,int value){
        this.userCoins.put(user,value);
    }

    public boolean exists(Guest guest){
        for(User user : this.users.keySet()){
            if(user.getUsername().compareTo(guest.getUsername()) == 0){
                if(this.users.get(user).compareTo(guest.getPassword()) ==0) return true;
            }
        }
        return false;
    }

    public boolean existsSession(User user){
        return this.sessions.containsKey(user);
    }

    public User getUser(String name){
        for(User user : this.users.keySet()){
            if(user.getUsername().compareTo(name) == 0) return user;
        }
        return null;
    }

    public synchronized ArrayList<TaskGroup> getTaskGroups() {
        ArrayList<TaskGroup> tasks = new ArrayList<>();
        this.taskgroups.forEach((user, taskGroup) -> {
            tasks.add(taskGroup);
        });
        return tasks;
    }

    public AuthSessionRI getSession(User user){
        return this.sessions.get(user);
    }

    public void removeSession(User user){
        this.sessions.remove(user);
    }

    public void takeMoney(User user, int amount) throws TaskOwnerRunOutOfMoney {
        if(this.users.containsKey(user)){
            // The user exists
            int newBalance = this.getCoins(user) - amount;
            if (newBalance < 0) {
                // owner has no more money to spent
                throw new TaskOwnerRunOutOfMoney();
            }
            this.setUserCoins(user,newBalance);
        }
    }

    /**
     * Returns coins from a User
     */
    public int getCoins(User user){
        return this.userCoins.get(user);
    }

    /**
     * Adds a worker to the arraylist of that user's worker
     * @param workerRI worker stub
     * @param user 's worker
     */
    public void insert(WorkerRI workerRI, User user){
        if(!this.userWorkers.containsKey(user)){
            // if the user has no Workers yet
            ArrayList<WorkerRI> newArray = new ArrayList<>();
            newArray.add(workerRI);
            this.userWorkers.put(user,newArray);      // create key and new ArrayList
            return;
        }
        this.userWorkers.get(user).add(workerRI);
    }

    public WorkerRI getWorkerStub(String username, int id) throws RemoteException {
        User user = this.getUser(username);
        ArrayList<WorkerRI> workers = this.userWorkers.get(user);
        for(WorkerRI workerRI: workers){
            if(workerRI.getId() == id) return workerRI;
        }
        return null;
    }


    public TaskGroup getTaskGroup(User user){
        return this.taskgroups.get(user);
    }

    public void deleteTaskGroup(User user) {
        this.taskgroups.remove(user);
    }


    //TODO delete , search

}
