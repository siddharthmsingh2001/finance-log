package com.financelog.backend.service;

import com.financelog.backend.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

    User registerNewUser(UUID cognitoSub, String email, String profileImageUrl);

    Optional<User> findByCognitoSub(UUID cognitoSub);

}
