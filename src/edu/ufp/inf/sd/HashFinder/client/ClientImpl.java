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

    /**
     * Adiciona o cliente ao servidor
     */
    public ClientImpl( ServerRI serverRI) throws RemoteException{
        this.serverRI = serverRI;
        this.serverRI.attach(this); //attach this client to server
    }

    /**
     * Recebe servidor de backup e torna-o o principal
     */
    @Override
    public void attachNewServer(ServerRI server) throws RemoteException {
        LOGGER.info("Attaching new server!");
        this.serverRI = server;
    }

    /**
     * Verifica se cliente está ok
     */
    @Override
    public void checkIfClientOk() throws RemoteException {
    }

    /**
     * Acesso ao token JWT
     */
    @Override
    public void sendToken(String token) throws RemoteException {
        this.token = token;
    }

    /**
     * Recebe mensagens do AuthSessionImpl
     */
    @Override
    public void sendMessage(String msg) throws RemoteException {
        LOGGER.info("[SERVER MESSAGE] " + msg);
    }

    /**
     * Devolve interface do server
     */
    public ServerRI getServerRI() {
        return serverRI;
    }

    /**
     * Altera token JWT
     * LIXO
     */
    public void updateToken(String plainToken){
        this.token = plainToken;
    }

    /**
     * Retorna palabra de hash desincriptada
     */
    private String getHashedToken(){
        MessageDigest algorithm = null;
        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Erro, Esse algoritmo não é suportado!");
        }
        byte[] hashByte = algorithm.digest(this.token.getBytes(StandardCharsets.UTF_8));
        return this.byteToString(hashByte);
    }


    /**
     * Converte bytes para string
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
