package com.financelog.backend.service.impl;

import com.financelog.backend.entity.User;
import com.financelog.backend.repo.UserRepository;
import com.financelog.backend.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByCognitoSub(UUID cognitoSub){
        return userRepository.findByCognitoSub(cognitoSub);
    }

    @Override
    public User registerNewUser(UUID cognitoSub, String email,String profileImageUrl){
        User user = User.create(cognitoSub, email);
        if (profileImageUrl != null && !profileImageUrl.equals("default_image")) {
            user.setProfileImageUrl(profileImageUrl);
        }
        return userRepository.save(user);
    }

}
