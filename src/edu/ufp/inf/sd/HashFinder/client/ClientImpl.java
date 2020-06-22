package edu.ufp.inf.sd.HashFinder.client;

import edu.ufp.inf.sd.HashFinder.server.Server;
import edu.ufp.inf.sd.HashFinder.server.ServerRI;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class ClientImpl extends UnicastRemoteObject implements ClientRI{
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private ServerRI serverRI;
    private String token;
    //private Client client;

    public ClientImpl( ServerRI serverRI) throws RemoteException{
        this.serverRI = serverRI;
        //this.client = client;
        this.serverRI.attach(this); //attach this client to server
    }

    @Override
    public void attachNewServer(ServerRI server) throws RemoteException {
        LOGGER.info("Attaching new server!");
        this.serverRI = server;
    }

    /**
     * Check if client  is online
     * @throws RemoteException if is not alive
     */
    @Override
    public void isAlive() throws RemoteException {
        // do nothing
    }

    @Override
    public void sendToken(String token) throws RemoteException {
        this.token = token;
    }

    @Override
    public void sendMessage(String msg) throws RemoteException {
        LOGGER.info("[SERVER MESSAGE] " + msg);
    }

    public ServerRI getServerRI() {
        return serverRI;
    }

    public void updateToken(String plainToken){
        this.token = plainToken;
    }

    /**
     * @return Hashed token
     */
    public String getHashedToken(){
        return this.getHashFromPlainToken(this.token);
    }

    /**
     * Returns the digest from the plain token
     * @param plainToken token in plain text
     * @return Hash in MD5 of the plain text
     */
    private String getHashFromPlainToken(String plainToken){
        MessageDigest algorithm = null;
        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Could not get instance of digest algorithm");
        }
        byte[] hashByte = algorithm.digest(plainToken.getBytes(StandardCharsets.UTF_8));
        return this.byteToString(hashByte);
    }


    /**
     * @param bytes w/ the digest
     * @return bytes in string
     */
    private String byteToString(byte[] bytes){
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", 0xFF & b));
        }
        return hexString.toString();
    }

    public String getToken() {
        return token;
    }
}
