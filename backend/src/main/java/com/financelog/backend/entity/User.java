package com.financelog.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User{

    protected User() {
    }

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "id_text", insertable = false, updatable = false)
    private String idText;

    @Column(name = "cognito_sub", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID cognitoSub;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "profile_image_url")
    private String profileImageUrl = "default_image";

    @Column(name = "default_currency", length = 3)
    private String defaultCurrency = "INR";

    @Column(name = "first_day_of_week")
    private Integer firstDayOfWeek = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static User create(UUID cognitoSub, String email){
        return new User()
                .setCognitoSub(cognitoSub)
                .setEmail(email);
    }

    public UUID getId() {
        return id;
    }

    public String getIdText() {
        return idText;
    }

    public UUID getCognitoSub() {
        return cognitoSub;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public Integer getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public User setEmail(String email) {
        this.email = email;
        return this;
    }

    public User setCognitoSub(UUID cognitoSub) {
        this.cognitoSub = cognitoSub;
        return this;
    }

    public User setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
        return this;
    }

    public User setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
        return this;
    }

    public User setFirstDayOfWeek(Integer firstDayOfWeek) {
        this.firstDayOfWeek = firstDayOfWeek;
        return this;
    }
}
