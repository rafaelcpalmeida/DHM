package edu.ufp.inf.sd.HashFinder.server;

import java.io.Serializable;

public class User implements Serializable {
    private final String username;
    private final int coins;
    private int amountOfWorkers;

    public User(String username) {
        this.username = username;
        this.coins = 0;
        amountOfWorkers = 0;
    }

    public String getUsername() {
        return username;
    }

    public int getCoins() {
        return coins;
    }

    public int getAmountOfWorkers() {
        return this.amountOfWorkers;
    }

    public void addWorker() {
        this.amountOfWorkers++;
    }
}
