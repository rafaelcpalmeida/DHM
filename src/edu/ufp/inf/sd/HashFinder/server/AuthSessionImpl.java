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

public class AuthSessionImpl extends UnicastRemoteObject implements AuthSessionRI {
    private static final Logger LOGGER = Logger.getLogger(AuthFactoryImpl.class.getName());
    private final DBMockup db;
    private final User user;
    private final ServerImpl server;
    private final ClientRI clientRI;

    public AuthSessionImpl(DBMockup db, User user, ServerImpl server, ClientRI clientRI) throws RemoteException {
        super();
        this.db = db;
        this.user = user;
        ArrayList<TaskGroup> taskGroups = this.fetchTaskGroups();
        this.server = server;
        this.clientRI = clientRI;
    }

    protected void sendMessage(String msg) {
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
    public void buyCoins(int amount) throws RemoteException {
        LOGGER.info(amount + " coins added to user account");
        this.db.giveMoney(this.user, amount);
        this.server.updateBackupServers();
    }

    /**
     * Pausa a Task e envia fannout a informar
     */
    @Override
    public String pauseTask() throws RemoteException {
        TaskGroup taskGroup = this.db.getTaskGroup(this.user);
        return taskGroup.getTask().pauseAllTask();
    }

    /**
     * Envia Fannout para workers iniciarem trabalho após paragem
     */
    @Override
    public String resumeTask() throws RemoteException {
        TaskGroup taskGroup = this.db.getTaskGroup(this.user);
        return taskGroup.getTask().resumeAllTask();
    }

    /**
     * Apaga TaskGroup
     */
    @Override
    public String deleteTaskGroup() throws RemoteException {
        TaskGroup taskGroup = this.db.getTaskGroup(this.user);
        if (taskGroup != null) {
            try {
                taskGroup.getTask().endTask();
            } catch (IOException e) {
                LOGGER.warning(e.toString());
            }
            this.db.deleteTaskGroup(this.user);
            this.server.updateBackupServers();
            return "TaskGroup removed!";
        }
        return "foda-se";
    }

    /**
     * Verifica a sessão
     */
    @Override
    public void checkIfClientOk() throws RemoteException {
    }

    /**
     * User wants to join a task group
     *
     * @param username the user we want to join the taskgroup
     * @throws RemoteException if remote error
     */
    @Override
    public void joinTaskGroup(String username) throws RemoteException {
        User taskOwner = this.db.getUser(username);
        if (taskOwner == null) {
            LOGGER.warning("Owner without TaskGroup");
            return;
        }
        TaskGroup taskGroup = this.getTaskGroupFrom(taskOwner);
        assert taskGroup != null;
        taskGroup.addUser(this.user);
        this.db.insert(taskGroup, this.user);
        this.server.updateBackupServers();
        LOGGER.info(username + " entering TaskGroup...");
    }

    /**
     * Listar TaskGroups
     */
    @Override
    public String printTaskGroups() throws RemoteException {
        ArrayList<TaskGroup> taskGroups = this.db.getTaskGroups();
        LOGGER.info("Printing available task groups ...");
        StringBuilder builder = new StringBuilder();
        if (!taskGroups.isEmpty()) {
            for (TaskGroup taskGroup : taskGroups) {
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
    private TaskGroup getTaskGroupFrom(User user) {
        if (!this.db.getTaskGroups().isEmpty()) {
            for (TaskGroup taskGroup : this.db.getTaskGroups()) {
                if (taskGroup.getOwner().getUsername().compareTo(user.getUsername()) == 0) return taskGroup;
            }
        }
        return null;
    }

    /**
     * Criar TaskGroup
     */
    @Override
    public String createTaskGroup() throws IOException, TimeoutException {
        ArrayList<String> cyphers = new ArrayList<>();
        cyphers.add("7B88910DCAC22723AB8E2A92FBF7221654D46CE5D5F57A997199FE19D3903898547C410170DECDBD44F42E428F010E68F8271E533691EFA23C93B141BD5E13E7");
        cyphers.add("D9D92A0853EB90634884302C7D35B2F1C359230C3B05EA88288BC40B702A43003876389856D8CD60EF3451E9CE878A0076B7CA4339714AE6AB10211FEC329BFD");
        cyphers.add("6FF1CAE1895156AE88C306DD95AF1C82915E10503A486CE41E26B11092767304F82FBF86C406F6587C6D9417446E9FAD16DB440B91EDD7A11792B4B5A7BC0A1D");
        TaskGroup taskGroup = new TaskGroup(this.user.getCoins(), this.user, this.db, this, cyphers);
        taskGroup.addUser(this.user);
        this.db.insert(taskGroup, user);
        this.server.updateBackupServers();
        LOGGER.info("TaskGroup created by  " + this.user.getUsername());
        return "";
    }

    /**
     * @throws RemoteException if remote error
     */
    @Override
    public void logout() throws RemoteException {
        this.db.removeSession(this.user);
        this.sendMessage("Logging out..");
        this.server.updateBackupServers();
    }

    /**
     * Adiciona worker à Task
     */
    public void addWorkerToTask(String taskOwner, WorkerRI worker) throws RemoteException {
        User userTaskOwner = this.db.getUser(taskOwner);
        if (taskOwner == null) {
            LOGGER.info("Task not found");
            return;
        }
        TaskGroup taskGroup = this.getTaskGroupFrom(userTaskOwner);
        assert taskGroup != null;
        taskGroup.getTask().addWorker(worker);
        this.user.addWorker();
        this.server.updateBackupServers();
        LOGGER.info("Worker joined Task!");
    }

    @Override
    public User getUserFromName(String username) throws RemoteException {
        return this.db.getUser(username);
    }

    @Override
    public String getCoins() throws RemoteException {
        return "You have " + this.db.getCoins(this.user) + " coins.";
    }

    public User getUser() {
        return this.user;
    }

    /**
     * Lista Taskgroups BD
     */
    private ArrayList<TaskGroup> fetchTaskGroups() {
        return null;
    }
}
