package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Worker;
import edu.ufp.inf.sd.rmi.util.rmisetup.SetupContextRMI;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    //make run-server PACKAGE_NAME=edu.ufp.inf.sd.dhm.server.Server SERVICE_NAME=DhmService
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    private SetupContextRMI contextRMI;

    private AuthFactoryRI authFactoryRI;

    public static void main(String[] args) {

        if (args != null && args.length < 3) {
            System.exit(-1);
        } else {
            assert args != null;
            Server srv = new Server(args);
            srv.rebindService();
        }
    }

    private void rebindService() {
        try {
            //Get proxy to rmiregistry
            Registry registry = contextRMI.getRegistry();
            //Bind service on rmiregistry and wait for calls
            if (registry != null) {
                //============ Create Servant ============
                authFactoryRI= new AuthFactoryImpl();

                //Get service url (including servicename)
                String serviceUrl = contextRMI.getServicesUrl(0);
                LOGGER.info("going to rebind service " + serviceUrl);

                //============ Rebind servant ============
                //Naming.bind(serviceUrl, helloWorldRI);
                registry.rebind(serviceUrl, authFactoryRI);
                LOGGER.info("service bound and running. :)");
            } else {
                //LOGGER.info("CalculadorServer - Constructor(): create registry on port 1099");
                LOGGER.info("registry not bound (check IPs). :(");
                //registry = LocateRegistry.createRegistry(1099);
            }
        } catch (RemoteException ex) {
            LOGGER.severe(ex.toString());
        }
    }

    public Server(String[] args){
        try {
            String registryIP   = args[0];
            String registryPort = args[1];
            String serviceName  = args[2];
            contextRMI = new SetupContextRMI(this.getClass(), registryIP, registryPort, new String[]{serviceName});
        } catch (RemoteException e) {
            LOGGER.severe(e.toString());
        }
    }

}
