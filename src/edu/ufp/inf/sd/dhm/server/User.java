package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Worker;

import java.util.ArrayList;

public class User {
    private final String username;
    private int coins;
    private ArrayList<Worker> workers;

    public User(String username) {
        this.username = username;
        this.coins = 0;
        workers = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }
}
