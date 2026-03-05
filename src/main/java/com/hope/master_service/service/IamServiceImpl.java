package com.hope.master_service.service;

import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.dto.user.LoginRequest;
import com.hope.master_service.dto.user.LoginResponse;
import com.hope.master_service.dto.user.User;
import com.hope.master_service.exception.HopeException;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static com.hope.master_service.constants.Constant.JS_CLIENT;

@Slf4j
@Service
public class IamServiceImpl extends AppService implements IamService {

    @Value("${keycloak.realm}")
    private String masterRealm;

    @Value("${keycloak.base-url}")
    private String baseUrl;

    @Value("${keycloak.client-id}")
    private String introspectionClientId;

    @Value("${keycloak.client-secret}")
    private String introspectionClientSecret;

    @Value("#{'${tenant.schemas}'.split(',')}")
    private List<String> tenantSchemas;

    @Value("${keycloak.token.access_token_life_span}")
    private Integer accessTokenLifeSpan;

    @Value("${keycloak.token.sso_session_idle_timeout}")
    private Integer ssoSessionIdleTimeout;

    @Value("${keycloak.token.sso_session_max_lifespan}")
    private Integer ssoSessionMaxLifespan;

    @Value("${keycloak.token.offline_session_idle_timeout}")
    private Integer offlineSessionIdleTimeout;

    @Value("${keycloak.token.offline_session_max_lifespan}")
    private Integer offlineSessionMaxLifespan;

    @Value("${keycloak.token.client_session_idle_timeout}")
    private Integer clientSessionIdleTimeout;

    @Value("${keycloak.token.client_session_max_lifespan}")
    private Integer clientSessionMaxLifespan;

    @Autowired
    private Keycloak keycloak;

    @Autowired
    private RestTemplate restTemplate;

    private UsersResource masterUsersResource;

    @PostConstruct
    private void init() {
        try {
            RealmResource realmResource = this.keycloak.realm(masterRealm);
            masterUsersResource = realmResource.users();
        } catch (Exception e) {
            log.error("Error while initiating IAM Configuration", e);
        }

        // Ensure each tenant schema has a corresponding Keycloak realm
        Set<String> existingRealms = keycloak.realms().findAll().stream()
                .map(RealmRepresentation::getRealm)
                .collect(Collectors.toSet());

        for (String schema : tenantSchemas) {
            try {
                if (!existingRealms.contains(schema)) {
                    log.info("Keycloak realm '{}' not found. Creating it...", schema);
                    createRealm(schema);
                } else {
                    configureIntrospectionClient(schema);
                }
            } catch (Exception e) {
                log.warn("Could not initialize realm '{}': {}", schema, e.getMessage());
            }
        }
    }

    private UsersResource getUsersResource(String realmName) {
        return keycloak.realm(realmName).users();
    }

    private UserResource getUserResourceByIamId(String iamId, String realmName) throws HopeException {
        UsersResource usersResource = realmName.equals(masterRealm)
                ? masterUsersResource
                : getUsersResource(realmName);
        try {
            return usersResource.get(iamId);
        } catch (Exception e) {
            log.error("User with IAM id: {} not found in realm: {}", iamId, realmName);
            throwError(ResponseCode.USER_NOT_FOUND, "Cannot find user with given IAM id.");
            return null;
        }
    }

    @Override
    public String createUser(User user) throws HopeException {
        String realmName = getRealmNameFromTenantContext();
        RealmResource realmResource = keycloak.realm(realmName);
        UsersResource usersResource = realmResource.users();

        UserRepresentation iamUser = new UserRepresentation();
        iamUser.setEmail(user.getEmail());
        iamUser.setEmailVerified(false);
        iamUser.setEnabled(true);
        iamUser.setUsername(user.getEmail());
        iamUser.setFirstName(user.getFirstName());
        iamUser.setLastName(user.getLastName());

        Response response;
        try {
            response = usersResource.create(iamUser);
            log.info("IAM user create response status: {}", response.getStatus());
        } catch (Exception e) {
            log.error("Error creating user in IAM: {}", e.getMessage(), e);
            throwError(ResponseCode.IAM_ERROR, e.getMessage());
            return null;
        }

        if (response.getStatus() != 201) {
            log.error("Failed to create user in IAM, status: {}", response.getStatus());
            throwError(ResponseCode.IAM_ERROR, "Failed to create user in Keycloak");
            return null;
        }

        String userId = CreatedResponseUtil.getCreatedId(response);
        log.info("Created IAM user with ID: {}", userId);

        // Assign role
        try {
            UserResource userResource = usersResource.get(userId);
            RoleRepresentation realmRole = realmResource.roles()
                    .get(String.valueOf(user.getRole())).toRepresentation();
            userResource.roles().realmLevel().add(List.of(realmRole));
        } catch (Exception e) {
            log.error("Error assigning role to user: {}", e.getMessage(), e);
            // Rollback: delete created user
            try {
                usersResource.get(userId).remove();
            } catch (Exception rollbackEx) {
                log.error("Failed to rollback user creation: {}", rollbackEx.getMessage());
            }
            throwError(ResponseCode.IAM_ERROR, e.getMessage());
        }

        return userId;
    }

    @Override
    public Optional<User> findByEmail(String email) throws HopeException {
        try {
            UsersResource usersResource = getUsersResource(getRealmNameFromTenantContext());
            return usersResource.searchByUsername(email, true).stream()
                    .findFirst()
                    .map(this::mapToUser);
        } catch (Exception e) {
            throwError(ResponseCode.IAM_ERROR, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) throws HopeException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("username", request.getUsername());
        formData.add("password", request.getPassword());
        formData.add("client_id", JS_CLIENT);
        formData.add("scope", "openid");

        String tokenUrl = baseUrl + "/realms/" + getRealmNameFromTenantContext()
                + "/protocol/openid-connect/token";

        try {
            HttpEntity<MultiValueMap<String, String>> httpRequest = new HttpEntity<>(formData, headers);
            ResponseEntity<LoginResponse> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST, httpRequest, LoginResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throwError(ResponseCode.INVALID_CREDENTIALS);
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage(), e);
            throwError(ResponseCode.LOGIN_FAILED, e.getMessage());
        }
        return null;
    }

    @Override
    public boolean resetPassword(String iamId, String newPassword) throws HopeException {
        UserResource userResource = getUserResourceByIamId(iamId, getRealmNameFromTenantContext());

        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(newPassword);

        try {
            userResource.resetPassword(passwordCred);
            UserRepresentation userRepresentation = userResource.toRepresentation();
            userRepresentation.setEmailVerified(true);
            userResource.update(userRepresentation);
        } catch (BadRequestException e) {
            throwError(ResponseCode.INVALID_PASSWORD);
        } catch (Exception e) {
            throwError(ResponseCode.RESET_PASSWORD_FAILED, e.getMessage());
        }
        return true;
    }

    @Override
    public void updateUserRole(String iamId, String existingRole, Roles newRole) throws HopeException {
        try {
            String realmName = getRealmNameFromTenantContext();
            RealmResource realmResource = keycloak.realm(realmName);
            UserResource userResource = realmResource.users().get(iamId);

            // Remove existing role
            RoleRepresentation oldRealmRole = realmResource.roles().get(existingRole).toRepresentation();
            userResource.roles().realmLevel().remove(List.of(oldRealmRole));

            // Assign new role
            RoleRepresentation newRealmRole = realmResource.roles().get(newRole.name()).toRepresentation();
            userResource.roles().realmLevel().add(List.of(newRealmRole));
        } catch (Exception e) {
            throwError(ResponseCode.IAM_ERROR, e.getMessage());
        }
    }

    @Override
    public void updateUserStatus(String iamId, boolean enabled) throws HopeException {
        UserResource userResource = getUserResourceByIamId(iamId, getRealmNameFromTenantContext());
        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setEnabled(enabled);
        try {
            userResource.update(userRepresentation);
        } catch (Exception e) {
            throwError(ResponseCode.IAM_ERROR, e.getMessage());
        }
    }

    @Override
    public void logout(String refreshToken) throws HopeException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", JS_CLIENT);
        formData.add("refresh_token", refreshToken);

        String logoutUrl = baseUrl + "/realms/" + getRealmNameFromTenantContext()
                + "/protocol/openid-connect/logout";

        try {
            HttpEntity<MultiValueMap<String, String>> httpRequest = new HttpEntity<>(formData, headers);
            restTemplate.exchange(logoutUrl, HttpMethod.POST, httpRequest, String.class);
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage(), e);
            throwError(ResponseCode.LOGOUT_FAILED, e.getMessage());
        }
    }

    @Override
    public void deleteUser(String iamId) throws HopeException {
        try {
            UserResource userResource = getUserResourceByIamId(iamId, getRealmNameFromTenantContext());
            userResource.remove();
            log.info("Deleted IAM user with ID: {}", iamId);
        } catch (Exception e) {
            log.error("Error deleting user from IAM: {}", e.getMessage(), e);
            throwError(ResponseCode.IAM_ERROR, "Failed to delete user: " + e.getMessage());
        }
    }

    @Override
    public void createRealm(String realmName) {
        RolesRepresentation rolesRepresentation = new RolesRepresentation();
        rolesRepresentation.setRealm(buildRealmRoles());

        RealmRepresentation realmRepresentation = new RealmRepresentation();
        realmRepresentation.setRealm(realmName);
        realmRepresentation.setEnabled(true);
        realmRepresentation.setRoles(rolesRepresentation);
        realmRepresentation.setPasswordPolicy("passwordHistory(1)");
        realmRepresentation.setAccessTokenLifespan(accessTokenLifeSpan);
        realmRepresentation.setSsoSessionIdleTimeout(ssoSessionIdleTimeout);
        realmRepresentation.setSsoSessionMaxLifespan(ssoSessionMaxLifespan);
        realmRepresentation.setOfflineSessionIdleTimeout(offlineSessionIdleTimeout);
        realmRepresentation.setOfflineSessionMaxLifespan(offlineSessionMaxLifespan);
        realmRepresentation.setClientSessionIdleTimeout(clientSessionIdleTimeout);
        realmRepresentation.setClientSessionMaxLifespan(clientSessionMaxLifespan);

        keycloak.realms().create(realmRepresentation);

        // Disable VERIFY_PROFILE required action
        List<RequiredActionProviderRepresentation> requiredActions =
                keycloak.realm(realmName).flows().getRequiredActions();
        requiredActions.stream()
                .filter(action -> "VERIFY_PROFILE".equals(action.getAlias()))
                .forEach(action -> {
                    action.setEnabled(false);
                    keycloak.realm(realmName).flows().updateRequiredAction(action.getAlias(), action);
                });

        createClient(realmName, JS_CLIENT);
        configureIntrospectionClient(realmName);
        log.info("Created Keycloak realm: {}", realmName);
    }

    @Override
    public void updateRealmStatus(String realmName, boolean enabled) {
        RealmResource realmResource = keycloak.realms().realm(realmName);
        RealmRepresentation realmRepresentation = realmResource.toRepresentation();
        realmRepresentation.setEnabled(enabled);
        realmResource.update(realmRepresentation);
    }

    @Override
    public void deleteRealm(String realmName) throws HopeException {
        try {
            keycloak.realms().realm(realmName).remove();
            log.info("Deleted Keycloak realm: {}", realmName);
        } catch (Exception e) {
            log.error("Error deleting realm: {}", realmName, e);
            throwError(ResponseCode.IAM_ERROR, "Failed to delete realm: " + realmName);
        }
    }

    private User mapToUser(UserRepresentation representation) {
        return User.builder()
                .iamId(representation.getId())
                .active(representation.isEnabled())
                .email(representation.getEmail())
                .firstName(representation.getFirstName())
                .lastName(representation.getLastName())
                .emailVerified(representation.isEmailVerified())
                .build();
    }

    private List<RoleRepresentation> buildRealmRoles() {
        List<String> roleNames = List.of(
                "SUPER_ADMIN", "ADMIN", "FRONTDESK", "BILLER", "ENB",
                "PSYCHIATRIST", "THERAPIST", "NURSE", "PATIENT", "ANONYMOUS",
                "PROVIDER_GROUP_ADMIN", "PROVIDER", "RESOLUTION_SPECIALIST"
        );
        return roleNames.stream()
                .map(name -> {
                    RoleRepresentation role = new RoleRepresentation();
                    role.setName(name);
                    return role;
                })
                .collect(Collectors.toList());
    }

    /**
     * Configures the admin-cli client in the realm as confidential so it can be used
     * for token introspection by TenantAwareTokenIntrospector.
     */
    private void configureIntrospectionClient(String realmName) {
        RealmResource realmResource = keycloak.realm(realmName);
        ClientsResource clientsResource = realmResource.clients();

        ClientRepresentation adminCli = clientsResource.findByClientId(introspectionClientId)
                .stream().findFirst().orElse(null);

        if (adminCli != null) {
            adminCli.setPublicClient(false);
            adminCli.setServiceAccountsEnabled(true);
            adminCli.setSecret(introspectionClientSecret);
            clientsResource.get(adminCli.getId()).update(adminCli);
            log.info("Configured '{}' as confidential client in realm '{}'", introspectionClientId, realmName);
        } else {
            log.warn("Client '{}' not found in realm '{}', creating it as confidential", introspectionClientId, realmName);
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(introspectionClientId);
            client.setPublicClient(false);
            client.setServiceAccountsEnabled(true);
            client.setSecret(introspectionClientSecret);
            client.setDirectAccessGrantsEnabled(false);
            clientsResource.create(client);
        }
    }

    private void createClient(String realmName, String clientId) {
        RealmResource realmResource = keycloak.realm(realmName);
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setName(clientId);
        client.setPublicClient(true);
        client.setDirectAccessGrantsEnabled(true);
        client.setStandardFlowEnabled(true);
        client.setImplicitFlowEnabled(true);
        client.setFrontchannelLogout(true);
        client.setAttributes(Map.of(
                "oauth2.device.authorization.grant.enabled", "false",
                "oidc.ciba.grant.enabled", "false"
        ));
        client.setRedirectUris(List.of("/*"));
        realmResource.clients().create(client);
        log.info("Created Keycloak client '{}' in realm '{}'", clientId, realmName);
    }
}
