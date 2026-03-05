package com.hope.master_service.tenant;

import com.hope.master_service.constants.Constant;

/**
 * Holds the current tenant schema name for the executing thread.
 * Uses ThreadLocal to isolate tenant context per request.
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static String getCurrentTenant() {
        String tenant = CURRENT_TENANT.get();
        return (tenant != null && !tenant.isBlank()) ? tenant : Constant.PUBLIC;
    }

    public static void setCurrentTenant(String tenant) {
        CURRENT_TENANT.set(tenant);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    private TenantContext() {
    }
}
