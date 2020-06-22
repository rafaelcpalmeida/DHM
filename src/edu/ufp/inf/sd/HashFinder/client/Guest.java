package edu.ufp.inf.sd.HashFinder.client;

import java.io.Serializable;

/**
 * Used to make the authentication
 */
public class Guest implements Serializable {
    private final String username;
    private final String password;

    public Guest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
