package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.User;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
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
     * @param user being registered in db
     * @return boolean
     * @throws RemoteException if remote error
     */
    @Override
    public boolean register(User user) throws RemoteException {
        if (!db.exists(user)) {
            edu.ufp.inf.sd.HashFinder.server.User registeredUser = new edu.ufp.inf.sd.HashFinder.server.User(user.getUsername());
            db.insert(registeredUser, user.getPassword());
            this.server.updateBackupServers();
            return true;
        }
        return false;
    }

    /**
     * @param user being logged into db
     * @return AuthSessionRI session remote object ( stub )
     * @throws RemoteException if remote error
     */
    @Override
    public AuthSessionRI login(User user) throws RemoteException {
        if (this.db.exists(user)) {
            edu.ufp.inf.sd.HashFinder.server.User loggedInUser = this.db.getUser(user.getUsername());
            if (!this.db.existsSession(loggedInUser)) {
                AuthSessionRI authSessionRI = new AuthSessionImpl(this.db, loggedInUser, this.server);
                this.db.insert(authSessionRI, loggedInUser);
                this.server.updateBackupServers();
                return authSessionRI;
            }
            AuthSessionRI authSessionRI = this.db.getSession(loggedInUser);
            if (!this.checkIfSessionIsValid(authSessionRI)) {
                AuthSessionRI sessionRI = new AuthSessionImpl(this.db, loggedInUser, this.server);
                this.db.update(sessionRI, loggedInUser); // updates sessions
                this.server.updateBackupServers();
                return sessionRI;
            }
        }
        LOGGER.warning("User not found!");
        return null;
    }

    /**
     * Checks if a sessions is running ...
     *
     * @param authSessionRI checking
     */
    private boolean checkIfSessionIsValid(AuthSessionRI authSessionRI) {
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
