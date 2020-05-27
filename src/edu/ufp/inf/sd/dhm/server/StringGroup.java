package edu.ufp.inf.sd.dhm.server;

import java.io.Serializable;

/**
 * Made to denote the ceiling and the delta of a major file string
 * For workers to know from where to where they need to hash.
 */
public class StringGroup implements Serializable {
    private int ceiling;
    private int delta;

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
