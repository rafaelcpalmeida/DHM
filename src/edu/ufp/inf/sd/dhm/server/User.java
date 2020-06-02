package edu.ufp.inf.sd.dhm.server;

import edu.ufp.inf.sd.dhm.client.Worker;

import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable {
    private final String username;
    private int coins;
    private int amountOfWorkers;

    public User(String username) {
        this.username = username;
        this.coins = 0;
        amountOfWorkers =0;
    }

    public String getUsername() {
        return username;
    }

    public void setCoins(int quantity) {
        this.coins = quantity;
    }

    public int getCoins() {
        return coins;
    }

    public int getAmountOfWorkers() {
        return this.amountOfWorkers;
    }

    public void addWorker(){
        this.amountOfWorkers ++;
    }
}
