package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Worker;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simulates a BD , with singleton pattern
 * Only 1 instance of DBMockup exists and it's
 * static.
 */
public class DBMockup {
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
     */
    public void insert(User user) {
    }

    /**
     * @param task being added to tasks
     */
    public void insert(Task task) {

    }

    /**
     * @param worker being added to tasks
     * @param task key
     */
    public void insert(Worker worker, Task task) {
    }

    /**
     * @param sessionRI being added to sessions
     * @param user key
     */
    public void insert(AuthSessionRI sessionRI, User user) {
    }

    /**
     * @param taskGroup being added to taskgroups
     * @param user key
     */
    public void insert(TaskGroup taskGroup, User user) {

    }

    /**
     * Gives money to user
     * @param user giving the money
     * @param quantity amount of money
     */
    public void giveMoney(User user, int quantity){

    }

    //TODO delete , search

}
