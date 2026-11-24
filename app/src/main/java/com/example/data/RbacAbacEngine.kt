package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import java.security.MessageDigest

/**
 * Three primary SaaS system role tiers + isolated Client Portal perimeter
 * per Gestion Pro Africa RBAC + ABAC specification.
 */
enum class SystemRoleTier(val labelFr: String) {
    SUPER_ADMIN("Super-Admin Plateforme"),
    PRESSING_ADMIN("Admin Pressing"),
    PRESSING_MANAGER("Gestionnaire Pressing"),
    CLIENT_PORTAL("Portail Client");

    companion object {
        fun fromSession(session: ActiveRoleSession): SystemRoleTier = when {
            session.isSuperAdmin && session.role == WorkspaceRole.DIRECTEUR -> SUPER_ADMIN
            session.role == WorkspaceRole.DIRECTEUR -> PRESSING_ADMIN
            else -> PRESSING_MANAGER
        }
    }
}

/**
 * Six predefined operational permission profiles assignable to any PRESSING_MANAGER,
 * plus CUSTOM when fine-tuned individually by the Admin Pressing.
 */
enum class ManagerPermissionProfile(
    val labelFr: String,
    val summaryFr: String,
    val defaultPermissions: Set<AccessPermission>,
    val defaultMaxDiscountPercent: Int,
    val defaultMaxRefundFcfa: Long,
    val defaultMaxExpenseFcfa: Long,
    val defaultMaxDisbursementFcfa: Long
) {
    RECEPTIONNISTE(
        labelFr = "Réceptionniste",
        summaryFr = "Clients, nouvelles commandes, photos, étiquettes QR et reçus",
        defaultPermissions = setOf(
            AccessPermission.CREATE_ORDER,
            AccessPermission.EDIT_ORDER,
            AccessPermission.VIEW_RECEPTION_PHOTOS,
            AccessPermission.REPRINT_GARMENT_LABEL,
            AccessPermission.REPRINT_CLIENT_RECEIPT,
            AccessPermission.MANAGE_CLIENTS,
            AccessPermission.SEND_WHATSAPP_NOTIFICATION
        ),
        defaultMaxDiscountPercent = 5,
        defaultMaxRefundFcfa = 0L,
        defaultMaxExpenseFcfa = 0L,
        defaultMaxDisbursementFcfa = 0L
    ),
    CAISSIER(
        labelFr = "Caissier",
        summaryFr = "Encaissements Mobile Money/Espèces, paiements partiels, ouverture/clôture Z",
        defaultPermissions = setOf(
            AccessPermission.CREATE_ORDER,
            AccessPermission.EDIT_ORDER,
            AccessPermission.APPLY_STANDARD_DISCOUNT,
            AccessPermission.COLLECT_PAYMENT,
            AccessPermission.MANAGE_CASH_REGISTER_Z,
            AccessPermission.MANAGE_CLIENTS,
            AccessPermission.SEND_WHATSAPP_NOTIFICATION,
            AccessPermission.REPRINT_CLIENT_RECEIPT,
            AccessPermission.REPRINT_GARMENT_LABEL
        ),
        defaultMaxDiscountPercent = 5,
        defaultMaxRefundFcfa = 5_000L,
        defaultMaxExpenseFcfa = 10_000L,
        defaultMaxDisbursementFcfa = 20_000L
    ),
    AGENT_ATELIER(
        labelFr = "Agent atelier",
        summaryFr = "Tri, lavage, séchage, repassage, photos réception et étiquettes sans prix",
        defaultPermissions = setOf(
            AccessPermission.ADVANCE_PRODUCTION,
            AccessPermission.VIEW_RECEPTION_PHOTOS,
            AccessPermission.REPRINT_GARMENT_LABEL
        ),
        defaultMaxDiscountPercent = 0,
        defaultMaxRefundFcfa = 0L,
        defaultMaxExpenseFcfa = 0L,
        defaultMaxDisbursementFcfa = 0L
    ),
    GESTIONNAIRE_STOCK(
        labelFr = "Gestionnaire stock",
        summaryFr = "Entrées, sorties, inventaires consommables et suivi machines atelier",
        defaultPermissions = setOf(
            AccessPermission.ADVANCE_PRODUCTION,
            AccessPermission.VIEW_RECEPTION_PHOTOS
        ),
        defaultMaxDiscountPercent = 0,
        defaultMaxRefundFcfa = 0L,
        defaultMaxExpenseFcfa = 15_000L,
        defaultMaxDisbursementFcfa = 15_000L
    ),
    LIVREUR(
        labelFr = "Livreur",
        summaryFr = "Collectes et livraisons GPS assignées, preuve de livraison et WhatsApp",
        defaultPermissions = setOf(
            AccessPermission.MANAGE_DELIVERY_RUNS,
            AccessPermission.SEND_WHATSAPP_NOTIFICATION
        ),
        defaultMaxDiscountPercent = 0,
        defaultMaxRefundFcfa = 0L,
        defaultMaxExpenseFcfa = 5_000L,
        defaultMaxDisbursementFcfa = 0L
    ),
    POLYVALENT(
        labelFr = "Polyvalent",
        summaryFr = "Accueil, commandes, clients, atelier, qualité et caisse sous plafond agence",
        defaultPermissions = setOf(
            AccessPermission.CREATE_ORDER,
            AccessPermission.EDIT_ORDER,
            AccessPermission.APPLY_STANDARD_DISCOUNT,
            AccessPermission.ADVANCE_PRODUCTION,
            AccessPermission.VALIDATE_QUALITY,
            AccessPermission.RETURN_FOR_REWORK,
            AccessPermission.VIEW_RECEPTION_PHOTOS,
            AccessPermission.REPRINT_GARMENT_LABEL,
            AccessPermission.COLLECT_PAYMENT,
            AccessPermission.MANAGE_CASH_REGISTER_Z,
            AccessPermission.MANAGE_CLIENTS,
            AccessPermission.SEND_WHATSAPP_NOTIFICATION,
            AccessPermission.HANDLE_SAV_CLAIMS,
            AccessPermission.REPRINT_CLIENT_RECEIPT
        ),
        defaultMaxDiscountPercent = 10,
        defaultMaxRefundFcfa = 10_000L,
        defaultMaxExpenseFcfa = 15_000L,
        defaultMaxDisbursementFcfa = 25_000L
    ),
    PERSONNALISE(
        labelFr = "Personnalisé",
        summaryFr = "Permissions, agences et plafonds ajustés individuellement par l'Admin Pressing",
        defaultPermissions = setOf(
            AccessPermission.CREATE_ORDER,
            AccessPermission.EDIT_ORDER,
            AccessPermission.MANAGE_CLIENTS,
            AccessPermission.REPRINT_GARMENT_LABEL
        ),
        defaultMaxDiscountPercent = 5,
        defaultMaxRefundFcfa = 5_000L,
        defaultMaxExpenseFcfa = 10_000L,
        defaultMaxDisbursementFcfa = 20_000L
    );

    companion object {
        fun fromRaw(raw: String): ManagerPermissionProfile =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: PERSONNALISE
    }
}

enum class PermissionOverrideEffect { ALLOW, DENY }

data class PermissionOverride(
    val permission: AccessPermission,
    val effect: PermissionOverrideEffect,
    val validUntilMillis: Long? = null, // null = permanent; non-null = temporary permission
    val branchScope: String? = null,
    val reason: String = ""
) {
    fun isActiveAt(nowMillis: Long): Boolean = validUntilMillis == null || nowMillis <= validUntilMillis
}

/**
 * Per-manager configurable ABAC policy persisted in Firestore under users/{uid}/manager_policies/{employeeId}.
 */
@Keep
@IgnoreExtraProperties
data class ManagerAccessPolicy(
    @DocumentId val id: String = "",
    val userId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val profileCode: String = ManagerPermissionProfile.RECEPTIONNISTE.name,
    val allowedAgencies: List<String> = emptyList(),
    val grantedPermissions: List<String> = emptyList(),
    val deniedPermissions: List<String> = emptyList(),
    val maxDiscountPercent: Int = 5,
    val maxRefundFcfa: Long = 5_000L,
    val maxExpenseFcfa: Long = 10_000L,
    val maxDisbursementFcfa: Long = 20_000L,
    val dailyOperationCapFcfa: Long = 100_000L,
    val allowDeferredPayment: Boolean = false,
    val allowCashRegisterClose: Boolean = false,
    val accessStartHour: Int = 7,
    val accessEndHour: Int = 21,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(ownerUid: String, isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to ownerUid,
            "employeeId" to employeeId.trim().take(128),
            "employeeName" to employeeName.trim().take(120),
            "profileCode" to ManagerPermissionProfile.fromRaw(profileCode).name,
            "allowedAgencies" to allowedAgencies.map { it.trim().take(100) }.filter { it.isNotBlank() }.distinct().take(20),
            "grantedPermissions" to grantedPermissions.distinct().take(200),
            "deniedPermissions" to deniedPermissions.distinct().take(200),
            "maxDiscountPercent" to maxDiscountPercent.coerceIn(0, 30),
            "maxRefundFcfa" to maxRefundFcfa.coerceIn(0L, 100_000_000L),
            "maxExpenseFcfa" to maxExpenseFcfa.coerceIn(0L, 100_000_000L),
            "maxDisbursementFcfa" to maxDisbursementFcfa.coerceIn(0L, 100_000_000L),
            "dailyOperationCapFcfa" to dailyOperationCapFcfa.coerceIn(0L, 500_000_000L),
            "allowDeferredPayment" to allowDeferredPayment,
            "allowCashRegisterClose" to allowCashRegisterClose,
            "accessStartHour" to accessStartHour.coerceIn(0, 23),
            "accessEndHour" to accessEndHour.coerceIn(1, 24),
            "active" to active,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        }
        return map
    }
}

enum class SensitiveOperationType(val labelFr: String) {
    EXCEPTIONAL_DISCOUNT("Remise exceptionnelle"),
    REFUND("Remboursement client"),
    ORDER_CANCELLATION("Annulation commande confirmée"),
    LARGE_EXPENSE("Dépense importante"),
    CASH_ADJUSTMENT("Correction / transfert de caisse"),
    STOCK_ADJUSTMENT("Ajustement d'inventaire"),
    PRICE_OVERRIDE("Modification exceptionnelle tarif");

    companion object {
        fun fromRaw(raw: String): SensitiveOperationType =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: EXCEPTIONAL_DISCOUNT
    }
}

enum class ApprovalStatus(val labelFr: String) {
    PENDING("En attente"),
    APPROVED("Approuvée"),
    REJECTED("Rejetée"),
    EXPIRED("Expirée"),
    EXECUTED("Exécutée"),
    CANCELLED("Annulée");

    companion object {
        fun fromRaw(raw: String): ApprovalStatus =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: PENDING
    }
}

/**
 * Firestore-backed Administrative Approval Request for sensitive operations exceeding
 * a manager's configured financial ceiling (Separation of Duties enforced).
 */
@Keep
@IgnoreExtraProperties
data class ApprovalRequestRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val agencyName: String = "",
    val requestedBy: String = "",
    val requestedByRole: String = "",
    val operationType: String = SensitiveOperationType.REFUND.name,
    val resourceRef: String = "",
    val amountFcfa: Long = 0L,
    val ceilingFcfa: Long = 0L,
    val reason: String = "",
    val payloadHash: String = "",
    val status: String = ApprovalStatus.PENDING.name,
    val reviewedBy: String = "",
    val reviewNote: String = "",
    val expiresAtMillis: Long = 0L,
    val executedAtMillis: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun operationEnum(): SensitiveOperationType = SensitiveOperationType.fromRaw(operationType)
    fun statusEnum(): ApprovalStatus = ApprovalStatus.fromRaw(status)

    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "agencyName" to agencyName.trim().take(100),
        "requestedBy" to requestedBy.trim().take(100),
        "requestedByRole" to requestedByRole.trim().take(60),
        "operationType" to operationEnum().name,
        "resourceRef" to resourceRef.trim().take(80),
        "amountFcfa" to amountFcfa.coerceIn(0L, 100_000_000L),
        "ceilingFcfa" to ceilingFcfa.coerceIn(0L, 100_000_000L),
        "reason" to reason.trim().take(300),
        "payloadHash" to payloadHash.ifBlank { AuthorizationEngine.computePayloadHash(operationType, resourceRef, amountFcfa, agencyName) },
        "status" to ApprovalStatus.PENDING.name,
        "reviewedBy" to "",
        "reviewNote" to "",
        "expiresAtMillis" to (if (expiresAtMillis > 0L) expiresAtMillis else System.currentTimeMillis() + 24 * 3_600_000L),
        "executedAtMillis" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

/**
 * Audited, time-limited, read-only-by-default Super-Admin support session (Section 3.9).
 */
data class SupportAccessSession(
    val ticketId: String,
    val superAdminName: String,
    val targetCompanyName: String,
    val reason: String,
    val startsAtMillis: Long,
    val expiresAtMillis: Long,
    val readOnly: Boolean = true,
    val revoked: Boolean = false
) {
    fun isValidAt(nowMillis: Long = System.currentTimeMillis()): Boolean =
        !revoked && ticketId.isNotBlank() && reason.isNotBlank() && nowMillis in startsAtMillis..expiresAtMillis
}

sealed class AuthorizationDecision {
    data object Allowed : AuthorizationDecision()
    data class RequiresApproval(val reasonFr: String, val limitFcfa: Long) : AuthorizationDecision()
    data class Denied(val reasonFr: String) : AuthorizationDecision()
}

/**
 * Centralized RBAC + ABAC Authorization Engine implementing Deny-by-Default,
 * explicit DENY precedence, temporary permission expiration, multi-agency isolation,
 * configurable financial ceilings (with anti-splitting daily total), and separation of duties.
 */
object AuthorizationEngine {

    /** Permissions that an Admin Pressing can NEVER delegate to a Pressing Manager. */
    val nonDelegablePermissions: Set<AccessPermission> = setOf(
        AccessPermission.EDIT_PRIVILEGE_MATRIX,
        AccessPermission.VIEW_HR_SALARIES,
        AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA,
        AccessPermission.VIEW_SECURITY_AUDIT_LOG
    )

    fun computePayloadHash(
        operationType: String,
        resourceRef: String,
        amountFcfa: Long,
        agencyName: String
    ): String {
        val raw = "${operationType.trim().uppercase()}|${resourceRef.trim().uppercase()}|$amountFcfa|${agencyName.trim().lowercase()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    /**
     * Resolves effective permissions for a user applying:
     * 1. Role or Manager Profile base permissions
     * 2. Active temporary/permanent ALLOW overrides
     * 3. Explicit DENY overrides (ALWAYS win over inherited/profile grants)
     * 4. Non-delegable guard for non-Director roles
     */
    fun resolveEffectivePermissions(
        role: WorkspaceRole,
        profile: ManagerPermissionProfile? = null,
        overrides: List<PermissionOverride> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ): Set<AccessPermission> {
        val base = when {
            role == WorkspaceRole.DIRECTEUR -> AccessPermission.entries.toMutableSet()
            profile != null -> profile.defaultPermissions.toMutableSet()
            else -> RolePermissionMatrix.defaultPermissionsFor(role).toMutableSet()
        }

        val activeOverrides = overrides.filter { it.isActiveAt(nowMillis) }
        activeOverrides
            .filter { it.effect == PermissionOverrideEffect.ALLOW }
            .forEach { base.add(it.permission) }

        // Explicit DENY always prevails over role & profile inheritance
        activeOverrides
            .filter { it.effect == PermissionOverrideEffect.DENY }
            .forEach { base.remove(it.permission) }

        // Non-Director roles can never receive non-delegable platform/governance permissions
        if (role != WorkspaceRole.DIRECTEUR) {
            base.removeAll(nonDelegablePermissions)
        }
        return base
    }

    /**
     * Checks whether a user can perform an action under ABAC context
     * (account status, subscription status, agency isolation, access hours, overrides).
     */
    fun evaluateContextualAccess(
        session: ActiveRoleSession,
        permission: AccessPermission,
        targetAgency: String? = null,
        subscriptionStatus: String = "ACTIVE",
        policy: ManagerAccessPolicy? = null,
        overrides: List<PermissionOverride> = emptyList(),
        currentHour24: Int = 12,
        nowMillis: Long = System.currentTimeMillis()
    ): AuthorizationDecision {
        if (subscriptionStatus.equals("SUSPENDED", ignoreCase = true) && !session.isSuperAdmin) {
            return AuthorizationDecision.Denied("L'abonnement de l'entreprise est suspendu. Opération bloquée.")
        }

        // Strict multi-agency perimeter check
        if (!targetAgency.isNullOrBlank() && session.role != WorkspaceRole.DIRECTEUR) {
            val allowedBySession = session.isolatedAgency.isNullOrBlank() ||
                session.isolatedAgency.equals(targetAgency, ignoreCase = true)
            val allowedByPolicy = policy == null || policy.allowedAgencies.isEmpty() ||
                policy.allowedAgencies.any { it.equals(targetAgency, ignoreCase = true) }
            if (!allowedBySession || !allowedByPolicy) {
                return AuthorizationDecision.Denied("Cette agence ne fait pas partie de votre périmètre d'accès.")
            }
        }

        // Access hours check for managers with configured working schedule
        if (policy != null && session.role != WorkspaceRole.DIRECTEUR) {
            if (!policy.active) {
                return AuthorizationDecision.Denied("Votre profil d'accès gestionnaire est désactivé.")
            }
            if (currentHour24 < policy.accessStartHour || currentHour24 >= policy.accessEndHour) {
                return AuthorizationDecision.Denied(
                    "Accès restreint aux horaires autorisés (${policy.accessStartHour}h–${policy.accessEndHour}h)."
                )
            }
            if (permission.name in policy.deniedPermissions) {
                return AuthorizationDecision.Denied("Interdiction explicite configurée sur « ${permission.labelFr} ».")
            }
        }

        // Explicit active DENY override always prevails
        val hasExplicitDeny = overrides.any {
            it.permission == permission && it.effect == PermissionOverrideEffect.DENY && it.isActiveAt(nowMillis)
        }
        if (hasExplicitDeny) {
            return AuthorizationDecision.Denied("Interdiction explicite appliquée pour « ${permission.labelFr} ».")
        }

        val hasActiveAllowOverride = overrides.any {
            it.permission == permission && it.effect == PermissionOverrideEffect.ALLOW && it.isActiveAt(nowMillis)
        }
        val grantedByPolicy = policy != null && permission.name in policy.grantedPermissions &&
            (session.role == WorkspaceRole.DIRECTEUR || permission !in nonDelegablePermissions)

        if (!session.has(permission) && !hasActiveAllowOverride && !grantedByPolicy) {
            return AuthorizationDecision.Denied(
                "Vous ne disposez pas de l'autorisation nécessaire pour effectuer cette opération."
            )
        }

        return AuthorizationDecision.Allowed
    }

    /**
     * Checks configurable financial ceilings (discount %, refund, expense, disbursement)
     * plus daily cumulative anti-splitting cap. Returns RequiresApproval when exceeded.
     */
    fun checkFinancialLimit(
        session: ActiveRoleSession,
        operation: SensitiveOperationType,
        amountFcfa: Long,
        discountPercent: Double = 0.0,
        dailyCumulativeFcfa: Long = 0L,
        policy: ManagerAccessPolicy
    ): AuthorizationDecision {
        if (session.role == WorkspaceRole.DIRECTEUR || session.isSuperAdmin) {
            return AuthorizationDecision.Allowed
        }
        if (amountFcfa < 0L || discountPercent < 0.0) {
            return AuthorizationDecision.Denied("Montant ou pourcentage invalide.")
        }
        if (dailyCumulativeFcfa + amountFcfa > policy.dailyOperationCapFcfa && policy.dailyOperationCapFcfa > 0L) {
            return AuthorizationDecision.RequiresApproval(
                reasonFr = "Plafond journalier cumulé dépassé (${policy.dailyOperationCapFcfa} FCFA). Validation administrative requise.",
                limitFcfa = policy.dailyOperationCapFcfa
            )
        }

        return when (operation) {
            SensitiveOperationType.EXCEPTIONAL_DISCOUNT -> {
                if (discountPercent > policy.maxDiscountPercent.toDouble() + 0.01) {
                    AuthorizationDecision.RequiresApproval(
                        reasonFr = "Remise (${discountPercent.toInt()} %) supérieure à votre plafond (${policy.maxDiscountPercent} %). Validation Admin requise.",
                        limitFcfa = policy.maxDiscountPercent.toLong()
                    )
                } else {
                    AuthorizationDecision.Allowed
                }
            }

            SensitiveOperationType.REFUND -> {
                if (amountFcfa > policy.maxRefundFcfa) {
                    AuthorizationDecision.RequiresApproval(
                        reasonFr = "Le montant dépasse votre plafond de remboursement (${policy.maxRefundFcfa} FCFA). Validation Admin requise.",
                        limitFcfa = policy.maxRefundFcfa
                    )
                } else {
                    AuthorizationDecision.Allowed
                }
            }

            SensitiveOperationType.LARGE_EXPENSE -> {
                if (amountFcfa > policy.maxExpenseFcfa) {
                    AuthorizationDecision.RequiresApproval(
                        reasonFr = "Dépense supérieure à votre plafond autorisé (${policy.maxExpenseFcfa} FCFA). Validation Admin requise.",
                        limitFcfa = policy.maxExpenseFcfa
                    )
                } else {
                    AuthorizationDecision.Allowed
                }
            }

            SensitiveOperationType.CASH_ADJUSTMENT -> {
                if (amountFcfa > policy.maxDisbursementFcfa) {
                    AuthorizationDecision.RequiresApproval(
                        reasonFr = "Décaissement / ajustement supérieur à votre plafond (${policy.maxDisbursementFcfa} FCFA). Validation Admin requise.",
                        limitFcfa = policy.maxDisbursementFcfa
                    )
                } else {
                    AuthorizationDecision.Allowed
                }
            }

            SensitiveOperationType.ORDER_CANCELLATION,
            SensitiveOperationType.STOCK_ADJUSTMENT,
            SensitiveOperationType.PRICE_OVERRIDE -> AuthorizationDecision.RequiresApproval(
                reasonFr = "Cette opération sensible (${operation.labelFr}) nécessite la validation de votre administrateur.",
                limitFcfa = 0L
            )
        }
    }

    /**
     * Enforces Separation of Duties, payload hash immutability, expiration, and single-execution
     * on Administrative Approval Requests.
     */
    fun validateApprovalReview(
        reviewer: ActiveRoleSession,
        request: ApprovalRequestRecord,
        nowMillis: Long = System.currentTimeMillis()
    ): AuthorizationDecision {
        if (reviewer.role != WorkspaceRole.DIRECTEUR && !reviewer.isSuperAdmin) {
            return AuthorizationDecision.Denied("Seul l'Admin Pressing / Directeur Général peut approuver ou rejeter cette demande.")
        }
        if (request.statusEnum() != ApprovalStatus.PENDING) {
            return AuthorizationDecision.Denied("Cette demande a déjà été traitée (${request.statusEnum().labelFr}).")
        }
        if (request.expiresAtMillis > 0L && nowMillis > request.expiresAtMillis) {
            return AuthorizationDecision.Denied("Cette demande de validation a expiré.")
        }
        // Strict Separation of Duties: requester cannot approve their own request
        if (request.requestedBy.trim().equals(reviewer.operatorName.trim(), ignoreCase = true) && !reviewer.isSuperAdmin) {
            return AuthorizationDecision.Denied("Séparation des responsabilités : vous ne pouvez pas approuver votre propre demande.")
        }
        // Integrity check: ensure amount/resource wasn't tampered with after creation
        val expectedHash = computePayloadHash(
            request.operationType,
            request.resourceRef,
            request.amountFcfa,
            request.agencyName
        )
        if (request.payloadHash.isNotBlank() && request.payloadHash != expectedHash) {
            return AuthorizationDecision.Denied("Intégrité compromise : le montant ou l'objet de la demande a été modifié.")
        }
        return AuthorizationDecision.Allowed
    }

    fun canExecuteApprovedRequest(
        request: ApprovalRequestRecord,
        nowMillis: Long = System.currentTimeMillis()
    ): AuthorizationDecision {
        if (request.statusEnum() == ApprovalStatus.EXECUTED || request.executedAtMillis > 0L) {
            return AuthorizationDecision.Denied("Cette demande approuvée a déjà été exécutée (exécution unique garantie).")
        }
        if (request.statusEnum() != ApprovalStatus.APPROVED) {
            return AuthorizationDecision.Denied("Une demande non approuvée ne peut pas être exécutée.")
        }
        if (request.expiresAtMillis > 0L && nowMillis > request.expiresAtMillis) {
            return AuthorizationDecision.Denied("L'approbation a expiré avant son exécution.")
        }
        val expectedHash = computePayloadHash(
            request.operationType,
            request.resourceRef,
            request.amountFcfa,
            request.agencyName
        )
        if (request.payloadHash.isNotBlank() && request.payloadHash != expectedHash) {
            return AuthorizationDecision.Denied("Approbation invalidée suite à une modification du contenu.")
        }
        return AuthorizationDecision.Allowed
    }
}
