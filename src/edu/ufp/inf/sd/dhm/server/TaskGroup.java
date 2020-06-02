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
        pumba.add("aOJSDHNAIYUBHSDIAJSDFSadsf");
        pumba.add("AKSDJIO:ASJDIOAJSD");

        this.task = new Task("sha1",0,pumba,null,10,this);
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
