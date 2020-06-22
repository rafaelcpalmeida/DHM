package edu.ufp.inf.sd.HashFinder.client;

import edu.ufp.inf.sd.HashFinder.server.AuthFactoryRI;
import edu.ufp.inf.sd.HashFinder.server.AuthSessionRI;
import edu.ufp.inf.sd.HashFinder.server.ServerRI;
import edu.ufp.inf.sd.rmi.util.rmisetup.SetupContextRMI;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final Logger LOGGER;
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "\033[32m%1$tF %1$tT\033[39m \u001b[33m[%4$-7s]\u001b[0m %5$s %n");
        LOGGER = Logger.getLogger(Client.class.getName());
    }
    //make run-client PACKAGE_NAME=edu.ufp.inf.sd.HashFinder.client.Client SERVICE_NAME=HashFinderService
    private SetupContextRMI contextRMI;
    private AuthFactoryRI authFactoryRI;
    private ServerRI serverRI;
    private ClientImpl client;
    private User user;

    public Client(String[] args) {
        try {
            String registryIP   = args[0];
            String registryPort = args[1];
            String serviceName  = args[2];
            contextRMI = new SetupContextRMI(this.getClass(), registryIP, registryPort, new String[]{serviceName});
        } catch (RemoteException e) {
            LOGGER.severe(e.getMessage());
        }
    }

    private void lookupService() {
        try {
            Registry registry = contextRMI.getRegistry();
            if (registry != null) {
                String serviceUrl = contextRMI.getServicesUrl(0);
                serverRI = (ServerRI) registry.lookup(serviceUrl);
                return;
            }
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "registry not bound (check IPs). :(");
        } catch (RemoteException | NotBoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, String.valueOf(ex));
            System.exit(-1);
        }
    }

    private void playService() {
        this.authFactoryRI = this.getUpdatedAuthFactoryRI();
        try {
            AuthSessionRI authSessionRI = this.loginService();
            this.playSession(authSessionRI);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private AuthFactoryRI getUpdatedAuthFactoryRI(){
        for(int attempt = 0 ; attempt < 5 ; attempt++){
            try {
                AuthFactoryRI authFactory= this.client.getServerRI().getAuthFactory(); // get the updated ServerRI
                LOGGER.info("new factory received");
                return authFactory;
            }catch (RemoteException e){
                LOGGER.warning("Could not get connection to Main server, attempting again ...");
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                LOGGER.severe(e.toString());
            }
        }
        LOGGER.severe("Server request timeout w/ 5 attempts , I'mma get the hell outta here. Bye");
        System.exit(-1);
        return null;
    }

    private AuthSessionRI getUpdatedSessionRI(){
        LOGGER.info("going to update session");
        AuthFactoryRI authFactoryRI = this.getUpdatedAuthFactoryRI();
        try {
            LOGGER.info("Returning session ...");
            return authFactoryRI.login(this.user);
        } catch (RemoteException e) {
            LOGGER.severe("Couldn't login ...");
        }
        LOGGER.info("Returning null");
        return null;
    }

    private void playSession(AuthSessionRI authSessionRI){
        try{
            if (authSessionRI != null) {
                LOGGER.info("Session started !");
                this.chooseOption(authSessionRI);
            }else {
                LOGGER.info("Credentials invalid");
                this.playService();
            }
        } catch (RemoteException ex) {
            LOGGER.severe("EXCEPTION -> "  + ex.toString());
            LOGGER.severe("Probably the main server is down , going to ask the new backup server");
            AuthSessionRI sessionRI = this.getUpdatedSessionRI();
            if(sessionRI != null){
                LOGGER.info("session not null!");
                this.playSession(sessionRI);
            }else{
                LOGGER.info("session null");
                LOGGER.severe("Could not get session due to an error in sessionRI , I'm dyyyyyyyyyyying");
                System.exit(-1);
            }

        } catch (IOException | TimeoutException e) {
            LOGGER.severe(e.toString());
        }
    }

    /**
     * Loggs the user and returns if successfully logged the AuThSessionRI
     * @return AuthSessionRi needed for all the actions
     */
    private AuthSessionRI loginService() throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        LOGGER.info("Welcome to our wonderful software , would u rather:\n1 - Register\n2 - Login\n> ");
        int option = scanner.nextInt();
        scanner.nextLine();
        switch (option){
            case 1:
                LOGGER.info("You are about to register our service ...\nPlease tell us the username u want to register:\n> ");
                String name = scanner.nextLine();
                LOGGER.info("Now tell us the password , dont worry , we ain't peeking:\n> ");
                String passwd = scanner.nextLine();
                User User = new User(name,passwd);
                if(this.authFactoryRI.register(User)){
                    // success
                    LOGGER.info("Welcome " + User.getUsername() + " , ur session is starting ...");
                    this.user = User;
                    return this.authFactoryRI.login(User);
                }
                LOGGER.info("Could not register your account :/");
                return null;
            case 2:
                LOGGER.info("username:\n> ");
                String name2 = scanner.nextLine();
                LOGGER.info("password:\n> ");
                String passwd2 = scanner.nextLine();
                User User2 = new User(name2,passwd2);
                LOGGER.info("Welcome " + User2.getUsername() + " , ur session is starting ...");
                this.user = User2;
                return this.authFactoryRI.login(User2);
            default:
                return this.loginService();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args != null && args.length < 3) {
            System.exit(-1);
        } else {
            assert args != null;
            Client cl = new Client(args);
            cl.lookupService();
            cl.start();
        }
    }

    private void start() throws RemoteException {
        this.client = new ClientImpl(this.serverRI);
        this.playService();
    }


    /**
     * Interactive menu for user to choose all options
     * @param authSessionRI needed for all actions
     */
    private void chooseOption(AuthSessionRI authSessionRI) throws IOException, TimeoutException {
        while(true){
            Scanner scanner = new Scanner(System.in);
            LOGGER.info("Hello , what do u want to do? " +
                    "\n1 - print task groups" +
                    "\n2 - create task group " +
                    "\n3 - join task group " +
                    "\n4 - add worker to task " +
                    "\n5 - how much coins do I have?" +
                    "\n6 - buy coins w/ bitcoins " +
                    "\n7 - pause task " +
                    "\n8 - resume task " +
                    "\n9 - delete task group " +
                    "\n> ");
            int option1 = scanner.nextInt();
            scanner.nextLine();
            switch (option1) {
                case 1 -> LOGGER.info(authSessionRI.printTaskGroups());
                case 2 -> LOGGER.info(authSessionRI.createTaskGroup());
                case 3 -> {
                    LOGGER.info("Which task u want to join?\n> ");
                    String option2 = scanner.nextLine();
                    scanner.nextLine();
                    authSessionRI.joinTaskGroup(option2);
                }
                case 4 -> {
                    LOGGER.info("Which task u want to join ur worker?\n> ");
                    String taskOwner = scanner.nextLine();
                    scanner.nextLine();
                    this.createWorker(authSessionRI, taskOwner);
                }
                case 5 -> LOGGER.info(authSessionRI.getCoins());
                case 6 -> {
                    LOGGER.info("How much do u wanna buy? only bitcoin...\n> ");
                    String amountToBuy = scanner.nextLine();
                    authSessionRI.buyCoins(Integer.parseInt(amountToBuy));
                }
                case 7 -> LOGGER.info(authSessionRI.pauseTask());
                case 8 -> LOGGER.info(authSessionRI.resumeTask());
                case 9 -> LOGGER.info(authSessionRI.deleteTaskGroup());
                default -> LOGGER.info("Wrong option ... ");
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
