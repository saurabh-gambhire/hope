package com.hope.master_service.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hope.master_service.config.AppConfig;
import com.hope.master_service.config.IamConfig;
import com.hope.master_service.constants.Constant;
import com.hope.master_service.dto.response.Response;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.exception.HopeException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that resolves the current tenant schema for each request.
 *
 * Tenant resolution priority:
 * 1. X-TENANT-ID request header
 * 2. Authenticated user's tenant key (from Keycloak token issuer URL)
 * 3. Default: "public" schema
 *
 * Also validates that authenticated users can only access their own tenant.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TenantFilter implements Filter {

    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            String tenantSchema = resolveTenant(httpRequest);
            validateTenantAccess(httpRequest, tenantSchema);
            TenantContext.setCurrentTenant(tenantSchema);
            log.debug("Tenant resolved: '{}' for URI: {}", tenantSchema, httpRequest.getRequestURI());
            chain.doFilter(request, response);
        } catch (HopeException e) {
            log.warn("Tenant access denied for URI {}: {}", httpRequest.getRequestURI(), e.getMessage());
            writeErrorResponse(httpResponse, e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Resolves tenant schema from request header or authenticated user's token.
     */
    private String resolveTenant(HttpServletRequest request) {
        // Priority 1: X-TENANT-ID header
        String headerTenant = request.getHeader(Constant.TENANT_HEADER);
        if (StringUtils.hasText(headerTenant)) {
            return headerTenant.toLowerCase();
        }

        // Priority 2: Authenticated user's tenant from token issuer
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof BearerTokenAuthentication bearerAuth) {
            String tenantKey = IamConfig.getTenantKeyFromTokensIssuerUrl(bearerAuth.getTokenAttributes());

            if (StringUtils.hasText(tenantKey)) {
                return tenantKey;
            }
        }

        // Default: public schema
        return Constant.PUBLIC;
    }

    /**
     * Validates that an authenticated user is authorized to access the resolved tenant.
     * A user belonging to tenant "new_beginning" cannot access tenant "hope_programme".
     * Users in the "public" (master) realm can access any tenant.
     */
    private void validateTenantAccess(HttpServletRequest request, String resolvedTenant) throws HopeException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof BearerTokenAuthentication bearerAuth)) {
            return; // Unauthenticated request — Spring Security will handle auth enforcement
        }

        String userTenantKey = IamConfig.getTenantKeyFromTokensIssuerUrl(bearerAuth.getTokenAttributes());
        if (!StringUtils.hasText(userTenantKey) || Constant.PUBLIC.equals(userTenantKey)) {
            return; // Public/master realm users can access any tenant
        }

        if (!userTenantKey.equals(resolvedTenant)) {
            throw new HopeException(ResponseCode.ACCESS_DENIED,
                    "User from tenant '" + userTenantKey + "' is not authorized to access tenant '" + resolvedTenant + "'");
        }
    }

    private void writeErrorResponse(HttpServletResponse response, HopeException e) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        Response errorResponse = Response.builder()
                .code(e.getErrorCode())
                .message(e.getMessage())
                .requestId(UUID.randomUUID().toString())
                .version(AppConfig.getVersion())
                .build();
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
