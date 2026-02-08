package com.financelog.backend.security;

import com.financelog.backend.entity.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.List;

public class SecurityUtils {

    private static final SecurityContextRepository repo = new HttpSessionSecurityContextRepository();

    private SecurityUtils(){}

    public static void authenticateUser(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticatedUser user
    ){
// 1. Create the Authentication object
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of()
                );

        // 2. Create a new empty context
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        // 3. Set it in the holder for the current thread
        SecurityContextHolder.setContext(context);

        // 4. Save it to the repository (this handles the Session part for you)
        repo.saveContext(context, request, response);
    }

}
