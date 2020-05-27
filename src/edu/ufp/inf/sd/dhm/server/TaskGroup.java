package edu.ufp.inf.sd.dhm.server;

/**
 * Has the task
 */
public class TaskGroup {
    private int coinPool;
    private final User owner;
    private Task task;
    private DBMockup db;

    public TaskGroup(int coinPool, User owner, DBMockup db) {
        this.coinPool = coinPool;
        this.owner = owner;
        this.db = db;
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
}
