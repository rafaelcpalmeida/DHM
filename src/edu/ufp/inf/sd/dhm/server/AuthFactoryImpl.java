package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Guest;
import edu.ufp.inf.sd.dhm.client.Worker;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthFactoryImpl extends UnicastRemoteObject implements AuthFactoryRI {
    private static final Logger LOGGER = Logger.getLogger(AuthFactoryImpl.class.getName());
    private DBMockup db;
    private ServerImpl server;

    public AuthFactoryImpl(ServerImpl server) throws RemoteException {
        super();
        db = DBMockup.getInstance();
        this.server = server;
    }

    public AuthFactoryImpl(DBMockup dbMockup) throws RemoteException {
        super();
        this.db = dbMockup;
    }


    /**
     * @param guest being registered in db
     * @return boolean
     * @throws RemoteException if remote error
     */
    @Override
    public boolean register(Guest guest) throws RemoteException {
        if(!db.exists(guest)){
            User user = new User(guest.getUsername());
            db.insert(user,guest.getPassword());
            this.server.updateBackupServers();
            return true;
        }
        return false;
    }

    /**
     * @param guest being logged into db
     * @return AuthSessionRI session remote object ( stub )
     * @throws RemoteException if remote error
     */
    @Override
    public AuthSessionRI login(Guest guest) throws RemoteException {
        LOGGER.info("1");
        if(this.db.exists(guest)){
            LOGGER.info("2");
            User user = this.db.getUser(guest.getUsername());
            if(!this.db.existsSession(user)){
                LOGGER.info("3");
                // Session not created , let's create one for this user
                AuthSessionRI authSessionRI = new AuthSessionImpl(this.db,user,this.server);
                this.db.insert(authSessionRI,user);
                LOGGER.info("4");
                this.server.updateBackupServers();
                return authSessionRI;
            }
            LOGGER.info("5");
            AuthSessionRI authSessionRI= this.db.getSession(user);
            if(!this.checkIfSessionIsValid(authSessionRI)){
                // if the session is not valid
                AuthSessionRI sessionRI = new AuthSessionImpl(this.db,user,this.server);
                this.db.update(sessionRI,user); // updates sessions
                this.server.updateBackupServers();
                return sessionRI;
            }
        }
        LOGGER.warning("User not found!");
        return null;
    }

    /**
     * Checks if a sessions is running ...
     * @param authSessionRI checking
     */
    private boolean checkIfSessionIsValid(AuthSessionRI authSessionRI){
        try {
            authSessionRI.isAlive();
            return true;
        } catch (RemoteException ignored) {
            return false;
        }
    }

    public DBMockup getDb() {
        return db;
    }

    public void setDb(DBMockup db) {
        this.db = db;
    }
}
