package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.ClientRI;
import edu.ufp.inf.sd.dhm.client.Worker;
import edu.ufp.inf.sd.dhm.client.WorkerRI;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class AuthSessionImpl extends UnicastRemoteObject implements AuthSessionRI{
    private static final Logger LOGGER = Logger.getLogger(AuthFactoryImpl.class.getName());
    private DBMockup db;
    private User user;
    private ArrayList<TaskGroup> taskGroups;
    private ServerImpl server;
    private String token;
    private ClientRI clientRI;

    public AuthSessionImpl(DBMockup db, User user, ServerImpl server, String plainToken, ClientRI clientRI) throws RemoteException{
        super();
        this.db = db;
        this.user = user;
        taskGroups = this.fetchTaskGroups();
        this.server = server;
        this.token = plainToken;
        this.clientRI = clientRI;
    }

    protected void sendMessage(String msg){
        try {
            this.clientRI.sendMessage(msg);
        } catch (RemoteException e) {
            LOGGER.severe(e.toString());
        }
    }

    /**
     * The user buys coins , calls the method giveMoney() in @DBMockup
     * @param amount of coins being purchased
     */
    @Override
    public void buyCoins(int amount,String token) throws RemoteException{
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return;
        }
        LOGGER.info("User " + this.user.getUsername() + " bought " + amount + " coins!");
        this.db.giveMoney(this.user,amount);
        this.server.updateBackupServers();
    }

    /**
     * Pauses task by sending a GeneralState to all workers
     */
    @Override
    public String pauseTask(String token) throws RemoteException {
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
        TaskGroup taskGroup = this.db.getTaskGroup(this.user);
        if(taskGroup == null){
            // no task group created yet for this user
            LOGGER.warning("User hasnt created a task group yet");
            return "You didn't create a task group yet , so u cant pause it ... ";
        }
        return taskGroup.getTask().pauseAllTask();
    }

    /**
     * Compares the token sent by the client
     * @param token digest sent by client
     * @return true if hashes are equal , false if not
     */
    private boolean isTokenValid(String token){
        String hashedToken = this.getHashFromPlainToken(this.token);
        return hashedToken.equals(token);
    }

    /**
     * Resumes task by sending a GeneralState to all workers
     *
     */
    @Override
    public String resumeTask(String token) throws RemoteException {
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
        TaskGroup taskGroup = this.db.getTaskGroup(this.user);
        if(taskGroup == null){
            // no task group created yet for this user
            LOGGER.warning("User hasnt created a task group yet");
            return "You didn't create a task group yet , so u cant resume it ... ";
        }
        return taskGroup.getTask().resumeAllTask();
    }

    @Override
    public String deleteTaskGroup(String token) throws RemoteException {
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
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
    public void joinTaskGroup(String username, String token) throws RemoteException {
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return;
        }
        User taskOwner = this.db.getUser(username);
        if(taskOwner == null){
            LOGGER.info("Owner not found ...");
            this.sendMessage("Owner not found...");
            return;
        }
        TaskGroup taskGroup = this.getTaskGroupFrom(taskOwner);
        taskGroup.addUser(this.user);
        //this.db.insert(taskGroup,this.user);
        this.server.updateBackupServers();
        LOGGER.info(username + " added to task group!");
    }

    /**
     * @return ArrayList<TaskGroup>
     * @throws RemoteException if remote error
     */
    private ArrayList<TaskGroup> listTaskGroups() throws RemoteException {
        return this.db.getTaskGroups();
    }

    /**
     * Prints all the task groups
     */
    @Override
    public String printTaskGroups(String token) throws RemoteException {
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
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
    public String createTaskGroup(String token,AvailableDigestAlgorithms algorithm , int deltaSize, ArrayList<String> digests) throws IOException, TimeoutException {
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
        TaskGroup taskGroup = new TaskGroup(this.user.getCoins(),this.user,this.db,this,digests,deltaSize,algorithm);
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
    public void logout(String token) throws RemoteException {
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return;
        }
        this.db.removeSession(this.user);
        this.sendMessage("Logging out.. see u soon");
        this.server.updateBackupServers();
    }

    /**
     * Adds a worker to a task
     * @param taskOwner who has the task
     * @param worker added to task
     */
    public void addWorkerToTask(String taskOwner, WorkerRI worker,String token) throws RemoteException{
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return;
        }
        User userTaskOwner = this.db.getUser(taskOwner);
        if(userTaskOwner == null){
            LOGGER.warning("Owner not found ...");
            this.sendMessage("Owner not found ...");
            return;
        }
        TaskGroup ownerTaskGroup = this.db.getTaskGroup(userTaskOwner);
        if(ownerTaskGroup == null){
            LOGGER.warning("Task group not found!");
            this.sendMessage("Task group not found!");
            return;
        }
        if(!ownerTaskGroup.hasUser(this.user)){
            LOGGER.warning("You didnt joined this taskgroup yet , pls join to start working!");
            this.sendMessage("You didnt joined this taskgroup yet , pls join to start working!");
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
    public User getUserFromName(String username, String token) throws RemoteException {
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
        return this.db.getUser(username);
    }

    @Override
    public String getCoins(String token) throws RemoteException {
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
        return "You currently have " + this.db.getCoins(this.user) + " coins.";
    }



    public User getUser(){
        return this.user;
    }

    /**
     * Returns all the taskgroup of the user from the bd
     * Goes to db.taskgroups to fetch the info.
     * @return ArrayList<TaskGroup> of this user
     */
    private ArrayList<TaskGroup> fetchTaskGroups() {
        return null;
    }

    /**
     * Returns the digest from the plain token
     * @param plainToken token in plain text
     * @return Hash in MD5 of the plain text
     */
    private String getHashFromPlainToken(String plainToken){
        MessageDigest algorithm = null;
        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Could not get instance of digest algorithm");
        }
        byte[] hashByte = algorithm.digest(plainToken.getBytes(StandardCharsets.UTF_8));
        return this.byteToString(hashByte);
    }


    /**
     * @param bytes w/ the digest
     * @return bytes in string
     */
    private String byteToString(byte[] bytes){
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", 0xFF & b));
        }
        return hexString.toString();
    }

    protected String getToken(){
        return this.token;
    }

    protected void updateServers(){
        this.server.updateBackupServers();
    }

}
