package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.ClientRI;
import edu.ufp.inf.sd.HashFinder.client.Guest;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
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
     * Registo
     * Insere user na BD
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
     * Retorna sessão (STUB)
     */
    @Override
    public AuthSessionRI login(Guest guest, ClientRI clientRI) throws RemoteException {
        if(this.db.exists(guest)){
            User user = this.db.getUser(guest.getUsername());
            if(!this.db.existsSession(user)){
                // Cria a sessão
                String newPlainToken = this.generateToken();
                AuthSessionRI authSessionRI = new AuthSessionImpl(this.db,user,this.server,newPlainToken,clientRI);
                clientRI.sendToken(newPlainToken);
                this.db.insert(authSessionRI,user);
                this.server.updateBackupServers();
                return authSessionRI;
            }
            AuthSessionRI authSessionRI= this.db.getSession(user);
            if(!this.checkIfSessionIsValid(authSessionRI)){
                // Sessão inválida
                String newPlainToken = this.generateToken();
                AuthSessionRI sessionRI = new AuthSessionImpl(this.db,user,this.server,newPlainToken, clientRI);
                clientRI.sendToken(newPlainToken);
                this.db.update(sessionRI,user); // updates sessions
                this.server.updateBackupServers();
                return sessionRI;
            }
            // Envia token da sessão ao cliente
            AuthSessionImpl authSession = (AuthSessionImpl) authSessionRI;
            clientRI.sendToken(authSession.getToken());
            return authSessionRI;
        }
        return null;
    }

    /**
     * Gera uma string 5<=30
     */
    private String generateToken(){
        StringBuilder builder = new StringBuilder();
        Random r = new Random();
        int minCaracteres = 5;
        int maxCaracteres = 30;
        int caracteresLenght = r.nextInt(maxCaracteres-minCaracteres) + minCaracteres;
        for(int i = 0 ; i<caracteresLenght ; i++){
            char c = (char)(r.nextInt(26) + 'a');
            builder.append(c);
        }
        return builder.toString();
    }


    /**
     * Verifica sessão
     */
    private boolean checkIfSessionIsValid(AuthSessionRI authSessionRI){
        try {
            authSessionRI.checkIfClientOk();
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
