package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Client;
import edu.ufp.inf.sd.dhm.client.ClientRI;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ServerImpl extends UnicastRemoteObject implements ServerRI , Serializable {
    private static final Logger LOGGER = Logger.getLogger(ServerImpl.class.getName());
    private HashMap<ClientRI,String> clientRIS;
    private Server backupServer;
    private ArrayList<ServerRI> backupServersRIS;
    private boolean run;                        // can this server run functions?

    public ServerImpl(boolean run, Server backupServer) throws RemoteException{
        super();
        this.run = run;
        this.backupServer = backupServer;
        this.backupServersRIS = new ArrayList<>();
        this.clientRIS = new HashMap<>();
        LOGGER.info("Started a new Server!");
        if(this.run)this.startThread();
    }

    private void startThread() {
        new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                ArrayList<ClientRI> toRemove = new ArrayList<>();

                for (Map.Entry<ClientRI, String> entry : this.clientRIS.entrySet()) {
                    ClientRI clientRI = entry.getKey();

                    try {
                        clientRI.isAlive();
                    } catch (Exception ignored) {
                        System.out.println("Client is out, removing!");
                        toRemove.add(clientRI);
                    }
                }

                if (toRemove.size() > 0) {
                    for (ClientRI clientRI: toRemove){
                        try {
                            this.detach(clientRI);
                        } catch (Exception ignored) { }
                    }
                }

                ArrayList<ServerRI> toRemoveServers = new ArrayList<>();

                for (ServerRI serverRI : this.backupServersRIS) {
                    try {
                        serverRI.isAlive();
                    } catch (Exception ignored) {
                        System.out.println("Backup server is out, removing!");
                        toRemoveServers.add(serverRI);
                    }
                }

                if (toRemoveServers.size() > 0) {
                    for (ServerRI serverRI : toRemoveServers) {
                        try {
                            this.removeBackupServer(serverRI);
                        } catch (Exception ignored) { }
                    }
                }

            }
        }).start();
    }

    /**
     * Sets if this server can run and be Main
     * @param b boolean if this server can run
     */
    @Override
    public void setRun(boolean b) throws RemoteException {
        this.run = b;
        this.notifyAllClients();
        this.startThread();
    }

    /**
     * Notifies all clients and servers that this server is the Main
     */
    private void notifyAllClients() {
        for (Map.Entry<ClientRI, String> entry : this.clientRIS.entrySet()) {
            ClientRI clientRI = entry.getKey();
            try {
                clientRI.attachNewServer(this);
            } catch (RemoteException ignored) { }
        }

        for (ServerRI serverRI : this.backupServersRIS) {
            try {
                serverRI.setServerRI(this);
            } catch (RemoteException e) {
                LOGGER.warning(e.toString());
            }
        }
    }

    @Override
    public void isAlive() throws RemoteException {
    //nothing
    }

    @Override
    public void removeBackupServer(int id) throws RemoteException {
        this.removeBackupServer(this.backupServersRIS.get(id));
    }

    private void removeBackupServer(ServerRI serverRI) throws RemoteException{
        LOGGER.info("Server exists: " + this.backupServersRIS.contains(serverRI));

        this.backupServersRIS.remove(serverRI);
        for (ServerRI server: this.backupServersRIS) {
            try {
                server.changeId(this.backupServersRIS.indexOf(server));
            } catch (Exception ignored) { }
        }

        this.updateBackupServers();
    }

    /**
     * Changes ID of server
     * @param id new Id of the server
     */
    @Override
    public void changeId(int id) throws RemoteException {
        this.backupServer.setId(id);
    }

    /**
     * Detach client
     * @param clientRI client being detached from clients list
     */
    @Override
    public void detach(ClientRI clientRI) throws RemoteException {
        this.clientRIS.remove(clientRI);
        this.updateBackupServers();
    }

    /**
     * Attach client
     * @param clientRI client beeing attached to clients list
     */
    @Override
    public void attach(ClientRI clientRI) throws RemoteException {
        this.clientRIS.put(clientRI, null);
        this.updateBackupServers();
    }

    /**
     * Create new thread to update all Servers
     */
    private void updateBackupServers() {
        new Thread(() -> {
            for (ServerRI backupServerRI: this.backupServersRIS) {
                try {
                    backupServerRI.copyInfo(this.clientRIS);
                    backupServerRI.copyBackupServers(this.backupServersRIS);
                } catch (Exception ignored) { }
            }
        }).start();
    }

    @Override
    public int attachBackupServer(ServerRI backupServerRI) throws RemoteException {
        LOGGER.info("Adding backup server!");
        this.backupServersRIS.add(backupServerRI);
        this.updateBackupServers();
        return this.backupServersRIS.indexOf(backupServerRI);
    }

    @Override
    public void setServerRI(ServerRI serverRI) throws RemoteException {
        this.backupServer.setServerRI(serverRI);
    }

    @Override
    public void copyInfo(HashMap<ClientRI, String> clientRIS) throws RemoteException {
        this.clientRIS = clientRIS;
    }

    @Override
    public void copyBackupServers(ArrayList<ServerRI> backupServersRIS) throws RemoteException {
        this.backupServersRIS = backupServersRIS;
    }
}
