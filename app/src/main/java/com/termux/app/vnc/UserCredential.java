package com.termux.app.vnc;

public class UserCredential {
    public final String username;
    public final String password;

    public UserCredential(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static UserCredential empty() {
        return new UserCredential("", "");
    }
}