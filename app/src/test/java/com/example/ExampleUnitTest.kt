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
        // Exact 27 granular privileges grouped across 5 domains (6 + 5 + 6 + 5 + 5 = 27)
        assertEquals(27, com.example.data.AccessPermission.entries.size)
        val byDomain = com.example.data.AccessPermission.entries.groupBy { it.domainFr }
        assertEquals(5, byDomain.size)
        assertEquals(6, byDomain["Commandes & Catalogue"]?.size)
        assertEquals(5, byDomain["Atelier & Qualité"]?.size)
        assertEquals(6, byDomain["Caisse & Finances"]?.size)
        assertEquals(5, byDomain["Clients, Livraison & SAV"]?.size)
        assertEquals(5, byDomain["Administration, RH & Sécurité"]?.size)

        val directorPerms = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.DIRECTEUR)
        assertEquals(27, directorPerms.size)

        val operatorPerms = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.OPERATEUR)
        assertTrue(com.example.data.AccessPermission.ADVANCE_PRODUCTION in operatorPerms)
        assertFalse(com.example.data.AccessPermission.VIEW_AGENCY_REVENUE in operatorPerms)
        assertFalse(com.example.data.AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA in operatorPerms)
        assertFalse(com.example.data.AccessPermission.COLLECT_PAYMENT in operatorPerms)

        val qualityPerms = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.CONTROLE_QUALITE)
        assertTrue(com.example.data.AccessPermission.VALIDATE_QUALITY in qualityPerms)
        assertTrue(com.example.data.AccessPermission.RETURN_FOR_REWORK in qualityPerms)
        assertFalse(com.example.data.AccessPermission.VIEW_AGENCY_REVENUE in qualityPerms)

        val driverPerms = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.LIVREUR)
        assertEquals(
            setOf(
                com.example.data.AccessPermission.MANAGE_DELIVERY_RUNS,
                com.example.data.AccessPermission.SEND_WHATSAPP_NOTIFICATION
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

    @Test
    fun thermalReceiptFormatter_adapts58mmAnd80mmWidthsAndEncodesEscPos() {
        val company = com.example.data.CompanyConfig(
            companyName = "Pressing Étoile d'Abidjan",
            address = "Rue des Jardins, Cocody",
            city = "Abidjan",
            phone = "+225 07 58 42 19 63",
            nif = "CI-2026-0098123A",
            currency = "FCFA",
            receiptFooter = "Merci pour votre confiance · Garde max 60 jours."
        )
        val item = com.example.data.OrderGarmentItem(
            garmentName = "Chemise Manches Longues",
            serviceName = "Lavage + Repassage",
            quantity = 3L,
            unitPrice = 1500L,
            lineTotal = 4500L,
            color = "Blanc",
            defects = "Tache col",
            barcode = "PR26-01"
        )
        val order = PressingOrder(
            id = "ord_58",
            orderNumber = "PRESS-2026-00105",
            agencyName = "Agence Cocody",
            clientName = "Kouassi Yao",
            clientPhone = "+225 07 11 22 33 44",
            priority = com.example.data.OrderPriority.EXPRESS.name,
            items = listOf(item.toMap()),
            subtotal = 4500L,
            discountAmount = 500L,
            totalAmount = 4000L,
            paidAmount = 2000L,
            remainingAmount = 2000L,
            paymentMethod = PaymentMethod.WAVE.name,
            dueDateMillis = System.currentTimeMillis() + 86_400_000L
        )

        val lines58 = com.example.core.ThermalReceiptFormatter.buildThermalReceiptLines(
            company = company,
            order = order,
            paperWidth = com.example.core.ThermalPaperWidth.MM_58,
            includeFinancials = true
        )
        assertTrue(lines58.isNotEmpty())
        assertTrue(lines58.all { it.text.length <= com.example.core.ThermalPaperWidth.MM_58.charsPerLine })
        assertTrue(lines58.any { it.text.contains("CI-2026-0098123A") })
        assertTrue(
            lines58.any {
                it.text.contains("RESTE À PAYER") &&
                    (it.style == com.example.core.ThermalLineStyle.TITLE || it.style == com.example.core.ThermalLineStyle.BOLD)
            }
        )

        val lines80 = com.example.core.ThermalReceiptFormatter.buildThermalReceiptLines(
            company = company,
            order = order,
            paperWidth = com.example.core.ThermalPaperWidth.MM_80,
            includeFinancials = true
        )
        assertTrue(lines80.all { it.text.length <= com.example.core.ThermalPaperWidth.MM_80.charsPerLine })
        assertTrue(lines80.any { it.text.contains("PRESS-2026-00105") })

        val escBytes = com.example.core.ThermalReceiptFormatter.encodeEscPosBytes(
            lines = lines58,
            qrContent = "PRESS-2026-00105",
            paperWidth = com.example.core.ThermalPaperWidth.MM_58,
            autoCut = true
        )
        // Starts with ESC @ (0x1B, 0x40) and ends with partial cut GS V 66 0 (0x1D, 0x56, 0x42, 0x00)
        assertEquals(0x1B.toByte(), escBytes[0])
        assertEquals(0x40.toByte(), escBytes[1])
        assertEquals(0x1D.toByte(), escBytes[escBytes.size - 4])
        assertEquals(0x56.toByte(), escBytes[escBytes.size - 3])
    }

    @Test
    fun thermalGarmentLabels_expandPhysicalPiecesAndMaskPricesForWorkshopRoles() {
        val company = com.example.data.CompanyConfig(companyName = "Pressing Pro", currency = "FCFA")
        val shirt = com.example.data.OrderGarmentItem(
            garmentName = "Chemise",
            serviceName = "Nettoyage à sec",
            fabric = "Soie délicate",
            quantity = 3L,
            unitPrice = 2000L,
            lineTotal = 6000L,
            defects = "Tache huile manche",
            barcode = "TAG-CH"
        )
        val order = PressingOrder(
            id = "ord_lbl",
            orderNumber = "PRESS-2026-00222",
            clientName = "Awa Koné",
            priority = com.example.data.OrderPriority.EXPRESS.name,
            items = listOf(shirt.toMap()),
            totalAmount = 6000L,
            paidAmount = 6000L,
            remainingAmount = 0L,
            dueDateMillis = System.currentTimeMillis() + 43_200_000L
        )

        val labels = com.example.core.ThermalReceiptFormatter.buildPhysicalGarmentLabels(order)
        assertEquals(3, labels.size)
        assertEquals("Pièce 1/3", labels[0].pieceCounterLabel)
        assertEquals("Pièce 2/3", labels[1].pieceCounterLabel)
        assertEquals("Pièce 3/3", labels[2].pieceCounterLabel)
        assertEquals(3, labels.map { it.barcodeValue }.distinct().size)
        assertTrue("URGENT" in labels[0].alertTags)
        assertTrue("TACHE" in labels[0].alertTags)
        assertTrue("DÉLICAT" in labels[0].alertTags)

        // RBAC check: non-financial roles see ZERO FCFA prices on receipt lines or labels
        val redactedLines = com.example.core.ThermalReceiptFormatter.buildThermalReceiptLines(
            company = company,
            order = order,
            paperWidth = com.example.core.ThermalPaperWidth.MM_58,
            includeFinancials = false
        )
        assertFalse(redactedLines.any { it.text.contains("FCFA") || it.text.contains("6 000") || it.text.contains("TOTAL") })

        val operator = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.OPERATEUR,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.OPERATEUR)
        )
        val quality = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.CONTROLE_QUALITE,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.CONTROLE_QUALITE)
        )
        assertFalse(operator.canPrintFinancialReceipt)
        assertTrue(operator.canPrintGarmentLabels)
        assertFalse(quality.canPrintFinancialReceipt)
        assertTrue(quality.canPrintGarmentLabels)
    }

    @Test
    fun dailyCashCloseZ_calculatesDrawerVarianceAndEnforcesAgencySettingsRbac() {
        val entries = listOf(
            com.example.data.AccountingEntry(id = "t1", entryType = "REVENUE", paymentMethod = PaymentMethod.ESPECES.name, amount = 25000L, agencyName = "Agence Cocody"),
            com.example.data.AccountingEntry(id = "t2", entryType = "REVENUE", paymentMethod = PaymentMethod.WAVE.name, amount = 15000L, agencyName = "Agence Cocody"),
            com.example.data.AccountingEntry(id = "t3", entryType = "REVENUE", paymentMethod = PaymentMethod.ORANGE_MONEY.name, amount = 10000L, agencyName = "Agence Cocody"),
            com.example.data.AccountingEntry(id = "t4", entryType = "EXPENSE", paymentMethod = PaymentMethod.ESPECES.name, amount = 5000L, agencyName = "Agence Cocody")
        )

        val exactClose = com.example.core.ThermalReceiptFormatter.calculateDailyCashClose(
            transactions = entries,
            countedCash = 20000L,
            agencyName = "Agence Cocody",
            targetDateKey = "2026-11-21"
        )
        assertEquals(20000L, exactClose.expectedCashInDrawer)
        assertEquals(0L, exactClose.variance)
        assertFalse(exactClose.requiresJustification)
        assertEquals(50000L, exactClose.totalDayRevenue)

        val shortfallClose = com.example.core.ThermalReceiptFormatter.calculateDailyCashClose(
            transactions = entries,
            countedCash = 18500L,
            agencyName = "Agence Cocody",
            targetDateKey = "2026-11-21"
        )
        assertEquals(-1500L, shortfallClose.variance)
        assertTrue(shortfallClose.requiresJustification)

        // Agency Administration RBAC matrix
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
        val driver = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.LIVREUR,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.LIVREUR)
        )

        assertTrue(director.canEditAgencySettingsAndTariffs)
        assertTrue(manager.canEditAgencySettingsAndTariffs)
        assertFalse(cashier.canEditAgencySettingsAndTariffs)
        assertTrue(cashier.canAccessRoute("settings"))
        assertTrue(cashier.canManageLocalPrinter)
        assertTrue(cashier.canRunCashClose)
        assertFalse(driver.canAccessRoute("settings"))
        assertFalse(driver.canRunCashClose)
    }

    @Test
    fun roleDashboardAnalytics_andDriverCodOnlyScoping_workAccurately() {
        val now = System.currentTimeMillis()
        val unpaidDelivery = PressingOrder(
            id = "d1",
            orderNumber = "PRESS-101",
            agencyName = "Agence Cocody",
            status = OrderStatus.READY.name,
            paymentStatus = com.example.data.PaymentStatus.PARTIAL.name,
            subtotal = 10000L,
            discountAmount = 2500L, // 25% > 10% cashier cap -> governance alert
            totalAmount = 7500L,
            paidAmount = 3000L,
            remainingAmount = 4500L,
            deliveryRequested = true,
            deliveryDriver = "Koné Ibrahim",
            deliveryZone = "Cocody Angré",
            itemCount = 3L,
            dueDateMillis = now + 7_200_000L
        )
        val paidDelivery = PressingOrder(
            id = "d2",
            orderNumber = "PRESS-102",
            agencyName = "Agence Yopougon",
            status = OrderStatus.WASHING.name,
            paymentStatus = com.example.data.PaymentStatus.PAID.name,
            subtotal = 6000L,
            totalAmount = 6000L,
            paidAmount = 6000L,
            remainingAmount = 0L,
            deliveryRequested = true,
            deliveryDriver = "Koné Ibrahim",
            deliveryZone = "Yopougon Niangon",
            itemCount = 2L,
            dueDateMillis = now - 3_600_000L // late
        )

        // Driver sees ONLY COD remainingAmount on unpaid delivery, zero prices/totals/CA
        val driverSession = com.example.data.ActiveRoleSession(
            role = com.example.data.WorkspaceRole.LIVREUR,
            operatorName = "Koné Ibrahim",
            isolatedAgency = null,
            permissions = com.example.data.RolePermissionMatrix.defaultPermissionsFor(com.example.data.WorkspaceRole.LIVREUR)
        )
        val scopedDriverOrders = com.example.data.TenantDataScoper.scopeOrders(listOf(unpaidDelivery, paidDelivery), driverSession)
        val scopedUnpaid = scopedDriverOrders.first { it.id == "d1" }
        val scopedPaid = scopedDriverOrders.first { it.id == "d2" }
        assertEquals(0L, scopedUnpaid.totalAmount)
        assertEquals(0L, scopedUnpaid.paidAmount)
        assertEquals(4500L, scopedUnpaid.remainingAmount)
        assertEquals(0L, scopedPaid.remainingAmount)
        assertEquals(4500L, com.example.data.RoleDashboardAnalytics.computeDriverCodTotal(scopedDriverOrders))

        // Punctuality: 1 on-time, 1 late -> 50%
        assertEquals(50, com.example.data.RoleDashboardAnalytics.computePunctualityRatePercent(listOf(unpaidDelivery, paidDelivery), now))

        // Agency comparison ranks Cocody (7500) ahead of Yopougon (6000)
        val comparison = com.example.data.RoleDashboardAnalytics.computeAgencyComparison(
            listOf(unpaidDelivery, paidDelivery),
            listOf("Agence Cocody", "Agence Yopougon"),
            now
        )
        assertEquals("Agence Cocody", comparison.first().agencyName)
        assertEquals(7500L, comparison.first().revenueFcfa)

        // Governance alerts detect cash variance, exceptional discount (>10%), open SAV claim, and denied access audit
        val alerts = com.example.data.RoleDashboardAnalytics.detectGovernanceAlerts(
            orders = listOf(unpaidDelivery, paidDelivery),
            transactions = listOf(
                com.example.data.AccountingEntry(entryType = "CASH_CLOSE", label = "Clôture Z 2026-11-21 · Manquant -1500")
            ),
            claims = listOf(com.example.data.ClaimRecord(status = "OPEN", subject = "Bouton manquant")),
            auditLog = listOf(com.example.data.RoleAuditEvent("10:00", "Opérateur", "Atelier", "Cocody", "Accès bloqué finance", allowed = false)),
            maxCashierDiscountPercent = 10.0
        )
        assertEquals(1, alerts.cashVariances.size)
        assertEquals(1, alerts.exceptionalDiscountOrders.size)
        assertEquals(1, alerts.openClaims.size)
        assertEquals(1, alerts.deniedAuditEventsCount)
        assertEquals(4, alerts.totalAlertCount)
    }

    @Test
    fun offlineRoomCacheEntity_andFcmTopicRouting_workCorrectly() {
        val sampleItems = listOf(
            com.example.data.OrderGarmentItem(
                garmentName = "Chemise classique",
                quantity = 2L,
                unitPrice = 1000L,
                lineTotal = 2000L,
                barcode = "PR-OFF-1"
            ),
            com.example.data.OrderGarmentItem(
                garmentName = "Costume 2 pièces",
                quantity = 1L,
                unitPrice = 3000L,
                lineTotal = 3000L,
                barcode = "PR-OFF-2"
            )
        )
        val encoded = com.example.data.local.OfflineGarmentJsonCodec.encode(sampleItems)
        val decoded = com.example.data.local.OfflineGarmentJsonCodec.decode(encoded)
        assertEquals(2, decoded.size)
        assertEquals("Chemise classique", decoded[0].garmentName)
        assertEquals(2L, decoded[0].quantity)
        assertEquals("PR-OFF-2", decoded[1].barcode)

        val entity = com.example.data.local.OfflineOrderEntity(
            localId = "offline_test_01",
            userId = "uid_cashier",
            orderNumber = "PRESS-OFF-001",
            agencyName = "Agence Cocody",
            clientId = "cli_1",
            clientName = "Konan Serge",
            clientPhone = "+225 07 00 11 22",
            clientAddress = "Cocody Angré",
            priority = com.example.data.OrderPriority.EXPRESS.name,
            itemsJson = encoded,
            discountAmount = 500L,
            expressFee = 1000L,
            deliveryFee = 0L,
            taxAmount = 0L,
            paidAmount = 3000L,
            paymentMethod = com.example.data.PaymentMethod.WAVE.name,
            deliveryRequested = false,
            deliveryZone = "",
            assignedEmployee = "Caissier 1",
            clientSignatureName = "Konan Serge",
            observations = "Tache col",
            dueDateMillis = System.currentTimeMillis() + 86_400_000L
        )
        val asOrder = entity.toPressingOrder()
        assertEquals("PRESS-OFF-001", asOrder.orderNumber)
        assertEquals(3L, asOrder.itemCount)
        assertEquals(5000L, asOrder.subtotal)
        assertEquals(5500L, asOrder.totalAmount)
        assertEquals(3000L, asOrder.paidAmount)
        assertEquals(2500L, asOrder.remainingAmount)
        assertEquals("PARTIAL", asOrder.paymentStatus)
        assertTrue(asOrder.observations.contains("Cache local (Room)"))

        // FCM topic subscription builder
        val cashierTopics = com.example.core.PressingNotificationManager.buildRoleTopics(
            role = com.example.data.WorkspaceRole.CAISSIER,
            agencyName = "Agence Cocody 8ème Tranche"
        )
        assertTrue("pressing_cashiers" in cashierTopics)
        assertTrue("pressing_all" in cashierTopics)
        assertTrue(cashierTopics.any { it.startsWith("agency_agence_cocody") })
    }

    @Test
    fun sixRoleSecuritySimulation_enforcesLoginBindingRoutesAndAntiPrivilegeEscalation() {
        val company = com.example.data.CompanyConfig(
            companyName = "Pressing Pro Abidjan",
            email = "dg@pressingpro.ci",
            agencies = listOf("Agence Cocody", "Agence Yopougon")
        )
        val employees = listOf(
            com.example.data.EmployeeRecord(
                id = "e_dir",
                fullName = "Kouassi DG",
                phone = "+2250700000001",
                role = "DIRECTEUR",
                agencyName = "Agence Cocody",
                monthlySalary = 650000L,
                active = true
            ),
            com.example.data.EmployeeRecord(
                id = "e_mgr",
                fullName = "Bamba Gérant",
                phone = "+2250700000002",
                role = "GERANT_AGENCE",
                agencyName = "Agence Cocody",
                monthlySalary = 350000L,
                active = true
            ),
            com.example.data.EmployeeRecord(
                id = "e_cash",
                fullName = "Awa Caissière",
                phone = "+2250700000003",
                role = "CAISSIER",
                agencyName = "Agence Cocody",
                monthlySalary = 160000L,
                active = true
            ),
            com.example.data.EmployeeRecord(
                id = "e_op",
                fullName = "Moussa Atelier",
                phone = "+2250700000004",
                role = "OPERATEUR",
                agencyName = "Agence Cocody",
                monthlySalary = 130000L,
                active = true
            ),
            com.example.data.EmployeeRecord(
                id = "e_qc",
                fullName = "Mariam Qualité",
                phone = "+2250700000005",
                role = "CONTROLE_QUALITE",
                agencyName = "Agence Yopougon",
                monthlySalary = 150000L,
                active = true
            ),
            com.example.data.EmployeeRecord(
                id = "e_drv",
                fullName = "Ibrahim Livreur",
                phone = "+2250700000006",
                role = "LIVREUR",
                agencyName = "Agence Cocody",
                monthlySalary = 120000L,
                active = true
            )
        )

        val dirSession = com.example.data.RoleSessionResolver.resolveForUser(
            userEmail = "dg@pressingpro.ci",
            userPhone = "+2250700000001",
            userDisplayName = "Kouassi DG",
            company = company,
            employees = employees
        )
        val mgrSession = com.example.data.RoleSessionResolver.resolveForUser(
            userEmail = "tel.2250700000002@pressingpro.africa",
            userPhone = "+2250700000002",
            userDisplayName = "Bamba Gérant",
            company = company,
            employees = employees
        )
        val cashSession = com.example.data.RoleSessionResolver.resolveForUser(
            userEmail = "tel.2250700000003@pressingpro.africa",
            userPhone = "+2250700000003",
            userDisplayName = "Awa Caissière",
            company = company,
            employees = employees
        )
        val opSession = com.example.data.RoleSessionResolver.resolveForUser(
            userEmail = "tel.2250700000004@pressingpro.africa",
            userPhone = "+2250700000004",
            userDisplayName = "Moussa Atelier",
            company = company,
            employees = employees
        )
        val qcSession = com.example.data.RoleSessionResolver.resolveForUser(
            userEmail = "tel.2250700000005@pressingpro.africa",
            userPhone = "+2250700000005",
            userDisplayName = "Mariam Qualité",
            company = company,
            employees = employees
        )
        val drvSession = com.example.data.RoleSessionResolver.resolveForUser(
            userEmail = "tel.2250700000006@pressingpro.africa",
            userPhone = "+2250700000006",
            userDisplayName = "Ibrahim Livreur",
            company = company,
            employees = employees
        )

        // Exact role + agency binding for all 6 profiles
        assertEquals(com.example.data.WorkspaceRole.DIRECTEUR, dirSession.role)
        assertNull(dirSession.isolatedAgency)
        assertEquals(com.example.data.WorkspaceRole.GERANT_AGENCE, mgrSession.role)
        assertEquals("Agence Cocody", mgrSession.isolatedAgency)
        assertEquals(com.example.data.WorkspaceRole.CAISSIER, cashSession.role)
        assertEquals("Agence Cocody", cashSession.isolatedAgency)
        assertEquals(com.example.data.WorkspaceRole.OPERATEUR, opSession.role)
        assertEquals("Agence Cocody", opSession.isolatedAgency)
        assertEquals(com.example.data.WorkspaceRole.CONTROLE_QUALITE, qcSession.role)
        assertEquals("Agence Yopougon", qcSession.isolatedAgency)
        assertEquals(com.example.data.WorkspaceRole.LIVREUR, drvSession.role)
        assertEquals("Agence Cocody", drvSession.isolatedAgency)

        // Route guards: non-directors never access roles_rbac; non-financial roles never access finance/pos/billing
        assertTrue(dirSession.canAccessRoute("roles_rbac"))
        assertFalse(mgrSession.canAccessRoute("roles_rbac"))
        assertFalse(cashSession.canAccessRoute("finance"))
        assertFalse(opSession.canAccessRoute("finance"))
        assertFalse(opSession.canAccessRoute("pos"))
        assertFalse(qcSession.canAccessRoute("finance"))
        assertFalse(qcSession.canAccessRoute("pos"))
        assertFalse(drvSession.canAccessRoute("finance"))
        assertFalse(drvSession.canAccessRoute("workshop_tracker"))
        assertFalse(drvSession.canAccessRoute("settings"))

        // Anti-privilege escalation: Gérant d'Agence CANNOT create/edit DIRECTEUR or GERANT_AGENCE or cross-agency staff
        assertFalse(mgrSession.canAssignRole(com.example.data.WorkspaceRole.DIRECTEUR))
        assertFalse(mgrSession.canAssignRole(com.example.data.WorkspaceRole.GERANT_AGENCE))
        assertTrue(mgrSession.canAssignRole(com.example.data.WorkspaceRole.CAISSIER))
        assertTrue(mgrSession.canAssignRole(com.example.data.WorkspaceRole.OPERATEUR))
        assertFalse(
            mgrSession.canModifyEmployee(
                existingEmployee = employees[0], // DIRECTEUR
                targetRole = com.example.data.WorkspaceRole.CAISSIER,
                targetAgencyName = "Agence Cocody",
                actorPhone = "+2250700000002"
            )
        )
        assertFalse(
            mgrSession.canModifyEmployee(
                existingEmployee = employees[1], // Self-modification blocked
                targetRole = com.example.data.WorkspaceRole.DIRECTEUR,
                targetAgencyName = "Agence Cocody",
                actorPhone = "+2250700000002"
            )
        )
        assertFalse(
            mgrSession.canModifyEmployee(
                existingEmployee = employees[4], // Agence Yopougon (cross-agency)
                targetRole = com.example.data.WorkspaceRole.CONTROLE_QUALITE,
                targetAgencyName = "Agence Yopougon",
                actorPhone = "+2250700000002"
            )
        )
        assertTrue(
            mgrSession.canModifyEmployee(
                existingEmployee = employees[2], // Caissière Cocody
                targetRole = com.example.data.WorkspaceRole.CAISSIER,
                targetAgencyName = "Agence Cocody",
                actorPhone = "+2250700000002"
            )
        )
        // Director can manage all roles across all agencies
        assertTrue(dirSession.canAssignRole(com.example.data.WorkspaceRole.DIRECTEUR))
        assertTrue(dirSession.canAssignRole(com.example.data.WorkspaceRole.GERANT_AGENCE))
        assertTrue(
            dirSession.canModifyEmployee(
                existingEmployee = employees[1],
                targetRole = com.example.data.WorkspaceRole.GERANT_AGENCE,
                targetAgencyName = "Agence Yopougon",
                actorPhone = "+2250700000001"
            )
        )
    }

    @Test
    fun clientPortalTrackingToken_preventsEnumerationAndSanitizesPrivateFields() {
        val token1 = com.example.data.TrackingTokenGenerator.generate()
        val token2 = com.example.data.TrackingTokenGenerator.generate()
        assertTrue(com.example.data.TrackingTokenGenerator.isValidFormat(token1))
        assertTrue(com.example.data.TrackingTokenGenerator.isValidFormat(token2))
        assertFalse(token1 == token2)

        val order = PressingOrder(
            id = "firestore_secret_doc_id",
            orderNumber = "PRESS-2026-00042",
            trackingToken = token1,
            agencyName = "Agence Cocody",
            clientName = "Kouassi Yao Bernard",
            clientPhone = "+225 07 99 88 77 66",
            clientAddress = "Rue des Jardins Villa 14",
            observations = "Note interne confidentielle: VIP difficile",
            status = OrderStatus.READY.name,
            paymentStatus = com.example.data.PaymentStatus.PARTIAL.name,
            totalAmount = 15000L,
            paidAmount = 10000L,
            remainingAmount = 5000L,
            itemCount = 4L
        )

        // Guessable order number, phone number, or client name MUST NOT resolve an order
        assertNull(com.example.data.ClientPortalOrderView.findByExactToken(listOf(order), "PRESS-2026-00042"))
        assertNull(com.example.data.ClientPortalOrderView.findByExactToken(listOf(order), "+225 07 99 88 77 66"))
        assertNull(com.example.data.ClientPortalOrderView.findByExactToken(listOf(order), "Kouassi"))
        assertNull(com.example.data.ClientPortalOrderView.findByExactToken(listOf(order), ""))

        // Exact trackingToken resolves ONLY a sanitized view with masked client name
        val view = com.example.data.ClientPortalOrderView.findByExactToken(listOf(order), token1.lowercase())
        assertNotNull(view)
        assertEquals("PRESS-2026-00042", view!!.orderNumber)
        assertEquals("Agence Cocody", view.agencyName)
        assertEquals(5000L, view.remainingAmount)
        assertEquals(4L, view.itemCount)
        assertFalse(view.maskedClientLabel.contains("Bernard"))
        assertFalse(view.maskedClientLabel.contains("07 99"))
        assertTrue(view.maskedClientLabel.startsWith("Kouassi"))
    }

    @Test
    fun qrCodeValidator_acceptsValidCodesAndRejectsMalformedOrInjectionPayloads() {
        val validGarment = com.example.data.QrCodeValidator.validate("PR26-0042-1")
        assertTrue(validGarment.isSuccess)
        assertEquals("PR26-0042-1", validGarment.getOrNull())

        val validReceiptQr = com.example.data.QrCodeValidator.validate("PRESSINGPRO|PRESS-2026-00042|TRK-AB12-CD34-EF56")
        assertTrue(validReceiptQr.isSuccess)
        assertEquals(
            "TRK-AB12-CD34-EF56",
            com.example.data.QrCodeValidator.extractTrackingToken(validReceiptQr.getOrThrow())
        )

        // Reject malicious / malformed inputs
        assertTrue(com.example.data.QrCodeValidator.validate("").isFailure)
        assertTrue(com.example.data.QrCodeValidator.validate("ab").isFailure)
        assertTrue(com.example.data.QrCodeValidator.validate("<script>alert(1)</script>").isFailure)
        assertTrue(com.example.data.QrCodeValidator.validate("javascript:alert(1)").isFailure)
        assertTrue(com.example.data.QrCodeValidator.validate("https://evil.example/phish").isFailure)
        assertTrue(com.example.data.QrCodeValidator.validate("PRESS-001; DROP TABLE orders").isFailure)
        assertTrue(com.example.data.QrCodeValidator.validate("CODE\nINJECTION").isFailure)
        assertTrue(com.example.data.QrCodeValidator.validate("A".repeat(120)).isFailure)
    }
}
