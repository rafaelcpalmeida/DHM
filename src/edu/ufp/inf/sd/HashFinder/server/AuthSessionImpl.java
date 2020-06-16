package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.WorkerRI;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class AuthSessionImpl extends UnicastRemoteObject implements AuthSessionRI {
    private static final Logger LOGGER = Logger.getLogger(AuthFactoryImpl.class.getName());
    private final DBMockup db;
    private final User user;

    public AuthSessionImpl(DBMockup db, User user) throws RemoteException {
        super();
        this.db = db;
        this.user = user;
    }

    /**
     * Entrar num TaskGroup
     *
     * @param username Utilizador a juntar
     */
    @Override
    public void joinTaskGroup(String username) {
        User taskOwner = this.db.getUser(username);
        if (taskOwner == null) {
            LOGGER.info("TaskGroup não criada ...");
            return;
        }
        TaskGroup taskGroup = this.searchTaskGroup(taskOwner);
        assert taskGroup != null;
        taskGroup.addUser(this.user);
        this.db.insert(taskGroup, this.user);
        LOGGER.info(username + " adicionado com sucesso!");
    }

    /**
     * @return ArrayList<TaskGroup>
     */
    @Override
    public ArrayList<TaskGroup> listTaskGroups() {
        return this.db.getTaskGroups();
    }

    /**
     * Lista os TaskGroups
     */
    @Override
    public String ListTaskGroups() {
        ArrayList<TaskGroup> taskGroups = this.listTaskGroups();
        LOGGER.info("Printing available task groups ...");
        StringBuilder builder = new StringBuilder();
        if (!taskGroups.isEmpty()) {
            for (TaskGroup taskGroup : taskGroups) {
                builder.append(taskGroup.toString());
                builder.append("\n");
            }
            return builder.toString();
        }
        return "Não existem TaskGroups disponíveis";
    }

    /**
     * Procurar TaskGroups de um determinado utilizador
     *
     * @param user who owns taskGroups
     * @return TaskGroup owned by user or null if none own
     */
    private TaskGroup searchTaskGroup(User user) {
        if (!this.db.getTaskGroups().isEmpty()) {
            for (TaskGroup taskGroup : this.db.getTaskGroups()) {
                if (taskGroup.getOwner().getUsername().compareTo(user.getUsername()) == 0) return taskGroup;
            }
        }
        return null;
    }

    /**
     * Criar TaskGroup
     *
     * @return TaskGroup created
     * @throws RemoteException if remote error
     */
    @Override
    public String createTaskGroup() throws IOException, TimeoutException {
        TaskGroup taskGroup = new TaskGroup(this.user.getCoins(), this.user, this.db);
        taskGroup.addUser(this.user);
        this.db.insert(taskGroup, user);     // inserting in db
        LOGGER.info("O utilizador " + this.user.getUsername() + "criou um TaskGroup ");
        return "";
    }

    /**
     *Elimina a sessão
     */
    @Override
    public void logout() {
        this.db.removeSession(this.user);
        //System.exit(1);
    }

    /**
     * Adiciona um Worker a uma Task
     *
     * @param taskOwner
     * @param worker
     */
    public void addWorkerToTask(String taskOwner, WorkerRI worker) throws RemoteException {
        User userTaskOwner = this.db.getUser(taskOwner);
        if (taskOwner == null) {
            LOGGER.info("TaskOwner não encontrado ...");
            return;
        }
        TaskGroup taskGroup = this.searchTaskGroup(userTaskOwner);
        taskGroup.getTask().addWorker(worker);
        this.user.addWorker();
        LOGGER.info("Worker adicionado com sucesso!");
    }

    @Override
    public User getUserFromName(String username) {
        return this.db.getUser(username);
    }

    public User getUser() {
        return this.user;
    }

}
