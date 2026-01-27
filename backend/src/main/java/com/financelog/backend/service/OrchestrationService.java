package com.financelog.backend.service;

import com.financelog.backend.dto.UserDto;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public interface OrchestrationService {

    UserDto getOrCreateUser(OidcUser oidcUser);

}
