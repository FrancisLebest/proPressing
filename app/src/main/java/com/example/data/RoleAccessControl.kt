package com.example.data

/**
 * Exact 27 granular RBAC privileges across 5 operational domains:
 * 1. Commandes & Catalogue (6)
 * 2. Atelier & Qualité (5)
 * 3. Caisse & Finances (6)
 * 4. Clients, Livraison & SAV (5)
 * 5. Administration, RH & Sécurité (5)
 */
enum class AccessPermission(
    val labelFr: String,
    val domainFr: String,
    val descriptionFr: String
) {
    // --- 1. Commandes & Catalogue (6) ---
    CREATE_ORDER(
        "Créer commande",
        "Commandes & Catalogue",
        "Réception de vêtements, prise de photo et création de tickets"
    ),
    EDIT_ORDER(
        "Modifier commande",
        "Commandes & Catalogue",
        "Consulter et mettre à jour les détails d'une commande de son périmètre"
    ),
    CANCEL_DELETE_ORDER(
        "Annuler / Supprimer commande",
        "Commandes & Catalogue",
        "Annuler une commande avant livraison avec motif journalisé"
    ),
    APPLY_STANDARD_DISCOUNT(
        "Appliquer remise standard",
        "Commandes & Catalogue",
        "Accorder une remise commerciale dans la limite du plafond agence"
    ),
    APPROVE_EXCEPTIONAL_DISCOUNT(
        "Valider remise exceptionnelle",
        "Commandes & Catalogue",
        "Autoriser une remise supérieure au plafond caissier"
    ),
    MANAGE_CATALOG(
        "Modifier catalogue tarifs",
        "Commandes & Catalogue",
        "Modifier la grille des 43 articles, délais express et tarifs FCFA"
    ),

    // --- 2. Atelier & Qualité (5) ---
    ADVANCE_PRODUCTION(
        "Avancer étape lavage/séchage/repassage",
        "Atelier & Qualité",
        "Faire progresser les vêtements sur les postes atelier et suivre les stocks"
    ),
    VALIDATE_QUALITY(
        "Valider contrôle qualité",
        "Atelier & Qualité",
        "Certifier la conformité finale et passer la commande au statut Prête"
    ),
    RETURN_FOR_REWORK(
        "Renvoyer en reprise",
        "Atelier & Qualité",
        "Rejeter une pièce non conforme et la renvoyer en lavage ou repassage"
    ),
    VIEW_RECEPTION_PHOTOS(
        "Consulter photos réception",
        "Atelier & Qualité",
        "Visualiser les photos de taches/défauts capturées à la réception et l'IA"
    ),
    REPRINT_GARMENT_LABEL(
        "Réimprimer étiquette vêtement (sans prix)",
        "Atelier & Qualité",
        "Imprimer les étiquettes individuelles 1/N de suivi atelier sans montant"
    ),

    // --- 3. Caisse & Finances (6) ---
    COLLECT_PAYMENT(
        "Encaisser paiement",
        "Caisse & Finances",
        "Encaisser un règlement Espèces, Wave, Orange Money, MTN MoMo ou Moov"
    ),
    MANAGE_CASH_REGISTER_Z(
        "Ouvrir / Clôturer Z de caisse",
        "Caisse & Finances",
        "Ouvrir la session caisse et exécuter la clôture Z journalière"
    ),
    ADD_EXPENSE(
        "Saisir dépense caisse",
        "Caisse & Finances",
        "Enregistrer une dépense d'exploitation ou sortie de caisse agence"
    ),
    VIEW_AGENCY_REVENUE(
        "Consulter CA agence",
        "Caisse & Finances",
        "Consulter le chiffre d'affaires, les encaissements et la marge de son agence"
    ),
    VIEW_MULTI_AGENCY_SYSCOHADA(
        "Consulter CA multi-agences & SYSCOHADA",
        "Caisse & Finances",
        "Vue consolidée multi-agences, journal SYSCOHADA complet et Super-Admin"
    ),
    EXPORT_FINANCIAL_REPORTS(
        "Exporter rapports financiers",
        "Caisse & Finances",
        "Exporter les bilans, analyses de ventes et états financiers"
    ),

    // --- 4. Clients, Livraison & SAV (5) ---
    MANAGE_CLIENTS(
        "Créer / Modifier fiche client",
        "Clients, Livraison & SAV",
        "Gérer le CRM clients, coordonnées, fidélité et historique"
    ),
    SEND_WHATSAPP_NOTIFICATION(
        "Envoyer notification WhatsApp",
        "Clients, Livraison & SAV",
        "Notifier les clients par WhatsApp (retrait prêt, rappel, livraison)"
    ),
    MANAGE_DELIVERY_RUNS(
        "Gérer tournées livraison",
        "Clients, Livraison & SAV",
        "Consulter, affecter et clôturer les collectes et livraisons GPS"
    ),
    HANDLE_SAV_CLAIMS(
        "Traiter litige / indemnisation SAV",
        "Clients, Livraison & SAV",
        "Ouvrir, qualifier et résoudre les réclamations et litiges clients"
    ),
    REPRINT_CLIENT_RECEIPT(
        "Réimprimer reçu client avec prix",
        "Clients, Livraison & SAV",
        "Imprimer ou partager les tickets de caisse et factures en FCFA"
    ),

    // --- 5. Administration, RH & Sécurité (5) ---
    MANAGE_USERS_AND_ROLES(
        "Gérer utilisateurs & rôles",
        "Administration, RH & Sécurité",
        "Créer, activer et affecter les employés aux agences et profils métier"
    ),
    EDIT_PRIVILEGE_MATRIX(
        "Modifier matrice des privilèges",
        "Administration, RH & Sécurité",
        "Ajuster les 27 privilèges RBAC par rôle métier"
    ),
    CONFIGURE_PRINTER_AND_AGENCY(
        "Configurer imprimante & paramètres agence",
        "Administration, RH & Sécurité",
        "Paramétrer les coordonnées légales, imprimantes thermiques et plafonds"
    ),
    VIEW_HR_SALARIES(
        "Consulter salaires & paie RH",
        "Administration, RH & Sécurité",
        "Accéder aux salaires mensuels, primes et commissions du personnel"
    ),
    VIEW_SECURITY_AUDIT_LOG(
        "Consulter journal d'audit sécurité",
        "Administration, RH & Sécurité",
        "Auditer les accès refusés, remises exceptionnelles et écarts de caisse"
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
    val defaultScope: DataIsolationScope,
    val privilegeRank: Int
) {
    DIRECTEUR(
        labelFr = "Directeur Général",
        shortTitleFr = "Directeur",
        descriptionFr = "Vue multi-agences stratégique, finances SYSCOHADA, gouvernance, RH et 27 privilèges.",
        defaultScope = DataIsolationScope.ALL_TENANT_AGENCIES,
        privilegeRank = 100
    ),
    GERANT_AGENCE(
        labelFr = "Gérant d'Agence",
        shortTitleFr = "Gérant",
        descriptionFr = "Pilotage opérationnel, caisse locale, équipe et tarifs isolés à son agence.",
        defaultScope = DataIsolationScope.SINGLE_AGENCY,
        privilegeRank = 70
    ),
    CAISSIER(
        labelFr = "Caissier / Accueil Comptoir",
        shortTitleFr = "Caisse",
        descriptionFr = "Prise de commande, encaissements Mobile Money/Espèces et clôture Z de sa session.",
        defaultScope = DataIsolationScope.SINGLE_AGENCY,
        privilegeRank = 40
    ),
    OPERATEUR(
        labelFr = "Opérateur Atelier",
        shortTitleFr = "Atelier",
        descriptionFr = "Production textile (lavage, séchage, repassage) et scan étiquettes sans aucune donnée financière.",
        defaultScope = DataIsolationScope.SINGLE_AGENCY,
        privilegeRank = 20
    ),
    CONTROLE_QUALITE(
        labelFr = "Contrôle Qualité & SAV",
        shortTitleFr = "Qualité",
        descriptionFr = "Inspection avant/après, validation Prêt, renvoi en reprise et SAV sans données financières.",
        defaultScope = DataIsolationScope.SINGLE_AGENCY,
        privilegeRank = 30
    ),
    LIVREUR(
        labelFr = "Livreur GPS",
        shortTitleFr = "Livreur",
        descriptionFr = "Tournées par quartier, itinéraire GPS, preuve de livraison et COD uniquement sur ses colis.",
        defaultScope = DataIsolationScope.PERSONAL_ASSIGNMENTS,
        privilegeRank = 20
    );

    companion object {
        fun fromEmployeeRole(employeeRole: EmployeeRole): WorkspaceRole = when (employeeRole) {
            EmployeeRole.DIRECTEUR -> DIRECTEUR
            EmployeeRole.GERANT_AGENCE -> GERANT_AGENCE
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
            // Commandes & Catalogue (6/6)
            AccessPermission.CREATE_ORDER,
            AccessPermission.EDIT_ORDER,
            AccessPermission.CANCEL_DELETE_ORDER,
            AccessPermission.APPLY_STANDARD_DISCOUNT,
            AccessPermission.APPROVE_EXCEPTIONAL_DISCOUNT,
            AccessPermission.MANAGE_CATALOG,
            // Atelier & Qualité (5/5)
            AccessPermission.ADVANCE_PRODUCTION,
            AccessPermission.VALIDATE_QUALITY,
            AccessPermission.RETURN_FOR_REWORK,
            AccessPermission.VIEW_RECEPTION_PHOTOS,
            AccessPermission.REPRINT_GARMENT_LABEL,
            // Caisse & Finances (5/6 — sans CA multi-agences & SYSCOHADA global)
            AccessPermission.COLLECT_PAYMENT,
            AccessPermission.MANAGE_CASH_REGISTER_Z,
            AccessPermission.ADD_EXPENSE,
            AccessPermission.VIEW_AGENCY_REVENUE,
            AccessPermission.EXPORT_FINANCIAL_REPORTS,
            // Clients, Livraison & SAV (5/5)
            AccessPermission.MANAGE_CLIENTS,
            AccessPermission.SEND_WHATSAPP_NOTIFICATION,
            AccessPermission.MANAGE_DELIVERY_RUNS,
            AccessPermission.HANDLE_SAV_CLAIMS,
            AccessPermission.REPRINT_CLIENT_RECEIPT,
            // Administration, RH & Sécurité (1/5 — paramètres agence & imprimante uniquement)
            AccessPermission.CONFIGURE_PRINTER_AND_AGENCY
        )

        WorkspaceRole.CAISSIER -> setOf(
            AccessPermission.CREATE_ORDER,
            AccessPermission.EDIT_ORDER,
            AccessPermission.APPLY_STANDARD_DISCOUNT,
            AccessPermission.VIEW_RECEPTION_PHOTOS,
            AccessPermission.REPRINT_GARMENT_LABEL,
            AccessPermission.COLLECT_PAYMENT,
            AccessPermission.MANAGE_CASH_REGISTER_Z,
            AccessPermission.MANAGE_CLIENTS,
            AccessPermission.SEND_WHATSAPP_NOTIFICATION,
            AccessPermission.HANDLE_SAV_CLAIMS,
            AccessPermission.REPRINT_CLIENT_RECEIPT
        )

        WorkspaceRole.OPERATEUR -> setOf(
            AccessPermission.ADVANCE_PRODUCTION,
            AccessPermission.VIEW_RECEPTION_PHOTOS,
            AccessPermission.REPRINT_GARMENT_LABEL
        )

        WorkspaceRole.CONTROLE_QUALITE -> setOf(
            AccessPermission.ADVANCE_PRODUCTION,
            AccessPermission.VALIDATE_QUALITY,
            AccessPermission.RETURN_FOR_REWORK,
            AccessPermission.VIEW_RECEPTION_PHOTOS,
            AccessPermission.REPRINT_GARMENT_LABEL,
            AccessPermission.HANDLE_SAV_CLAIMS
        )

        WorkspaceRole.LIVREUR -> setOf(
            AccessPermission.MANAGE_DELIVERY_RUNS,
            AccessPermission.SEND_WHATSAPP_NOTIFICATION
        )
    }

    fun requiredPermissionForRoute(route: String): AccessPermission? = when (route) {
        "dashboard", "help", "client_portal", "more" -> null
        "orders" -> AccessPermission.EDIT_ORDER
        "order_detail" -> AccessPermission.EDIT_ORDER
        "workshop_tracker" -> AccessPermission.ADVANCE_PRODUCTION
        "new_order" -> AccessPermission.CREATE_ORDER
        "pos_cash" -> AccessPermission.COLLECT_PAYMENT
        "invoices" -> AccessPermission.REPRINT_CLIENT_RECEIPT
        "quotes" -> AccessPermission.CREATE_ORDER
        "clients" -> AccessPermission.MANAGE_CLIENTS
        "marketing" -> AccessPermission.SEND_WHATSAPP_NOTIFICATION
        "catalog" -> AccessPermission.MANAGE_CATALOG
        "finance" -> AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA
        "stocks" -> AccessPermission.ADVANCE_PRODUCTION
        "deliveries" -> AccessPermission.MANAGE_DELIVERY_RUNS
        "staff" -> AccessPermission.MANAGE_USERS_AND_ROLES
        "roles_rbac" -> AccessPermission.EDIT_PRIVILEGE_MATRIX
        "claims" -> AccessPermission.HANDLE_SAV_CLAIMS
        "ai_assistant" -> AccessPermission.VIEW_RECEPTION_PHOTOS
        "reports" -> AccessPermission.EXPORT_FINANCIAL_REPORTS
        "saas_admin" -> AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA
        "settings" -> AccessPermission.CONFIGURE_PRINTER_AND_AGENCY
        else -> null
    }
}

data class ActiveRoleSession(
    val role: WorkspaceRole = WorkspaceRole.DIRECTEUR,
    val operatorName: String = "Directeur Général",
    val isolatedAgency: String? = null,
    val permissions: Set<AccessPermission> = RolePermissionMatrix.defaultPermissionsFor(WorkspaceRole.DIRECTEUR),
    val isSuperAdmin: Boolean = false
) {
    val effectiveScope: DataIsolationScope
        get() = when {
            role == WorkspaceRole.LIVREUR -> DataIsolationScope.PERSONAL_ASSIGNMENTS
            isolatedAgency != null -> DataIsolationScope.SINGLE_AGENCY
            else -> role.defaultScope
        }

    /** Strictly false for OPERATEUR, CONTROLE_QUALITE, and LIVREUR (who only sees COD balance separately). */
    val canViewFinancialAmounts: Boolean
        get() = has(AccessPermission.VIEW_AGENCY_REVENUE) ||
            has(AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA) ||
            has(AccessPermission.COLLECT_PAYMENT)

    /** True ONLY for drivers collecting unpaid COD balances on their assigned delivery runs. */
    val canViewDeliveryCodAmount: Boolean
        get() = role == WorkspaceRole.LIVREUR && has(AccessPermission.MANAGE_DELIVERY_RUNS)

    /** Only Directeur Général and Gérant d'Agence can modify agency profile, 43-garment tariffs, and discount caps. */
    val canEditAgencySettingsAndTariffs: Boolean
        get() = has(AccessPermission.CONFIGURE_PRINTER_AND_AGENCY) || has(AccessPermission.MANAGE_CATALOG)

    /** Directeur, Gérant d'Agence, and Caissier can configure the local Bluetooth thermal printer. */
    val canManageLocalPrinter: Boolean
        get() = has(AccessPermission.CONFIGURE_PRINTER_AND_AGENCY) ||
            has(AccessPermission.MANAGE_CASH_REGISTER_Z) ||
            has(AccessPermission.COLLECT_PAYMENT)

    /** Directeur, Gérant d'Agence, and Caissier can execute daily Z de Caisse closing. */
    val canRunCashClose: Boolean
        get() = has(AccessPermission.MANAGE_CASH_REGISTER_Z)

    /** Client receipts with FCFA prices are restricted to roles with explicit financial receipt privilege. */
    val canPrintFinancialReceipt: Boolean
        get() = has(AccessPermission.REPRINT_CLIENT_RECEIPT) && canViewFinancialAmounts

    /** Workshop garment labels (without prices) are available to reception, cashier, workshop, and QC roles. */
    val canPrintGarmentLabels: Boolean
        get() = has(AccessPermission.REPRINT_GARMENT_LABEL)

    /** Quality Control, Agency Manager, and Director can send a non-compliant garment back to rework. */
    val canReworkGarment: Boolean
        get() = has(AccessPermission.RETURN_FOR_REWORK)

    /** Operational order visibility (Directeur, Gérant, Caissier, Opérateur, Qualité). */
    val canViewOperationalOrders: Boolean
        get() = has(AccessPermission.CREATE_ORDER) ||
            has(AccessPermission.EDIT_ORDER) ||
            has(AccessPermission.ADVANCE_PRODUCTION) ||
            has(AccessPermission.VALIDATE_QUALITY)

    fun canTransitionOrderTo(target: OrderStatus): Boolean = when (target) {
        OrderStatus.READY -> has(AccessPermission.VALIDATE_QUALITY)
        OrderStatus.DELIVERED -> has(AccessPermission.MANAGE_DELIVERY_RUNS)
        OrderStatus.CANCELLED -> has(AccessPermission.CANCEL_DELETE_ORDER)
        OrderStatus.PICKED_UP -> has(AccessPermission.COLLECT_PAYMENT) || has(AccessPermission.CREATE_ORDER)
        else -> has(AccessPermission.ADVANCE_PRODUCTION) || has(AccessPermission.RETURN_FOR_REWORK)
    }

    fun has(permission: AccessPermission): Boolean = permission in permissions

    fun canAccessRoute(route: String): Boolean {
        return when (route) {
            "dashboard", "help", "client_portal", "more" -> true
            "orders" -> canViewOperationalOrders
            "order_detail" -> canViewOperationalOrders || canViewDeliveryCodAmount
            "finance" -> has(AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA) || has(AccessPermission.VIEW_AGENCY_REVENUE)
            "marketing" -> has(AccessPermission.SEND_WHATSAPP_NOTIFICATION) && has(AccessPermission.MANAGE_CLIENTS)
            "settings" -> canEditAgencySettingsAndTariffs || canManageLocalPrinter || canRunCashClose
            "roles_rbac" -> isSuperAdmin || has(AccessPermission.EDIT_PRIVILEGE_MATRIX) || has(AccessPermission.VIEW_SECURITY_AUDIT_LOG)
            "saas_admin" -> isSuperAdmin || (role == WorkspaceRole.DIRECTEUR && has(AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA))
            else -> {
                val req = RolePermissionMatrix.requiredPermissionForRoute(route) ?: return false
                has(req)
            }
        }
    }

    fun scopeLabel(): String = when {
        role == WorkspaceRole.LIVREUR -> "Tournées de $operatorName${isolatedAgency?.let { " · $it" } ?: ""}"
        isolatedAgency != null -> "Agence isolée : $isolatedAgency"
        else -> "Toutes les agences (Multi-agences)"
    }

    /**
     * Anti-privilege-escalation check:
     * Only DIRECTEUR may assign DIRECTEUR or GERANT_AGENCE.
     * Any other role with user-management permission can only assign roles strictly below its own rank.
     */
    fun canAssignRole(targetRole: WorkspaceRole): Boolean {
        if (role == WorkspaceRole.DIRECTEUR || isSuperAdmin) return true
        val canManageAgencyTeam = has(AccessPermission.MANAGE_USERS_AND_ROLES) ||
            role == WorkspaceRole.GERANT_AGENCE
        if (!canManageAgencyTeam) return false
        return targetRole.privilegeRank < role.privilegeRank
    }

    /**
     * Anti-privilege-escalation guard for creating/updating/deleting employee records:
     * - Blocks non-Directors from modifying a record belonging to an equal/higher role (e.g., DIRECTEUR/GERANT_AGENCE)
     * - Blocks self-role modification
     * - Enforces isolated agency boundary for non-Directors
     */
    fun canModifyEmployee(
        existingEmployee: EmployeeRecord?,
        targetRole: WorkspaceRole,
        targetAgencyName: String,
        actorPhone: String = ""
    ): Boolean {
        if (!canAssignRole(targetRole)) return false
        if (role != WorkspaceRole.DIRECTEUR) {
            val ownAgency = isolatedAgency
            if (!ownAgency.isNullOrBlank() && targetAgencyName.isNotBlank() &&
                !targetAgencyName.equals(ownAgency, ignoreCase = true)
            ) {
                return false
            }
            if (existingEmployee != null) {
                val existingRole = WorkspaceRole.fromRaw(existingEmployee.role)
                if (existingRole.privilegeRank >= role.privilegeRank) return false
                val samePersonByName = existingEmployee.fullName.trim().equals(operatorName.trim(), ignoreCase = true)
                val actorDigits = actorPhone.filter { it.isDigit() }
                val empDigits = existingEmployee.phone.filter { it.isDigit() }
                val samePersonByPhone = actorDigits.length >= 8 && empDigits.length >= 8 &&
                    actorDigits.takeLast(8) == empDigits.takeLast(8)
                if ((samePersonByName || samePersonByPhone) && existingRole != targetRole) {
                    return false
                }
            }
        }
        return true
    }
}

/**
 * Strict validator for QR codes and barcodes scanned via CameraX / ML Kit or entered manually.
 * Prevents oversized payloads, control characters, script/URL injection, and malformed tags.
 */
object QrCodeValidator {
    private val allowedPattern = Regex("^[A-Za-z0-9._:/|\\-]{3,80}$")
    private val forbiddenTokens = listOf(
        "javascript:", "data:", "file://", "http://", "https://", "<script", "</script", "drop table", "select "
    )

    fun validate(raw: String): Result<String> {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) {
            return Result.failure(IllegalArgumentException("Code QR / code-barres vide."))
        }
        if (cleaned.length < 3 || cleaned.length > 80) {
            return Result.failure(IllegalArgumentException("Code invalide : longueur attendue entre 3 et 80 caractères."))
        }
        if (cleaned.any { it.isISOControl() || it == '\n' || it == '\r' }) {
            return Result.failure(IllegalArgumentException("Code invalide : caractères de contrôle non autorisés."))
        }
        val lower = cleaned.lowercase()
        if (forbiddenTokens.any { lower.contains(it) }) {
            return Result.failure(IllegalArgumentException("Format QR non reconnu par Pressing Pro (contenu bloqué)."))
        }
        if (!allowedPattern.matches(cleaned)) {
            return Result.failure(IllegalArgumentException("Format d'étiquette invalide. Utilisez uniquement lettres, chiffres et - _ . : / |"))
        }
        return Result.success(cleaned)
    }

    /** Extracts a client portal tracking token (TRK-XXXX-XXXX-XXXX) from either raw token or pipe-delimited QR payload. */
    fun extractTrackingToken(raw: String): String? {
        val validated = validate(raw).getOrNull() ?: return null
        val upper = validated.uppercase()
        if (TrackingTokenGenerator.isValidFormat(upper)) return upper
        val tokenSegment = upper.split("|", ":", "/").firstOrNull { TrackingTokenGenerator.isValidFormat(it) }
        return tokenSegment
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

        // Strict client-side redaction:
        // - OPERATEUR & CONTROLE_QUALITE: ZERO financial data (all FCFA amounts zeroed).
        // - LIVREUR: zero prices/totals/CA/discounts, keeping ONLY COD remainingAmount on unpaid delivery orders.
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
                val codOnlyRemaining = if (
                    session.canViewDeliveryCodAmount &&
                    order.paymentStatusEnum() != PaymentStatus.PAID &&
                    order.remainingAmount > 0L
                ) {
                    order.remainingAmount
                } else {
                    0L
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
                    remainingAmount = codOnlyRemaining
                )
            }
        }
    }

    fun scopeTransactions(
        transactions: List<AccountingEntry>,
        session: ActiveRoleSession
    ): List<AccountingEntry> {
        val canSeeMoney = session.canViewFinancialAmounts
        if (!canSeeMoney) return emptyList()

        val byAgency = if (session.isolatedAgency.isNullOrBlank()) {
            transactions
        } else {
            transactions.filter {
                it.agencyName.isBlank() || it.agencyName.equals(session.isolatedAgency, ignoreCase = true)
            }
        }

        // Cashiers only see counter collections & cash register Z entries, never global company expenses/payroll/SYSCOHADA
        return if (session.role == WorkspaceRole.CAISSIER && !session.has(AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA)) {
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
        val scoped = if (!session.has(AccessPermission.MANAGE_CLIENTS)) {
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
        return if (session.has(AccessPermission.VIEW_HR_SALARIES)) {
            byAgency
        } else {
            byAgency.map { it.copy(monthlySalary = 0L, commissionRate = 0.0) }
        }
    }

    fun scopeStocks(
        stocks: List<StockItem>,
        session: ActiveRoleSession
    ): List<StockItem> {
        val canSeeStocks = session.has(AccessPermission.ADVANCE_PRODUCTION) ||
            session.has(AccessPermission.CONFIGURE_PRINTER_AND_AGENCY) ||
            session.has(AccessPermission.VIEW_AGENCY_REVENUE)
        if (!canSeeStocks) return emptyList()
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
        if (!session.has(AccessPermission.HANDLE_SAV_CLAIMS)) return emptyList()
        if (session.isolatedAgency.isNullOrBlank()) return claims
        val orderNums = scopedOrders.map { it.orderNumber }.toSet()
        return claims.filter { it.orderNumber.isBlank() || it.orderNumber in orderNums }
    }
}

/**
 * Pure analytics helpers for the 6 role-specialized dashboards (unit-testable, zero Android dependencies).
 */
data class AgencyPerformanceSummary(
    val agencyName: String,
    val revenueFcfa: Long,
    val orderCount: Int,
    val piecesCount: Long,
    val lateCount: Int
)

data class GovernanceAlertsSnapshot(
    val cashVariances: List<AccountingEntry>,
    val exceptionalDiscountOrders: List<PressingOrder>,
    val openClaims: List<ClaimRecord>,
    val deniedAuditEventsCount: Int
) {
    val totalAlertCount: Int
        get() = cashVariances.size + exceptionalDiscountOrders.size + openClaims.size + deniedAuditEventsCount
}

object RoleDashboardAnalytics {

    fun computePunctualityRatePercent(orders: List<PressingOrder>, nowMillis: Long = System.currentTimeMillis()): Int {
        val activeOrDone = orders.filter { it.orderStatusEnum() != OrderStatus.CANCELLED }
        if (activeOrDone.isEmpty()) return 100
        val lateCount = activeOrDone.count { it.isLate(nowMillis) }
        return (((activeOrDone.size - lateCount).toDouble() / activeOrDone.size.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    fun computeAgencyComparison(
        orders: List<PressingOrder>,
        configuredAgencies: List<String>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<AgencyPerformanceSummary> {
        val agencyNames = (configuredAgencies + orders.map { it.agencyName })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf("Agence Principale") }

        return agencyNames.map { agency ->
            val agencyOrders = orders.filter {
                it.orderStatusEnum() != OrderStatus.CANCELLED &&
                    (it.agencyName.equals(agency, ignoreCase = true) ||
                        (it.agencyName.isBlank() && agency == agencyNames.first()))
            }
            AgencyPerformanceSummary(
                agencyName = agency,
                revenueFcfa = agencyOrders.sumOf { it.totalAmount },
                orderCount = agencyOrders.size,
                piecesCount = agencyOrders.sumOf { it.itemCount.coerceAtLeast(1L) },
                lateCount = agencyOrders.count { it.isLate(nowMillis) }
            )
        }.sortedByDescending { it.revenueFcfa }
    }

    fun detectGovernanceAlerts(
        orders: List<PressingOrder>,
        transactions: List<AccountingEntry>,
        claims: List<ClaimRecord>,
        auditLog: List<RoleAuditEvent>,
        maxCashierDiscountPercent: Double
    ): GovernanceAlertsSnapshot {
        val cashVariances = transactions.filter { entry ->
            entry.entryType == "CASH_CLOSE" &&
                (entry.label.contains("Manquant", ignoreCase = true) ||
                    entry.label.contains("Excédent", ignoreCase = true))
        }
        val exceptionalDiscounts = orders.filter { ord ->
            ord.status != OrderStatus.CANCELLED.name &&
                ord.subtotal > 0L &&
                ord.discountAmount > 0L &&
                ((ord.discountAmount.toDouble() / ord.subtotal.toDouble()) * 100.0) > maxCashierDiscountPercent + 0.01
        }
        val openClaims = claims.filter { it.status != "RESOLVED" }
        val deniedCount = auditLog.count { !it.allowed }
        return GovernanceAlertsSnapshot(
            cashVariances = cashVariances,
            exceptionalDiscountOrders = exceptionalDiscounts,
            openClaims = openClaims,
            deniedAuditEventsCount = deniedCount
        )
    }

    fun computeQcConformityPercent(orders: List<PressingOrder>): Int {
        val checkedOrRework = orders.filter {
            it.orderStatusEnum() in listOf(
                OrderStatus.READY,
                OrderStatus.PICKED_UP,
                OrderStatus.DELIVERED,
                OrderStatus.QUALITY_CHECK
            ) || it.statusHistory.any { h -> h.contains("Reprise", ignoreCase = true) }
        }
        if (checkedOrRework.isEmpty()) return 100
        val reworkCount = checkedOrRework.count { ord ->
            ord.statusHistory.any { it.contains("Reprise", ignoreCase = true) }
        }
        return (((checkedOrRework.size - reworkCount).toDouble() / checkedOrRework.size.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    fun computeDriverCodTotal(orders: List<PressingOrder>): Long =
        orders.filter {
            it.orderStatusEnum() != OrderStatus.DELIVERED &&
                it.orderStatusEnum() != OrderStatus.CANCELLED &&
                it.paymentStatusEnum() != PaymentStatus.PAID &&
                it.remainingAmount > 0L
        }.sumOf { it.remainingAmount }
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

        val ownerEmail = company?.email?.trim()?.lowercase().orEmpty()
        val isPlatformSuperAdmin = cleanEmail == "lebest2london@gmail.com"
        val isExplicitCompanyOwner = isPlatformSuperAdmin ||
            (ownerEmail.isNotBlank() && cleanEmail.isNotBlank() && cleanEmail == ownerEmail)

        val matchedEmployee = if (isExplicitCompanyOwner) {
            null
        } else {
            val activeStaff = employees.filter { it.active }
            val byPhone = if (effectivePhone.isNotBlank()) {
                activeStaff.firstOrNull { emp -> emp.phone.isNotBlank() && phonesMatch(emp.phone, effectivePhone) }
            } else {
                null
            }
            val byName = if (byPhone == null && cleanName.length >= 3) {
                activeStaff.firstOrNull { emp ->
                    emp.fullName.trim().equals(cleanName, ignoreCase = true)
                }
            } else {
                null
            }
            byPhone ?: byName
        }

        val resolvedRole = if (isPlatformSuperAdmin) {
            WorkspaceRole.DIRECTEUR
        } else if (matchedEmployee != null) {
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
            isPlatformSuperAdmin && cleanName.isNotBlank() -> "$cleanName (Super-Admin)"
            isPlatformSuperAdmin -> "Super-Admin · Gestion Pro Africa"
            matchedEmployee != null && matchedEmployee.fullName.isNotBlank() -> matchedEmployee.fullName.trim()
            cleanName.isNotBlank() -> cleanName
            company != null && company.companyName.isNotBlank() && resolvedRole == WorkspaceRole.DIRECTEUR ->
                "Directeur · ${company.companyName}"
            else -> resolvedRole.labelFr
        }

        val perms = if (isPlatformSuperAdmin) {
            AccessPermission.entries.toSet()
        } else {
            rolePermissionsMap[resolvedRole] ?: RolePermissionMatrix.defaultPermissionsFor(resolvedRole)
        }

        return ActiveRoleSession(
            role = resolvedRole,
            operatorName = resolvedOperatorName,
            isolatedAgency = resolvedAgency,
            permissions = perms,
            isSuperAdmin = isPlatformSuperAdmin || isExplicitCompanyOwner
        )
    }
}
