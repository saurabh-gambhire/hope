package com.hope.master_service.config;

import com.hope.master_service.dto.enums.RoleType;
import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.modules.user.UserEntity;
import com.hope.master_service.modules.user.UserRepository;
import com.hope.master_service.tenant.TenantContext;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DefaultUserSeeder {

    private final UserRepository userRepository;
    private final Keycloak keycloak;

    @Value("#{'${tenant.schemas}'.split(',')}")
    private List<String> tenantSchemas;

    @Value("${app.default-user.email:admin@hopeehr.com}")
    private String defaultEmail;

    @Value("${app.default-user.password:Admin@123}")
    private String defaultPassword;

    @Value("${app.default-user.first-name:System}")
    private String defaultFirstName;

    @Value("${app.default-user.last-name:Admin}")
    private String defaultLastName;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        for (String schema : tenantSchemas) {
            try {
                TenantContext.setCurrentTenant(schema);
                seedDefaultUser(schema);
            } catch (Exception e) {
                log.error("Failed to seed default user for tenant '{}': {}", schema, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @Transactional
    protected void seedDefaultUser(String realmName) {
        if (userRepository.existsByEmail(defaultEmail)) {
            log.info("Default user already exists in tenant '{}'. Skipping.", realmName);
            return;
        }

        // Create user in Keycloak
        String iamId = createKeycloakUser(realmName);
        if (iamId == null) {
            log.error("Could not create default user in Keycloak for realm '{}'. Skipping DB insert.", realmName);
            return;
        }

        // Create user in DB
        UserEntity entity = UserEntity.builder()
                .iamId(iamId)
                .email(defaultEmail)
                .firstName(defaultFirstName)
                .lastName(defaultLastName)
                .role(Roles.SUPER_ADMIN)
                .roleType(RoleType.STAFF)
                .active(true)
                .emailVerified(true)
                .build();
        userRepository.save(entity);

        log.info("Default SUPER_ADMIN user '{}' created for tenant '{}'", defaultEmail, realmName);
    }

    private String createKeycloakUser(String realmName) {
        try {
            RealmResource realmResource = keycloak.realm(realmName);
            UsersResource usersResource = realmResource.users();

            // Check if user already exists in Keycloak
            List<UserRepresentation> existing = usersResource.searchByEmail(defaultEmail, true);
            if (existing != null && !existing.isEmpty()) {
                log.info("Default user already exists in Keycloak realm '{}'. Using existing IAM ID.", realmName);
                return existing.get(0).getId();
            }

            // Create user
            UserRepresentation user = new UserRepresentation();
            user.setEmail(defaultEmail);
            user.setUsername(defaultEmail);
            user.setFirstName(defaultFirstName);
            user.setLastName(defaultLastName);
            user.setEnabled(true);
            user.setEmailVerified(true);

            // Set password
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(defaultPassword);
            credential.setTemporary(false);
            user.setCredentials(List.of(credential));

            Response response = usersResource.create(user);
            if (response.getStatus() != 201) {
                log.error("Failed to create default user in Keycloak realm '{}', status: {}", realmName, response.getStatus());
                return null;
            }

            String userId = CreatedResponseUtil.getCreatedId(response);

            // Assign SUPER_ADMIN role
            try {
                RoleRepresentation role = realmResource.roles().get(Roles.SUPER_ADMIN.name()).toRepresentation();
                usersResource.get(userId).roles().realmLevel().add(List.of(role));
            } catch (Exception e) {
                log.warn("Could not assign SUPER_ADMIN role in realm '{}': {}", realmName, e.getMessage());
            }

            return userId;
        } catch (Exception e) {
            log.error("Error creating default user in Keycloak realm '{}': {}", realmName, e.getMessage(), e);
            return null;
        }
    }
}
