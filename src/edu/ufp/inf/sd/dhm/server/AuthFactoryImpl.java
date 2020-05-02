package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Guest;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class AuthFactoryImpl extends UnicastRemoteObject implements AuthFactoryRI {
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
        return false;
    }

    /**
     * @param guest being logged into db
     * @return AuthSessionRI session remote object ( stub )
     * @throws RemoteException if remote error
     */
    @Override
    public AuthSessionRI login(Guest guest) throws RemoteException {
        return null;
    }
}
