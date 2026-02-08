package com.financelog.backend.service;

import com.financelog.backend.entity.AuthenticatedUser;

public interface CognitoAuthService {

    AuthenticatedUser login(String email, String password);

    void signUp(String email, String password, String givenName, String familyName, String profileImageUrl);

    void confirmSignUp(String email, String code);

    void resendConfirmationCode(String email);
}
