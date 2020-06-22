package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.rmi.util.rmisetup.SetupContextRMI;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    //make run-server PACKAGE_NAME=edu.ufp.inf.sd.HashFinder.server.Server SERVICE_NAME=HashFinderService
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private int id;
    private SetupContextRMI contextRMI;

    private ServerRI serverRI;
    private ServerRI backupServerRI;

    public static void main(String[] args) {
        if (args != null && args.length < 4) {
            System.exit(-1);
        } else {
            assert args != null;
            Server srv = new Server(args);
            boolean isBackupServer = Boolean.parseBoolean(args[3]);
            if(isBackupServer) {
                try{
                    srv.serverRI = (ServerRI) srv.lookupService();
                } catch (Exception e){
                    LOGGER.severe(e.toString());
                }

                if (srv.serverRI == null) {
                    LOGGER.severe("Main server is dead :X Going to start backup server!");
                    srv.rebindBackupService();
                }
                try{
                    srv.backupServerRI = new ServerImpl(false,srv);
                } catch (RemoteException e) {
                    LOGGER.severe(e.toString());
                }
                srv.waiting();
                return;
            }

            srv.rebindService();
        }
    }

    private void waiting() {
        try {
            this.id = this.serverRI.attachBackupServer(this.backupServerRI);
            LOGGER.info("This server id -> " + this.id);
        } catch (RemoteException e) {
            LOGGER.severe("Main server is out, starting backup server!");
            this.rebindBackupService();
        }

        while (true) {
            try {
                this.serverRI.isAlive();
            } catch (RemoteException re) {
                this.serverRI = null;
                LOGGER.info("" + this.id);
                if (this.id == 0) {
                    // Reached the head of backup servers queue , is the next to work
                    LOGGER.severe("Main server is out, starting backup server!");
                    this.rebindBackupService();
                    try {
                        this.backupServerRI.removeBackupServer(this.id);
                    } catch (RemoteException e) {
                        LOGGER.severe(e.toString());
                    }
                    break;
                }
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

    /**
     * Rebinds main server
     */
    private void rebindService() {
        try {
            Registry registry = contextRMI.getRegistry();
            if (registry != null) {
                serverRI = new ServerImpl(true, null);
                String serviceUrl = contextRMI.getServicesUrl(0);
                registry.rebind(serviceUrl, serverRI);
            } else {
                LOGGER.info("registry not bound (check IPs). :(");
            }
        } catch (RemoteException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
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

    public void setId(int id) {
        this.id = id;
    }

    public void setServerRI(ServerRI serverRI) {
        this.serverRI =  serverRI;
    }
}