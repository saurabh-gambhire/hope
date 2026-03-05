package com.hope.master_service.tenant;

import com.hope.master_service.constants.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Routing data source that switches the PostgreSQL schema
 * based on the current tenant from TenantContext.
 *
 * Uses a single database connection pool but sets the schema
 * on each connection before returning it.
 */
@Slf4j
public class TenantDataSource extends AbstractRoutingDataSource {

    @Override
    protected String determineCurrentLookupKey() {
        return Constant.DEFAULT;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = getResolvedDefaultDataSource().getConnection();
        String schema = TenantContext.getCurrentTenant();
        connection.setSchema(schema);
        log.trace("Connection schema set to: {}", schema);
        return connection;
    }
}
