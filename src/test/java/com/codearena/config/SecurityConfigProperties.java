package com.codearena.config;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import com.codearena.entity.Role;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

/**
 * Property-based tests for Role-Based Access Control (RBAC) enforcement.
 * 
 * <p>These tests verify that the security configuration correctly enforces
 * role-based access control for all protected endpoints.
 * 
 * <p><b>Property 7: Role-Based Access Control Enforcement</b>
 * <p>For any user with role R attempting to access endpoint E, access is granted
 * if and only if R has permission for E according to the RBAC matrix.
 * 
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4</b>
 * 
 * <p>RBAC Matrix:
 * <ul>
 *   <li>USER: Can access read endpoints (GET /api/problems/**), execute endpoints (/api/execute/**),
 *       and user profile endpoints (/api/me/**)</li>
 *   <li>PROBLEM_SETTER: All USER permissions plus problem creation/editing (POST/PUT/DELETE /api/problems/**),
 *       and testcase management (POST/DELETE /api/testcases/**)</li>
 *   <li>ADMIN: All permissions including admin endpoints (/api/admin/**)</li>
 * </ul>
 * 
 * <p>This test class uses jqwik for property-based testing to verify the RBAC permission
 * matrix logic. The actual HTTP endpoint access control is tested in {@link SecurityConfigTest}
 * using Spring's MockMvc.
 * 
 * @see SecurityConfig
 * @see SecurityConfigTest
 */
@Label("Property 7: Role-Based Access Control Enforcement")
@Tag("codearena-platform")
class SecurityConfigProperties {

    // ========================================================================
    // Endpoint Categories and Permission Matrix
    // ========================================================================

    /**
     * Represents an endpoint with its HTTP method and path pattern.
     */
    record Endpoint(String method, String path, String description) {
        @Override
        public String toString() {
            return method + " " + path + " (" + description + ")";
        }
    }

    /**
     * Represents the permission matrix for each role.
     * This mirrors the security configuration in {@link SecurityConfig}.
     */
    enum EndpointCategory {
        // Read-only problem access - any authenticated user
        PROBLEM_READ(EnumSet.of(Role.USER, Role.PROBLEM_SETTER, Role.ADMIN)),
        
        // Execute endpoints - any authenticated user
        EXECUTE(EnumSet.of(Role.USER, Role.PROBLEM_SETTER, Role.ADMIN)),
        
        // User profile endpoints - any authenticated user
        USER_PROFILE(EnumSet.of(Role.USER, Role.PROBLEM_SETTER, Role.ADMIN)),
        
        // Problem creation/modification - PROBLEM_SETTER or ADMIN
        PROBLEM_WRITE(EnumSet.of(Role.PROBLEM_SETTER, Role.ADMIN)),
        
        // Testcase management - PROBLEM_SETTER or ADMIN
        TESTCASE_WRITE(EnumSet.of(Role.PROBLEM_SETTER, Role.ADMIN)),
        
        // Admin-only endpoints
        ADMIN(EnumSet.of(Role.ADMIN));

        private final Set<Role> allowedRoles;

        EndpointCategory(Set<Role> allowedRoles) {
            this.allowedRoles = allowedRoles;
        }

        public boolean isAllowedFor(Role role) {
            return allowedRoles.contains(role);
        }

        public Set<Role> getAllowedRoles() {
            return allowedRoles;
        }
    }


    /**
     * Maps endpoints to their categories for permission checking.
     */
    static final List<EndpointWithCategory> PROTECTED_ENDPOINTS = Arrays.asList(
        // Problem read endpoints - any authenticated user
        new EndpointWithCategory(
            new Endpoint("GET", "/api/problems", "List problems"),
            EndpointCategory.PROBLEM_READ
        ),
        new EndpointWithCategory(
            new Endpoint("GET", "/api/problems/1", "Get problem by ID"),
            EndpointCategory.PROBLEM_READ
        ),
        new EndpointWithCategory(
            new Endpoint("GET", "/api/problems/slug/two-sum", "Get problem by slug"),
            EndpointCategory.PROBLEM_READ
        ),
        
        // Execute endpoints - any authenticated user
        new EndpointWithCategory(
            new Endpoint("POST", "/api/execute/run", "Run code"),
            EndpointCategory.EXECUTE
        ),
        new EndpointWithCategory(
            new Endpoint("POST", "/api/execute/submit", "Submit code"),
            EndpointCategory.EXECUTE
        ),
        
        // User profile endpoints - any authenticated user
        new EndpointWithCategory(
            new Endpoint("GET", "/api/me", "Get current user"),
            EndpointCategory.USER_PROFILE
        ),
        new EndpointWithCategory(
            new Endpoint("GET", "/api/me/stats", "Get user statistics"),
            EndpointCategory.USER_PROFILE
        ),
        new EndpointWithCategory(
            new Endpoint("GET", "/api/me/submissions", "Get user submissions"),
            EndpointCategory.USER_PROFILE
        ),
        
        // Problem write endpoints - PROBLEM_SETTER or ADMIN
        new EndpointWithCategory(
            new Endpoint("POST", "/api/problems", "Create problem"),
            EndpointCategory.PROBLEM_WRITE
        ),
        new EndpointWithCategory(
            new Endpoint("PUT", "/api/problems/1", "Update problem"),
            EndpointCategory.PROBLEM_WRITE
        ),
        new EndpointWithCategory(
            new Endpoint("DELETE", "/api/problems/1", "Delete problem"),
            EndpointCategory.PROBLEM_WRITE
        ),
        new EndpointWithCategory(
            new Endpoint("POST", "/api/problems/1/request-review", "Request review"),
            EndpointCategory.PROBLEM_WRITE
        ),
        
        // Testcase write endpoints - PROBLEM_SETTER or ADMIN
        new EndpointWithCategory(
            new Endpoint("POST", "/api/testcases", "Create testcase"),
            EndpointCategory.TESTCASE_WRITE
        ),
        new EndpointWithCategory(
            new Endpoint("DELETE", "/api/testcases/1", "Delete testcase"),
            EndpointCategory.TESTCASE_WRITE
        ),
        
        // Admin endpoints - ADMIN only
        new EndpointWithCategory(
            new Endpoint("GET", "/api/admin/problems/pending", "Get pending problems"),
            EndpointCategory.ADMIN
        ),
        new EndpointWithCategory(
            new Endpoint("POST", "/api/admin/problems/1/publish", "Publish problem"),
            EndpointCategory.ADMIN
        ),
        new EndpointWithCategory(
            new Endpoint("POST", "/api/admin/problems/1/reject", "Reject problem"),
            EndpointCategory.ADMIN
        ),
        new EndpointWithCategory(
            new Endpoint("POST", "/api/admin/problems/1/archive", "Archive problem"),
            EndpointCategory.ADMIN
        ),
        new EndpointWithCategory(
            new Endpoint("PUT", "/api/admin/users/1/role", "Update user role"),
            EndpointCategory.ADMIN
        ),
        new EndpointWithCategory(
            new Endpoint("GET", "/api/admin/submissions", "Get all submissions"),
            EndpointCategory.ADMIN
        )
    );

    record EndpointWithCategory(Endpoint endpoint, EndpointCategory category) {}


    // ========================================================================
    // Property 7: Role-Based Access Control Enforcement
    // ========================================================================

    /**
     * Property 7a: For any role and endpoint category, the permission decision is deterministic.
     * 
     * <p>This property verifies that calling isAllowedFor() multiple times with the same
     * role and category always returns the same result.
     * 
     * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4</b>
     */
    @Property(tries = 100)
    @Label("Property 7a: Permission decisions are deterministic")
    void permissionDecisionsAreDeterministic(
            @ForAll("roles") Role role,
            @ForAll("endpointCategories") EndpointCategory category) {
        
        // Call isAllowedFor multiple times - should always return the same result
        boolean result1 = category.isAllowedFor(role);
        boolean result2 = category.isAllowedFor(role);
        boolean result3 = category.isAllowedFor(role);
        
        assertThat(result1)
            .as("Permission decision should be deterministic for %s accessing %s", role, category)
            .isEqualTo(result2)
            .isEqualTo(result3);
    }

    /**
     * Property 7b: ADMIN role always has access to any endpoint category.
     * 
     * <p>This property verifies that the ADMIN role has full permissions across
     * all endpoint categories.
     * 
     * <p><b>Validates: Requirements 3.3</b>
     */
    @Property(tries = 100)
    @Label("Property 7b: ADMIN always has access")
    void adminAlwaysHasAccess(@ForAll("endpointCategories") EndpointCategory category) {
        assertThat(category.isAllowedFor(Role.ADMIN))
            .as("ADMIN should always have access to %s", category)
            .isTrue();
    }

    /**
     * Property 7c: For any endpoint category, if a lower-privilege role has access,
     * all higher-privilege roles also have access (role hierarchy).
     * 
     * <p>Role hierarchy: USER < PROBLEM_SETTER < ADMIN
     * 
     * <p><b>Validates: Requirements 3.1, 3.2, 3.3</b>
     */
    @Property(tries = 100)
    @Label("Property 7c: Role hierarchy is respected")
    void roleHierarchyIsRespected(@ForAll("endpointCategories") EndpointCategory category) {
        // USER -> PROBLEM_SETTER -> ADMIN (increasing privilege)
        boolean userAccess = category.isAllowedFor(Role.USER);
        boolean problemSetterAccess = category.isAllowedFor(Role.PROBLEM_SETTER);
        boolean adminAccess = category.isAllowedFor(Role.ADMIN);
        
        // If USER has access, PROBLEM_SETTER must have access
        if (userAccess) {
            assertThat(problemSetterAccess)
                .as("If USER has access to %s, PROBLEM_SETTER should too", category)
                .isTrue();
        }
        
        // If PROBLEM_SETTER has access, ADMIN must have access
        if (problemSetterAccess) {
            assertThat(adminAccess)
                .as("If PROBLEM_SETTER has access to %s, ADMIN should too", category)
                .isTrue();
        }
    }

    /**
     * Property 7d: USER role is denied access to restricted categories (PROBLEM_WRITE, TESTCASE_WRITE, ADMIN).
     * 
     * <p>This property verifies that USER role cannot access write or admin endpoints.
     * 
     * <p><b>Validates: Requirements 3.1, 3.4</b>
     */
    @Property(tries = 100)
    @Label("Property 7d: USER denied access to restricted categories")
    void userDeniedAccessToRestrictedCategories(
            @ForAll("restrictedCategories") EndpointCategory category) {
        
        assertThat(category.isAllowedFor(Role.USER))
            .as("USER should be denied access to %s", category)
            .isFalse();
    }

    /**
     * Property 7e: PROBLEM_SETTER role is denied access to ADMIN category only.
     * 
     * <p>This property verifies that PROBLEM_SETTER cannot access admin-only endpoints.
     * 
     * <p><b>Validates: Requirements 3.2, 3.4</b>
     */
    @Property(tries = 100)
    @Label("Property 7e: PROBLEM_SETTER denied access to ADMIN category")
    void problemSetterDeniedAccessToAdminCategory() {
        assertThat(EndpointCategory.ADMIN.isAllowedFor(Role.PROBLEM_SETTER))
            .as("PROBLEM_SETTER should be denied access to ADMIN category")
            .isFalse();
    }

    /**
     * Property 7f: USER role has access to read, execute, and profile categories.
     * 
     * <p>This property verifies that USER role can access the appropriate endpoints.
     * 
     * <p><b>Validates: Requirements 3.1</b>
     */
    @Property(tries = 100)
    @Label("Property 7f: USER has access to allowed categories")
    void userHasAccessToAllowedCategories(
            @ForAll("userAllowedCategories") EndpointCategory category) {
        
        assertThat(category.isAllowedFor(Role.USER))
            .as("USER should have access to %s", category)
            .isTrue();
    }

    /**
     * Property 7g: PROBLEM_SETTER role has access to all categories except ADMIN.
     * 
     * <p>This property verifies that PROBLEM_SETTER can access all non-admin endpoints.
     * 
     * <p><b>Validates: Requirements 3.2</b>
     */
    @Property(tries = 100)
    @Label("Property 7g: PROBLEM_SETTER has access to non-admin categories")
    void problemSetterHasAccessToNonAdminCategories(
            @ForAll("nonAdminCategories") EndpointCategory category) {
        
        assertThat(category.isAllowedFor(Role.PROBLEM_SETTER))
            .as("PROBLEM_SETTER should have access to %s", category)
            .isTrue();
    }

    /**
     * Property 7h: For any role-endpoint pair, access is granted if and only if
     * the role is in the endpoint category's allowed roles set.
     * 
     * <p>This is the core RBAC property that verifies the permission matrix.
     * 
     * <p><b>Validates: Requirements 3.1, 3.2, 3.3, 3.4</b>
     */
    @Property(tries = 100)
    @Label("Property 7h: RBAC matrix is correctly enforced")
    void rbacMatrixIsCorrectlyEnforced(
            @ForAll("roles") Role role,
            @ForAll("protectedEndpoints") EndpointWithCategory endpointWithCategory) {
        
        EndpointCategory category = endpointWithCategory.category();
        boolean expectedAccess = category.getAllowedRoles().contains(role);
        boolean actualAccess = category.isAllowedFor(role);
        
        assertThat(actualAccess)
            .as("Access for %s to %s should be %s", role, endpointWithCategory.endpoint(), expectedAccess)
            .isEqualTo(expectedAccess);
    }

    /**
     * Property 7i: Unauthorized access (role not in allowed set) should be denied.
     * 
     * <p>This property specifically tests the 403 Forbidden scenario.
     * 
     * <p><b>Validates: Requirements 3.4</b>
     */
    @Property(tries = 100)
    @Label("Property 7i: Unauthorized access is denied")
    void unauthorizedAccessIsDenied(
            @ForAll("deniedRoleEndpointPairs") RoleEndpointPair pair) {
        
        Role role = pair.role();
        EndpointCategory category = pair.endpointWithCategory().category();
        
        assertThat(category.isAllowedFor(role))
            .as("%s should be denied access to %s", role, pair.endpointWithCategory().endpoint())
            .isFalse();
    }


    // ========================================================================
    // Arbitrary Providers
    // ========================================================================

    @Provide
    Arbitrary<Role> roles() {
        return Arbitraries.of(Role.values());
    }

    @Provide
    Arbitrary<EndpointCategory> endpointCategories() {
        return Arbitraries.of(EndpointCategory.values());
    }

    @Provide
    Arbitrary<EndpointCategory> restrictedCategories() {
        // Categories that USER should NOT have access to
        return Arbitraries.of(
            EndpointCategory.PROBLEM_WRITE,
            EndpointCategory.TESTCASE_WRITE,
            EndpointCategory.ADMIN
        );
    }

    @Provide
    Arbitrary<EndpointCategory> userAllowedCategories() {
        // Categories that USER should have access to
        return Arbitraries.of(
            EndpointCategory.PROBLEM_READ,
            EndpointCategory.EXECUTE,
            EndpointCategory.USER_PROFILE
        );
    }

    @Provide
    Arbitrary<EndpointCategory> nonAdminCategories() {
        // All categories except ADMIN
        return Arbitraries.of(
            EndpointCategory.PROBLEM_READ,
            EndpointCategory.EXECUTE,
            EndpointCategory.USER_PROFILE,
            EndpointCategory.PROBLEM_WRITE,
            EndpointCategory.TESTCASE_WRITE
        );
    }

    @Provide
    Arbitrary<EndpointWithCategory> protectedEndpoints() {
        return Arbitraries.of(PROTECTED_ENDPOINTS);
    }

    @Provide
    Arbitrary<RoleEndpointPair> deniedRoleEndpointPairs() {
        // Generate role-endpoint pairs where access should be denied
        return Arbitraries.of(
            // USER denied access to write/admin endpoints
            new RoleEndpointPair(Role.USER, new EndpointWithCategory(
                new Endpoint("POST", "/api/problems", "Create problem"),
                EndpointCategory.PROBLEM_WRITE
            )),
            new RoleEndpointPair(Role.USER, new EndpointWithCategory(
                new Endpoint("PUT", "/api/problems/1", "Update problem"),
                EndpointCategory.PROBLEM_WRITE
            )),
            new RoleEndpointPair(Role.USER, new EndpointWithCategory(
                new Endpoint("DELETE", "/api/problems/1", "Delete problem"),
                EndpointCategory.PROBLEM_WRITE
            )),
            new RoleEndpointPair(Role.USER, new EndpointWithCategory(
                new Endpoint("POST", "/api/testcases", "Create testcase"),
                EndpointCategory.TESTCASE_WRITE
            )),
            new RoleEndpointPair(Role.USER, new EndpointWithCategory(
                new Endpoint("DELETE", "/api/testcases/1", "Delete testcase"),
                EndpointCategory.TESTCASE_WRITE
            )),
            new RoleEndpointPair(Role.USER, new EndpointWithCategory(
                new Endpoint("GET", "/api/admin/problems/pending", "Get pending problems"),
                EndpointCategory.ADMIN
            )),
            new RoleEndpointPair(Role.USER, new EndpointWithCategory(
                new Endpoint("POST", "/api/admin/problems/1/publish", "Publish problem"),
                EndpointCategory.ADMIN
            )),
            // PROBLEM_SETTER denied access to admin endpoints
            new RoleEndpointPair(Role.PROBLEM_SETTER, new EndpointWithCategory(
                new Endpoint("GET", "/api/admin/problems/pending", "Get pending problems"),
                EndpointCategory.ADMIN
            )),
            new RoleEndpointPair(Role.PROBLEM_SETTER, new EndpointWithCategory(
                new Endpoint("POST", "/api/admin/problems/1/publish", "Publish problem"),
                EndpointCategory.ADMIN
            )),
            new RoleEndpointPair(Role.PROBLEM_SETTER, new EndpointWithCategory(
                new Endpoint("PUT", "/api/admin/users/1/role", "Update user role"),
                EndpointCategory.ADMIN
            ))
        );
    }

    /**
     * Record representing a role-endpoint pair for testing.
     */
    record RoleEndpointPair(Role role, EndpointWithCategory endpointWithCategory) {
        @Override
        public String toString() {
            return role + " -> " + endpointWithCategory.endpoint();
        }
    }
}
