package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.User;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simulates a BD , with singleton pattern
 * Only 1 instance of DBMockup exists and it's
 * static.
 */
public class DBMockup {
    private static DBMockup dbMockup = null;
    private final HashMap<edu.ufp.inf.sd.HashFinder.server.User, AuthSessionRI> sessions;   // User -> session
    private final HashMap<edu.ufp.inf.sd.HashFinder.server.User, String> users;             // User -> passw
    private final HashMap<edu.ufp.inf.sd.HashFinder.server.User, TaskGroup> taskgroups;     // User -> taskgroup

    /**
     * constructor , private , only getInstance()
     * method can access it.
     */
    private DBMockup() {
        sessions = new HashMap<>();
        users = new HashMap<>();
        taskgroups = new HashMap<>();
        // Task -> user.worker
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
    public synchronized void insert(edu.ufp.inf.sd.HashFinder.server.User user, String passwd) {
        if (!this.users.containsKey(user)) this.users.put(user, passwd);
    }

    /**
     * @param sessionRI being added to sessions
     * @param user      key
     */
    public void insert(AuthSessionRI sessionRI, edu.ufp.inf.sd.HashFinder.server.User user) {
        if (!this.sessions.containsKey(user)) this.sessions.put(user, sessionRI);
    }

    /**
     * @param taskGroup being added to taskgroups
     * @param user      key
     */
    public synchronized void insert(TaskGroup taskGroup, edu.ufp.inf.sd.HashFinder.server.User user) {
        if (!this.taskgroups.containsKey(user)) this.taskgroups.put(user, taskGroup);
    }


    public boolean exists(User guest) {
        for (edu.ufp.inf.sd.HashFinder.server.User user : this.users.keySet()) {
            if (user.getUsername().compareTo(guest.getUsername()) == 0) {
                if (this.users.get(user).compareTo(guest.getPassword()) == 0) return true;
            }
        }
        return false;
    }

    public boolean existsSession(edu.ufp.inf.sd.HashFinder.server.User user) {
        return this.sessions.containsKey(user);
    }

    public edu.ufp.inf.sd.HashFinder.server.User getUser(String name) {
        for (edu.ufp.inf.sd.HashFinder.server.User user : this.users.keySet()) {
            if (user.getUsername().compareTo(name) == 0) return user;
        }
        return null;
    }

    public synchronized ArrayList<TaskGroup> getTaskGroups() {
        ArrayList<TaskGroup> tasks = new ArrayList<>();
        this.taskgroups.forEach((user, taskGroup) -> tasks.add(taskGroup));
        return tasks;
    }

    public AuthSessionRI getSession(edu.ufp.inf.sd.HashFinder.server.User user) {
        return this.sessions.get(user);
    }

    public void removeSession(edu.ufp.inf.sd.HashFinder.server.User user) {
        this.sessions.remove(user);
    }

}
