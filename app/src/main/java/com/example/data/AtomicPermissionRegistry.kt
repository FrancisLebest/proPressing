package com.example.data

enum class PermissionActionType(val labelFr: String) {
    CONSULTER("Consulter"),
    CREER("Créer"),
    MODIFIER("Modifier"),
    ANNULER("Annuler"),
    VALIDER("Valider"),
    IMPRIMER("Imprimer"),
    EXPORTER("Exporter"),
    ADMINISTRER("Administrer")
}

enum class PermissionRiskLevel(val labelFr: String) {
    LOW("Standard"),
    MEDIUM("Modéré"),
    HIGH("Sensible"),
    CRITICAL("Critique")
}

data class AtomicPermission(
    val code: String,
    val labelFr: String,
    val moduleFr: String,
    val actionType: PermissionActionType,
    val riskLevel: PermissionRiskLevel = PermissionRiskLevel.LOW,
    val isDelegable: Boolean = true,
    val minPlan: SubscriptionPlan = SubscriptionPlan.STARTER,
    val linkedPrivilege: AccessPermission? = null
)

/**
 * Multi-tenant authorization context for strict organization, branch, subscription,
 * session validity, and ABAC evaluation.
 */
data class TenantAuthorizationContext(
    val userId: String,
    val userActive: Boolean = true,
    val sessionRevoked: Boolean = false,
    val activeOrganizationId: String,
    val targetOrganizationId: String = activeOrganizationId,
    val organizationStatus: String = "ACTIVE",
    val subscriptionPlan: SubscriptionPlan = SubscriptionPlan.BUSINESS,
    val subscriptionStatus: String = "ACTIVE",
    val activeModules: Set<String> = setOf("PRESSING", "LAVERIE", "BLANCHISSERIE", "FINANCES", "LIVRAISON", "STOCKS"),
    val roleTier: SystemRoleTier = SystemRoleTier.PRESSING_MANAGER,
    val workspaceRole: WorkspaceRole = WorkspaceRole.CAISSIER,
    val assignedBranches: Set<String> = emptySet(),
    val targetBranch: String? = null,
    val currentHour24: Int = 12,
    val nowMillis: Long = System.currentTimeMillis()
)

data class AtomicPermissionOverride(
    val permissionCode: String,
    val effect: PermissionOverrideEffect,
    val validUntilMillis: Long? = null,
    val reason: String = ""
) {
    fun isActiveAt(nowMillis: Long): Boolean = validUntilMillis == null || nowMillis <= validUntilMillis
}

/**
 * Versioned centralized registry of 138 granular atomic permissions across all 19 modules
 * of GESTION PRO AFRICA — Pressing Pro (Section 7 of the Security Architecture Spec).
 */
object AtomicPermissionRegistry {

    const val REGISTRY_VERSION = "2026.1.0"

    val allPermissions: List<AtomicPermission> = listOf(
        // 1. Entreprises (Plateforme — non délégable) — 6
        AtomicPermission("platform.organizations.view", "Consulter les entreprises", "Plateforme SaaS", PermissionActionType.CONSULTER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.ENTERPRISE),
        AtomicPermission("platform.organizations.create", "Créer une entreprise", "Plateforme SaaS", PermissionActionType.CREER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.ENTERPRISE),
        AtomicPermission("platform.organizations.update", "Modifier une entreprise", "Plateforme SaaS", PermissionActionType.MODIFIER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.ENTERPRISE),
        AtomicPermission("platform.organizations.suspend", "Suspendre une entreprise", "Plateforme SaaS", PermissionActionType.ANNULER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.ENTERPRISE),
        AtomicPermission("platform.organizations.activate", "Activer une entreprise", "Plateforme SaaS", PermissionActionType.VALIDER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.ENTERPRISE),
        AtomicPermission("platform.organizations.archive", "Archiver une entreprise", "Plateforme SaaS", PermissionActionType.ADMINISTRER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.ENTERPRISE),

        // 2. Administration entreprise — 6
        AtomicPermission("organization.settings.view", "Consulter paramètres entreprise", "Administration", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.CONFIGURE_PRINTER_AND_AGENCY),
        AtomicPermission("organization.settings.update", "Modifier paramètres entreprise", "Administration", PermissionActionType.MODIFIER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.CONFIGURE_PRINTER_AND_AGENCY),
        AtomicPermission("organization.branding.update", "Modifier logo & identité", "Administration", PermissionActionType.MODIFIER, PermissionRiskLevel.MEDIUM, false, SubscriptionPlan.STARTER, AccessPermission.CONFIGURE_PRINTER_AND_AGENCY),
        AtomicPermission("organization.branches.view", "Consulter les agences", "Administration", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER),
        AtomicPermission("organization.branches.create", "Créer une agence", "Administration", PermissionActionType.CREER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.ENTERPRISE, AccessPermission.CONFIGURE_PRINTER_AND_AGENCY),
        AtomicPermission("organization.branches.update", "Modifier une agence", "Administration", PermissionActionType.MODIFIER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.ENTERPRISE, AccessPermission.CONFIGURE_PRINTER_AND_AGENCY),

        // 3. Utilisateurs & Accès — 10
        AtomicPermission("users.view", "Consulter les utilisateurs", "Utilisateurs", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("users.invite", "Inviter un utilisateur", "Utilisateurs", PermissionActionType.CREER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("users.create_manager", "Créer un gestionnaire", "Utilisateurs", PermissionActionType.CREER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("users.update_manager", "Modifier un gestionnaire", "Utilisateurs", PermissionActionType.MODIFIER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("users.activate", "Activer un compte", "Utilisateurs", PermissionActionType.VALIDER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("users.deactivate", "Désactiver un compte", "Utilisateurs", PermissionActionType.ANNULER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("users.revoke_sessions", "Révoquer les sessions actives", "Utilisateurs", PermissionActionType.ADMINISTRER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("users.assign_branch", "Affecter aux agences", "Utilisateurs", PermissionActionType.MODIFIER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("users.assign_permissions", "Attribuer des permissions", "Utilisateurs", PermissionActionType.ADMINISTRER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.STARTER, AccessPermission.EDIT_PRIVILEGE_MATRIX),
        AtomicPermission("users.view_activity", "Consulter historique activité", "Utilisateurs", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_SECURITY_AUDIT_LOG),

        // 4. Commandes — 16
        AtomicPermission("orders.view_own", "Voir ses propres commandes", "Commandes", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.CREATE_ORDER),
        AtomicPermission("orders.view_assigned", "Voir commandes assignées", "Commandes", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.EDIT_ORDER),
        AtomicPermission("orders.view_branch", "Voir commandes de l'agence", "Commandes", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.EDIT_ORDER),
        AtomicPermission("orders.view_all", "Voir toutes les commandes (multi-agences)", "Commandes", PermissionActionType.CONSULTER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.ENTERPRISE, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),
        AtomicPermission("orders.create", "Créer une commande", "Commandes", PermissionActionType.CREER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.CREATE_ORDER),
        AtomicPermission("orders.update_draft", "Modifier commande brouillon", "Commandes", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.EDIT_ORDER),
        AtomicPermission("orders.update_confirmed", "Modifier commande confirmée", "Commandes", PermissionActionType.MODIFIER, PermissionRiskLevel.HIGH, true, SubscriptionPlan.STARTER, AccessPermission.EDIT_ORDER),
        AtomicPermission("orders.assign", "Affecter commande à un employé", "Commandes", PermissionActionType.MODIFIER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.EDIT_ORDER),
        AtomicPermission("orders.change_priority", "Modifier priorité (Express)", "Commandes", PermissionActionType.MODIFIER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.EDIT_ORDER),
        AtomicPermission("orders.change_deadline", "Modifier date promise", "Commandes", PermissionActionType.MODIFIER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.EDIT_ORDER),
        AtomicPermission("orders.request_cancellation", "Demander annulation commande", "Commandes", PermissionActionType.ANNULER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.EDIT_ORDER),
        AtomicPermission("orders.approve_cancellation", "Approuver annulation commande", "Commandes", PermissionActionType.VALIDER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.CANCEL_DELETE_ORDER),
        AtomicPermission("orders.cancel", "Annuler une commande", "Commandes", PermissionActionType.ANNULER, PermissionRiskLevel.HIGH, true, SubscriptionPlan.STARTER, AccessPermission.CANCEL_DELETE_ORDER),
        AtomicPermission("orders.archive", "Archiver une commande", "Commandes", PermissionActionType.ADMINISTRER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.EDIT_ORDER),
        AtomicPermission("orders.print_receipt", "Imprimer reçu client", "Commandes", PermissionActionType.IMPRIMER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.REPRINT_CLIENT_RECEIPT),
        AtomicPermission("orders.export", "Exporter les commandes", "Commandes", PermissionActionType.EXPORTER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.EXPORT_FINANCIAL_REPORTS),

        // 5. Vêtements — 8
        AtomicPermission("garments.view", "Consulter les vêtements", "Vêtements", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.CREATE_ORDER),
        AtomicPermission("garments.create", "Ajouter des vêtements", "Vêtements", PermissionActionType.CREER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.CREATE_ORDER),
        AtomicPermission("garments.update", "Modifier fiche vêtement", "Vêtements", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.EDIT_ORDER),
        AtomicPermission("garments.capture_photo", "Photographier les vêtements", "Vêtements", PermissionActionType.CREER, PermissionRiskLevel.LOW, true, SubscriptionPlan.BUSINESS, AccessPermission.CREATE_ORDER),
        AtomicPermission("garments.view_photo", "Consulter photos privées", "Vêtements", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_RECEPTION_PHOTOS),
        AtomicPermission("garments.record_defect", "Enregistrer défauts", "Vêtements", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.CREATE_ORDER),
        AtomicPermission("garments.record_stain", "Enregistrer taches", "Vêtements", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.CREATE_ORDER),
        AtomicPermission("garments.generate_label", "Générer étiquette QR", "Vêtements", PermissionActionType.IMPRIMER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.REPRINT_GARMENT_LABEL),

        // 6. Atelier & Production — 12
        AtomicPermission("production.view", "Consulter file atelier", "Atelier", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("production.receive", "Réceptionner en atelier", "Atelier", PermissionActionType.VALIDER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("production.sort", "Trier les articles", "Atelier", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("production.wash", "Traiter en lavage", "Atelier", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("production.dry_clean", "Nettoyage à sec", "Atelier", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("production.dry", "Traiter en séchage", "Atelier", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("production.iron", "Traiter en repassage", "Atelier", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("production.package", "Emballer & cintrer", "Atelier", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("production.quality_check", "Effectuer contrôle qualité", "Atelier", PermissionActionType.VALIDER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.VALIDATE_QUALITY),
        AtomicPermission("production.mark_ready", "Marquer commande prête", "Atelier", PermissionActionType.VALIDER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.VALIDATE_QUALITY),
        AtomicPermission("production.request_rework", "Renvoyer en reprise", "Atelier", PermissionActionType.ANNULER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.RETURN_FOR_REWORK),
        AtomicPermission("production.assign_employee", "Affecter poste employé", "Atelier", PermissionActionType.ADMINISTRER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.ADVANCE_PRODUCTION),

        // 7. Clients & Fidélité — 8
        AtomicPermission("customers.view", "Consulter les clients", "Clients", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.MANAGE_CLIENTS),
        AtomicPermission("customers.create", "Créer un client", "Clients", PermissionActionType.CREER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.MANAGE_CLIENTS),
        AtomicPermission("customers.update", "Modifier un client", "Clients", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.MANAGE_CLIENTS),
        AtomicPermission("customers.archive", "Archiver un client", "Clients", PermissionActionType.ANNULER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.MANAGE_CLIENTS),
        AtomicPermission("customers.view_history", "Consulter historique client", "Clients", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.MANAGE_CLIENTS),
        AtomicPermission("customers.view_debts", "Consulter créances client", "Clients", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.COLLECT_PAYMENT),
        AtomicPermission("customers.manage_loyalty", "Gérer fidélité & points", "Clients", PermissionActionType.MODIFIER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_CLIENTS),
        AtomicPermission("customers.export", "Exporter base clients", "Clients", PermissionActionType.EXPORTER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.BUSINESS, AccessPermission.EXPORT_FINANCIAL_REPORTS),

        // 8. Tarifs & Remises — 7
        AtomicPermission("pricing.view", "Consulter la grille tarifaire", "Tarifs", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER),
        AtomicPermission("pricing.create", "Créer un article / tarif", "Tarifs", PermissionActionType.CREER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_CATALOG),
        AtomicPermission("pricing.update", "Modifier les tarifs de référence", "Tarifs", PermissionActionType.MODIFIER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_CATALOG),
        AtomicPermission("pricing.archive", "Archiver un tarif", "Tarifs", PermissionActionType.ANNULER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_CATALOG),
        AtomicPermission("pricing.manage_promotions", "Gérer les promotions", "Tarifs", PermissionActionType.ADMINISTRER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_CATALOG),
        AtomicPermission("discounts.apply", "Appliquer remise sous plafond", "Tarifs", PermissionActionType.MODIFIER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.APPLY_STANDARD_DISCOUNT),
        AtomicPermission("discounts.approve", "Valider remise exceptionnelle", "Tarifs", PermissionActionType.VALIDER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.APPROVE_EXCEPTIONAL_DISCOUNT),

        // 9. Paiements — 10
        AtomicPermission("payments.view_own", "Voir ses encaissements", "Paiements", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.COLLECT_PAYMENT),
        AtomicPermission("payments.view_branch", "Voir paiements de l'agence", "Paiements", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.VIEW_AGENCY_REVENUE),
        AtomicPermission("payments.view_all", "Voir paiements multi-agences", "Paiements", PermissionActionType.CONSULTER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.ENTERPRISE, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),
        AtomicPermission("payments.create", "Encaisser un paiement", "Paiements", PermissionActionType.CREER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.COLLECT_PAYMENT),
        AtomicPermission("payments.receive_partial", "Accepter paiement partiel", "Paiements", PermissionActionType.CREER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.COLLECT_PAYMENT),
        AtomicPermission("payments.verify", "Vérifier statut Mobile Money", "Paiements", PermissionActionType.VALIDER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.COLLECT_PAYMENT),
        AtomicPermission("payments.request_refund", "Demander un remboursement", "Paiements", PermissionActionType.ANNULER, PermissionRiskLevel.HIGH, true, SubscriptionPlan.STARTER, AccessPermission.COLLECT_PAYMENT),
        AtomicPermission("payments.approve_refund", "Approuver un remboursement", "Paiements", PermissionActionType.VALIDER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.STARTER, AccessPermission.APPROVE_EXCEPTIONAL_DISCOUNT),
        AtomicPermission("payments.refund", "Exécuter remboursement sous plafond", "Paiements", PermissionActionType.ANNULER, PermissionRiskLevel.HIGH, true, SubscriptionPlan.STARTER, AccessPermission.COLLECT_PAYMENT),
        AtomicPermission("payments.reconcile", "Rapprocher les paiements", "Paiements", PermissionActionType.ADMINISTRER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_AGENCY_REVENUE),

        // 10. Caisse — 10
        AtomicPermission("cash.view_own", "Consulter sa propre caisse", "Caisse", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.COLLECT_PAYMENT),
        AtomicPermission("cash.view_branch", "Consulter caisses de l'agence", "Caisse", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.VIEW_AGENCY_REVENUE),
        AtomicPermission("cash.view_all", "Consulter toutes les caisses", "Caisse", PermissionActionType.CONSULTER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.ENTERPRISE, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),
        AtomicPermission("cash.open", "Ouvrir session de caisse", "Caisse", PermissionActionType.CREER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.MANAGE_CASH_REGISTER_Z),
        AtomicPermission("cash.close", "Clôturer Z de caisse", "Caisse", PermissionActionType.VALIDER, PermissionRiskLevel.HIGH, true, SubscriptionPlan.STARTER, AccessPermission.MANAGE_CASH_REGISTER_Z),
        AtomicPermission("cash.receive", "Enregistrer entrée caisse", "Caisse", PermissionActionType.CREER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.COLLECT_PAYMENT),
        AtomicPermission("cash.disburse", "Effectuer décaissement sous plafond", "Caisse", PermissionActionType.MODIFIER, PermissionRiskLevel.HIGH, true, SubscriptionPlan.STARTER, AccessPermission.ADD_EXPENSE),
        AtomicPermission("cash.transfer", "Transférer fonds de caisse", "Caisse", PermissionActionType.ADMINISTRER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_CASH_REGISTER_Z),
        AtomicPermission("cash.view_discrepancies", "Consulter écarts de caisse", "Caisse", PermissionActionType.CONSULTER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.VIEW_AGENCY_REVENUE),
        AtomicPermission("cash.approve_adjustment", "Valider régularisation caisse", "Caisse", PermissionActionType.VALIDER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),

        // 11. Facturation — 7
        AtomicPermission("invoices.view", "Consulter les factures", "Facturation", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.REPRINT_CLIENT_RECEIPT),
        AtomicPermission("invoices.create", "Créer une facture", "Facturation", PermissionActionType.CREER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.REPRINT_CLIENT_RECEIPT),
        AtomicPermission("invoices.issue", "Émettre une facture officielle", "Facturation", PermissionActionType.VALIDER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.REPRINT_CLIENT_RECEIPT),
        AtomicPermission("invoices.print", "Imprimer / partager facture", "Facturation", PermissionActionType.IMPRIMER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.REPRINT_CLIENT_RECEIPT),
        AtomicPermission("invoices.export", "Exporter les factures", "Facturation", PermissionActionType.EXPORTER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.EXPORT_FINANCIAL_REPORTS),
        AtomicPermission("invoices.request_credit_note", "Demander un avoir", "Facturation", PermissionActionType.ANNULER, PermissionRiskLevel.HIGH, true, SubscriptionPlan.BUSINESS, AccessPermission.REPRINT_CLIENT_RECEIPT),
        AtomicPermission("invoices.approve_credit_note", "Valider un avoir", "Facturation", PermissionActionType.VALIDER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),

        // 12. Dépenses — 6
        AtomicPermission("expenses.view", "Consulter les dépenses", "Dépenses", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.ADD_EXPENSE),
        AtomicPermission("expenses.create", "Saisir dépense sous plafond", "Dépenses", PermissionActionType.CREER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.ADD_EXPENSE),
        AtomicPermission("expenses.request_approval", "Demander validation dépense", "Dépenses", PermissionActionType.CREER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.STARTER, AccessPermission.ADD_EXPENSE),
        AtomicPermission("expenses.approve", "Approuver une dépense", "Dépenses", PermissionActionType.VALIDER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.VIEW_AGENCY_REVENUE),
        AtomicPermission("expenses.reject", "Rejeter une dépense", "Dépenses", PermissionActionType.ANNULER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.VIEW_AGENCY_REVENUE),
        AtomicPermission("expenses.export", "Exporter journal dépenses", "Dépenses", PermissionActionType.EXPORTER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.BUSINESS, AccessPermission.EXPORT_FINANCIAL_REPORTS),

        // 13. Stocks & Fournisseurs — 9
        AtomicPermission("inventory.view", "Consulter les stocks", "Stocks", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.BUSINESS, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("inventory.receive", "Enregistrer entrée de stock", "Stocks", PermissionActionType.CREER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("inventory.issue", "Enregistrer sortie de stock", "Stocks", PermissionActionType.MODIFIER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("inventory.transfer", "Transférer stock inter-agences", "Stocks", PermissionActionType.MODIFIER, PermissionRiskLevel.HIGH, true, SubscriptionPlan.ENTERPRISE, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("inventory.count", "Réaliser un inventaire", "Stocks", PermissionActionType.VALIDER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("inventory.request_adjustment", "Demander ajustement stock", "Stocks", PermissionActionType.MODIFIER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("inventory.approve_adjustment", "Valider ajustement stock", "Stocks", PermissionActionType.VALIDER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.BUSINESS, AccessPermission.CONFIGURE_PRINTER_AND_AGENCY),
        AtomicPermission("inventory.view_cost", "Consulter coûts d'achat stock", "Stocks", PermissionActionType.CONSULTER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_AGENCY_REVENUE),
        AtomicPermission("inventory.export", "Exporter état des stocks", "Stocks", PermissionActionType.EXPORTER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.EXPORT_FINANCIAL_REPORTS),

        // 14. Personnel & RH — 7
        AtomicPermission("employees.view", "Consulter liste employés", "Personnel", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("employees.create", "Créer fiche employé", "Personnel", PermissionActionType.CREER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("employees.update", "Modifier fiche employé", "Personnel", PermissionActionType.MODIFIER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("employees.manage_attendance", "Gérer présences & congés", "Personnel", PermissionActionType.MODIFIER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_USERS_AND_ROLES),
        AtomicPermission("employees.view_performance", "Consulter performances équipe", "Personnel", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_AGENCY_REVENUE),
        AtomicPermission("employees.view_salary", "Consulter salaires & primes", "Personnel", PermissionActionType.CONSULTER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_HR_SALARIES),
        AtomicPermission("employees.manage_payroll", "Gérer la paie RH", "Personnel", PermissionActionType.ADMINISTRER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_HR_SALARIES),

        // 15. Comptabilité SYSCOHADA (non délégable aux gestionnaires) — 8
        AtomicPermission("accounting.view", "Consulter comptabilité SYSCOHADA", "Comptabilité", PermissionActionType.CONSULTER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),
        AtomicPermission("accounting.create_entry", "Créer écriture comptable", "Comptabilité", PermissionActionType.CREER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),
        AtomicPermission("accounting.post_entry", "Valider écriture au journal", "Comptabilité", PermissionActionType.VALIDER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),
        AtomicPermission("accounting.reverse_entry", "Contrepasser une écriture", "Comptabilité", PermissionActionType.ANNULER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),
        AtomicPermission("accounting.close_period", "Clôturer période comptable", "Comptabilité", PermissionActionType.ADMINISTRER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),
        AtomicPermission("accounting.view_balance_sheet", "Consulter bilan comptable", "Comptabilité", PermissionActionType.CONSULTER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),
        AtomicPermission("accounting.view_income_statement", "Consulter compte de résultat", "Comptabilité", PermissionActionType.CONSULTER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),
        AtomicPermission("accounting.export", "Exporter états SYSCOHADA", "Comptabilité", PermissionActionType.EXPORTER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.EXPORT_FINANCIAL_REPORTS),

        // 16. Livraisons GPS — 8
        AtomicPermission("deliveries.view_assigned", "Voir ses livraisons assignées", "Livraisons", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_DELIVERY_RUNS),
        AtomicPermission("deliveries.view_branch", "Voir livraisons de l'agence", "Livraisons", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_DELIVERY_RUNS),
        AtomicPermission("deliveries.view_all", "Voir livraisons multi-agences", "Livraisons", PermissionActionType.CONSULTER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.ENTERPRISE, AccessPermission.MANAGE_DELIVERY_RUNS),
        AtomicPermission("deliveries.create", "Planifier collecte / livraison", "Livraisons", PermissionActionType.CREER, PermissionRiskLevel.LOW, true, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_DELIVERY_RUNS),
        AtomicPermission("deliveries.assign", "Affecter un livreur", "Livraisons", PermissionActionType.MODIFIER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_DELIVERY_RUNS),
        AtomicPermission("deliveries.update_status", "Mettre à jour statut tournée", "Livraisons", PermissionActionType.MODIFIER, PermissionRiskLevel.LOW, true, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_DELIVERY_RUNS),
        AtomicPermission("deliveries.capture_proof", "Capturer preuve / signature", "Livraisons", PermissionActionType.CREER, PermissionRiskLevel.LOW, true, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_DELIVERY_RUNS),
        AtomicPermission("deliveries.confirm", "Confirmer livraison finale", "Livraisons", PermissionActionType.VALIDER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.MANAGE_DELIVERY_RUNS),

        // 17. Rapports & Analyses — 7
        AtomicPermission("reports.view_operational", "Consulter rapports opérationnels", "Rapports", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER, AccessPermission.ADVANCE_PRODUCTION),
        AtomicPermission("reports.view_own_activity", "Consulter son activité personnelle", "Rapports", PermissionActionType.CONSULTER, PermissionRiskLevel.LOW, true, SubscriptionPlan.STARTER),
        AtomicPermission("reports.view_branch", "Consulter rapports d'agence", "Rapports", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, true, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_AGENCY_REVENUE),
        AtomicPermission("reports.view_revenue", "Consulter chiffre d'affaires", "Rapports", PermissionActionType.CONSULTER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER, AccessPermission.VIEW_AGENCY_REVENUE),
        AtomicPermission("reports.view_profit", "Consulter marges & bénéfices", "Rapports", PermissionActionType.CONSULTER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA),
        AtomicPermission("reports.view_financial", "Consulter rapports financiers", "Rapports", PermissionActionType.CONSULTER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.EXPORT_FINANCIAL_REPORTS),
        AtomicPermission("reports.export", "Exporter rapports PDF / Excel", "Rapports", PermissionActionType.EXPORTER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.BUSINESS, AccessPermission.EXPORT_FINANCIAL_REPORTS),

        // 18. Sécurité & Audit — 4
        AtomicPermission("audit.view_organization", "Consulter journal d'audit entreprise", "Sécurité", PermissionActionType.CONSULTER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_SECURITY_AUDIT_LOG),
        AtomicPermission("audit.view_platform", "Consulter journal d'audit plateforme", "Sécurité", PermissionActionType.CONSULTER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.ENTERPRISE),
        AtomicPermission("security.manage_sessions", "Gérer sessions & révocations", "Sécurité", PermissionActionType.ADMINISTRER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.VIEW_SECURITY_AUDIT_LOG),
        AtomicPermission("security.manage_policies", "Gérer politiques de sécurité", "Sécurité", PermissionActionType.ADMINISTRER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.BUSINESS, AccessPermission.EDIT_PRIVILEGE_MATRIX),

        // 19. Abonnements SaaS — 6
        AtomicPermission("subscriptions.view_own", "Consulter forfait entreprise", "Abonnements", PermissionActionType.CONSULTER, PermissionRiskLevel.MEDIUM, false, SubscriptionPlan.STARTER),
        AtomicPermission("subscriptions.renew", "Renouveler abonnement", "Abonnements", PermissionActionType.VALIDER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER),
        AtomicPermission("subscriptions.change_plan", "Changer de forfait", "Abonnements", PermissionActionType.MODIFIER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER),
        AtomicPermission("subscriptions.purchase_module", "Activer module complémentaire", "Abonnements", PermissionActionType.CREER, PermissionRiskLevel.HIGH, false, SubscriptionPlan.STARTER),
        AtomicPermission("platform.subscriptions.manage", "Administrer abonnements SaaS", "Abonnements", PermissionActionType.ADMINISTRER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.ENTERPRISE),
        AtomicPermission("platform.plans.manage", "Gérer forfaits & tarifs SaaS", "Abonnements", PermissionActionType.ADMINISTRER, PermissionRiskLevel.CRITICAL, false, SubscriptionPlan.ENTERPRISE)
    )

    val byCode: Map<String, AtomicPermission> = allPermissions.associateBy { it.code }
    val byModule: Map<String, List<AtomicPermission>> = allPermissions.groupBy { it.moduleFr }
    val delegablePermissions: List<AtomicPermission> = allPermissions.filter { it.isDelegable }

    private fun planRank(plan: SubscriptionPlan): Int = when (plan) {
        SubscriptionPlan.STARTER -> 1
        SubscriptionPlan.BUSINESS -> 2
        SubscriptionPlan.ENTERPRISE -> 3
    }

    /**
     * Section 22: Subscription & module gating — a user permission can NEVER unlock
     * a capability outside the organization's active subscription plan.
     */
    fun isAllowedBySubscription(
        permission: AtomicPermission,
        plan: SubscriptionPlan,
        subscriptionStatus: String
    ): Boolean {
        if (subscriptionStatus.equals("SUSPENDED", ignoreCase = true)) return false
        if (subscriptionStatus.equals("EXPIRED", ignoreCase = true)) {
            // Read-only / export policy on expired subscription (data never auto-deleted)
            return permission.actionType == PermissionActionType.CONSULTER ||
                permission.actionType == PermissionActionType.EXPORTER ||
                permission.code == "subscriptions.renew"
        }
        return planRank(plan) >= planRank(permission.minPlan)
    }

    fun defaultAtomicCodesForProfile(profile: ManagerPermissionProfile): Set<String> = when (profile) {
        ManagerPermissionProfile.RECEPTIONNISTE -> setOf(
            "orders.view_own", "orders.view_branch", "orders.create", "orders.update_draft",
            "orders.print_receipt", "garments.view", "garments.create", "garments.update",
            "garments.capture_photo", "garments.view_photo", "garments.record_defect",
            "garments.record_stain", "garments.generate_label", "customers.view",
            "customers.create", "customers.update", "customers.view_history", "pricing.view",
            "reports.view_own_activity"
        )
        ManagerPermissionProfile.CAISSIER -> setOf(
            "orders.view_own", "orders.view_branch", "orders.create", "orders.print_receipt",
            "customers.view", "customers.create", "customers.view_debts", "pricing.view",
            "discounts.apply", "payments.view_own", "payments.create", "payments.receive_partial",
            "payments.verify", "payments.request_refund", "cash.view_own", "cash.open",
            "cash.close", "cash.receive", "invoices.view", "invoices.create", "invoices.issue",
            "invoices.print", "reports.view_own_activity"
        )
        ManagerPermissionProfile.AGENT_ATELIER -> setOf(
            "orders.view_assigned", "garments.view", "garments.view_photo", "garments.generate_label",
            "production.view", "production.receive", "production.sort", "production.wash",
            "production.dry_clean", "production.dry", "production.iron", "production.package",
            "reports.view_own_activity"
        )
        ManagerPermissionProfile.GESTIONNAIRE_STOCK -> setOf(
            "inventory.view", "inventory.receive", "inventory.issue", "inventory.count",
            "inventory.request_adjustment", "reports.view_operational", "reports.view_own_activity"
        )
        ManagerPermissionProfile.LIVREUR -> setOf(
            "orders.view_assigned", "deliveries.view_assigned", "deliveries.update_status",
            "deliveries.capture_proof", "deliveries.confirm", "reports.view_own_activity"
        )
        ManagerPermissionProfile.POLYVALENT -> (
            defaultAtomicCodesForProfile(ManagerPermissionProfile.RECEPTIONNISTE) +
                defaultAtomicCodesForProfile(ManagerPermissionProfile.CAISSIER) +
                defaultAtomicCodesForProfile(ManagerPermissionProfile.AGENT_ATELIER) +
                setOf("production.quality_check", "production.mark_ready", "production.request_rework")
            )
        ManagerPermissionProfile.PERSONNALISE -> setOf(
            "orders.view_own", "orders.create", "customers.view", "pricing.view", "reports.view_own_activity"
        )
    }

    /**
     * Full centralized atomic RBAC + ABAC evaluation implementing the 18-step security pipeline
     * (Section 10): Auth -> Active -> Session -> Tenant -> Organization Status -> Subscription ->
     * Branch Isolation -> Explicit DENY -> Delegability -> Role/Profile/Override -> Access Hours.
     */
    fun checkAtomicAccess(
        ctx: TenantAuthorizationContext,
        permissionCode: String,
        profile: ManagerPermissionProfile? = null,
        overrides: List<AtomicPermissionOverride> = emptyList(),
        policy: ManagerAccessPolicy? = null
    ): AuthorizationDecision {
        if (ctx.userId.isBlank()) return AuthorizationDecision.Denied("Authentification requise (401).")
        if (!ctx.userActive) return AuthorizationDecision.Denied("Compte utilisateur désactivé ou suspendu.")
        if (ctx.sessionRevoked) return AuthorizationDecision.Denied("Session révoquée : reconnexion requise.")

        val perm = byCode[permissionCode]
            ?: return AuthorizationDecision.Denied("Permission inconnue (DENY BY DEFAULT) : $permissionCode")

        // Super-Admin restrictions (Section 3.9): platform permissions allowed, tenant commercial ops denied by default
        if (ctx.roleTier == SystemRoleTier.SUPER_ADMIN) {
            val isPlatformScope = perm.code.startsWith("platform.") ||
                perm.code == "audit.view_platform" ||
                perm.code == "security.manage_sessions"
            return if (isPlatformScope) {
                AuthorizationDecision.Allowed
            } else {
                AuthorizationDecision.Denied(
                    "Restriction Super-Admin : ouvrez une session d'assistance avec ticket et motif pour consulter une entreprise."
                )
            }
        }

        // Strict cross-tenant isolation (Enterprise A vs Enterprise B)
        if (ctx.activeOrganizationId.isBlank() || ctx.activeOrganizationId != ctx.targetOrganizationId) {
            return AuthorizationDecision.Denied("Ressource introuvable dans votre entreprise active.")
        }

        if (ctx.organizationStatus.equals("SUSPENDED", ignoreCase = true)) {
            return AuthorizationDecision.Denied("Entreprise suspendue : opération bloquée.")
        }

        // Subscription & module gating (Section 22)
        if (!isAllowedBySubscription(perm, ctx.subscriptionPlan, ctx.subscriptionStatus)) {
            return AuthorizationDecision.Denied(
                "Module ou fonctionnalité non inclus dans votre forfait ${ctx.subscriptionPlan.labelFr} (${ctx.subscriptionStatus})."
            )
        }

        // Platform-only codes are strictly forbidden to PRESSING_ADMIN and PRESSING_MANAGER
        if (perm.code.startsWith("platform.") || perm.code == "audit.view_platform") {
            return AuthorizationDecision.Denied("Accès strictement réservé à l'administration globale de la plateforme.")
        }

        // Branch perimeter isolation for non-Directors
        if (ctx.roleTier == SystemRoleTier.PRESSING_MANAGER && !ctx.targetBranch.isNullOrBlank()) {
            if (ctx.assignedBranches.isNotEmpty() && ctx.assignedBranches.none { it.equals(ctx.targetBranch, ignoreCase = true) }) {
                return AuthorizationDecision.Denied("Cette agence ne fait pas partie de votre périmètre d'accès.")
            }
        }

        // Working hours ABAC guard for managers
        if (ctx.roleTier == SystemRoleTier.PRESSING_MANAGER && policy != null) {
            if (!policy.active) return AuthorizationDecision.Denied("Accès gestionnaire désactivé.")
            if (ctx.currentHour24 < policy.accessStartHour || ctx.currentHour24 >= policy.accessEndHour) {
                return AuthorizationDecision.Denied("Hors horaires d'accès autorisés (${policy.accessStartHour}h–${policy.accessEndHour}h).")
            }
        }

        // Explicit DENY always prevails over role, profile, or ALLOW overrides
        val activeOverrides = overrides.filter { it.isActiveAt(ctx.nowMillis) }
        if (activeOverrides.any { it.permissionCode == permissionCode && it.effect == PermissionOverrideEffect.DENY }) {
            return AuthorizationDecision.Denied("Interdiction explicite active sur « ${perm.labelFr} ».")
        }

        // PRESSING_ADMIN gets all non-platform permissions within their active org & subscription
        if (ctx.roleTier == SystemRoleTier.PRESSING_ADMIN) {
            return AuthorizationDecision.Allowed
        }

        // Non-delegable permissions can NEVER be granted to PRESSING_MANAGER
        if (!perm.isDelegable) {
            return AuthorizationDecision.Denied("Permission non délégable réservée à l'Admin Pressing.")
        }

        val profileSet = profile?.let { defaultAtomicCodesForProfile(it) }.orEmpty()
        val explicitAllow = activeOverrides.any {
            it.permissionCode == permissionCode && it.effect == PermissionOverrideEffect.ALLOW
        }
        val grantedByPolicy = policy != null && permissionCode in policy.grantedPermissions

        return if (permissionCode in profileSet || explicitAllow || grantedByPolicy) {
            AuthorizationDecision.Allowed
        } else {
            AuthorizationDecision.Denied("Vous ne disposez pas de l'autorisation nécessaire pour effectuer cette opération.")
        }
    }

    /**
     * Scenario 24: Guarantees that an organization can NEVER delete or deactivate
     * its last active owner/Director.
     */
    fun canRemoveOrDeactivateEmployee(
        employees: List<EmployeeRecord>,
        targetEmployeeId: String
    ): AuthorizationDecision {
        val target = employees.firstOrNull { it.id == targetEmployeeId }
            ?: return AuthorizationDecision.Denied("Collaborateur introuvable.")
        if (WorkspaceRole.fromRaw(target.role) == WorkspaceRole.DIRECTEUR) {
            val activeDirectors = employees.count {
                it.active && WorkspaceRole.fromRaw(it.role) == WorkspaceRole.DIRECTEUR
            }
            if (activeDirectors <= 1) {
                return AuthorizationDecision.Denied("Impossible de supprimer ou désactiver le dernier propriétaire actif de l'entreprise.")
            }
        }
        return AuthorizationDecision.Allowed
    }
}
