package edu.ufp.inf.sd.HashFinder.server;

import java.io.Serializable;


public class StringGroup implements Serializable {
    private final int ceiling;
    private final int delta;

    public StringGroup(int ceiling, int delta) {
        this.ceiling = ceiling;
        this.delta = delta;
    }

    public int getCeiling() {
        return ceiling;
    }

    public int getDelta() {
        return delta;
    }
}
