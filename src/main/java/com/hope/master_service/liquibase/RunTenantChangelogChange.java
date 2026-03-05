package com.hope.master_service.liquibase;

import liquibase.Liquibase;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Liquibase custom change that applies tenant-specific changelogs
 * (db/tenant/master.yaml) to each tenant schema.
 *
 * Tenant schemas are discovered dynamically by querying the database
 * and filtering out system/internal schemas.
 */
public class RunTenantChangelogChange implements CustomTaskChange {

    private static final Logger log = LoggerFactory.getLogger(RunTenantChangelogChange.class);

    private static final String TENANT_CHANGELOG = "db/tenant/master.yaml";

    private static final Set<String> EXCLUDED_SCHEMAS = Set.of(
            "public", "information_schema", "pg_catalog", "pg_toast"
    );

    private ResourceAccessor resourceAccessor;

    @Override
    public void execute(Database database) throws CustomChangeException {
        JdbcConnection connection = (JdbcConnection) database.getConnection();
        String originalSchema = null;

        try {
            originalSchema = database.getDefaultSchemaName();
            List<String> tenantSchemas = discoverTenantSchemas(connection);

            if (tenantSchemas.isEmpty()) {
                log.warn("No tenant schemas found. Skipping tenant changelog execution.");
                return;
            }

            for (String schema : tenantSchemas) {
                log.info("Applying tenant changelogs to schema: {}", schema);
                database.setDefaultSchemaName(schema);

                Liquibase liquibase = new Liquibase(
                        TENANT_CHANGELOG,
                        new ClassLoaderResourceAccessor(),
                        database
                );
                liquibase.update("");
                log.info("Successfully applied tenant changelogs to schema: {}", schema);
            }
        } catch (Exception e) {
            throw new CustomChangeException("Failed to run tenant changelogs", e);
        } finally {
            if (originalSchema != null) {
                try {
                    database.setDefaultSchemaName(originalSchema);
                } catch (Exception e) {
                    log.error("Failed to restore original schema: {}", originalSchema, e);
                }
            }
        }
    }

    private List<String> discoverTenantSchemas(JdbcConnection connection) throws Exception {
        List<String> schemas = new ArrayList<>();
        String query = "SELECT schema_name FROM information_schema.schemata";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String schemaName = rs.getString("schema_name");
                if (!EXCLUDED_SCHEMAS.contains(schemaName) && !schemaName.startsWith("pg_")) {
                    schemas.add(schemaName);
                }
            }
        }

        log.info("Discovered tenant schemas: {}", schemas);
        return schemas;
    }

    @Override
    public String getConfirmationMessage() {
        return "Tenant changelogs applied successfully to all tenant schemas";
    }

    @Override
    public void setUp() throws SetupException {
        // No setup needed
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        this.resourceAccessor = resourceAccessor;
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
