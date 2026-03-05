package com.hope.master_service.config;

import com.hope.master_service.dto.enums.Roles;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
public class IamConfig {

    @Value("${keycloak.auth-url}")
    private String authUrl;

    @Value("${keycloak.realm}")
    private String masterRealm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .serverUrl(authUrl)
                .realm(masterRealm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }

    /**
     * Extracts token attributes from the current SecurityContext.
     * Works with opaque token authentication (BearerTokenAuthentication).
     */
    public static Map<String, Object> getTokenAttributes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof BearerTokenAuthentication bearerAuth) {
            return bearerAuth.getTokenAttributes();
        }
        return Collections.emptyMap();
    }

    public static String getRealmFromTokensIssuerUrl(String issuerUrl) {
        String realm = extractRealmFromIssuerUrl(issuerUrl);
        return realm.equals("public") ? "master" : realm;
    }

    public static String getTenantKeyFromTokensIssuerUrl(Map<String, Object> attributes) {
        if (attributes.containsKey("iss")) {
            String realm = extractRealmFromIssuerUrl(attributes.get("iss").toString());
            return realm.equals("master") ? "public" : realm;
        }
        return null;
    }

    private static String extractRealmFromIssuerUrl(String issuerUrl) {
        String[] parts = issuerUrl.split("/");
        return parts[parts.length - 1];
    }

    public static Roles getRole(Map<String, Object> attributes) {
        return determineUserRole(getRoles(attributes));
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRoles(Map<String, Object> attributes) {
        Map<String, Object> realmAccess = (Map<String, Object>) attributes.get("realm_access");
        return (realmAccess != null && realmAccess.containsKey("roles")) ?
                (List<String>) realmAccess.get("roles") : Collections.emptyList();
    }

    private static Roles determineUserRole(List<String> roles) {
        for (String role : roles) {
            try {
                return Roles.valueOf(role);
            } catch (IllegalArgumentException ignored) {
                // Role not in enum, continue checking
            }
        }
        return Roles.ANONYMOUS;
    }
}
