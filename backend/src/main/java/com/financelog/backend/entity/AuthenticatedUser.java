package com.financelog.backend.entity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AuthenticatedUser implements UserDetails {

    private final UUID cognitoSub;
    private final String email;
    private final String givenName;
    private final String familyName;
    private final String profileImageUrl;
    private final String accessToken;
    private final String idToken;

    public AuthenticatedUser(
            UUID cognitoSub,
            String email,
            String givenName,
            String familyName,
            String profileImageUrl,
            String accessToken,
            String idToken
    ) {
        this.cognitoSub = cognitoSub;
        this.email = email;
        this.givenName = givenName;
        this.familyName = familyName;
        this.profileImageUrl = profileImageUrl;
        this.accessToken = accessToken;
        this.idToken = idToken;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // Add roles here if needed (e.g., SimpleGrantedAuthority("ROLE_USER"))
    }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    public UUID getCognitoSub() { return cognitoSub; }
    public String getAccessToken() { return accessToken; }
    public String getIdToken() { return idToken; }
    public String getGivenName(){ return givenName; }
    public String getFamilyName(){ return familyName; }
    public String getProfileImageUrl(){ return profileImageUrl; }

}
