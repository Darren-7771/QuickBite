package com.example.projectrplbo.model;

public abstract class Akun {
    protected String username;
    protected String password;

    public Akun() {}

    public Akun(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public abstract boolean login();
    public abstract void logout();

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
