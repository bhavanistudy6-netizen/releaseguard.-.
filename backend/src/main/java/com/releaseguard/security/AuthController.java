package com.releaseguard.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public AuthController(
            AuthenticationManager authenticationManager,
            LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String username = request.username().trim().toLowerCase();

        if (loginAttemptService.isBlocked(username)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Account is temporarily locked due to excessive failed attempts. Please try again later."
            );
        }

        Authentication authentication;
        try {
            var token = new UsernamePasswordAuthenticationToken(username, request.password());
            authentication = authenticationManager.authenticate(token);
        } catch (DisabledException e) {
            loginAttemptService.loginFailed(username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is disabled. Please contact an administrator.");
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginAttemptService.loginFailed(username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        } catch (Exception e) {
            loginAttemptService.loginFailed(username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        loginAttemptService.loginSucceeded(username);

        // Session fixation protection: establish session and change session ID upon login
        HttpSession session = httpRequest.getSession(true);
        if (session != null) {
            try {
                httpRequest.changeSessionId();
            } catch (Exception ignored) {
            }
        }

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        if (session != null) {
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        }

        ReleaseGuardUserDetails userDetails = (ReleaseGuardUserDetails) authentication.getPrincipal();
        UserResponse response = new UserResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getDisplayName(),
                userDetails.getRoles(),
                userDetails.isEnabled(),
                null,
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal ReleaseGuardUserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        UserResponse response = new UserResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getDisplayName(),
                principal.getRoles(),
                principal.isEnabled(),
                null,
                null
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CSRF token not available");
        }

        return ResponseEntity.ok(new CsrfResponse(
                csrfToken.getToken(),
                csrfToken.getParameterName(),
                csrfToken.getHeaderName()
        ));
    }
}

