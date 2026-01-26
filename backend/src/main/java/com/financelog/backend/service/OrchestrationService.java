package com.financelog.backend.service;

import com.financelog.backend.dto.UserDto;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

public interface OrchestrationService {

    UserDto getOrCreateUser(OAuth2AuthenticationToken authenticationToken);

}
