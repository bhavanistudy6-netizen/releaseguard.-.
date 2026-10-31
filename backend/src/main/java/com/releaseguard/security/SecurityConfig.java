package com.releaseguard.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // SPA-friendly CSRF handler
        var requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null); // defer token resolution

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers("/actuator/**", "/api/v1/auth/login")
                )
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Actuator probes and error dispatch
                        .requestMatchers("/actuator/health", "/actuator/info", "/error").permitAll()

                        // Public authentication endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()

                        // Admin user management
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Applications & Releases creation: DEVELOPER or ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications").hasAnyRole("DEVELOPER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/releases").hasAnyRole("DEVELOPER", "ADMIN")

                        // SBOM uploads & Vulnerability Report imports: DEVELOPER or ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/releases/*/sbom").hasAnyRole("DEVELOPER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/releases/*/vulnerability-imports").hasAnyRole("DEVELOPER", "ADMIN")

                        // Decisions: Authenticated users with DEVELOPER, REVIEWER, or ADMIN
                        // Fine-grained action checks (SUBMIT vs APPROVE/REJECT/REVOKE/RETIRE) are enforced in service/controller
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/releases/*/decisions").hasAnyRole("DEVELOPER", "REVIEWER", "ADMIN")

                        // Deployments and Rollbacks: DEPLOYER or ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/deployments").hasAnyRole("DEPLOYER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/deployments/rollbacks").hasAnyRole("DEPLOYER", "ADMIN")

                        // Read endpoints (GET) for all business entities: VIEWER, DEVELOPER, REVIEWER, DEPLOYER, ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("VIEWER", "DEVELOPER", "REVIEWER", "DEPLOYER", "ADMIN")

                        // Deny all other requests
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}

