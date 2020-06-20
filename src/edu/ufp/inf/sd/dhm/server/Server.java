package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Worker;
import edu.ufp.inf.sd.rmi.util.rmisetup.SetupContextRMI;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    //make run-server PACKAGE_NAME=edu.ufp.inf.sd.dhm.server.Server SERVICE_NAME=DhmService
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private SetupContextRMI contextRMI;

    private AuthFactoryRI authFactoryRI;
    private ServerRI serverRI;
    private ServerRI backupServerRI;

    public static void main(String[] args) {

        if (args != null && args.length < 3) {
            System.exit(-1);
        } else {
            assert args != null;
            Server srv = new Server(args);
            if(srv.isBackupServer()){
                // If this is a Backup server
                srv.serverRI = (ServerRI) srv.lookupService();
            }else {
                // Main server
                if (srv.serverRI == null) {
                    LOGGER.severe("Main server is dead :X Going to start backup server!");
                    srv.rebindBackupService();
                }

                try{
                    srv.backupServerRI = (ServerRI) new ServerImpl(false,srv);
                } catch (RemoteException e) {
                    LOGGER.severe(e.toString());
                }
            }


            srv.rebindService();
        }
    }

    /**
     * Rebinds a backup server to the registry
     */
    private void rebindBackupService() {
        try {
            Registry registry = contextRMI.getRegistry();
            if (registry != null) {
                String serviceUrl = contextRMI.getServicesUrl(0);
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "going to rebind service @ {0}", serviceUrl);
                //============ Rebind servant ==========
                this.backupServerRI.setRun(true);
                registry.rebind(serviceUrl, this.backupServerRI);
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "service bound and running. :)");
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "registry not bound (check IPs). :(");
            }
        } catch (RemoteException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean isBackupServer(){
        Scanner scanner = new Scanner(System.in);
        int option = 0;
        while(option < 1 || option > 2){
            LOGGER.info("Hello Admin, what type of server is this? " +
                    "\n1 - Main Server" +
                    "\n2 - Backup Server " +
                    "\n> ");
            option = scanner.nextInt();
            scanner.nextLine();
        }
        return option == 1 ? false : true;
    }

    private void printChooseMessage(){
        LOGGER.info("Hello Admin, what type of server is this? " +
                "\n1 - Main Server" +
                "\n2 - Backup Server " +
                "\n> ");
    }

    private void rebindService() {
        try {
            //Get proxy to rmiregistry
            Registry registry = contextRMI.getRegistry();
            //Bind service on rmiregistry and wait for calls
            if (registry != null) {
                //============ Create Servant ============
                authFactoryRI= new AuthFactoryImpl();

                //Get service url (including servicename)
                String serviceUrl = contextRMI.getServicesUrl(0);
                LOGGER.info("going to rebind service " + serviceUrl);

                //============ Rebind servant ============
                //Naming.bind(serviceUrl, helloWorldRI);
                registry.rebind(serviceUrl, authFactoryRI);
                LOGGER.info("service bound and running. :)");
            } else {
                //LOGGER.info("CalculadorServer - Constructor(): create registry on port 1099");
                LOGGER.info("registry not bound (check IPs). :(");
                //registry = LocateRegistry.createRegistry(1099);
            }
        } catch (RemoteException ex) {
            LOGGER.severe(ex.toString());
        }
    }

    public Server(String[] args){
        try {
            String registryIP   = args[0];
            String registryPort = args[1];
            String serviceName  = args[2];
            contextRMI = new SetupContextRMI(this.getClass(), registryIP, registryPort, new String[]{serviceName});
        } catch (RemoteException e) {
            LOGGER.severe(e.toString());
        }
    }

    private Remote lookupService() {
        try {
            Registry registry = this.contextRMI.getRegistry();
            if (registry != null) {
                String serviceUrl = this.contextRMI.getServicesUrl(0);
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "going to lookup service @ {0}", serviceUrl);
                return registry.lookup(serviceUrl);
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "registry not bound (check IPs). :(");
            }
        } catch (RemoteException | NotBoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
