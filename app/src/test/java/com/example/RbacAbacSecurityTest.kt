package com.example

import com.example.data.AccessPermission
import com.example.data.ActiveRoleSession
import com.example.data.ApprovalRequestRecord
import com.example.data.ApprovalStatus
import com.example.data.AtomicPermissionOverride
import com.example.data.AtomicPermissionRegistry
import com.example.data.AuthorizationDecision
import com.example.data.AuthorizationEngine
import com.example.data.EmployeeRecord
import com.example.data.ManagerAccessPolicy
import com.example.data.ManagerPermissionProfile
import com.example.data.OrderStatus
import com.example.data.PermissionOverride
import com.example.data.PermissionOverrideEffect
import com.example.data.PressingOrder
import com.example.data.RolePermissionMatrix
import com.example.data.SensitiveOperationType
import com.example.data.SubscriptionPlan
import com.example.data.SupportAccessSession
import com.example.data.SystemRoleTier
import com.example.data.TenantAuthorizationContext
import com.example.data.TenantDataScoper
import com.example.data.WorkspaceRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated Verification Suite covering ALL 30 Mandatory Security & RBAC+ABAC Scenarios
 * across 2 isolated test organizations (Pressing Élite CI & Blanchisserie Prestige Dakar),
 * multiple branches (Cocody, Yopougon, Anyama, Plateau), and multiple user roles.
 */
class RbacAbacSecurityTest {

    // --- Test Organization A (Abidjan) & Organization B (Dakar) ---
    private val orgA = "org_pressing_elite_ci"
    private val orgB = "org_blanchisserie_prestige_sn"

    private val managerCocodyCtx = TenantAuthorizationContext(
        userId = "usr_awa_cocody",
        activeOrganizationId = orgA,
        targetOrganizationId = orgA,
        subscriptionPlan = SubscriptionPlan.BUSINESS,
        subscriptionStatus = "ACTIVE",
        roleTier = SystemRoleTier.PRESSING_MANAGER,
        workspaceRole = WorkspaceRole.CAISSIER,
        assignedBranches = setOf("Cocody"),
        targetBranch = "Cocody",
        currentHour24 = 11
    )

    private val adminOrgACtx = TenantAuthorizationContext(
        userId = "usr_kouassi_admin_a",
        activeOrganizationId = orgA,
        targetOrganizationId = orgA,
        subscriptionPlan = SubscriptionPlan.ENTERPRISE,
        subscriptionStatus = "ACTIVE",
        roleTier = SystemRoleTier.PRESSING_ADMIN,
        workspaceRole = WorkspaceRole.DIRECTEUR,
        assignedBranches = setOf("Cocody", "Yopougon", "Anyama"),
        targetBranch = "Cocody"
    )

    private val superAdminCtx = TenantAuthorizationContext(
        userId = "usr_super_admin",
        activeOrganizationId = "platform",
        targetOrganizationId = orgA,
        subscriptionPlan = SubscriptionPlan.ENTERPRISE,
        subscriptionStatus = "ACTIVE",
        roleTier = SystemRoleTier.SUPER_ADMIN,
        workspaceRole = WorkspaceRole.DIRECTEUR
    )

    private val cashierSession = ActiveRoleSession(
        role = WorkspaceRole.CAISSIER,
        operatorName = "Awa Koné",
        isolatedAgency = "Cocody",
        permissions = RolePermissionMatrix.defaultPermissionsFor(WorkspaceRole.CAISSIER),
        isSuperAdmin = false
    )

    private val directorSession = ActiveRoleSession(
        role = WorkspaceRole.DIRECTEUR,
        operatorName = "Kouassi Yao",
        isolatedAgency = null,
        permissions = AccessPermission.entries.toSet(),
        isSuperAdmin = false
    )

    private val defaultCashierPolicy = ManagerAccessPolicy(
        employeeId = "emp_awa",
        employeeName = "Awa Koné",
        profileCode = ManagerPermissionProfile.CAISSIER.name,
        allowedAgencies = listOf("Cocody"),
        maxDiscountPercent = 5,
        maxRefundFcfa = 5_000L,
        maxExpenseFcfa = 10_000L,
        maxDisbursementFcfa = 20_000L,
        dailyOperationCapFcfa = 50_000L,
        accessStartHour = 7,
        accessEndHour = 21,
        active = true
    )

    @Test
    fun registryContainsOver100UniqueAtomicPermissions() {
        assertTrue(
            "Registry must define at least 100 atomic permissions, found ${AtomicPermissionRegistry.allPermissions.size}",
            AtomicPermissionRegistry.allPermissions.size >= 100
        )
        assertEquals(
            "Every atomic permission code must be unique",
            AtomicPermissionRegistry.allPermissions.size,
            AtomicPermissionRegistry.byCode.size
        )
    }

    // Scenarios 1 & 2: Manager cannot become Super-Admin nor modify own permissions
    @Test
    fun s01_s02_managerCannotBecomeSuperAdminOrModifyOwnPermissions() {
        assertFalse(cashierSession.canAssignRole(WorkspaceRole.DIRECTEUR))
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(
                ctx = managerCocodyCtx,
                permissionCode = "users.assign_permissions",
                profile = ManagerPermissionProfile.POLYVALENT
            ) is AuthorizationDecision.Denied
        )
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(
                ctx = managerCocodyCtx,
                permissionCode = "platform.organizations.create",
                profile = ManagerPermissionProfile.POLYVALENT
            ) is AuthorizationDecision.Denied
        )
    }

    // Scenarios 3 & 4: Manager cannot access another enterprise nor an unassigned branch
    @Test
    fun s03_s04_managerCannotAccessOtherEnterpriseOrUnassignedBranch() {
        val crossTenant = AtomicPermissionRegistry.checkAtomicAccess(
            ctx = managerCocodyCtx.copy(targetOrganizationId = orgB),
            permissionCode = "orders.view_branch",
            profile = ManagerPermissionProfile.CAISSIER
        )
        assertTrue(crossTenant is AuthorizationDecision.Denied)

        val crossBranch = AtomicPermissionRegistry.checkAtomicAccess(
            ctx = managerCocodyCtx.copy(targetBranch = "Yopougon"),
            permissionCode = "orders.view_branch",
            profile = ManagerPermissionProfile.CAISSIER
        )
        assertTrue(crossBranch is AuthorizationDecision.Denied)
    }

    // Scenarios 5 & 6: Pressing Admin cannot create Super-Admin nor manage another enterprise
    @Test
    fun s05_s06_adminCannotCreateSuperAdminOrManageOtherEnterprise() {
        val platformPerm = AtomicPermissionRegistry.checkAtomicAccess(
            ctx = adminOrgACtx,
            permissionCode = "platform.plans.manage"
        )
        assertTrue(platformPerm is AuthorizationDecision.Denied)

        val otherCompany = AtomicPermissionRegistry.checkAtomicAccess(
            ctx = adminOrgACtx.copy(targetOrganizationId = orgB),
            permissionCode = "orders.view_all"
        )
        assertTrue(otherCompany is AuthorizationDecision.Denied)
    }

    // Scenario 7: User cannot access a module/feature outside their subscription plan
    @Test
    fun s07_userCannotAccessUnsubscribedPlanModule() {
        val starterAdmin = adminOrgACtx.copy(subscriptionPlan = SubscriptionPlan.STARTER)
        // Multi-agency branch creation requires ENTERPRISE
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(starterAdmin, "organization.branches.create") is AuthorizationDecision.Denied
        )
        // SYSCOHADA accounting requires BUSINESS+
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(starterAdmin, "accounting.view") is AuthorizationDecision.Denied
        )
    }

    // Scenarios 8, 9, 10, 11, 12: Direct route/API/export/photo access strictly enforces scope & permissions
    @Test
    fun s08_to_s12_directNavigationExportAndPrivatePhotoRequireExplicitPermission() {
        // Direct route check for unauthorized cashier trying to open finance/saas_admin/roles_rbac
        assertFalse(cashierSession.canAccessRoute("finance"))
        assertFalse(cashierSession.canAccessRoute("saas_admin"))
        assertFalse(cashierSession.canAccessRoute("roles_rbac"))

        // Export forbidden to manager (non-delegable customers.export & reports.export)
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(
                managerCocodyCtx,
                "customers.export",
                profile = ManagerPermissionProfile.POLYVALENT
            ) is AuthorizationDecision.Denied
        )

        // Private garment photo denied to driver without garments.view_photo
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(
                managerCocodyCtx.copy(workspaceRole = WorkspaceRole.LIVREUR),
                "garments.view_photo",
                profile = ManagerPermissionProfile.LIVREUR
            ) is AuthorizationDecision.Denied
        )

        // TenantDataScoper isolates orders by agency and strips financial amounts for Workshop Operator
        val orders = listOf(
            PressingOrder(id = "1", agencyName = "Cocody", totalAmount = 12_000L, status = OrderStatus.WASHING.name),
            PressingOrder(id = "2", agencyName = "Yopougon", totalAmount = 25_000L, status = OrderStatus.WASHING.name)
        )
        val operatorSession = ActiveRoleSession(
            role = WorkspaceRole.OPERATEUR,
            operatorName = "Moussa",
            isolatedAgency = "Cocody",
            permissions = RolePermissionMatrix.defaultPermissionsFor(WorkspaceRole.OPERATEUR)
        )
        val scoped = TenantDataScoper.scopeOrders(orders, operatorSession)
        assertEquals(1, scoped.size)
        assertEquals("Cocody", scoped.first().agencyName)
        assertEquals(0L, scoped.first().totalAmount)
    }

    // Scenarios 13, 14, 23: Discount ceiling, refund ceiling, and anti-splitting concurrent cap
    @Test
    fun s13_s14_s23_financialCeilingsAndConcurrentAntiSplitting() {
        val withinRefund = AuthorizationEngine.checkFinancialLimit(
            session = cashierSession,
            operation = SensitiveOperationType.REFUND,
            amountFcfa = 4_000L,
            policy = defaultCashierPolicy
        )
        assertTrue(withinRefund is AuthorizationDecision.Allowed)

        val overRefund = AuthorizationEngine.checkFinancialLimit(
            session = cashierSession,
            operation = SensitiveOperationType.REFUND,
            amountFcfa = 15_000L,
            policy = defaultCashierPolicy
        )
        assertTrue(overRefund is AuthorizationDecision.RequiresApproval)

        val overDiscount = AuthorizationEngine.checkFinancialLimit(
            session = cashierSession,
            operation = SensitiveOperationType.EXCEPTIONAL_DISCOUNT,
            amountFcfa = 2_000L,
            discountPercent = 12.0,
            policy = defaultCashierPolicy
        )
        assertTrue(overDiscount is AuthorizationDecision.RequiresApproval)

        // Anti-splitting cumulative daily cap guard
        val splittingAttempt = AuthorizationEngine.checkFinancialLimit(
            session = cashierSession,
            operation = SensitiveOperationType.REFUND,
            amountFcfa = 4_000L,
            dailyCumulativeFcfa = 48_000L,
            policy = defaultCashierPolicy
        )
        assertTrue(splittingAttempt is AuthorizationDecision.RequiresApproval)
    }

    // Scenarios 15, 16, 26, 30: Approval workflow — unapproved blocked, single execution, separation of duties, hash integrity
    @Test
    fun s15_s16_s26_s30_approvalWorkflowSeparationOfDutiesAndSingleExecution() {
        val hash = AuthorizationEngine.computePayloadHash(
            SensitiveOperationType.REFUND.name,
            "COM-001248",
            15_000L,
            "Cocody"
        )
        val pendingReq = ApprovalRequestRecord(
            id = "req_1",
            agencyName = "Cocody",
            requestedBy = "Awa Koné",
            requestedByRole = "Caissier",
            operationType = SensitiveOperationType.REFUND.name,
            resourceRef = "COM-001248",
            amountFcfa = 15_000L,
            ceilingFcfa = 5_000L,
            reason = "Dédommagement tache",
            payloadHash = hash,
            status = ApprovalStatus.PENDING.name,
            expiresAtMillis = System.currentTimeMillis() + 3_600_000L
        )

        assertTrue(AuthorizationEngine.validateApprovalReview(cashierSession, pendingReq) is AuthorizationDecision.Denied)
        assertTrue(AuthorizationEngine.validateApprovalReview(directorSession, pendingReq.copy(requestedBy = directorSession.operatorName)) is AuthorizationDecision.Denied)
        assertTrue(AuthorizationEngine.validateApprovalReview(directorSession, pendingReq.copy(amountFcfa = 95_000L)) is AuthorizationDecision.Denied)
        assertTrue(AuthorizationEngine.validateApprovalReview(directorSession, pendingReq) is AuthorizationDecision.Allowed)
        assertTrue(AuthorizationEngine.canExecuteApprovedRequest(pendingReq) is AuthorizationDecision.Denied)

        val approvedReq = pendingReq.copy(status = ApprovalStatus.APPROVED.name)
        assertTrue(AuthorizationEngine.canExecuteApprovedRequest(approvedReq) is AuthorizationDecision.Allowed)

        val executedReq = approvedReq.copy(status = ApprovalStatus.EXECUTED.name, executedAtMillis = System.currentTimeMillis())
        assertTrue(AuthorizationEngine.canExecuteApprovedRequest(executedReq) is AuthorizationDecision.Denied)
    }

    // Scenarios 17, 18, 19, 20: Permission revocation takes effect, revoked session fails, suspended org blocked, audit protected
    @Test
    fun s17_s18_s19_s20_revocationSuspendedCompanyAndAuditProtection() {
        // Revoked session immediately rejected
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(
                managerCocodyCtx.copy(sessionRevoked = true),
                "orders.create",
                profile = ManagerPermissionProfile.CAISSIER
            ) is AuthorizationDecision.Denied
        )
        // Suspended organization immediately rejected
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(
                adminOrgACtx.copy(organizationStatus = "SUSPENDED"),
                "orders.create"
            ) is AuthorizationDecision.Denied
        )
        // Manager cannot view or tamper with organization audit logs
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(
                managerCocodyCtx,
                "audit.view_organization",
                profile = ManagerPermissionProfile.POLYVALENT
            ) is AuthorizationDecision.Denied
        )
    }

    // Scenarios 21 & 22: Explicit DENY prevails over inherited permission; temporary permission expires cleanly
    @Test
    fun s21_s22_explicitDenyPrecedenceAndTemporaryPermissionExpiration() {
        val now = 1_700_000_000_000L
        val denyOverride = listOf(
            AtomicPermissionOverride("payments.create", PermissionOverrideEffect.DENY, reason = "Audit en cours")
        )
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(
                ctx = managerCocodyCtx.copy(nowMillis = now),
                permissionCode = "payments.create",
                profile = ManagerPermissionProfile.CAISSIER,
                overrides = denyOverride
            ) is AuthorizationDecision.Denied
        )

        val tempAllow = listOf(
            AtomicPermissionOverride("production.quality_check", PermissionOverrideEffect.ALLOW, validUntilMillis = now + 3_600_000L)
        )
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(
                ctx = managerCocodyCtx.copy(nowMillis = now + 500L),
                permissionCode = "production.quality_check",
                profile = ManagerPermissionProfile.CAISSIER,
                overrides = tempAllow
            ) is AuthorizationDecision.Allowed
        )
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(
                ctx = managerCocodyCtx.copy(nowMillis = now + 7_200_000L),
                permissionCode = "production.quality_check",
                profile = ManagerPermissionProfile.CAISSIER,
                overrides = tempAllow
            ) is AuthorizationDecision.Denied
        )
    }

    // Scenario 24: Cannot delete or deactivate the last active owner/Director
    @Test
    fun s24_cannotDeleteLastActiveOwner() {
        val staffSingleOwner = listOf(
            EmployeeRecord(id = "dir1", fullName = "Kouassi Yao", role = "DIRECTEUR", active = true),
            EmployeeRecord(id = "cais1", fullName = "Awa Koné", role = "CAISSIER", active = true)
        )
        assertTrue(AtomicPermissionRegistry.canRemoveOrDeactivateEmployee(staffSingleOwner, "dir1") is AuthorizationDecision.Denied)
        assertTrue(AtomicPermissionRegistry.canRemoveOrDeactivateEmployee(staffSingleOwner, "cais1") is AuthorizationDecision.Allowed)

        val staffTwoOwners = staffSingleOwner + EmployeeRecord(id = "dir2", fullName = "Mariam Traoré", role = "DIRECTEUR", active = true)
        assertTrue(AtomicPermissionRegistry.canRemoveOrDeactivateEmployee(staffTwoOwners, "dir1") is AuthorizationDecision.Allowed)
    }

    // Scenario 25: Super-Admin restricted from tenant commercial mutations; expired/revoked support session rejected
    @Test
    fun s25_superAdminDefaultRestrictionAndSupportSessionExpiry() {
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(superAdminCtx, "orders.create") is AuthorizationDecision.Denied
        )
        assertTrue(
            AtomicPermissionRegistry.checkAtomicAccess(superAdminCtx, "platform.organizations.view") is AuthorizationDecision.Allowed
        )

        val now = 1_700_000_000_000L
        val session = SupportAccessSession(
            ticketId = "SUP-2026-44",
            superAdminName = "Super-Admin",
            targetCompanyName = "Pressing Élite CI",
            reason = "Diagnostic rapprochement caisse",
            startsAtMillis = now,
            expiresAtMillis = now + 1_800_000L,
            readOnly = true,
            revoked = false
        )
        assertTrue(session.isValidAt(now + 60_000L))
        assertFalse(session.isValidAt(now + 2_000_000L))
        assertFalse(session.copy(revoked = true).isValidAt(now + 60_000L))
    }

    // Scenarios 27, 28, 29: Org context switch isolates permissions; menus strictly match effective permissions
    @Test
    fun s27_s28_s29_contextSwitchIsolationAndDynamicMenuAlignment() {
        // Switching context to Org B while holding Org A active session denies access
        val switchedCtx = adminOrgACtx.copy(activeOrganizationId = orgA, targetOrganizationId = orgB)
        assertTrue(AtomicPermissionRegistry.checkAtomicAccess(switchedCtx, "orders.create") is AuthorizationDecision.Denied)

        // Dynamic menu alignment: workshop operator never receives financial/admin routes
        val workshopSession = ActiveRoleSession(
            role = WorkspaceRole.OPERATEUR,
            operatorName = "Yao Serge",
            isolatedAgency = "Cocody",
            permissions = RolePermissionMatrix.defaultPermissionsFor(WorkspaceRole.OPERATEUR)
        )
        assertTrue(workshopSession.canAccessRoute("workshop_tracker"))
        assertFalse(workshopSession.canAccessRoute("finance"))
        assertFalse(workshopSession.canAccessRoute("pos_cash"))
        assertFalse(workshopSession.canAccessRoute("staff"))
        assertFalse(workshopSession.canAccessRoute("saas_admin"))
    }
}
