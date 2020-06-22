package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.ClientRI;

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
    private AuthFactoryImpl authFactory;
    private Server backupServer;
    private ArrayList<ServerRI> backupServersRIS;
    private boolean run;                        // can this server run functions?

    public ServerImpl(boolean run, Server backupServer) throws RemoteException{
        super();
        this.run = run;
        this.authFactory = new AuthFactoryImpl(this);
        this.backupServer = backupServer;
        this.backupServersRIS = new ArrayList<>();
        this.clientRIS = new HashMap<>();
        LOGGER.info("Started a new Server!");
        if(this.run)this.startThread();
    }

    /**
     * Rafa
     */
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
                        clientRI.checkIfClientOk();
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
                        serverRI.checkIfClientOk();
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
     * Rafa
     * @param b boolean if this server can run
     */
    @Override
    public void setRun(boolean b) throws RemoteException {
        this.run = b;
        this.notifyAllClients();
        this.startThread();
    }

    /**
     * Envia estada a dizer quem é o server
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

    /**
     * Verifica se o server está ok
     */
    @Override
    public void checkIfClientOk() throws RemoteException {
    //nothing
    }

    @Override
    public void removeBackupServer(int id) throws RemoteException {
        this.removeBackupServer(this.backupServersRIS.get(id));
    }

    /**
     * Elimina o servidor de backup
     */
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
     * Altera ID do server
     */
    @Override
    public void changeId(int id) throws RemoteException {
        this.backupServer.setId(id);
    }

    /**
     * Elimina cliente da lista de clientes
     */
    @Override
    public void detach(ClientRI clientRI) throws RemoteException {
        this.clientRIS.remove(clientRI);
        this.updateBackupServers();
    }

    /**
     * Adiciona cliente à lista de clientes
     */
    @Override
    public void attach(ClientRI clientRI) throws RemoteException {
        this.clientRIS.put(clientRI, null);
        this.updateBackupServers();
    }

    @Override
    public AuthFactoryRI getAuthFactory() throws RemoteException {
        return this.authFactory;
    }

    /**
     * Thread para enviar informação aos outros servers
     */
    public void updateBackupServers() {
        new Thread(() -> {
            for (ServerRI backupServerRI: this.backupServersRIS) {
                try {
                    backupServerRI.copyInfo(this.clientRIS,this.authFactory.getDb());
                    backupServerRI.copyBackupServers(this.backupServersRIS);
                } catch (Exception ignored) { }
            }
        }).start();
    }

    /**
     * Seleciona o próximo servidor
     */
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

    /**
     * Copia informação para os outros servidores
     */
    @Override
    public void copyInfo(HashMap<ClientRI, String> clientRIS,DBMockup dbMockup) throws RemoteException {
        LOGGER.info("Received info and updated my db");
        this.clientRIS = clientRIS;
        this.authFactory.setDb(dbMockup);       // updates db
    }

    /**
     * Lista dos servidores de backup
     */
    @Override
    public void copyBackupServers(ArrayList<ServerRI> backupServersRIS) throws RemoteException {
        this.backupServersRIS = backupServersRIS;
    }

    public void setAuthFactory(AuthFactoryImpl authFactory) {
        this.authFactory = authFactory;
    }

}
