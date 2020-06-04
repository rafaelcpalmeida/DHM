package edu.ufp.inf.sd.dhm.server;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Has the task
 */
public class TaskGroup {
    private int coinPool;
    private final User owner;
    private Task task;
    private DBMockup db;
    private ArrayList<User> users;

    public TaskGroup(int coinPool, User owner, DBMockup db) throws IOException, TimeoutException {
        this.coinPool = coinPool;
        this.owner = owner;
        this.db = db;
        this.users = new ArrayList<>();
        ArrayList<String> pumba = new ArrayList<>();
        pumba.add("ca244d081350810113cfafa278ffd581");
        pumba.add("0af02a064941987e351881689616f2cc");
        pumba.add("d29e09dce7d175b2ed0a6de9082d2518");
        this.task = new Task(AvailableDigestAlgorithms.MD2,100,pumba,5,this);
    }

    /**
     * @param user being added to user's ArrayList
     */
    public void addUser(User user){
        this.users.add(user);
    }
    /**
     * Creates a task
     * @return Task
     */
    public Task createTask() {
        return null;
    }

    /**
     * Removes the task
     */
    public void removeTask() {

    }

    /**
     * Pauses the task , no more workers can mine passwords
     */
    public void pauseTask() {
        // TODO really needed??
    }

    public int getCoinPool() {
        return coinPool;
    }

    public User getOwner() {
        return owner;
    }

    public Task getTask() {
        return task;
    }

    public DBMockup getDb() {
        return db;
    }

    @Override
    public String toString() {
        return "TaskGroup{" +
                "owner= " + this.owner.getUsername() + "\n" +
                "amout of users= " + this.users.size() + "\n" +
                "coin pool= " + this.coinPool + "\n" +
                '}';
    }
}
