package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.ClientRI;
import edu.ufp.inf.sd.HashFinder.client.WorkerRI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
     * Adicionar dinheiro à conta
     * Insere BD giveMoney(())
     */
    @Override
    public void buyCoins(int amount,String token) throws RemoteException{
        //Lixo
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return;
        }
        LOGGER.info(amount + " coins added to user account");
        this.db.giveMoney(this.user,amount);
        this.server.updateBackupServers();
    }

    /**
     * Pausa a Task e envia fannout a informar
     */
    @Override
    public String pauseTask(String token) throws RemoteException {
       //Lixo
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
        TaskGroup taskGroup = this.db.getTaskGroup(this.user);
        return taskGroup.getTask().pauseAllTask();
    }

    /**
     * JWT
     * Lixo
     */
    private boolean isTokenValid(String token){
        String hashedToken = this.getHashFromPlainToken(this.token);
        return hashedToken.equals(token);
    }

    /**
     * Envia Fannout para workers iniciarem trabalho após paragem
     */
    @Override
    public String resumeTask(String token) throws RemoteException {
        //Lixo
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
        TaskGroup taskGroup = this.db.getTaskGroup(this.user);
        return taskGroup.getTask().resumeAllTask();
    }

    /**
     * Apaga TaskGroup
     */
    @Override
    public String deleteTaskGroup(String token) throws RemoteException {
        //Lixo
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
        return;
    }

    /**
     * Verifica a sessão
     */
    @Override
    public void checkIfClientOk() throws RemoteException {
    }

    /**
     * User wants to join a task group
     * @param username the user we want to join the taskgroup
     * @throws RemoteException if remote error
     */
    @Override
    public void joinTaskGroup(String username, String token) throws RemoteException {
        //Lixo
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return;
        }
        User taskOwner = this.db.getUser(username);
        if(taskOwner == null){
            LOGGER.info("Owner withou TaskGroup");
            return;
        }
        TaskGroup taskGroup = this.getTaskGroupFrom(taskOwner);
        taskGroup.addUser(this.user);
        this.db.insert(taskGroup,this.user);
        this.server.updateBackupServers();
        LOGGER.info(username + " entering TaskGroup...");
    }

    /**
     * Listar TaskGroups
     */
    @Override
    public String printTaskGroups(String token) throws RemoteException {
        //Lixo
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
        ArrayList<TaskGroup> taskGroups = this.db.getTaskGroups();
        LOGGER.info("Printing available task groups ...");
        StringBuilder builder = new StringBuilder();
        if(!taskGroups.isEmpty()){
            for(TaskGroup taskGroup : taskGroups){
                builder.append(taskGroup.toString());
                builder.append("\n");
            }
            return builder.toString();
        }
        return "No TaskGroups found";
    }

    /**
     * Retorna TaskGroups de um determinado user
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
     *Criar TaskGroup
     */
    @Override
    public String createTaskGroup(String token) throws IOException, TimeoutException {
        //Lixo
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
        TaskGroup taskGroup = new TaskGroup(this.user.getCoins(),this.user,this.db,this);
        taskGroup.addUser(this.user);
        this.db.insert(taskGroup,user);
        this.server.updateBackupServers();
        LOGGER.info("TaskGrouo created by  " + this.user.getUsername());
        return;
    }

    /**
     * @throws RemoteException if remote error
     */
    @Override
    public void logout(String token) throws RemoteException {
        //Lixo
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return;
        }
        this.db.removeSession(this.user);
        this.sendMessage("Logging out..");
        this.server.updateBackupServers();
    }

    /**
     * Adiciona worker à Task
     */
    public void addWorkerToTask(String taskOwner, WorkerRI worker,String token) throws RemoteException{
        //Lixo
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return;
        }
        User userTaskOwner = this.db.getUser(taskOwner);
        if(taskOwner == null){
            LOGGER.info("Task not found");
            return;
        }
        TaskGroup taskGroup = this.getTaskGroupFrom(userTaskOwner);
        taskGroup.getTask().addWorker(worker);
        this.user.addWorker();
        this.server.updateBackupServers();
        LOGGER.info("Worker joined Task!");
    }

    @Override
    public User getUserFromName(String username, String token) throws RemoteException {
        //Lixo
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
        return this.db.getUser(username);
    }

    @Override
    public String getCoins(String token) throws RemoteException {
        //Lixo
        if(!this.isTokenValid(token)){
            this.sendMessage("Warning. Invalid token!");
            return null;
        }
        return "You have " + this.db.getCoins(this.user) + " coins.";
    }

    public User getUser(){
        return this.user;
    }

    /**
     * Lista Taskgroups BD
     */
    private ArrayList<TaskGroup> fetchTaskGroups() {
        return null;
    }

    /**
     * Retorna o tipo de algoritmo
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
     * Converte byte para string
     */
    private String byteToString(byte[] bytes){
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", 0xFF & b));
        }
        return hexString.toString();
    }

    //Lixo
    protected String getToken(){
        return this.token;
    }


}
