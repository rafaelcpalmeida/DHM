package edu.ufp.inf.sd.dhm.client;

import edu.ufp.inf.sd.dhm.server.*;
import edu.ufp.inf.sd.rmi.util.rmisetup.SetupContextRMI;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    //make run-client PACKAGE_NAME=edu.ufp.inf.sd.dhm.client.Client SERVICE_NAME=DhmService
    private SetupContextRMI contextRMI;
    private AuthFactoryRI authFactoryRI;
    public Client(String[] args) {
        try {
            String registryIP   = args[0];
            String registryPort = args[1];
            String serviceName  = args[2];
            contextRMI = new SetupContextRMI(this.getClass(), registryIP, registryPort, new String[]{serviceName});
        } catch (RemoteException e) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private Remote lookupService() {
        try {
            //Get proxy to rmiregistry
            Registry registry = contextRMI.getRegistry();
            //Lookup service on rmiregistry and wait for calls
            if (registry != null) {
                //Get service url (including servicename)
                String serviceUrl = contextRMI.getServicesUrl(0);
                //Logger.getLogger(this.getClass().getName()).log(Level.INFO, "going to lookup service @ {0}", serviceUrl);

                //============ Get proxy to HelloWorld service ============
                authFactoryRI = (AuthFactoryRI) registry.lookup(serviceUrl);
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "registry not bound (check IPs). :(");
                //registry = LocateRegistry.createRegistry(1099);
            }
        } catch (RemoteException | NotBoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return authFactoryRI;
    }

    private void playService() {
        try {

            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Registering retoles ...");

            Guest guest = new Guest("retoles","sougay123");
            if(this.authFactoryRI.register(guest)) Logger.getLogger(this.getClass().getName()).log(Level.INFO, "boi do retoles foi registado com sucesso! ...");
            Guest guest2 = new Guest("retoles2","sougay123");
            if(this.authFactoryRI.register(guest)) Logger.getLogger(this.getClass().getName()).log(Level.INFO, "boi do retoles2 foi registado com sucesso! ...");
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Loggin into retoles");
            AuthSessionRI sessionRI = this.authFactoryRI.login(guest);
            if(sessionRI != null){
                //  prompt for the user's name
                this.chooseOption(guest.getUsername(), sessionRI,2);
                this.chooseOption(guest.getUsername(), sessionRI,1);
                //sessionRI.logout();
           }
            AuthSessionRI sessionRI2 = this.authFactoryRI.login(guest2);
            if(sessionRI != null){
                //  prompt for the user's name
                this.chooseOption(guest2.getUsername(), sessionRI2,1);
                this.chooseOption(guest2.getUsername(), sessionRI2,3);
                //sessionRI.logout();
            }
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "going to finish, bye. ;)");
        } catch (RemoteException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        //rabbitmqtest();
        if (args != null && args.length < 3) {
            System.exit(-1);
        } else {
            assert args != null;
            //1. ============ Setup client RMI context ============
            Client client = new Client(args);
            //2. ============ Lookup service ============
            client.lookupService();
            //3. ============ Play with service ============
            client.playService();
        }
    }

    private static void rabbitmqtest() throws IOException, TimeoutException {
        System.out.println("creating taskgroup ....");
        User user = new User("rolotes");
        TaskGroup taskGroup = new TaskGroup(20, user, null);
        /*
        System.out.println("creating task ....");
        Task task = new Task(null,2,null,null,20,taskGroup);
        System.out.println("creating workers ....");
        Worker worker = new Worker(1,user);
        Worker worker2 = new Worker(2,user);
        //task.publish("rolotes123","rolotes_task_send.worker1");
        task.publishToAllWorkers("GENERAL MESSAGE");
        task.publishToWorkersQueue("JUST 1 CAN GET THIS");
        System.out.println("finish");
        */
        System.out.println("creating task");
        Task task = new Task(null, 1, new ArrayList<>(), null, 10, taskGroup);
        System.out.println("creating 2 workers");
        Worker worker = new Worker(1, user);
        Worker worker2 = new Worker(2, user);
        task.addWorker(worker);
        task.addWorker(worker2);
        System.out.println("all done");
    }

    /**
     * Prints all the task groups
     */
    private void printTaskGroups(AuthSessionRI authSessionRI) throws RemoteException {
        System.out.println("Printing available task groups ...");
        ArrayList<TaskGroup> taskGroups = authSessionRI.listTaskGroups();
        if(!taskGroups.isEmpty()){
            for(TaskGroup taskGroup : taskGroups){
                System.out.println(taskGroup.toString());
            }
            return;
        }
        System.out.println("Cannot print taskGroups because there aren't any.");
    }

    private void chooseOption(String username, AuthSessionRI authSessionRI, int option) throws IOException {

        Console c = System.console();
        System.out.print("Hello , " + username + ", what do u want to du? \n1 - print task groups\n2 - create task group \n3 - join task group \n>");

        switch (option){
            case 1:
                this.printTaskGroups(authSessionRI);
                break;
            case 2:
                authSessionRI.createTaskGroup();
                break;
            case 3:
                System.out.println("Which task u want to join?");
                //String option2 = c.readLine();
                authSessionRI.joinTaskGroup("retoles");
                break;
            default:
                System.out.println("wrong option ... ");
                this.chooseOption(username,authSessionRI,option);
        }
    }

}
