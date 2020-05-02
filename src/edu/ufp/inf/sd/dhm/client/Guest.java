package edu.ufp.inf.sd.dhm.client;

/**
 * Used to make the authentication
 */
public class Guest {
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
