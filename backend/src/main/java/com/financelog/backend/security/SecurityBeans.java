package com.financelog.backend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Configuration
public class SecurityBeans {

    private final String cognitoLogoutUrl;
    private final String clientId;
    private final String appUrl;

    public SecurityBeans(
            @Value("${api.auth.cognito.logoutUrl}") String cognitoLogoutUrl,
            @Value("${spring.security.oauth2.client.registration.cognito.client-id}") String clientId,
            @Value("${api.app.url}") String appUrl
    ){
        this.cognitoLogoutUrl = cognitoLogoutUrl;
        this.clientId = clientId;
        this.appUrl = appUrl;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://app.finance-log.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler(){
        return (request, response, authentication) -> {
            response.sendRedirect("https://app.finance-log.com/");
        };
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(){
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        };
    }

    @Bean
    LogoutSuccessHandler logoutSuccessHandler(){
        return (request, response, authentication) -> {
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(cognitoLogoutUrl)
                    .queryParam("client_id", clientId)
                    .queryParam("logout_uri", appUrl)
                    .build()
                    .toUriString();
            response.sendRedirect(redirectUrl);
        }
    }
}
