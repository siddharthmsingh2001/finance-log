package com.financelog.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationEntryPoint authEntryPoint;

    public SecurityConfig(AuthenticationEntryPoint authEntryPoint) {
        this.authEntryPoint = authEntryPoint;
    }

    /**
     * CHAIN 1: Public Endpoints (Auth, Health, and Pre-Signup Utilities)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v1/auth/**", "/actuator/health", "/v1/user/upload-url")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/auth/**", "/actuator/health", "/v1/user/upload-url").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /**
     * CHAIN 2: Protected Application API
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v1/user/**") // Catch all user-related endpoints
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Everything else in /v1/user (like /v1/user/me) requires a session
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        return http.build();
    }
}
/*
 * Spring Security learns about OAuth endpoints from spring.security.oauth2.client.provider.cognito.issuer-uri
 * Spring automatically calls the endpoint {issuer-uri}/.well-known/openid-configuration and obtains
 * - authorization endpoint
 * - token endpoint
 * - JWKS endpoint
 * - logout endpoint
 *
 * Flow:
 * Spa calls: https://api.finance-log.com/oauth2/authorization/{registrationId}} (auto-created when enable .oauth2Login())
 * Spring Redirects to cognito authorization endpoint:
 *      https://finance-log-dev.auth.ap-south-1.amazoncognito.com/oauth2/authorize
        ?response_type=code
        &client_id=XXXX
        &redirect_uri=https://api.finance-log.com/login/oauth2/code/cognito
        &scope=openid+email+profile
        &state=abc123
 * User logs in / signs up at https://finance-log-dev.auth.ap-south-1.amazoncognito.com/login
 * After success cognito redirects back to backend:
 * https://api.finance-log.com/login/oauth2/code/cognito
  ?code=AUTH_CODE
  &state=abc123
 * Spring exchanges code for tokens internally at token_endpoint
   https://finance-log-dev.auth.ap-south-1.amazoncognito.com/oauth2/token
 * Spring then creates a SecurityContext and stores it in HTTP Session and sends back Set-Cookie: JSESSIONID=.......
 * Spring also sends a redirect to oauth2.defaultSuccessUrl which is matches to the endpoint on our backend.
 * When user clicks on logout the SPA calls the backend /logout which then clears the Spring Session
 * Spring also redirects to logout_url in cognito to clear cognito's session at
   https://finance-log-dev.auth.ap-south-1.amazoncognito.com/logout
 */