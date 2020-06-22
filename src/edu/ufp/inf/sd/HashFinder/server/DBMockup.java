package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.Guest;
import edu.ufp.inf.sd.HashFinder.client.Worker;
import edu.ufp.inf.sd.HashFinder.client.WorkerRI;
import edu.ufp.inf.sd.HashFinder.server.exceptions.TaskOwnerRunOutOfMoney;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;


public class DBMockup implements Serializable {
    private static DBMockup dbMockup = null;
    private HashMap<User, AuthSessionRI> sessions;
    private HashMap<User, String> users;
    private HashMap<User, TaskGroup> taskgroups;
    private HashMap<Task, ArrayList<Worker>> tasks;
    private HashMap<User,Integer> userCoins;
    private HashMap<User, ArrayList<WorkerRI>> userWorkers;


    private DBMockup() {
        sessions = new HashMap<User, AuthSessionRI>();
        users = new HashMap<>();
        taskgroups = new HashMap<User, TaskGroup>();
        tasks = new HashMap<Task, ArrayList<Worker>>();
        userWorkers = new HashMap<User,ArrayList<WorkerRI>>();
        userCoins = new HashMap<>();
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
     * Insere user
     */
    public synchronized void insert(User user, String passwd) {
        if(!this.users.containsKey(user)){
            this.users.put(user,passwd);
            this.userCoins.put(user,0);
        }
    }

    /**
     * Insere Task
     */
    public void insert(Task task) {
        if(!this.tasks.containsKey(task)) this.tasks.put(task,new ArrayList<Worker>());
    }

    /**
     * Insere worker na task
     */
    public void insert(Worker worker, Task task) {
        ArrayList<Worker> workers=this.tasks.get(task);
        if(!workers.contains(worker)) workers.add(worker);
    }

    /**
     * Insere sessão
     */
    public void insert(AuthSessionRI sessionRI, User user) {
        if(!this.sessions.containsKey(user)) this.sessions.put(user,sessionRI);
    }

    /**
     * Atualiza sessão
     */
    public void update(AuthSessionRI sessionRI, User user){
        if(this.sessions.containsKey(user)) this.sessions.put(user,sessionRI);
    }

    /**
     * Insere TaskGroup
     */
    public synchronized void insert(TaskGroup taskGroup, User user) {
        if(!this.taskgroups.containsKey(user)) this.taskgroups.put(user,taskGroup);
    }


    /**
     * Carrega dinheiro
     */
    public void giveMoney(User user, int quantity){
        if(this.users.containsKey(user)){
            this.setUserCoins(user,this.getCoins(user) + quantity);
        }
    }


    public void setUserCoins(User user,int value){
        this.userCoins.put(user,value);
    }

    /**
     * Verifica se user existe
     */
    public boolean exists(Guest guest){
        for(User user : this.users.keySet()){
            if(user.getUsername().compareTo(guest.getUsername()) == 0){
                if(this.users.get(user).compareTo(guest.getPassword()) ==0) return true;
            }
        }
        return false;
    }

    public boolean existsSession(User user){
        return this.sessions.containsKey(user);
    }

    public User getUser(String name){
        for(User user : this.users.keySet()){
            if(user.getUsername().compareTo(name) == 0) return user;
        }
        return null;
    }

    public synchronized ArrayList<TaskGroup> getTaskGroups() {
        ArrayList<TaskGroup> tasks = new ArrayList<>();
        this.taskgroups.forEach((user, taskGroup) -> {
            if(!tasks.contains(taskGroup))
                tasks.add(taskGroup);
        });
        return tasks;
    }

    public AuthSessionRI getSession(User user){
        return this.sessions.get(user);
    }

    public void removeSession(User user){
        this.sessions.remove(user);
    }

    /**
     * Retira dinheiro quando são efetuadas transações
     */
    public void takeMoney(User user, int amount) throws TaskOwnerRunOutOfMoney {
        if(this.users.containsKey(user)){
            // The user exists
            int newBalance = this.getCoins(user) - amount;
            if (newBalance < 0) {
                // owner has no more money to spent
                throw new TaskOwnerRunOutOfMoney();
            }
            this.setUserCoins(user,newBalance);
        }
    }

    /**
     * Returns coins from a User
     */
    public int getCoins(User user){
        return this.userCoins.get(user);
    }

    /**
     * Adiciona users ao Worker
     */
    public void insert(WorkerRI workerRI, User user){
        if(!this.userWorkers.containsKey(user)){
            // if the user has no Workers yet
            ArrayList<WorkerRI> newArray = new ArrayList<>();
            newArray.add(workerRI);
            this.userWorkers.put(user,newArray);      // create key and new ArrayList
            return;
        }
        this.userWorkers.get(user).add(workerRI);
    }

    public WorkerRI getWorkerStub(String username, int id) throws RemoteException {
        User user = this.getUser(username);
        ArrayList<WorkerRI> workers = this.userWorkers.get(user);
        for(WorkerRI workerRI: workers){
            if(workerRI.getId() == id) return workerRI;
        }
        return null;
    }


    public TaskGroup getTaskGroup(User user){
        return this.taskgroups.get(user);
    }

    public void deleteTaskGroup(User user) {
        this.taskgroups.remove(user);
    }

}
