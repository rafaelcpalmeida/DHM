package edu.ufp.inf.sd.dhm.client;

import edu.ufp.inf.sd.dhm.server.*;
import edu.ufp.inf.sd.rmi.util.rmisetup.SetupContextRMI;
import edu.ufp.inf.sd.rmi.util.threading.ThreadPool;

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
            AuthSessionRI authSessionRI = this.loginService();
            if (authSessionRI != null) {
                System.out.println("Session started !");
                this.chooseOption(authSessionRI);
            }
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Going to finish ...");
        } catch (RemoteException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loggs the user and returns if successfully logged the AuThSessionRI
     * @return AuthSessionRi needed for all the actions
     */
    private AuthSessionRI loginService() throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to our wonderful software , would u rather:\n1 - Register\n2 - Login\n> ");
        int option = scanner.nextInt();
        scanner.nextLine();
        switch (option){
            case 1:
                System.out.println("You are about to register our service ...\nPlease tell us the username u want to register:\n> ");
                String name = scanner.nextLine();
                scanner.nextLine();
                System.out.println("Now tell us the password , dont worry , we ain't peeking:\n> ");
                String passwd = scanner.nextLine();
                scanner.nextLine();
                Guest guest = new Guest(name,passwd);
                if(this.authFactoryRI.register(guest)){
                    // success
                    System.out.println("Welcome " + guest.getUsername() + " , ur session is starting ...");
                    return this.authFactoryRI.login(guest);
                }
                System.out.println("Could not register your account :/");
                return null;
            case 2:
                System.out.println("username:\n> ");
                String name2 = scanner.nextLine();
                scanner.nextLine();
                System.out.println("password:\n> ");
                String passwd2 = scanner.nextLine();
                scanner.nextLine();
                Guest guest2 = new Guest(name2,passwd2);
                System.out.println("Welcome " + guest2.getUsername() + " , ur session is starting ...");
                return this.authFactoryRI.login(guest2);
            default:
                return this.loginService();
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


    /**
     * Interactive menu for user to choose all options
     * @param authSessionRI needed for all actions
     */
    private void chooseOption(AuthSessionRI authSessionRI) throws IOException, TimeoutException {
        while(true){
            Scanner scanner = new Scanner(System.in);
            System.out.print("Hello , what do u want to do? " +
                    "\n1 - print task groups" +
                    "\n2 - create task group " +
                    "\n3 - join task group " +
                    "\n4 - add worker to task " +
                    "\n> ");
            int option1 = scanner.nextInt();
            scanner.nextLine();
            switch (option1){
                case 1:
                    System.out.println(authSessionRI.printTaskGroups());
                    break;
                case 2:
                    System.out.println(authSessionRI.createTaskGroup());
                    break;
                case 3:
                    System.out.println("Which task u want to join?\n> ");
                    String option2 = scanner.nextLine();
                    scanner.nextLine();
                    authSessionRI.joinTaskGroup(option2);
                    break;
                case 4:
                    System.out.println("Which task u want to join ur worker?\n> ");
                    String taskOwner = scanner.nextLine();
                    scanner.nextLine();
                    this.createWorker(authSessionRI,taskOwner);
                    break;
                default:
                    System.out.println("Wrong option ... ");
            }
        }
    }

    /**
     * Creates a new worker who has 1 thread
     * @param authSessionRI with all the methods from the session
     * @param taskOwner owner's name
     */
    private void createWorker(AuthSessionRI authSessionRI, String taskOwner) throws IOException, TimeoutException {
        Worker worker = new Worker(authSessionRI.getUser().getAmountOfWorkers() + 1,authSessionRI.getUser(),taskOwner);
        authSessionRI.addWorkerToTask(taskOwner,worker);
    }
}
