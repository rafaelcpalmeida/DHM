package edu.ufp.inf.sd.dhm.server;

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
                    ClientRI player = entry.getKey();

                    try {
                        player.isAlive();
                    } catch (Exception ignored) {
                        System.out.println("Client is out, removing!");
                        toRemove.add(player);
                    }
                }

                if (toRemove.size() > 0) {
                    for (ClientRI player: toRemove){
                        try {
                            this.detach(player);
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
                            this.removeBackupServer(serverRI, this.privateKey);
                        } catch (Exception ignored) { }
                    }
                }

            }
        }).start();
    }
}
