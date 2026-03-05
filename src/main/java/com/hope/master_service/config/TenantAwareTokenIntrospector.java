package com.hope.master_service.config;

import com.hope.master_service.constants.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tenant-aware opaque token introspector that resolves the Keycloak realm
 * from the X-TENANT-ID header and introspects the token against the correct
 * realm's introspection endpoint.
 *
 * Each tenant maps to a separate Keycloak realm. Introspectors are cached
 * per realm to avoid re-creating them on every request.
 */
@Slf4j
public class TenantAwareTokenIntrospector implements OpaqueTokenIntrospector {

    private final String keycloakBaseUrl;
    private final String clientId;
    private final String clientSecret;
    private final Map<String, OpaqueTokenIntrospector> introspectorCache = new ConcurrentHashMap<>();

    public TenantAwareTokenIntrospector(String keycloakBaseUrl, String clientId, String clientSecret) {
        this.keycloakBaseUrl = keycloakBaseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        String realm = resolveRealm();
        log.debug("Introspecting token against Keycloak realm: {}", realm);
        OpaqueTokenIntrospector introspector = introspectorCache.computeIfAbsent(realm, this::createIntrospector);
        return introspector.introspect(token);
    }

    private String resolveRealm() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String tenant = request.getHeader(Constant.TENANT_HEADER);
            if (tenant != null && !tenant.isBlank() && !Constant.PUBLIC.equalsIgnoreCase(tenant)) {
                return tenant.toLowerCase();
            }
        }
        return "master";
    }

    private OpaqueTokenIntrospector createIntrospector(String realm) {
        String introspectionUri = keycloakBaseUrl + "/realms/" + realm
                + "/protocol/openid-connect/token/introspect";
        log.info("Creating token introspector for realm '{}' at: {}", realm, introspectionUri);
        return new SpringOpaqueTokenIntrospector(introspectionUri, clientId, clientSecret);
    }
}
