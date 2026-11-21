package com.example.data

enum class AccessPermission(
    val labelFr: String,
    val domainFr: String,
    val descriptionFr: String
) {
    VIEW_EXECUTIVE_DASHBOARD(
        "Tableau de bord Direction",
        "Pilotage & Finances",
        "Accès aux KPIs exécutifs, courbes et répartition globale"
    ),
    VIEW_FINANCIAL_KPIS(
        "Indicateurs financiers & CA",
        "Pilotage & Finances",
        "Consultation du chiffre d'affaires, marges et encaissements"
    ),
    VIEW_ACCOUNTING_SYSCOHADA(
        "Comptabilité SYSCOHADA",
        "Pilotage & Finances",
        "Accès au journal comptable, comptes de trésorerie et bilan"
    ),
    ADD_EXPENSE(
        "Saisir dépenses & écritures",
        "Pilotage & Finances",
        "Enregistrer des dépenses, charges et opérations de trésorerie"
    ),
    VIEW_REPORTS(
        "Rapports & Analyses",
        "Pilotage & Finances",
        "Consultation des statistiques de ventes, fidélité et exports"
    ),

    VIEW_ORDERS(
        "Consulter les commandes",
        "Commandes & Atelier",
        "Accès à la liste et au suivi des commandes de son périmètre"
    ),
    CREATE_ORDER(
        "Créer une commande",
        "Commandes & Atelier",
        "Réception de vêtements, prise de photo et création de tickets"
    ),
    ADVANCE_PRODUCTION(
        "Avancer le traitement atelier",
        "Commandes & Atelier",
        "Passer les vêtements de Lavage à Séchage, Repassage, Finition"
    ),
    VALIDATE_QUALITY(
        "Valider le contrôle qualité",
        "Commandes & Atelier",
        "Certifier la qualité avant passage au statut Prête"
    ),

    COLLECT_PAYMENT(
        "Encaisser un paiement",
        "Caisse & Facturation",
        "Enregistrer un règlement Espèces, Mobile Money ou Carte"
    ),
    USE_POS(
        "Caisse Enregistreuse (POS)",
        "Caisse & Facturation",
        "Utiliser le comptoir tactile d'encaissement rapide"
    ),
    VIEW_INVOICES(
        "Facturation & Reçus",
        "Caisse & Facturation",
        "Consulter, imprimer et partager les factures clients"
    ),
    MANAGE_QUOTES(
        "Devis & Simulateur",
        "Caisse & Facturation",
        "Créer et envoyer des devis professionnels"
    ),

    VIEW_CLIENTS(
        "Consulter le CRM Clients",
        "Clients & Marketing",
        "Accès aux fiches clients, historique et fidélité"
    ),
    EDIT_CLIENTS(
        "Créer / modifier clients",
        "Clients & Marketing",
        "Ajouter ou mettre à jour des coordonnées et remises clients"
    ),
    DELETE_CLIENTS(
        "Supprimer une fiche client",
        "Clients & Marketing",
        "Suppression définitive d'un client du CRM"
    ),
    MANAGE_MARKETING(
        "Relances & Marketing",
        "Clients & Marketing",
        "Campagnes WhatsApp, relances impayés et fidélité"
    ),

    MANAGE_CATALOG(
        "Gérer articles & tarifs",
        "Stocks, Tarifs & Livraisons",
        "Modifier la grille tarifaire, délais et prestations"
    ),
    VIEW_STOCKS(
        "Consulter stocks & machines",
        "Stocks, Tarifs & Livraisons",
        "Visualiser les consommables et l'état des équipements"
    ),
    EDIT_STOCKS(
        "Modifier stocks & maintenance",
        "Stocks, Tarifs & Livraisons",
        "Réapprovisionner les produits et mettre à jour les machines"
    ),
    VIEW_DELIVERIES(
        "Consulter les livraisons GPS",
        "Stocks, Tarifs & Livraisons",
        "Accéder aux tournées de collecte et livraison"
    ),
    DISPATCH_DELIVERIES(
        "Affecter & clôturer livraisons",
        "Stocks, Tarifs & Livraisons",
        "Assigner un livreur ou confirmer la remise au client"
    ),

    MANAGE_CLAIMS(
        "Réclamations & Qualité SAV",
        "Administration & Sécurité",
        "Traiter et résoudre les tickets de réclamation clients"
    ),
    USE_AI_ASSISTANT(
        "Assistant IA Gemini",
        "Administration & Sécurité",
        "Utiliser l'inspection textile et le conseiller IA Gemini"
    ),
    MANAGE_STAFF_AND_ROLES(
        "Personnel, Rôles & Droits",
        "Administration & Sécurité",
        "Gérer les employés, les rôles et les privilèges d'accès"
    ),
    VIEW_SALARIES(
        "Consulter salaires & primes",
        "Administration & Sécurité",
        "Voir les salaires mensuels et taux de commission du personnel"
    ),
    MANAGE_SAAS_ADMIN(
        "Super-Admin SaaS & Abonnement",
        "Administration & Sécurité",
        "Changer de formule SaaS et exporter la sauvegarde complète"
    ),
    MANAGE_SETTINGS(
        "Paramètres Entreprise",
        "Administration & Sécurité",
        "Modifier l'identité légale, les agences et le setup 10 étapes"
    )
}

enum class DataIsolationScope(val labelFr: String, val badgeFr: String) {
    ALL_TENANT_AGENCIES(
        "Toutes les agences de l'entreprise (isolé par compte cloud)",
        "Multi-agences"
    ),
    SINGLE_AGENCY(
        "Données strictement isolées à l'agence assignée",
        "Agence isolée"
    ),
    PERSONAL_ASSIGNMENTS(
        "Données strictement limitées aux tournées assignées au livreur",
        "Tournées privées"
    )
}

enum class WorkspaceRole(
    val labelFr: String,
    val shortTitleFr: String,
    val descriptionFr: String,
    val defaultScope: DataIsolationScope
) {
    DIRECTEUR(
        labelFr = "Directeur Général",
        shortTitleFr = "Directeur",
        descriptionFr = "Contrôle total de l'entité, finances SYSCOHADA, RH, droits et multi-agences.",
        defaultScope = DataIsolationScope.ALL_TENANT_AGENCIES
    ),
    GERANT_AGENCE(
        labelFr = "Gérant d'Agence",
        shortTitleFr = "Gérant",
        descriptionFr = "Pilotage opérationnel et financier isolé exclusivement à son agence.",
        defaultScope = DataIsolationScope.SINGLE_AGENCY
    ),
    CAISSIER(
        labelFr = "Caissier(ère) / Réception",
        shortTitleFr = "Caisse",
        descriptionFr = "Accueil clients, prise de commandes, caisse POS et encaissements comptoir.",
        defaultScope = DataIsolationScope.SINGLE_AGENCY
    ),
    OPERATEUR(
        labelFr = "Opérateur Atelier",
        shortTitleFr = "Atelier",
        descriptionFr = "Production textile (lavage, séchage, repassage) et suivi machines sans données financières.",
        defaultScope = DataIsolationScope.SINGLE_AGENCY
    ),
    CONTROLE_QUALITE(
        labelFr = "Contrôle Qualité & SAV",
        shortTitleFr = "Qualité",
        descriptionFr = "Validation finale du linge, inspection IA et gestion des réclamations sans accès caisse.",
        defaultScope = DataIsolationScope.SINGLE_AGENCY
    ),
    LIVREUR(
        labelFr = "Livreur / Coursier GPS",
        shortTitleFr = "Livreur",
        descriptionFr = "Espace strictement isolé sur ses collectes et livraisons assignées avec carte GPS.",
        defaultScope = DataIsolationScope.PERSONAL_ASSIGNMENTS
    );

    companion object {
        fun fromEmployeeRole(employeeRole: EmployeeRole): WorkspaceRole = when (employeeRole) {
            EmployeeRole.DIRECTEUR -> DIRECTEUR
            EmployeeRole.CAISSIER -> CAISSIER
            EmployeeRole.OPERATEUR -> OPERATEUR
            EmployeeRole.CONTROLE_QUALITE -> CONTROLE_QUALITE
            EmployeeRole.LIVREUR -> LIVREUR
        }

        fun fromRaw(raw: String): WorkspaceRole {
            val normalized = raw.trim().uppercase()
            return entries.firstOrNull { it.name == normalized }
                ?: when {
                    normalized.contains("GERANT") -> GERANT_AGENCE
                    normalized.contains("DIRECTEUR") || normalized.contains("ADMIN") -> DIRECTEUR
                    normalized.contains("CAISS") -> CAISSIER
                    normalized.contains("QUALIT") -> CONTROLE_QUALITE
                    normalized.contains("LIVR") -> LIVREUR
                    else -> OPERATEUR
                }
        }
    }
}

object RolePermissionMatrix {
    fun defaultPermissionsFor(role: WorkspaceRole): Set<AccessPermission> = when (role) {
        WorkspaceRole.DIRECTEUR -> AccessPermission.entries.toSet()

        WorkspaceRole.GERANT_AGENCE -> setOf(
            AccessPermission.VIEW_EXECUTIVE_DASHBOARD,
            AccessPermission.VIEW_FINANCIAL_KPIS,
            AccessPermission.VIEW_ORDERS,
            AccessPermission.CREATE_ORDER,
            AccessPermission.ADVANCE_PRODUCTION,
            AccessPermission.VALIDATE_QUALITY,
            AccessPermission.COLLECT_PAYMENT,
            AccessPermission.USE_POS,
            AccessPermission.VIEW_INVOICES,
            AccessPermission.MANAGE_QUOTES,
            AccessPermission.VIEW_CLIENTS,
            AccessPermission.EDIT_CLIENTS,
            AccessPermission.MANAGE_MARKETING,
            AccessPermission.MANAGE_CATALOG,
            AccessPermission.VIEW_STOCKS,
            AccessPermission.EDIT_STOCKS,
            AccessPermission.VIEW_DELIVERIES,
            AccessPermission.DISPATCH_DELIVERIES,
            AccessPermission.MANAGE_CLAIMS,
            AccessPermission.USE_AI_ASSISTANT,
            AccessPermission.VIEW_REPORTS
        )

        WorkspaceRole.CAISSIER -> setOf(
            AccessPermission.VIEW_ORDERS,
            AccessPermission.CREATE_ORDER,
            AccessPermission.COLLECT_PAYMENT,
            AccessPermission.USE_POS,
            AccessPermission.VIEW_INVOICES,
            AccessPermission.MANAGE_QUOTES,
            AccessPermission.VIEW_CLIENTS,
            AccessPermission.EDIT_CLIENTS,
            AccessPermission.MANAGE_CLAIMS
        )

        WorkspaceRole.OPERATEUR -> setOf(
            AccessPermission.VIEW_ORDERS,
            AccessPermission.ADVANCE_PRODUCTION,
            AccessPermission.VIEW_STOCKS,
            AccessPermission.EDIT_STOCKS
        )

        WorkspaceRole.CONTROLE_QUALITE -> setOf(
            AccessPermission.VIEW_ORDERS,
            AccessPermission.ADVANCE_PRODUCTION,
            AccessPermission.VALIDATE_QUALITY,
            AccessPermission.MANAGE_CLAIMS,
            AccessPermission.USE_AI_ASSISTANT
        )

        WorkspaceRole.LIVREUR -> setOf(
            AccessPermission.VIEW_DELIVERIES,
            AccessPermission.DISPATCH_DELIVERIES
        )
    }

    fun requiredPermissionForRoute(route: String): AccessPermission? = when (route) {
        "dashboard", "help", "client_portal" -> null
        "orders", "order_detail", "workshop_tracker" -> AccessPermission.VIEW_ORDERS
        "new_order" -> AccessPermission.CREATE_ORDER
        "pos_cash" -> AccessPermission.USE_POS
        "invoices" -> AccessPermission.VIEW_INVOICES
        "quotes" -> AccessPermission.MANAGE_QUOTES
        "clients" -> AccessPermission.VIEW_CLIENTS
        "marketing" -> AccessPermission.MANAGE_MARKETING
        "catalog" -> AccessPermission.MANAGE_CATALOG
        "finance" -> AccessPermission.VIEW_ACCOUNTING_SYSCOHADA
        "stocks" -> AccessPermission.VIEW_STOCKS
        "deliveries" -> AccessPermission.VIEW_DELIVERIES
        "staff", "roles_rbac" -> AccessPermission.MANAGE_STAFF_AND_ROLES
        "claims" -> AccessPermission.MANAGE_CLAIMS
        "ai_assistant" -> AccessPermission.USE_AI_ASSISTANT
        "reports" -> AccessPermission.VIEW_REPORTS
        "saas_admin" -> AccessPermission.MANAGE_SAAS_ADMIN
        "settings" -> AccessPermission.MANAGE_SETTINGS
        "more" -> null
        else -> null
    }
}

data class ActiveRoleSession(
    val role: WorkspaceRole = WorkspaceRole.DIRECTEUR,
    val operatorName: String = "Directeur Général",
    val isolatedAgency: String? = null,
    val permissions: Set<AccessPermission> = RolePermissionMatrix.defaultPermissionsFor(WorkspaceRole.DIRECTEUR)
) {
    val effectiveScope: DataIsolationScope
        get() = when {
            role == WorkspaceRole.LIVREUR -> DataIsolationScope.PERSONAL_ASSIGNMENTS
            isolatedAgency != null -> DataIsolationScope.SINGLE_AGENCY
            else -> role.defaultScope
        }

    val canViewFinancialAmounts: Boolean
        get() = has(AccessPermission.VIEW_FINANCIAL_KPIS) ||
            has(AccessPermission.COLLECT_PAYMENT) ||
            has(AccessPermission.VIEW_ACCOUNTING_SYSCOHADA)

    fun canTransitionOrderTo(target: OrderStatus): Boolean = when (target) {
        OrderStatus.READY -> has(AccessPermission.VALIDATE_QUALITY)
        OrderStatus.DELIVERED -> has(AccessPermission.DISPATCH_DELIVERIES)
        OrderStatus.CANCELLED -> has(AccessPermission.CREATE_ORDER) || has(AccessPermission.MANAGE_SETTINGS)
        OrderStatus.PICKED_UP -> has(AccessPermission.COLLECT_PAYMENT) || has(AccessPermission.CREATE_ORDER)
        else -> has(AccessPermission.ADVANCE_PRODUCTION)
    }

    fun has(permission: AccessPermission): Boolean = permission in permissions

    fun canAccessRoute(route: String): Boolean {
        if (route == "order_detail" && role == WorkspaceRole.LIVREUR) {
            return has(AccessPermission.VIEW_DELIVERIES)
        }
        val req = RolePermissionMatrix.requiredPermissionForRoute(route) ?: return true
        return has(req)
    }

    fun scopeLabel(): String = when {
        role == WorkspaceRole.LIVREUR -> "Tournées de $operatorName${isolatedAgency?.let { " · $it" } ?: ""}"
        isolatedAgency != null -> "Entité isolée : $isolatedAgency"
        else -> "Toutes les agences (Tenant isolé)"
    }
}

data class RoleAuditEvent(
    val timestampLabel: String,
    val actor: String,
    val roleLabel: String,
    val scopeLabel: String,
    val action: String,
    val allowed: Boolean = true
)

/**
 * Pure tenant + agency + role data isolation scoper.
 * Ensures every role only receives the exact private records authorized by its privileges.
 */
object TenantDataScoper {

    fun scopeOrders(
        orders: List<PressingOrder>,
        session: ActiveRoleSession
    ): List<PressingOrder> {
        val byAgency = if (session.isolatedAgency.isNullOrBlank()) {
            orders
        } else {
            orders.filter {
                it.agencyName.isBlank() || it.agencyName.equals(session.isolatedAgency, ignoreCase = true)
            }
        }

        val roleFiltered = when (session.role) {
            WorkspaceRole.LIVREUR -> byAgency.filter { order ->
                val isDeliveryRelated = order.deliveryRequested ||
                    order.orderStatusEnum() in listOf(OrderStatus.READY, OrderStatus.DELIVERED)
                val assignedToDriver = order.deliveryDriver.isBlank() ||
                    order.deliveryDriver.equals(session.operatorName, ignoreCase = true)
                isDeliveryRelated && assignedToDriver
            }

            WorkspaceRole.OPERATEUR -> byAgency.filter { order ->
                order.orderStatusEnum() != OrderStatus.CANCELLED
            }

            WorkspaceRole.CONTROLE_QUALITE -> byAgency.filter { order ->
                order.orderStatusEnum() != OrderStatus.CANCELLED
            }

            else -> byAgency
        }

        // Strict client-side redaction: never expose monetary values to non-financial roles
        return if (session.canViewFinancialAmounts) {
            roleFiltered
        } else {
            roleFiltered.map { order ->
                val redactedItems = order.items.map { rawItem ->
                    rawItem.toMutableMap().apply {
                        put("unitPrice", 0L)
                        put("lineTotal", 0L)
                    }
                }
                order.copy(
                    items = redactedItems,
                    subtotal = 0L,
                    discountAmount = 0L,
                    expressFee = 0L,
                    deliveryFee = 0L,
                    taxAmount = 0L,
                    totalAmount = 0L,
                    paidAmount = 0L,
                    remainingAmount = 0L
                )
            }
        }
    }

    fun scopeTransactions(
        transactions: List<AccountingEntry>,
        session: ActiveRoleSession
    ): List<AccountingEntry> {
        val canSeeMoney = session.has(AccessPermission.VIEW_FINANCIAL_KPIS) ||
            session.has(AccessPermission.VIEW_ACCOUNTING_SYSCOHADA) ||
            session.has(AccessPermission.COLLECT_PAYMENT)
        if (!canSeeMoney) return emptyList()

        val byAgency = if (session.isolatedAgency.isNullOrBlank()) {
            transactions
        } else {
            transactions.filter {
                it.agencyName.isBlank() || it.agencyName.equals(session.isolatedAgency, ignoreCase = true)
            }
        }

        // Cashiers only see revenue/refund counter entries, never global company expenses/payroll
        return if (session.role == WorkspaceRole.CAISSIER && !session.has(AccessPermission.VIEW_ACCOUNTING_SYSCOHADA)) {
            byAgency.filter { it.entryType == "REVENUE" || it.entryType == "CASH_OPEN" || it.entryType == "CASH_CLOSE" }
        } else {
            byAgency
        }
    }

    fun scopeClients(
        clients: List<ClientRecord>,
        scopedOrders: List<PressingOrder>,
        session: ActiveRoleSession,
        allOrders: List<PressingOrder> = emptyList()
    ): List<ClientRecord> {
        val scoped = if (!session.has(AccessPermission.VIEW_CLIENTS)) {
            // Drivers only see clients belonging to their assigned deliveries
            if (session.role == WorkspaceRole.LIVREUR) {
                val allowedIds = scopedOrders.mapNotNull { it.clientId.takeIf { id -> id.isNotBlank() } }.toSet()
                val allowedNames = scopedOrders.map { it.clientName.lowercase() }.toSet()
                clients.filter { it.id in allowedIds || it.fullName.lowercase() in allowedNames }
            } else {
                emptyList()
            }
        } else if (session.isolatedAgency.isNullOrBlank()) {
            clients
        } else {
            val allowedIds = scopedOrders.mapNotNull { it.clientId.takeIf { id -> id.isNotBlank() } }.toSet()
            val allowedNames = scopedOrders.map { it.clientName.lowercase() }.toSet()
            if (allOrders.isEmpty()) {
                if (scopedOrders.isEmpty()) clients else clients.filter {
                    it.id in allowedIds || it.fullName.lowercase() in allowedNames
                }
            } else {
                val otherAgencyOrders = allOrders.filter {
                    it.agencyName.isNotBlank() && !it.agencyName.equals(session.isolatedAgency, ignoreCase = true)
                }
                val otherAgencyClientIds = otherAgencyOrders.mapNotNull { it.clientId.takeIf { id -> id.isNotBlank() } }.toSet()
                val otherAgencyClientNames = otherAgencyOrders.map { it.clientName.lowercase() }.toSet()
                clients.filter { client ->
                    val inThisAgency = client.id in allowedIds || client.fullName.lowercase() in allowedNames
                    val belongsOnlyToOtherAgency =
                        (client.id in otherAgencyClientIds || client.fullName.lowercase() in otherAgencyClientNames) && !inThisAgency
                    !belongsOnlyToOtherAgency
                }
            }
        }
        return if (session.canViewFinancialAmounts) {
            scoped
        } else {
            scoped.map { it.copy(balanceDue = 0L, creditLimit = 0L, discountPercent = 0.0) }
        }
    }

    fun scopeEmployees(
        employees: List<EmployeeRecord>,
        session: ActiveRoleSession
    ): List<EmployeeRecord> {
        val byAgency = if (session.isolatedAgency.isNullOrBlank()) {
            employees
        } else {
            employees.filter {
                it.agencyName.isBlank() || it.agencyName.equals(session.isolatedAgency, ignoreCase = true)
            }
        }
        return if (session.has(AccessPermission.VIEW_SALARIES)) {
            byAgency
        } else {
            byAgency.map { it.copy(monthlySalary = 0L, commissionRate = 0.0) }
        }
    }

    fun scopeStocks(
        stocks: List<StockItem>,
        session: ActiveRoleSession
    ): List<StockItem> {
        if (!session.has(AccessPermission.VIEW_STOCKS)) return emptyList()
        return if (session.canViewFinancialAmounts) {
            stocks
        } else {
            stocks.map { it.copy(unitCost = 0L) }
        }
    }

    fun scopeClaims(
        claims: List<ClaimRecord>,
        scopedOrders: List<PressingOrder>,
        session: ActiveRoleSession
    ): List<ClaimRecord> {
        if (!session.has(AccessPermission.MANAGE_CLAIMS)) return emptyList()
        if (session.isolatedAgency.isNullOrBlank()) return claims
        val orderNums = scopedOrders.map { it.orderNumber }.toSet()
        return claims.filter { it.orderNumber.isBlank() || it.orderNumber in orderNums }
    }
}

/**
 * Automatically resolves an authenticated Firebase user to their exact Firestore role
 * and isolated agency without exposing any client-side role-switching capability.
 */
object RoleSessionResolver {

    private fun digitsOnly(raw: String): String = raw.filter { it.isDigit() }

    private fun phonesMatch(a: String, b: String): Boolean {
        val da = digitsOnly(a)
        val db = digitsOnly(b)
        if (da.length < 8 || db.length < 8) return false
        if (da == db) return true
        val tailA = da.takeLast(8)
        val tailB = db.takeLast(8)
        return tailA == tailB
    }

    fun resolveForUser(
        userEmail: String,
        userPhone: String,
        userDisplayName: String,
        company: CompanyConfig?,
        employees: List<EmployeeRecord>,
        rolePermissionsMap: Map<WorkspaceRole, Set<AccessPermission>> = WorkspaceRole.entries.associateWith {
            RolePermissionMatrix.defaultPermissionsFor(it)
        }
    ): ActiveRoleSession {
        val cleanEmail = userEmail.trim().lowercase()
        val effectivePhone = userPhone.ifBlank {
            AuthInputValidator.extractPhoneFromAuthIdentifier(userEmail)
        }
        val cleanName = userDisplayName.trim()

        // Company owner check: if authenticated email matches company's official owner email, grant Directeur
        val ownerEmail = company?.email?.trim()?.lowercase().orEmpty()
        val isExplicitCompanyOwner = ownerEmail.isNotBlank() && cleanEmail.isNotBlank() && cleanEmail == ownerEmail

        val matchedEmployee = if (isExplicitCompanyOwner) {
            null
        } else {
            val activeStaff = employees.filter { it.active }
            // 1. Exact phone match against Firestore employees
            val byPhone = if (effectivePhone.isNotBlank()) {
                activeStaff.firstOrNull { emp -> emp.phone.isNotBlank() && phonesMatch(emp.phone, effectivePhone) }
            } else {
                null
            }
            // 2. Match by full name if employee record shares the authenticated profile name (and is non-blank)
            val byName = if (byPhone == null && cleanName.length >= 3) {
                activeStaff.firstOrNull { emp ->
                    emp.fullName.trim().equals(cleanName, ignoreCase = true)
                }
            } else {
                null
            }
            byPhone ?: byName
        }

        val resolvedRole = if (matchedEmployee != null) {
            WorkspaceRole.fromRaw(matchedEmployee.role)
        } else {
            WorkspaceRole.DIRECTEUR
        }

        val fallbackAgency = company?.agencies?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Agence Principale"
        val resolvedAgency = when (resolvedRole) {
            WorkspaceRole.DIRECTEUR -> null
            else -> matchedEmployee?.agencyName?.takeIf { it.isNotBlank() } ?: fallbackAgency
        }

        val resolvedOperatorName = when {
            matchedEmployee != null && matchedEmployee.fullName.isNotBlank() -> matchedEmployee.fullName.trim()
            cleanName.isNotBlank() -> cleanName
            company != null && company.companyName.isNotBlank() && resolvedRole == WorkspaceRole.DIRECTEUR ->
                "Directeur · ${company.companyName}"
            else -> resolvedRole.labelFr
        }

        val perms = rolePermissionsMap[resolvedRole] ?: RolePermissionMatrix.defaultPermissionsFor(resolvedRole)

        return ActiveRoleSession(
            role = resolvedRole,
            operatorName = resolvedOperatorName,
            isolatedAgency = resolvedAgency,
            permissions = perms
        )
    }
}

