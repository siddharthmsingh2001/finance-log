package com.financelog.backend.service.impl;

import com.financelog.backend.dto.UserDto;
import com.financelog.backend.entity.AuthenticatedUser;
import com.financelog.backend.entity.User;
import com.financelog.backend.service.OrchestrationService;
import com.financelog.backend.service.UserService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrchestrationServiceImpl implements OrchestrationService {

    private final UserService userService;

    public OrchestrationServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDto getOrCreateUser(AuthenticatedUser authUser) {
        User user = userService
                .findByCognitoSub(authUser.getCognitoSub())
                .orElseGet( ()->
                        userService.registerNewUser(
                                authUser.getCognitoSub(),
                                authUser.getUsername(),
                                authUser.getProfileImageUrl()
                        )
                );
        return UserDto.from(user, authUser);
    }
}
