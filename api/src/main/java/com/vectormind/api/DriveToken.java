package com.vectormind.api;

import jakarta.persistence.*;
import java.time.Instant;

/** JPA entity that stores a user’s Drive OAuth tokens. */
@Entity
@Table(name = "drive_tokens")
public class DriveToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, length = 2048)
    private String accessToken;

    @Column(length = 2048)
    private String refreshToken;

    @Column(nullable = false)
    private Instant expiryTime;

    /* JPA needs a no-arg constructor */
    protected DriveToken() {}

    public DriveToken(String userId,
                      String accessToken,
                      String refreshToken,
                      Instant expiryTime) {
        this.userId      = userId;
        this.accessToken = accessToken;
        this.refreshToken= refreshToken;
        this.expiryTime  = expiryTime;
    }

    /* getters + setters */
    public String getUserId()      { return userId; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken(){ return refreshToken; }
    public Instant getExpiryTime() { return expiryTime; }

    public void setAccessToken(String t) { this.accessToken = t; }
    public void setRefreshToken(String t){ this.refreshToken = t; }
    public void setExpiryTime(Instant t) { this.expiryTime  = t; }
}
