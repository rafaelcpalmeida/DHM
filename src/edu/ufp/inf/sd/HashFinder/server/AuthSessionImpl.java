package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.WorkerRI;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class AuthSessionImpl extends UnicastRemoteObject implements AuthSessionRI{
    private static final Logger LOGGER = Logger.getLogger(AuthFactoryImpl.class.getName());
    private final DBMockup db;
    private final User user;
    private ArrayList<TaskGroup> taskGroups;
    private final ServerImpl server;

    public AuthSessionImpl(DBMockup db, User user, ServerImpl server) throws RemoteException{
        super();
        this.db = db;
        this.user = user;
        this.server = server;
    }

    /**
     * The user buys coins , calls the method giveMoney() in @DBMockup
     * @param amount of coins being purchased
     */
    public void buyCoins(int amount){
        LOGGER.info("User " + this.user.getUsername() + " bought " + amount + " coins!");
        this.db.giveMoney(this.user,amount);
        this.server.updateBackupServers();
    }

    @Override
    public String pauseTask() throws RemoteException {
        TaskGroup taskGroup = this.db.getTaskGroup(this.user);
        return taskGroup.getTask().pauseAllTask();
    }

    @Override
    public String resumeTask() throws RemoteException {
        TaskGroup taskGroup = this.db.getTaskGroup(this.user);
        return taskGroup.getTask().resumeAllTask();
    }

    @Override
    public String deleteTaskGroup() throws RemoteException {
        TaskGroup taskGroup = this.db.getTaskGroup(this.user);
        if(taskGroup != null){
            try {
                taskGroup.getTask().endTask();
            } catch (IOException e) {
                LOGGER.warning(e.toString());
            }
            this.db.deleteTaskGroup(this.user);
            this.server.updateBackupServers();
            return "TaskGroup removed!";
        }
        return "TaskGroup not found!";
    }

    /**
     * @throws RemoteException throwned if sessions is not available ( maybe beacause
     * the server who had it creashed)
     */
    @Override
    public void isAlive() throws RemoteException {
        //do nothing
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
            LOGGER.info("Owner not found ...");
            return;
        }
        TaskGroup taskGroup = this.getTaskGroupFrom(taskOwner);
        taskGroup.addUser(this.user);
        this.db.insert(taskGroup,this.user);
        this.server.updateBackupServers();
        LOGGER.info(username + " added to task group!");
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
        LOGGER.info("Printing available task groups ...");
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
    public String createTaskGroup() throws IOException, TimeoutException {
        TaskGroup taskGroup = new TaskGroup(this.user.getCoins(),this.user,this.db);
        taskGroup.addUser(this.user);
        this.db.insert(taskGroup,user);     // inserting in db
        this.server.updateBackupServers();
        LOGGER.info("task group added for owner " + this.user.getUsername());
        return "Task Group created!";
    }

    /**
     * @throws RemoteException if remote error
     */
    @Override
    public void logout() throws RemoteException {
        this.db.removeSession(this.user);
        this.server.updateBackupServers();
        //System.exit(1);
    }

    /**
     * Adds a worker to a task
     * @param taskOwner who has the task
     * @param worker added to task
     */
    public void addWorkerToTask(String taskOwner, WorkerRI worker) throws RemoteException{
        User userTaskOwner = this.db.getUser(taskOwner);
        if(taskOwner == null){
            LOGGER.info("Owner not found ...");
            return;
        }
        LOGGER.info("adding worker ...");
        TaskGroup taskGroup = this.getTaskGroupFrom(userTaskOwner);
        taskGroup.getTask().addWorker(worker);
        this.user.addWorker();
        this.server.updateBackupServers();
        LOGGER.info("Worker added to task!");
    }

    @Override
    public User getUserFromName(String username) throws RemoteException {
        return this.db.getUser(username);
    }

    @Override
    public String getCoins() throws RemoteException {
        return "You currently have " + this.db.getCoins(this.user) + " coins.";
    }

    public User getUser(){
        return this.user;
    }

}