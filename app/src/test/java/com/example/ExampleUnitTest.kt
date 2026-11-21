package com.example

import com.example.core.formatMoney
import com.example.core.generateQrMatrix
import com.example.data.AuthErrorMapper
import com.example.data.AuthInputValidator
import com.example.data.AuthUserProfile
import com.example.data.OrderStatus
import com.example.data.PasswordStrengthLevel
import com.example.data.PaymentMethod
import com.example.data.PressingOrder
import com.example.data.StockItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun formatMoney_formatsFcfaCorrectly() {
        val formatted = formatMoney(12500L, "FCFA")
        assertTrue(formatted.endsWith("FCFA"))
        assertTrue(formatted.contains("12") && formatted.contains("500"))
    }

    @Test
    fun paymentMethod_mapsSyscohadaTreasuryAccounts() {
        assertEquals("571", PaymentMethod.ESPECES.syscohadaTreasuryAccount)
        assertEquals("5211", PaymentMethod.ORANGE_MONEY.syscohadaTreasuryAccount)
        assertEquals("5212", PaymentMethod.MTN_MOMO.syscohadaTreasuryAccount)
        assertEquals("5213", PaymentMethod.WAVE.syscohadaTreasuryAccount)
    }

    @Test
    fun pressingOrder_isLateDetectsOverdueActiveOrdersOnly() {
        val pastDue = System.currentTimeMillis() - 3_600_000L
        val futureDue = System.currentTimeMillis() + 3_600_000L

        val lateActive = PressingOrder(status = OrderStatus.WASHING.name, dueDateMillis = pastDue)
        val deliveredPast = PressingOrder(status = OrderStatus.DELIVERED.name, dueDateMillis = pastDue)
        val onTime = PressingOrder(status = OrderStatus.WASHING.name, dueDateMillis = futureDue)

        assertTrue(lateActive.isLate())
        assertFalse(deliveredPast.isLate())
        assertFalse(onTime.isLate())
    }

    @Test
    fun stockItem_isLowStockEvaluatesConsumablesOnly() {
        val lowDetergent = StockItem(itemKind = "CONSUMABLE", quantity = 3L, minThreshold = 5L)
        val okDetergent = StockItem(itemKind = "CONSUMABLE", quantity = 10L, minThreshold = 5L)
        val machine = StockItem(itemKind = "MACHINE", quantity = 1L, minThreshold = 2L)

        assertTrue(lowDetergent.isLowStock())
        assertFalse(okDetergent.isLowStock())
        assertFalse(machine.isLowStock())
    }

    @Test
    fun qrMatrix_hasDeterministicDimensionsAndFinderPatterns() {
        val m1 = generateQrMatrix("PRESS-2026-00001")
        val m2 = generateQrMatrix("PRESS-2026-00001")
        assertEquals(21, m1.size)
        assertEquals(21, m1[0].size)
        assertTrue(m1[0][0]) // top-left finder corner
        assertTrue(m1[0][20]) // top-right finder corner
        assertTrue(m1[20][0]) // bottom-left finder corner
        assertEquals(m1[10][10], m2[10][10])
    }

    @Test
    fun authInputValidator_validatesEmailsProperly() {
        assertNull(AuthInputValidator.validateEmail("directeur@monpressing.ci"))
        assertNotNull(AuthInputValidator.validateEmail(""))
        assertNotNull(AuthInputValidator.validateEmail("pas-un-email"))
        assertNotNull(AuthInputValidator.validateEmail("contact @pressing.ci"))
    }

    @Test
    fun authInputValidator_evaluatesPasswordStrength() {
        val weak = AuthInputValidator.evaluatePassword("abc")
        assertEquals(PasswordStrengthLevel.WEAK, weak.level)
        assertFalse(weak.isAcceptable)

        val strong = AuthInputValidator.evaluatePassword("Pressing2026")
        assertEquals(PasswordStrengthLevel.STRONG, strong.level)
        assertTrue(strong.isAcceptable)

        val veryStrong = AuthInputValidator.evaluatePassword("Pressing2026!")
        assertEquals(PasswordStrengthLevel.VERY_STRONG, veryStrong.level)
        assertTrue(veryStrong.isAcceptable)
    }

    @Test
    fun authInputValidator_validatesRegistrationRules() {
        val errors = AuthInputValidator.validateRegistration(
            fullName = "A",
            email = "invalide",
            password = "weak",
            confirmPassword = "other",
            acceptedTerms = false
        )
        assertEquals(5, errors.size)

        val ok = AuthInputValidator.validateRegistration(
            fullName = "Kouassi Yao",
            email = "kouassi@pressingpro.ci",
            password = "Pressing2026!",
            confirmPassword = "Pressing2026!",
            acceptedTerms = true
        )
        assertTrue(ok.isEmpty())
    }

    @Test
    fun authErrorMapper_translatesFirebaseCodesToFrench() {
        assertTrue(AuthErrorMapper.mapFirebaseErrorCode("ERROR_NETWORK_REQUEST_FAILED").contains("réseau", ignoreCase = true))
        assertTrue(AuthErrorMapper.mapFirebaseErrorCode("ERROR_OPERATION_NOT_ALLOWED").contains("Console Firebase"))
        assertTrue(AuthErrorMapper.mapFirebaseErrorCode("ERROR_USER_DISABLED").contains("désactivé"))
    }

    @Test
    fun authUserProfile_computesDisplayLabelAndInitials() {
        val full = AuthUserProfile(
            uid = "uid-123",
            displayName = "Kouassi Yao",
            email = "kouassi@pressing.ci",
            photoUrl = null,
            providerIds = listOf("google.com"),
            isEmailVerified = true,
            creationTimestamp = null,
            lastSignInTimestamp = null
        )
        assertEquals("Kouassi Yao", full.displayLabel)
        assertEquals("KY", full.initials)
        assertTrue(full.hasGoogleProvider)

        val fallback = full.copy(displayName = "", email = "admin@pressing.ci")
        assertEquals("admin", fallback.displayLabel)
        assertEquals("AD", fallback.initials)
    }

    @Test
    fun rolePermissionMatrix_grantsExpectedDefaultPrivileges() {
        val directorPerms = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.DIRECTEUR)
        assertEquals(com.example.data.AccessPermission.entries.size, directorPerms.size)

        val operatorPerms = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.OPERATEUR)
        assertTrue(com.example.data.AccessPermission.ADVANCE_PRODUCTION in operatorPerms)
        assertFalse(com.example.data.AccessPermission.VIEW_FINANCIAL_KPIS in operatorPerms)
        assertFalse(com.example.data.AccessPermission.VIEW_ACCOUNTING_SYSCOHADA in operatorPerms)
        assertFalse(com.example.data.AccessPermission.COLLECT_PAYMENT in operatorPerms)

        val qualityPerms = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.CONTROLE_QUALITE)
        assertTrue(com.example.data.AccessPermission.VALIDATE_QUALITY in qualityPerms)
        assertFalse(com.example.data.AccessPermission.VIEW_FINANCIAL_KPIS in qualityPerms)

        val driverPerms = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.LIVREUR)
        assertEquals(
            setOf(
                com.example.data.AccessPermission.VIEW_DELIVERIES,
                com.example.data.AccessPermission.DISPATCH_DELIVERIES
            ),
            driverPerms
        )
    }

    @Test
    fun tenantDataScoper_isolatesOrdersByAgencyAndDriver() {
        val orders = listOf(
            PressingOrder(id = "1", agencyName = "Agence Cocody", deliveryRequested = true, deliveryDriver = "Koné Ibrahim", status = OrderStatus.READY.name),
            PressingOrder(id = "2", agencyName = "Agence Plateau", deliveryRequested = true, deliveryDriver = "Touré Moussa", status = OrderStatus.READY.name),
            PressingOrder(id = "3", agencyName = "Agence Cocody", deliveryRequested = false, status = OrderStatus.WASHING.name)
        )

        val cocodyManager = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.GERANT_AGENCE,
            operatorName = "Gérant Cocody",
            isolatedAgency = "Agence Cocody",
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.GERANT_AGENCE)
        )
        val managerScoped = com.example.data.TenantDataScoper.scopeOrders(orders, cocodyManager)
        assertEquals(listOf("1", "3"), managerScoped.map { it.id })

        val driverSession = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.LIVREUR,
            operatorName = "Koné Ibrahim",
            isolatedAgency = "Agence Cocody",
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.LIVREUR)
        )
        val driverScoped = com.example.data.TenantDataScoper.scopeOrders(orders, driverSession)
        assertEquals(listOf("1"), driverScoped.map { it.id })
    }

    @Test
    fun tenantDataScoper_hidesFinancialsAndRedactsSalariesByPrivilege() {
        val entries = listOf(
            com.example.data.AccountingEntry(id = "e1", entryType = "REVENUE", agencyName = "Agence Cocody", amount = 15000L),
            com.example.data.AccountingEntry(id = "e2", entryType = "EXPENSE", agencyName = "Agence Cocody", amount = 50000L),
            com.example.data.AccountingEntry(id = "e3", entryType = "REVENUE", agencyName = "Agence Plateau", amount = 25000L)
        )

        val operatorSession = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.OPERATEUR,
            operatorName = "Opérateur 1",
            isolatedAgency = "Agence Cocody",
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.OPERATEUR)
        )
        assertTrue(com.example.data.TenantDataScoper.scopeTransactions(entries, operatorSession).isEmpty())

        val cashierSession = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.CAISSIER,
            operatorName = "Caissière Cocody",
            isolatedAgency = "Agence Cocody",
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.CAISSIER)
        )
        val cashierEntries = com.example.data.TenantDataScoper.scopeTransactions(entries, cashierSession)
        assertEquals(listOf("e1"), cashierEntries.map { it.id })

        val staff = listOf(
            com.example.data.EmployeeRecord(id = "emp1", fullName = "Awa Koné", agencyName = "Agence Cocody", monthlySalary = 180000L, commissionRate = 5.0)
        )
        val redactedStaff = com.example.data.TenantDataScoper.scopeEmployees(staff, cashierSession)
        assertEquals(0L, redactedStaff.first().monthlySalary)
        assertEquals(0.0, redactedStaff.first().commissionRate, 0.001)

        val directorSession = com.example.data.ActiveRoleSession()
        val visibleStaff = com.example.data.TenantDataScoper.scopeEmployees(staff, directorSession)
        assertEquals(180000L, visibleStaff.first().monthlySalary)
    }

    @Test
    fun activeRoleSession_enforcesRouteAccessGuards() {
        val operatorSession = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.OPERATEUR,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.OPERATEUR)
        )
        assertTrue(operatorSession.canAccessRoute("dashboard"))
        assertTrue(operatorSession.canAccessRoute("orders"))
        assertTrue(operatorSession.canAccessRoute("stocks"))
        assertFalse(operatorSession.canAccessRoute("finance"))
        assertFalse(operatorSession.canAccessRoute("roles_rbac"))
        assertFalse(operatorSession.canAccessRoute("settings"))
    }

    @Test
    fun activeRoleSession_controlsFinancialVisibilityPerRole() {
        val director = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.DIRECTEUR,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.DIRECTEUR)
        )
        val manager = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.GERANT_AGENCE,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.GERANT_AGENCE)
        )
        val cashier = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.CAISSIER,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.CAISSIER)
        )
        val operator = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.OPERATEUR,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.OPERATEUR)
        )
        val quality = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.CONTROLE_QUALITE,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.CONTROLE_QUALITE)
        )
        val driver = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.LIVREUR,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.LIVREUR)
        )

        assertTrue(director.canViewFinancialAmounts)
        assertTrue(manager.canViewFinancialAmounts)
        assertTrue(cashier.canViewFinancialAmounts)
        assertFalse(operator.canViewFinancialAmounts)
        assertFalse(quality.canViewFinancialAmounts)
        assertFalse(driver.canViewFinancialAmounts)
    }

    @Test
    fun activeRoleSession_enforcesOrderStatusTransitionsByRole() {
        val operator = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.OPERATEUR,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.OPERATEUR)
        )
        val quality = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.CONTROLE_QUALITE,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.CONTROLE_QUALITE)
        )
        val driver = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.LIVREUR,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.LIVREUR)
        )

        // Opérateur: atelier stages only, cannot validate READY or DELIVERED or CANCELLED
        assertTrue(operator.canTransitionOrderTo(OrderStatus.WASHING))
        assertTrue(operator.canTransitionOrderTo(OrderStatus.IRONING))
        assertTrue(operator.canTransitionOrderTo(OrderStatus.QUALITY_CHECK))
        assertFalse(operator.canTransitionOrderTo(OrderStatus.READY))
        assertFalse(operator.canTransitionOrderTo(OrderStatus.DELIVERED))
        assertFalse(operator.canTransitionOrderTo(OrderStatus.CANCELLED))

        // Contrôle Qualité: can validate READY, cannot mark DELIVERED
        assertTrue(quality.canTransitionOrderTo(OrderStatus.READY))
        assertFalse(quality.canTransitionOrderTo(OrderStatus.DELIVERED))

        // Livreur: can mark DELIVERED only, cannot alter atelier steps or READY
        assertTrue(driver.canTransitionOrderTo(OrderStatus.DELIVERED))
        assertFalse(driver.canTransitionOrderTo(OrderStatus.WASHING))
        assertFalse(driver.canTransitionOrderTo(OrderStatus.READY))
    }

    @Test
    fun tenantDataScoper_isolatesClientsAndClaimsByAgency() {
        val cocodyOrder = PressingOrder(id = "o1", orderNumber = "PRESS-001", agencyName = "Agence Cocody", clientId = "c1")
        val plateauOrder = PressingOrder(id = "o2", orderNumber = "PRESS-002", agencyName = "Agence Plateau", clientId = "c2")
        val allOrders = listOf(cocodyOrder, plateauOrder)
        val clients = listOf(
            com.example.data.ClientRecord(id = "c1", fullName = "Awa Koné"),
            com.example.data.ClientRecord(id = "c2", fullName = "Moussa Traoré"),
            com.example.data.ClientRecord(id = "c3", fullName = "Nouveau Client")
        )
        val claims = listOf(
            com.example.data.ClaimRecord(id = "cl1", orderNumber = "PRESS-001", subject = "Bouton"),
            com.example.data.ClaimRecord(id = "cl2", orderNumber = "PRESS-002", subject = "Retard")
        )

        val cocodySession = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.GERANT_AGENCE,
            operatorName = "Gérant Cocody",
            isolatedAgency = "Agence Cocody",
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.GERANT_AGENCE)
        )
        val scopedOrders = com.example.data.TenantDataScoper.scopeOrders(allOrders, cocodySession)
        val scopedClients = com.example.data.TenantDataScoper.scopeClients(clients, scopedOrders, cocodySession, allOrders)
        val scopedClaims = com.example.data.TenantDataScoper.scopeClaims(claims, scopedOrders, cocodySession)

        assertEquals(listOf("c1", "c3"), scopedClients.map { it.id })
        assertEquals(listOf("cl1"), scopedClaims.map { it.id })
    }

    @Test
    fun tenantDataScoper_redactsAllFinancialDataForNonFinancialRoles() {
        val itemMap = com.example.data.OrderGarmentItem(
            garmentName = "Costume 2 pièces",
            unitPrice = 4500L,
            lineTotal = 9000L,
            quantity = 2L
        ).toMap()
        val orders = listOf(
            PressingOrder(
                id = "ord1",
                agencyName = "Agence Cocody",
                items = listOf(itemMap),
                subtotal = 9000L,
                totalAmount = 10000L,
                paidAmount = 5000L,
                remainingAmount = 5000L,
                deliveryRequested = true,
                deliveryDriver = "Koné Ibrahim",
                status = OrderStatus.WASHING.name
            )
        )
        val clients = listOf(
            com.example.data.ClientRecord(
                id = "c1",
                fullName = "Awa Koné",
                balanceDue = 5000L,
                creditLimit = 50000L,
                discountPercent = 10.0
            )
        )
        val stocks = listOf(
            StockItem(id = "s1", name = "Lessive Pro", quantity = 10L, unitCost = 18500L)
        )

        val operatorSession = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.OPERATEUR,
            operatorName = "Opérateur Atelier",
            isolatedAgency = "Agence Cocody",
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.OPERATEUR)
        )
        val scopedOrders = com.example.data.TenantDataScoper.scopeOrders(orders, operatorSession)
        assertEquals(0L, scopedOrders.first().totalAmount)
        assertEquals(0L, scopedOrders.first().paidAmount)
        assertEquals(0L, scopedOrders.first().remainingAmount)
        assertEquals(0L, scopedOrders.first().parsedItems().first().unitPrice)
        assertEquals(0L, scopedOrders.first().parsedItems().first().lineTotal)

        val scopedStocks = com.example.data.TenantDataScoper.scopeStocks(stocks, operatorSession)
        assertEquals(0L, scopedStocks.first().unitCost)

        val driverSession = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.LIVREUR,
            operatorName = "Koné Ibrahim",
            isolatedAgency = "Agence Cocody",
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.LIVREUR)
        )
        val driverOrders = com.example.data.TenantDataScoper.scopeOrders(
            orders.map { it.copy(clientId = "c1", clientName = "Awa Koné") },
            driverSession
        )
        val driverClients = com.example.data.TenantDataScoper.scopeClients(clients, driverOrders, driverSession)
        assertEquals(0L, driverClients.first().balanceDue)
        assertEquals(0L, driverClients.first().creditLimit)
        assertEquals(0.0, driverClients.first().discountPercent, 0.001)
    }

    @Test
    fun garmentWorkshopStatus_andExhaustiveCatalog_meetRequirements() {
        assertEquals(com.example.data.GarmentWorkshopStatus.WASHING, com.example.data.GarmentWorkshopStatus.RECEIVED.nextStage())
        assertEquals(com.example.data.GarmentWorkshopStatus.DRYING, com.example.data.GarmentWorkshopStatus.WASHING.nextStage())
        assertEquals(com.example.data.GarmentWorkshopStatus.IRONING, com.example.data.GarmentWorkshopStatus.DRYING.nextStage())
        assertEquals(com.example.data.GarmentWorkshopStatus.QUALITY_CHECK, com.example.data.GarmentWorkshopStatus.IRONING.nextStage())
        assertEquals(com.example.data.GarmentWorkshopStatus.READY, com.example.data.GarmentWorkshopStatus.QUALITY_CHECK.nextStage())
        assertNull(com.example.data.GarmentWorkshopStatus.READY.nextStage())

        val catalog = com.example.data.DefaultCatalogTemplates.ivorianStarterServices()
        assertTrue(catalog.size >= 40)
        assertTrue(catalog.map { it.category }.distinct().size >= 7)

        val operatorSession = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.OPERATEUR,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.OPERATEUR)
        )
        val driverSession = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.LIVREUR,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.LIVREUR)
        )
        assertTrue(operatorSession.canAccessRoute("workshop_tracker"))
        assertFalse(driverSession.canAccessRoute("workshop_tracker"))
    }

    @Test
    fun authInputValidator_validatesAndNormalizesAfricanPhoneNumbers() {
        assertEquals("+225", com.example.data.AfricanDialCodes.defaultCountry.dialCode)
        assertNull(AuthInputValidator.validatePhone("+225", "07 58 42 19 63"))
        assertNotNull(AuthInputValidator.validatePhone("+225", "1234"))
        assertNotNull(AuthInputValidator.validatePhone("+225", ""))

        val e164 = AuthInputValidator.normalizePhoneToE164("+225", "07 58 42 19 63")
        assertEquals("+2250758421963", e164)

        val e164AlreadyPrefixed = AuthInputValidator.normalizePhoneToE164("+225", "+225 07 58 42 19 63")
        assertEquals("+2250758421963", e164AlreadyPrefixed)

        val authId = AuthInputValidator.phoneToAuthIdentifier(e164)
        assertEquals("tel.2250758421963@pressingpro.africa", authId)
        assertEquals("+2250758421963", AuthInputValidator.extractPhoneFromAuthIdentifier(authId))
    }

    @Test
    fun roleSessionResolver_bindsAuthenticatedUsersToExactFirestoreRoleAndAgency() {
        val company = com.example.data.CompanyConfig(
            companyName = "Pressing Étoile",
            email = "directeur@etoile.ci",
            agencies = listOf("Agence Cocody", "Agence Plateau")
        )
        val employees = listOf(
            com.example.data.EmployeeRecord(
                id = "emp1",
                fullName = "Awa Koné",
                phone = "+225 07 58 42 19 63",
                role = "CAISSIER",
                agencyName = "Agence Cocody",
                active = true
            ),
            com.example.data.EmployeeRecord(
                id = "emp2",
                fullName = "Moussa Traoré",
                phone = "05 98 76 54 32",
                role = "OPERATEUR",
                agencyName = "Agence Plateau",
                active = true
            ),
            com.example.data.EmployeeRecord(
                id = "emp3",
                fullName = "Koné Ibrahim",
                phone = "+2250102030405",
                role = "LIVREUR",
                agencyName = "Agence Cocody",
                active = true
            )
        )

        // Phone-authenticated cashier automatically isolated to Agence Cocody
        val cashierSession = com.example.data.RoleSessionResolver.resolveForUser(
            userEmail = "tel.2250758421963@pressingpro.africa",
            userPhone = "+2250758421963",
            userDisplayName = "Awa Koné",
            company = company,
            employees = employees
        )
        assertEquals(com.example.data.WorkspaceRole.CAISSIER, cashierSession.role)
        assertEquals("Agence Cocody", cashierSession.isolatedAgency)
        assertTrue(cashierSession.canViewFinancialAmounts)
        assertFalse(cashierSession.canAccessRoute("finance"))
        assertFalse(cashierSession.canAccessRoute("roles_rbac"))

        // Operator matched by local phone digits isolated to Agence Plateau with financial redaction
        val operatorSession = com.example.data.RoleSessionResolver.resolveForUser(
            userEmail = "tel.2250598765432@pressingpro.africa",
            userPhone = "+2250598765432",
            userDisplayName = "Moussa Traoré",
            company = company,
            employees = employees
        )
        assertEquals(com.example.data.WorkspaceRole.OPERATEUR, operatorSession.role)
        assertEquals("Agence Plateau", operatorSession.isolatedAgency)
        assertFalse(operatorSession.canViewFinancialAmounts)
        assertTrue(operatorSession.canAccessRoute("workshop_tracker"))

        // Company owner gets full Directeur multi-agency access
        val ownerSession = com.example.data.RoleSessionResolver.resolveForUser(
            userEmail = "directeur@etoile.ci",
            userPhone = "",
            userDisplayName = "Kouassi Yao",
            company = company,
            employees = employees
        )
        assertEquals(com.example.data.WorkspaceRole.DIRECTEUR, ownerSession.role)
        assertNull(ownerSession.isolatedAgency)
        assertTrue(ownerSession.canAccessRoute("roles_rbac"))
    }

    @Test
    fun photoCompression_scalesProportionallyToMax1024WithoutUpscaling() {
        assertEquals(1024, com.example.core.GARMENT_PHOTO_MAX_DIM)
        assertEquals(80, com.example.core.GARMENT_PHOTO_JPEG_QUALITY)

        // Landscape 4000x3000 -> 1024x768
        val landscape = com.example.core.computeTargetPhotoDimensions(4000, 3000)
        assertEquals(1024 to 768, landscape)

        // Portrait 1500x3000 -> 512x1024
        val portrait = com.example.core.computeTargetPhotoDimensions(1500, 3000)
        assertEquals(512 to 1024, portrait)

        // Already small 800x600 -> unchanged (no upscaling)
        val small = com.example.core.computeTargetPhotoDimensions(800, 600)
        assertEquals(800 to 600, small)

        // Base64 size estimation in KB
        assertEquals(0, com.example.core.estimateBase64SizeKb(""))
        assertTrue(com.example.core.estimateBase64SizeKb("A".repeat(40_000)) in 29..31)
    }

    @Test
    fun workshopUrgencyFilters_classifyAndRankOrdersByPromisedDeadline() {
        val now = 1_700_000_000_000L
        val hour = 3_600_000L

        val lateOrder = PressingOrder(
            id = "late",
            status = OrderStatus.WASHING.name,
            priority = com.example.data.OrderPriority.STANDARD.name,
            dueDateMillis = now - 3 * hour
        )
        val expressOrder = PressingOrder(
            id = "express",
            status = OrderStatus.DRYING.name,
            priority = com.example.data.OrderPriority.EXPRESS.name,
            dueDateMillis = now + 10 * hour
        )
        val dueTodayOrder = PressingOrder(
            id = "today",
            status = OrderStatus.IRONING.name,
            priority = com.example.data.OrderPriority.STANDARD.name,
            dueDateMillis = now + 8 * hour
        )
        val due48hOrder = PressingOrder(
            id = "due48",
            status = OrderStatus.ACCEPTED.name,
            priority = com.example.data.OrderPriority.STANDARD.name,
            dueDateMillis = now + 36 * hour
        )
        val standardLater = PressingOrder(
            id = "later",
            status = OrderStatus.PENDING.name,
            priority = com.example.data.OrderPriority.ECONOMY.name,
            dueDateMillis = now + 72 * hour
        )

        assertTrue(com.example.ui.screens.matchesWorkshopUrgency(lateOrder, com.example.ui.screens.WorkshopUrgencyFilter.LATE, now))
        assertFalse(com.example.ui.screens.matchesWorkshopUrgency(dueTodayOrder, com.example.ui.screens.WorkshopUrgencyFilter.LATE, now))

        assertTrue(com.example.ui.screens.matchesWorkshopUrgency(expressOrder, com.example.ui.screens.WorkshopUrgencyFilter.EXPRESS, now))
        assertTrue(com.example.ui.screens.matchesWorkshopUrgency(dueTodayOrder, com.example.ui.screens.WorkshopUrgencyFilter.DUE_24H, now))
        assertTrue(com.example.ui.screens.matchesWorkshopUrgency(due48hOrder, com.example.ui.screens.WorkshopUrgencyFilter.DUE_48H, now))
        assertFalse(com.example.ui.screens.matchesWorkshopUrgency(standardLater, com.example.ui.screens.WorkshopUrgencyFilter.DUE_48H, now))

        // Priority ordering: Late < Express < Due 24h < Due 48h < Later
        assertTrue(
            com.example.ui.screens.workshopUrgencyRank(lateOrder, now) <
                com.example.ui.screens.workshopUrgencyRank(expressOrder, now)
        )
        assertTrue(
            com.example.ui.screens.workshopUrgencyRank(expressOrder, now) <
                com.example.ui.screens.workshopUrgencyRank(dueTodayOrder, now)
        )
        assertTrue(
            com.example.ui.screens.workshopUrgencyRank(dueTodayOrder, now) <
                com.example.ui.screens.workshopUrgencyRank(due48hOrder, now)
        )
        assertTrue(
            com.example.ui.screens.workshopUrgencyRank(due48hOrder, now) <
                com.example.ui.screens.workshopUrgencyRank(standardLater, now)
        )

        // Human-readable French remaining deadline labels
        assertEquals("Retard 3h", com.example.ui.screens.formatRemainingDeadlineFr(lateOrder.dueDateMillis, now))
        assertEquals("Dans 8h", com.example.ui.screens.formatRemainingDeadlineFr(dueTodayOrder.dueDateMillis, now))
        assertEquals("Demain (36h)", com.example.ui.screens.formatRemainingDeadlineFr(due48hOrder.dueDateMillis, now))
    }
}
