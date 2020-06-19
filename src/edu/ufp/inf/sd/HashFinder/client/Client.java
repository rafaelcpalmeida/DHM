package edu.ufp.inf.sd.HashFinder.client;

import edu.ufp.inf.sd.HashFinder.server.AuthFactoryRI;
import edu.ufp.inf.sd.HashFinder.server.AuthSessionRI;
import edu.ufp.inf.sd.rmi.util.rmisetup.SetupContextRMI;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Client {
    private static final Logger LOGGER;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "\033[32m%1$tF %1$tT\033[39m \u001b[33m[%4$-7s]\u001b[0m %5$s %n");
        LOGGER = Logger.getLogger(Client.class.getName());
    }

    private SetupContextRMI contextRMI;
    private AuthFactoryRI authFactoryRI;

    public Client(String[] args) {
        try {
            String registryIP = args[0];
            String registryPort = args[1];
            String serviceName = args[2];
            contextRMI = new SetupContextRMI(this.getClass(), registryIP, registryPort, new String[]{serviceName});
        } catch (RemoteException e) {
            LOGGER.severe(e.getMessage());
        }
    }

    private void lookupService() {
        try {
            //Get proxy to rmiregistry
            Registry registry = contextRMI.getRegistry();
            //Lookup service on rmiregistry and wait for calls
            if (registry != null) {
                //Get service url (including servicename)
                String serviceUrl = contextRMI.getServicesUrl(0);
                //============ Get proxy to HelloWorld service ============
                authFactoryRI = (AuthFactoryRI) registry.lookup(serviceUrl);
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "registry not bound (check IPs). :(");
            }
        } catch (RemoteException | NotBoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void playService() {
        try {
            AuthSessionRI authSessionRI = this.loginService();
            if (authSessionRI != null) {
                LOGGER.info("Session started...");
                this.Menu(authSessionRI);
            }
            LOGGER.info("Finishing...");
        } catch (RemoteException ex) {
            LOGGER.severe(ex.toString());
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs the user and returns if successfully logged the AuThSessionRI
     *
     * @return AuthSessionRi needed for all the actions
     */
    private AuthSessionRI loginService() throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        LOGGER.info("Menu\n1 - Registar\n2 - Login");
        int option = scanner.nextInt();
        scanner.nextLine();
        String name;
        String password;
        switch (option) {
            case 1:
                LOGGER.info("User:");
                name = scanner.nextLine();
                LOGGER.info("Password:");
                password = scanner.nextLine();
                User user = new User(name, password);
                if (this.authFactoryRI.register(user)) {
                    LOGGER.info("Bem Vindo " + user.getUsername());
                    return this.authFactoryRI.login(user);
                }
                LOGGER.info("Erro, utilizador já registado");
                return null;
            case 2:
                LOGGER.info("User:");
                name = scanner.nextLine();
                LOGGER.info("Password:");
                password = scanner.nextLine();
                user = new User(name, password);
                LOGGER.info("Bem Vindo " + user.getUsername());
                return this.authFactoryRI.login(user);
            default:
                return this.loginService();
        }
    }

    public static void main(String[] args) {
        Thread thread = new Thread(() -> { Application.launch(GUI.class, args); });
        thread.start();
        if (args != null && args.length < 3) {
            System.exit(-1);
        } else {
            assert args != null;
            //1. ============ Setup client RMI context ============
            Client client = new Client(args);
            //2. ============ Lookup service ============
            client.lookupService();
            //3. ============ Play with service ============
            client.playService();
        }
    }


    /**
     * Menu de interação com Switch Case
     *
     * @param authSessionRI objeto da sessão
     */
    private void Menu(AuthSessionRI authSessionRI) throws IOException, TimeoutException {
        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("1 - Listar TaskGroups disponíveis" +
                    "\n2 - Criar Task Group " +
                    "\n3 - Entrar num TaskGroup " +
                    "\n4 - Adicionar worker a uma Task " +
                    "\n> ");
            int option1 = scanner.nextInt();
            scanner.nextLine();
            switch (option1) {
                case 1 -> LOGGER.info(authSessionRI.ListTaskGroups());
                case 2 -> LOGGER.info(authSessionRI.createTaskGroup());
                case 3 -> {
                    LOGGER.info("Selecione a Task para se juntar\n> ");
                    String option2 = scanner.nextLine();
                    scanner.nextLine();
                    authSessionRI.joinTaskGroup(option2);
                }
                case 4 -> {
                    LOGGER.info("Selecione a Task para o seu worker trabalhar\n> ");
                    String taskOwner = scanner.nextLine();
                    scanner.nextLine();
                    this.createWorker(authSessionRI, taskOwner);
                }
                default -> LOGGER.info("Erro, opção inválida");
            }
        }
    }

    /**
     * Cria um worker/thread
     *
     * @param authSessionRI objeto da sessão sessão
     * @param taskOwner     owner's name
     */
    private void createWorker(AuthSessionRI authSessionRI, String taskOwner) throws IOException, TimeoutException {
        Worker worker = new Worker(authSessionRI.getUser().getAmountOfWorkers() + 1, authSessionRI.getUser(), taskOwner);
        authSessionRI.addWorkerToTask(taskOwner, worker);
    }

    public static class GUI extends Application {
        @Override
        public void start(Stage primaryStage) {
            primaryStage.setTitle("Menu");
            Button btn = new Button();
            btn.setText("Entrar");
            btn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    System.out.println("Thank you sensei!");
                }
            });
            Button btn2 = new Button();
            btn2.setText("Registar");
            btn2.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    System.out.println("Thank you sensei!");
                }
            });
            Button btn3 = new Button();
            btn3.setText("Chuapa-mos Capela <3");
            btn3.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    System.out.println("Thank you sensei!");
                }
            });
            StackPane root = new StackPane();
            VBox vbox = new VBox(5);
            vbox.setAlignment(Pos.CENTER);
            vbox.getChildren().addAll(btn, btn2, btn3);
            root.getChildren().add(vbox);
            primaryStage.setScene(new Scene(root, 300, 250));
            primaryStage.show();
        }
    }
}
