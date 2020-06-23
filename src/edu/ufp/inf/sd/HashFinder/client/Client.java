package edu.ufp.inf.sd.HashFinder.client;

import edu.ufp.inf.sd.HashFinder.server.*;
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

    private SetupContextRMI contextRMI;
    private AuthFactoryRI authFactoryRI;
    private ServerRI serverRI;
    private ClientImpl client;
    private Guest user;


    /**
     * RMI
     */
    public Client(String[] args) {
        try {
            String registryIP = args[0];
            String registryPort = args[1];
            String serviceName = args[2];
            contextRMI = new SetupContextRMI(this.getClass(), registryIP, registryPort, new String[]{serviceName});
        } catch (RemoteException e) {
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * RMI
     */
    private void lookupService() {
        try {
            Registry registry = contextRMI.getRegistry();
            if (registry != null) {
                String serviceUrl = contextRMI.getServicesUrl(0);
                serverRI = (ServerRI) registry.lookup(serviceUrl);
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "registry not bound (check IPs). :(");
            }
        } catch (RemoteException | NotBoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
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

    /**
     * Backup servers
     */
    private AuthFactoryRI getUpdatedAuthFactoryRI() {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                AuthFactoryRI authFactory = this.client.getServerRI().getAuthFactory();
                LOGGER.info("new factory received");
                return authFactory;
            } catch (RemoteException e) {
                LOGGER.warning("Could not get connection to Main server, attempting again ...");
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                LOGGER.severe(e.toString());
            }
        }
        LOGGER.severe("Server request timeout");
        System.exit(-1);
        return null;
    }

    /**
     * Backup servers
     */
    private AuthSessionRI getUpdatedSessionRI() {
        LOGGER.info("Updating Session");
        AuthFactoryRI authFactoryRI = this.getUpdatedAuthFactoryRI();
        try {
            LOGGER.info("Returning session ...");
            return authFactoryRI.login(this.user, this.client);
        } catch (RemoteException e) {
            LOGGER.severe("Error logging in");
        }
        return null;
    }

    /**
     * Verifica a sessão
     * Em caso de dar Exception altera o servidor
     */
    private void playSession(AuthSessionRI authSessionRI) {
        try {
            if (authSessionRI != null) {
                LOGGER.info("Session started !");
                this.chooseOption(authSessionRI);
            } else {
                LOGGER.info("Invalid session");
                this.playService();
            }
        } catch (RemoteException ex) {
            LOGGER.severe("Changing to backup server");
            AuthSessionRI sessionRI = this.getUpdatedSessionRI();
            if (sessionRI != null) {
                this.playSession(sessionRI);
            } else {
                LOGGER.info("Invalid session");
                System.exit(-1);
            }

        } catch (IOException | TimeoutException e) {
            LOGGER.severe(e.toString());
        }
    }

    /**
     * Login e Registo
     * Interação com o utilizador
     *
     * @return AuthSessionRi Sessão
     */
    private AuthSessionRI loginService() throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        LOGGER.info("\n\t1 - Register\n\t2 - Login\n> ");
        int option = scanner.nextInt();
        scanner.nextLine();
        switch (option) {
            case 1:
                LOGGER.info("Choose your username:");
                String regName = scanner.nextLine();
                LOGGER.info("Choose your password:\n> ");
                String regPassw = scanner.nextLine();
                Guest guest = new Guest(regName, regPassw);
                if (this.authFactoryRI.register(guest)) {
                    LOGGER.info("Welcome " + guest.getUsername());
                    this.user = guest;
                    return this.authFactoryRI.login(guest, this.client);
                }
                LOGGER.info("Error registering");
                return null;
            case 2:
                LOGGER.info("Username:\n> ");
                String name = scanner.nextLine();
                LOGGER.info("Password:\n> ");
                String passw = scanner.nextLine();
                Guest guest2 = new Guest(name, passw);
                LOGGER.info("Welcome " + guest2.getUsername());
                this.user = guest2;
                return this.authFactoryRI.login(guest2, this.client);
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
     * Menu principal
     */
    private void chooseOption(AuthSessionRI authSessionRI) throws IOException, TimeoutException {
        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("\t1-List TaskGroups" +
                    "\n\t2-Create TaskGroup" +
                    "\n\t3-Join TaskGroup" +
                    "\n\t4-Remove TaskGroup" +
                    "\n\t5-Wallet" +
                    "\n\t6-Buy coins" +
                    "\n\t7-Attach worker" +
                    "\n\t8-Resume task " +
                    "\n\t9-Pause task " +
                    "\n\t0-logout " +
                    "\n> ");
            int option1 = scanner.nextInt();
            scanner.nextLine();
            switch (option1) {
                case 1 -> LOGGER.info(authSessionRI.printTaskGroups());
                case 2 -> LOGGER.info(authSessionRI.createTaskGroup());
                case 3 -> {
                    LOGGER.info("Which task u want to join?\n> ");
                    String option2 = scanner.nextLine();
                    authSessionRI.joinTaskGroup(option2);
                }
                case 4 -> LOGGER.info(authSessionRI.deleteTaskGroup());
                case 5 -> LOGGER.info(authSessionRI.getCoins());
                case 6 -> {
                    LOGGER.info("Amount to buy\n> ");
                    int amountToBuy = scanner.nextInt();
                    authSessionRI.buyCoins(amountToBuy);
                }
                case 7 -> {
                    LOGGER.info("Task ID to join\n> ");
                    String taskOwner = scanner.nextLine();
                    this.createWorker(authSessionRI, taskOwner);
                }
                case 8 -> LOGGER.info(authSessionRI.resumeTask());
                case 9 -> LOGGER.info(authSessionRI.pauseTask());
                case 0 -> {
                    authSessionRI.logout();
                    this.playService();
                }
                default -> LOGGER.info("Wrong option ... ");
            }
        }
    }

    /**
     * Cria 1 worker com 1 thread
     */
    private void createWorker(AuthSessionRI authSessionRI, String taskOwner) throws IOException, TimeoutException {
        Worker worker = new Worker(authSessionRI.getUser().getAmountOfWorkers() + 1, authSessionRI.getUser(), taskOwner);
        authSessionRI.addWorkerToTask(taskOwner, worker);
    }
}
