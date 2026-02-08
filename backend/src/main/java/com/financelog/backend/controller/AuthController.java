package com.financelog.backend.controller;

import com.financelog.backend.dto.*;
import com.financelog.backend.entity.AuthenticatedUser;
import com.financelog.backend.security.SecurityUtils;
import com.financelog.backend.service.CognitoAuthService;
import com.financelog.backend.service.OrchestrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/${api.auth.version}/auth")
public class AuthController {

    private final CognitoAuthService authService;
    private  final OrchestrationService orchestrationService;

    public AuthController(CognitoAuthService authService, OrchestrationService orchestrationService){
        this.authService = authService;
        this.orchestrationService = orchestrationService;
    }

    /**
     * Authenticates a user with Cognito and establishes a local Spring Session.
     */
    @PostMapping("/login")
    public ResponseEntity<APIResponse<UserDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        // 1. Validate with Cognito
        AuthenticatedUser user = authService.login(request.email(), request.password());

        // 2. Set up Spring Security Context & Session Cookie
        SecurityUtils.authenticateUser(httpRequest, httpResponse, user);

        // 3. Ensure DB user exists + map to DTO
        UserDto userDto = orchestrationService.getOrCreateUser(user);

        // 4. Return user representation
        return ResponseEntity.ok(
                APIResponse.ok(userDto, "Login successful. Welcome back!")
        );
    }

    /**
     * Registers a new user in Cognito.
     */
    @PostMapping("/signup")
    public ResponseEntity<APIResponse<Map<String, Object>>> signup(
            @Valid @RequestBody SignUpRequestDto dto
    ) {
        authService.signUp(
                dto.email(),
                dto.password(),
                dto.givenName(),
                dto.familyName(),
                dto.profileImageUrl()
        );

        Map<String, Object> data = Map.of(
                "email", dto.email(),
                "confirmationRequired", true
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.created(
                        data, "Registration successful. Please verify your email."
                ));
    }

    /**
    * Confirms the user's email with the 6-digit code.
    */
    @PostMapping("/confirm")
    public ResponseEntity<APIResponse<Void>> confirm(
            @Valid @RequestBody ConfirmRequestDto dto
    ) {
        authService.confirmSignUp(dto.email(), dto.code());

        return ResponseEntity.ok(
                APIResponse.ok(
                        null,
                        "Account confirmed successfully. You can now log in."
                )
        );
    }
}
