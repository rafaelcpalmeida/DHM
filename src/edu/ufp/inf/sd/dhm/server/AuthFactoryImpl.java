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

    public AuthFactoryImpl() throws RemoteException {
        super();
        db = DBMockup.getInstance();
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
        if(this.db.exists(guest)){
            User user = this.db.getUser(guest.getUsername());
            if(!this.db.existsSession(user)){
                // Session not created , let's create one for this user
                AuthSessionRI authSessionRI = new AuthSessionImpl(this.db,user);
                this.db.insert(authSessionRI,user);
                return authSessionRI;
            }
            return this.db.getSession(user);
        }
        LOGGER.warning("User not found!");
        return null;
    }
}
