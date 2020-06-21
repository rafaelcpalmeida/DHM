package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.ClientRI;
import edu.ufp.inf.sd.dhm.client.Guest;
import edu.ufp.inf.sd.dhm.client.Worker;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
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
    public AuthSessionRI login(Guest guest, ClientRI clientRI) throws RemoteException {
        if(this.db.exists(guest)){
            User user = this.db.getUser(guest.getUsername());
            if(!this.db.existsSession(user)){
                // Session not created , let's create one for this user
                String newPlainToken = this.getRandomPlainToken();
                AuthSessionRI authSessionRI = new AuthSessionImpl(this.db,user,this.server,newPlainToken,clientRI);
                clientRI.sendToken(newPlainToken);
                this.db.insert(authSessionRI,user);
                this.server.updateBackupServers();
                return authSessionRI;
            }
            AuthSessionRI authSessionRI= this.db.getSession(user);
            if(!this.checkIfSessionIsValid(authSessionRI)){
                // if the session is not valid
                String newPlainToken = this.getRandomPlainToken();
                AuthSessionRI sessionRI = new AuthSessionImpl(this.db,user,this.server,newPlainToken, clientRI);
                clientRI.sendToken(newPlainToken);
                this.db.update(sessionRI,user); // updates sessions
                this.server.updateBackupServers();
                return sessionRI;
            }
        }
        LOGGER.warning("User not found!");
        return null;
    }

    /**
     * @return random string between 5 and 30 caracters
     */
    private String getRandomPlainToken(){
        StringBuilder builder = new StringBuilder();
        Random r = new Random();
        int minCaracteres = 5;
        int maxCaracteres = 30;
        int caracteresLenght = r.nextInt(minCaracteres-maxCaracteres) + minCaracteres;
        for(int i = 0 ; i<caracteresLenght ; i++){
            //get a random letter
            char c = (char)(r.nextInt(26) + 'a');
            builder.append(c);
        }
        return builder.toString();
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
