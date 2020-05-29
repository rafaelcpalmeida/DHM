package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Guest;
import edu.ufp.inf.sd.dhm.client.Worker;

import java.io.Serializable;
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

    /**
     * constructor , private , only getInstance()
     * method can access it.
     */
    private DBMockup() {
        sessions = new HashMap<User, AuthSessionRI>();
        users = new HashMap<>();
        taskgroups = new HashMap<User, TaskGroup>();
        tasks = new HashMap<Task, ArrayList<Worker>>();
    }

    /**
     * @return DBMockup instance
     */
    public static DBMockup getInstance() {
        if (dbMockup == null)
            dbMockup = new DBMockup();
        return dbMockup;
    }


    /**
     * @param user being added to users
     * @param passwd password
     */
    public void insert(User user, String passwd) {
        if(!this.users.containsKey(user)) this.users.put(user,passwd);
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

    /**
     * @param taskGroup being added to taskgroups
     * @param user key
     */
    public void insert(TaskGroup taskGroup, User user) {
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
            user.setCoins(user.getCoins() + quantity);
        }
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

    public AuthSessionRI getSession(User user){
        return this.sessions.get(user);
    }

    public void removeSession(User user){
        this.sessions.remove(user);
    }


    //TODO delete , search

}
