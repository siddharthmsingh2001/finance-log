package com.financelog.backend.controller;

import com.financelog.backend.dto.UserDto;
import com.financelog.backend.service.OrchestrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/${api.user.version}")
public class UserController {

    private final OrchestrationService orchestrationService;

    public UserController(OrchestrationService orchestrationService){
        this.orchestrationService = orchestrationService;
    }

    @GetMapping("/user-info")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal OidcUser oidcUser){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(orchestrationService.getOrCreateUser(oidcUser));
    }

}
