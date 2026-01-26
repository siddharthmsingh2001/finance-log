package com.financelog.backend.service.impl;

import com.financelog.backend.dto.UserDto;
import com.financelog.backend.entity.User;
import com.financelog.backend.service.OrchestrationService;
import com.financelog.backend.service.UserService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrchestrationServiceImpl implements OrchestrationService {

    private final UserService userService;

    public OrchestrationServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDto getOrCreateUser(OAuth2AuthenticationToken authenticationToken) {
        OAuth2User oAuth2User = authenticationToken.getPrincipal();
        String sub = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        assert sub != null;
        UUID cognitoSub = UUID.fromString(sub);

        User user = userService
                .findByCognitoSub(cognitoSub)
                .orElseGet(() -> userService.registerNewUser(cognitoSub, email));

        return UserDto.from(user);
    }
}
