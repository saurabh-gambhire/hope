package com.hope.master_service.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class SecurityUtils {

	public static final List<String> ALLOWED_ENDPOINTS = List.of(
			// Swagger / OpenAPI
			"/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/api-docs",
			"/v3/api-docs/**", "/v3/api-docs",
			// Actuator
			"/actuator/**",
			// Public auth endpoints
			"/api/master/users/login",
			"/api/master/users/forgot-password",
			"/api/master/users/verify-otp",
			"/api/master/users/set-password",
			"/api/master/users/activate",
			"/api/master/users/logout",
			"/api/master/us-states"
	);

	private static final AntPathMatcher pathMatcher = new AntPathMatcher();

	public static boolean isAllowed(String uri) {
		return ALLOWED_ENDPOINTS.stream()
			.anyMatch(pattern -> pathMatcher.match(pattern, uri));
	}

	/**
	 * Get current authenticated user ID
	 */
	public static UUID getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		log.info("inside security core");
		if (authentication instanceof BearerTokenAuthentication bearerAuth) {
			Map<String, Object> attributes = bearerAuth.getTokenAttributes();
			String userId = (String) attributes.get("sub");
			if (userId != null) {
				try {
					return UUID.fromString(userId);
				} catch (IllegalArgumentException e) {
					// If sub is not a valid UUID, return null
					return null;
				}
			}
		}

		return null;
	}

	/**
	 * Get current authenticated username
	 */
	public static String getCurrentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		return authentication.getName();
	}

	/**
	 * Get current user roles
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getCurrentUserRoles() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return List.of();
		}

		if (authentication instanceof BearerTokenAuthentication bearerAuth) {
			Map<String, Object> attributes = bearerAuth.getTokenAttributes();
			Map<String, Object> realmAccess = (Map<String, Object>) attributes.get("realm_access");
			if (realmAccess != null) {
				return (List<String>) realmAccess.get("roles");
			}
		}

		return List.of();
	}

	/**
	 * Check if current user has specific role
	 */
	public static boolean hasRole(String role) {
		return getCurrentUserRoles().contains(role);
	}

	/**
	 * Check if user is authenticated
	 */
	public static boolean isAuthenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.isAuthenticated();
	}
}


