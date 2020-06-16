package edu.ufp.inf.sd.HashFinder.server;

import edu.ufp.inf.sd.HashFinder.client.User;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

public class AuthFactoryImpl extends UnicastRemoteObject implements AuthFactoryRI {
    private static final Logger LOGGER = Logger.getLogger(AuthFactoryImpl.class.getName());
    private final DBMockup db;

    public AuthFactoryImpl() throws RemoteException {
        super();
        db = DBMockup.getInstance();
    }


    /**
     * @param guest Insere utilizador na DBMockup
     * @return boolean
     */
    @Override
    public boolean register(User guest) {
        if (!db.exists(guest)) {
            edu.ufp.inf.sd.HashFinder.server.User user = new edu.ufp.inf.sd.HashFinder.server.User(guest.getUsername());
            db.insert(user, guest.getPassword());
            return true;
        }
        return false;
    }

    /**
     * Verifica se o utilizador existe na DB e retorna a sessão
     * @param guest
     * @return AuthSessionRI sessão
     * @throws RemoteException if remote error
     */
    @Override
    public AuthSessionRI login(User guest) throws RemoteException {
        if (this.db.exists(guest)) {
            edu.ufp.inf.sd.HashFinder.server.User user = this.db.getUser(guest.getUsername());
            if (!this.db.existsSession(user)) {
                // Session not created , let's create one for this user
                AuthSessionRI authSessionRI = new AuthSessionImpl(this.db, user);
                this.db.insert(authSessionRI, user);
                return authSessionRI;
            }
            return this.db.getSession(user);
        }
        LOGGER.warning("Utilizador encontrado!");
        return null;
    }
}
