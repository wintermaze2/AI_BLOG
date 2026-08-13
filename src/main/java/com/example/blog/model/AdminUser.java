package com.example.blog.model;

/**
 * 관리자 계정.
 */
public class AdminUser {
    private long id;
    private String username;
    private String passwordHash;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
