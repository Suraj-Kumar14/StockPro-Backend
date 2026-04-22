package com.stockpro.config;

/**
 * Role Constants for Inventory Management System
 *
 * ROLE HIERARCHY & PERMISSIONS:
 * =============================
 *
 * 1. ADMIN
 *    - Full system access
 *    - Can view all endpoints
 *    - Can deactivate/manage user accounts
 *    - Can access /auth/admin/* endpoints
 *    - Can access all other modules
 *
 * 2. INVENTORY_MANAGER
 *    - Manage inventory module
 *    - Can access /inventory/* endpoints
 *    - Cannot access warehouse or purchase modules
 *    - Regular user operations allowed (/auth/user/*)
 *
 * 3. WAREHOUSE_STAFF (DEFAULT ROLE)
 *    - Manage warehouse operations
 *    - Can access /warehouse/* endpoints
 *    - Cannot access inventory or purchase modules
 *    - Regular user operations allowed (/auth/user/*)
 *    - Default role for new users (both email and OAuth2 registration)
 *
 * 4. PURCHASE_OFFICER
 *    - Manage purchase operations
 *    - Can access /purchase/* endpoints
 *    - Cannot access inventory or warehouse modules
 *    - Regular user operations allowed (/auth/user/*)
 *
 * ROLE ASSIGNMENT FLOW:
 * ====================
 * 1. New user registers with email → WAREHOUSE_STAFF role
 * 2. New user logs in via Google OAuth2 → WAREHOUSE_STAFF role
 * 3. Only ADMIN can promote users to different roles
 * 4. Admin can demote users or deactivate accounts
 *
 * SECURITY CONFIG REFERENCE (see SecurityConfig.java):
 * - "/auth/admin/**" → hasRole("ADMIN")
 * - "/inventory/**" → hasAnyRole("ADMIN", "INVENTORY_MANAGER")
 * - "/warehouse/**" → hasAnyRole("ADMIN", "WAREHOUSE_STAFF")
 * - "/purchase/**" → hasAnyRole("ADMIN", "PURCHASE_OFFICER")
 * - "/auth/user/**" → authenticated (all roles)
 */
public class Roles {
    public static final String ADMIN = "ADMIN";
    public static final String INVENTORY_MANAGER = "INVENTORY_MANAGER";
    public static final String WAREHOUSE_STAFF = "WAREHOUSE_STAFF";
    public static final String PURCHASE_OFFICER = "PURCHASE_OFFICER";

    private Roles() {
        // Prevent instantiation
    }
}
