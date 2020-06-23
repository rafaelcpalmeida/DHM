package edu.ufp.inf.sd.HashFinder.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Has the task
 */
public class TaskGroup {
    private final int coinPool;
    private final User owner;
    private final Task task;
    private final DBMockup db;
    private final ArrayList<User> users;
    private final AuthSessionImpl ownerSession;

    public TaskGroup(int coinPool, User owner, DBMockup db, AuthSessionImpl session, ArrayList<String> cyphers) throws IOException, TimeoutException {
        this.coinPool = coinPool;
        this.owner = owner;
        this.db = db;
        this.users = new ArrayList<>();
        this.ownerSession = session;
        this.task = new Task(AvailableDigestAlgorithms.SHA_512, 100, cyphers, 1000, this);
    }

    /**
     * @param user being added to user's ArrayList
     */
    public void addUser(User user) {
        this.users.add(user);
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

    public AuthSessionImpl getOwnerSession() {
        return ownerSession;
    }

}
