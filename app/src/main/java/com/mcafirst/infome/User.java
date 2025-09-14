package com.mcafirst.infome;

public class User {
    public String name, email, role;
    public User() {}   // empty constructor required
    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }
}
