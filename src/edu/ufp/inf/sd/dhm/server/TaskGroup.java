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
        pumba.add("7B88910DCAC22723AB8E2A92FBF7221654D46CE5D5F57A997199FE19D3903898547C410170DECDBD44F42E428F010E68F8271E533691EFA23C93B141BD5E13E7");
        pumba.add("D9D92A0853EB90634884302C7D35B2F1C359230C3B05EA88288BC40B702A43003876389856D8CD60EF3451E9CE878A0076B7CA4339714AE6AB10211FEC329BFD");
        pumba.add("6FF1CAE1895156AE88C306DD95AF1C82915E10503A486CE41E26B11092767304F82FBF86C406F6587C6D9417446E9FAD16DB440B91EDD7A11792B4B5A7BC0A1D");
        this.task = new Task(AvailableDigestAlgorithms.SHA_512,100,pumba,1000,this);
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
