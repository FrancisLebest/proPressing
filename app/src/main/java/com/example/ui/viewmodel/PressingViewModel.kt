package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.formatMoney
import com.example.core.nowFormatted
import com.example.data.AccessPermission
import com.example.data.AccountingEntry
import com.example.data.ActiveRoleSession
import com.example.data.AiRepository
import com.example.data.ClaimRecord
import com.example.data.ClientRecord
import com.example.data.CompanyConfig
import com.example.data.DefaultCatalogTemplates
import com.example.data.EmployeeRecord
import com.example.data.GarmentAiAnalysis
import com.example.data.GarmentWorkshopStatus
import com.example.data.OrderGarmentItem
import com.example.data.OrderPriority
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.PressingOrder
import com.example.data.PressingRepository
import com.example.data.RoleAuditEvent
import com.example.data.RolePermissionMatrix
import com.example.data.RoleSessionResolver
import com.example.data.ServiceItem
import com.example.data.StockItem
import com.example.data.TenantDataScoper
import com.example.data.WorkspaceRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val VM_TAG = "PressingViewModel"
private const val STOP_TIMEOUT_MS = 5_000L

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiStreamState {
    data object Idle : AiStreamState
    data object Loading : AiStreamState
    data class Streaming(val text: String) : AiStreamState
    data class Success(val text: String) : AiStreamState
    data class Error(val message: String) : AiStreamState
}

sealed interface GarmentVisionState {
    data object Idle : GarmentVisionState
    data object Loading : GarmentVisionState
    data class Success(val analysis: GarmentAiAnalysis) : GarmentVisionState
    data class Error(val message: String) : GarmentVisionState
}

class PressingViewModel(
    private val repository: PressingRepository,
    private val aiRepository: AiRepository,
    val currentUserId: String,
    val currentUserEmail: String = "",
    val currentUserPhone: String = "",
    val currentUserName: String = ""
) : ViewModel() {

    private val safeUserDisplayName: String = sanitizeOperatorName(currentUserName, fallback = "")

    private fun sanitizeOperatorName(raw: String, fallback: String = "Utilisateur"): String {
        val cleaned = raw.trim()
        if (cleaned.isBlank()) return fallback
        if (cleaned == currentUserId) return fallback
        if (cleaned.startsWith("tel.", ignoreCase = true) && cleaned.endsWith("@pressingpro.africa", ignoreCase = true)) {
            return fallback
        }
        return cleaned.take(80)
    }

    private fun sanitizeErrorMessage(error: Throwable, fallback: String): String {
        val raw = error.localizedMessage?.trim().orEmpty()
        if (raw.isBlank()) return fallback
        val containsTechnicalLeak = raw.contains(currentUserId, ignoreCase = true) ||
            raw.contains("users/", ignoreCase = true) ||
            raw.contains("PERMISSION_DENIED", ignoreCase = true) ||
            raw.contains("UNAVAILABLE", ignoreCase = true) ||
            raw.contains("Firebase", ignoreCase = true) ||
            raw.contains("pressingpro.africa", ignoreCase = true)
        return if (containsTechnicalLeak) fallback else raw
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val rootError = fsError ?: error
                Log.w(VM_TAG, "Error observing $label", rootError)
                emit(
                    UiState.Error(
                        sanitizeErrorMessage(
                            rootError,
                            "Erreur de synchronisation ($label). Vérifiez votre connexion."
                        )
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val companyState: StateFlow<UiState<CompanyConfig?>> =
        repository.observeCompanyConfig(currentUserId).toUiState("entreprise")

    val servicesState: StateFlow<UiState<List<ServiceItem>>> =
        repository.observeServices(currentUserId).toUiState("services")

    val clientsState: StateFlow<UiState<List<ClientRecord>>> =
        repository.observeClients(currentUserId).toUiState("clients")

    val ordersState: StateFlow<UiState<List<PressingOrder>>> =
        repository.observeOrders(currentUserId).toUiState("commandes")

    val transactionsState: StateFlow<UiState<List<AccountingEntry>>> =
        repository.observeTransactions(currentUserId).toUiState("comptabilité")

    val stocksState: StateFlow<UiState<List<StockItem>>> =
        repository.observeStocks(currentUserId).toUiState("stocks")

    val employeesState: StateFlow<UiState<List<EmployeeRecord>>> =
        repository.observeEmployees(currentUserId).toUiState("personnel")

    val claimsState: StateFlow<UiState<List<ClaimRecord>>> =
        repository.observeClaims(currentUserId).toUiState("réclamations")

    private val _rolePermissionsMap = MutableStateFlow(
        WorkspaceRole.entries.associateWith { RolePermissionMatrix.defaultPermissionsFor(it) }
    )
    val rolePermissionsMap: StateFlow<Map<WorkspaceRole, Set<AccessPermission>>> =
        _rolePermissionsMap.asStateFlow()

    private val _activeRoleSession = MutableStateFlow(
        ActiveRoleSession(
            role = WorkspaceRole.DIRECTEUR,
            operatorName = sanitizeOperatorName(currentUserName, "Directeur Général"),
            isolatedAgency = null,
            permissions = RolePermissionMatrix.defaultPermissionsFor(WorkspaceRole.DIRECTEUR)
        )
    )
    val activeRoleSession: StateFlow<ActiveRoleSession> = _activeRoleSession.asStateFlow()

    private val _auditLog = MutableStateFlow(
        listOf(
            RoleAuditEvent(
                timestampLabel = nowFormatted(),
                actor = sanitizeOperatorName(currentUserName, "Directeur Général"),
                roleLabel = WorkspaceRole.DIRECTEUR.shortTitleFr,
                scopeLabel = "Espace isolé (Multi-agences)",
                action = "Ouverture de session sécurisée",
                allowed = true
            )
        )
    )
    val auditLog: StateFlow<List<RoleAuditEvent>> = _auditLog.asStateFlow()

    init {
        // 1. Immediate one-shot profile lookup right after login so role & agency bind immediately
        viewModelScope.launch {
            repository.fetchUserRoleContext(currentUserId).onSuccess { (company, staff) ->
                val resolved = RoleSessionResolver.resolveForUser(
                    userEmail = currentUserEmail,
                    userPhone = currentUserPhone,
                    userDisplayName = safeUserDisplayName,
                    company = company,
                    employees = staff,
                    rolePermissionsMap = _rolePermissionsMap.value
                )
                applyResolvedSession(resolved)
            }
        }

        // 2. Continuous real-time synchronization of company, staff roster, and role matrix
        viewModelScope.launch {
            combine(companyState, employeesState, _rolePermissionsMap) { compUi, empUi, permsMap ->
                val company = (compUi as? UiState.Success)?.data
                val staff = (empUi as? UiState.Success)?.data.orEmpty()
                RoleSessionResolver.resolveForUser(
                    userEmail = currentUserEmail,
                    userPhone = currentUserPhone,
                    userDisplayName = safeUserDisplayName,
                    company = company,
                    employees = staff,
                    rolePermissionsMap = permsMap
                )
            }.collect { resolved ->
                applyResolvedSession(resolved)
            }
        }
    }

    private fun applyResolvedSession(resolved: ActiveRoleSession) {
        val cleanResolved = resolved.copy(
            operatorName = sanitizeOperatorName(resolved.operatorName, resolved.role.labelFr)
        )
        val previous = _activeRoleSession.value
        _activeRoleSession.value = cleanResolved
        if (previous.role != cleanResolved.role || previous.isolatedAgency != cleanResolved.isolatedAgency) {
            recordAudit(
                "Profil rattaché automatiquement : ${cleanResolved.role.labelFr} (${cleanResolved.isolatedAgency ?: "Multi-agences"})",
                allowed = true
            )
        }
    }

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _garmentVisionState = MutableStateFlow<GarmentVisionState>(GarmentVisionState.Idle)
    val garmentVisionState: StateFlow<GarmentVisionState> = _garmentVisionState.asStateFlow()

    private val _aiAdvisorState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val aiAdvisorState: StateFlow<AiStreamState> = _aiAdvisorState.asStateFlow()

    private fun recordAudit(action: String, allowed: Boolean = true) {
        val session = _activeRoleSession.value
        val event = RoleAuditEvent(
            timestampLabel = nowFormatted(),
            actor = session.operatorName,
            roleLabel = session.role.shortTitleFr,
            scopeLabel = session.isolatedAgency ?: "Multi-agences",
            action = action,
            allowed = allowed
        )
        _auditLog.update { (listOf(event) + it).take(30) }
    }

    private fun requirePermission(permission: AccessPermission, actionLabel: String): Boolean {
        val session = _activeRoleSession.value
        if (session.has(permission)) return true
        recordAudit("Refus : $actionLabel (${permission.labelFr})", allowed = false)
        _feedbackMessage.value = "Action bloquée : privilège « ${permission.labelFr} » requis."
        return false
    }

    // Role session is strictly bound to the authenticated Firestore user profile — no client-side role switching.

    fun selectIsolatedAgency(agency: String?) {
        val current = _activeRoleSession.value
        if (current.role != WorkspaceRole.DIRECTEUR && agency.isNullOrBlank()) {
            recordAudit("Tentative vue multi-agences refusée", allowed = false)
            _feedbackMessage.value = "Ce rôle reste isolé sur une seule agence."
            return
        }
        _activeRoleSession.update { it.copy(isolatedAgency = agency?.takeIf { a -> a.isNotBlank() }) }
        recordAudit("Périmètre isolé : ${agency?.takeIf { it.isNotBlank() } ?: "Toutes agences"}", allowed = true)
        _feedbackMessage.value = if (agency.isNullOrBlank()) {
            "Vue multi-agences activée (tenant isolé)."
        } else {
            "Données isolées sur : $agency"
        }
    }

    fun toggleRolePermission(role: WorkspaceRole, permission: AccessPermission) {
        if (_activeRoleSession.value.role != WorkspaceRole.DIRECTEUR) {
            recordAudit("Modification droits refusée (${role.shortTitleFr})", allowed = false)
            _feedbackMessage.value = "Seul le Directeur Général peut modifier la matrice des privilèges."
            return
        }
        if (role == WorkspaceRole.DIRECTEUR && permission == AccessPermission.MANAGE_STAFF_AND_ROLES) {
            _feedbackMessage.value = "Le droit d'administration des rôles reste verrouillé pour le Directeur."
            return
        }
        var grantedNow = false
        _rolePermissionsMap.update { currentMap ->
            val currentSet = currentMap[role] ?: RolePermissionMatrix.defaultPermissionsFor(role)
            grantedNow = permission !in currentSet
            val nextSet = if (grantedNow) currentSet + permission else currentSet - permission
            currentMap + (role to nextSet)
        }
        if (_activeRoleSession.value.role == role) {
            val updated = _rolePermissionsMap.value[role].orEmpty()
            _activeRoleSession.update { it.copy(permissions = updated) }
        }
        recordAudit(
            "${if (grantedNow) "Accordé" else "Révoqué"} : ${permission.labelFr} (${role.shortTitleFr})",
            allowed = true
        )
    }

    fun resetRolePermissions(role: WorkspaceRole) {
        if (_activeRoleSession.value.role != WorkspaceRole.DIRECTEUR) return
        val defaults = RolePermissionMatrix.defaultPermissionsFor(role)
        _rolePermissionsMap.update { it + (role to defaults) }
        if (_activeRoleSession.value.role == role) {
            _activeRoleSession.update { it.copy(permissions = defaults) }
        }
        recordAudit("Réinitialisation des privilèges (${role.shortTitleFr})", allowed = true)
        _feedbackMessage.value = "Privilèges par défaut restaurés pour ${role.labelFr}."
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun resetGarmentVision() {
        _garmentVisionState.value = GarmentVisionState.Idle
    }

    private fun showResult(result: Result<*>, successMsg: String) {
        result.fold(
            onSuccess = { _feedbackMessage.value = successMsg },
            onFailure = { e ->
                _feedbackMessage.value = sanitizeErrorMessage(e, "Opération échouée. Veuillez réessayer.")
            }
        )
    }

    fun saveCompanyConfig(config: CompanyConfig, silent: Boolean = false, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveCompanyConfig(currentUserId, config)
            _isBusy.value = false
            if (!silent) showResult(res, "Configuration entreprise enregistrée.")
            if (res.isSuccess) onDone()
        }
    }

    fun seedStarterServices(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.seedStarterServicesIfEmpty(
                currentUserId,
                DefaultCatalogTemplates.ivorianStarterServices()
            )
            _isBusy.value = false
            if (res.isSuccess) onDone() else showResult(res, "")
        }
    }

    fun saveService(item: ServiceItem, onDone: () -> Unit = {}) {
        if (!requirePermission(AccessPermission.MANAGE_CATALOG, "Modifier le catalogue")) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveService(currentUserId, item)
            _isBusy.value = false
            showResult(res, "Service enregistré dans le catalogue.")
            if (res.isSuccess) onDone()
        }
    }

    fun deleteService(serviceId: String) {
        if (!requirePermission(AccessPermission.MANAGE_CATALOG, "Supprimer un service")) return
        viewModelScope.launch {
            val res = repository.deleteService(currentUserId, serviceId)
            showResult(res, "Service supprimé.")
        }
    }

    fun saveClient(client: ClientRecord, onCreated: (String) -> Unit = {}) {
        if (!requirePermission(AccessPermission.EDIT_CLIENTS, "Enregistrer un client")) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveClient(currentUserId, client)
            _isBusy.value = false
            res.fold(
                onSuccess = { id ->
                    _feedbackMessage.value = "Fiche client enregistrée."
                    onCreated(id)
                },
                onFailure = { e ->
                    _feedbackMessage.value = sanitizeErrorMessage(e, "Erreur d'enregistrement client.")
                }
            )
        }
    }

    fun deleteClient(clientId: String) {
        if (!requirePermission(AccessPermission.DELETE_CLIENTS, "Supprimer un client")) return
        viewModelScope.launch {
            val res = repository.deleteClient(currentUserId, clientId)
            showResult(res, "Client supprimé.")
        }
    }

    fun createOrder(
        orderNumber: String,
        agencyName: String,
        clientId: String,
        clientName: String,
        clientPhone: String,
        clientAddress: String,
        priority: OrderPriority,
        items: List<OrderGarmentItem>,
        discountAmount: Long,
        expressFee: Long,
        deliveryFee: Long,
        taxAmount: Long,
        paidAmount: Long,
        paymentMethod: PaymentMethod,
        deliveryRequested: Boolean,
        deliveryZone: String,
        assignedEmployee: String,
        clientSignatureName: String,
        observations: String,
        dueDateMillis: Long,
        onCreated: (String) -> Unit
    ) {
        if (!requirePermission(AccessPermission.CREATE_ORDER, "Créer une commande")) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createOrder(
                userId = currentUserId,
                orderNumber = orderNumber,
                agencyName = agencyName,
                clientId = clientId,
                clientName = clientName,
                clientPhone = clientPhone,
                clientAddress = clientAddress,
                priority = priority,
                items = items,
                discountAmount = discountAmount,
                expressFee = expressFee,
                deliveryFee = deliveryFee,
                taxAmount = taxAmount,
                paidAmount = paidAmount,
                paymentMethod = paymentMethod,
                deliveryRequested = deliveryRequested,
                deliveryZone = deliveryZone,
                assignedEmployee = assignedEmployee,
                clientSignatureName = clientSignatureName,
                observations = observations,
                dueDateMillis = dueDateMillis
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { orderId ->
                    recordAudit("Création commande $orderNumber ($agencyName)", allowed = true)
                    _feedbackMessage.value = "Commande $orderNumber créée et comptabilisée."
                    onCreated(orderId)
                },
                onFailure = { e ->
                    _feedbackMessage.value = sanitizeErrorMessage(e, "Échec de création de commande.")
                }
            )
        }
    }

    fun updateOrderStatus(
        order: PressingOrder,
        newStatus: OrderStatus,
        assignedEmployee: String = order.assignedEmployee,
        qualityChecked: Boolean = order.qualityChecked,
        note: String = ""
    ) {
        val session = _activeRoleSession.value
        if (!session.canTransitionOrderTo(newStatus)) {
            recordAudit("Refus transition ${order.orderNumber} → ${newStatus.labelFr}", allowed = false)
            _feedbackMessage.value = "Votre rôle ne permet pas de passer cette commande à « ${newStatus.labelFr} »."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateOrderStatus(
                currentUserId, order, newStatus, assignedEmployee, qualityChecked, note
            )
            _isBusy.value = false
            if (res.isSuccess) {
                recordAudit("Statut ${order.orderNumber} → ${newStatus.labelFr}", allowed = true)
            }
            showResult(res, "Statut mis à jour : ${newStatus.labelFr}")
        }
    }

    fun updateGarmentWorkshopStatus(
        order: PressingOrder,
        garmentIndex: Int,
        newWorkshopStatus: GarmentWorkshopStatus
    ) {
        val session = _activeRoleSession.value
        val requiredPerm = when (newWorkshopStatus) {
            GarmentWorkshopStatus.READY -> AccessPermission.VALIDATE_QUALITY
            else -> AccessPermission.ADVANCE_PRODUCTION
        }
        if (!session.has(requiredPerm)) {
            recordAudit("Refus atelier ${order.orderNumber} → ${newWorkshopStatus.labelFr}", allowed = false)
            _feedbackMessage.value = "Privilège « ${requiredPerm.labelFr} » requis pour cette étape."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateGarmentWorkshopStatus(
                userId = currentUserId,
                order = order,
                garmentIndex = garmentIndex,
                newStatus = newWorkshopStatus,
                operatorName = session.operatorName,
                allowReadyOrderSync = session.has(AccessPermission.VALIDATE_QUALITY)
            )
            _isBusy.value = false
            if (res.isSuccess) {
                recordAudit("Atelier ${order.orderNumber} (article #${garmentIndex + 1}) → ${newWorkshopStatus.labelFr}", allowed = true)
            }
            showResult(res, "Article passé à : ${newWorkshopStatus.labelFr}")
        }
    }

    fun recordOrderPayment(order: PressingOrder, amount: Long, method: PaymentMethod) {
        if (!requirePermission(AccessPermission.COLLECT_PAYMENT, "Encaisser ${order.orderNumber}")) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.recordOrderPayment(currentUserId, order, amount, method)
            _isBusy.value = false
            if (res.isSuccess) {
                recordAudit("Encaissement ${order.orderNumber} (${method.labelFr})", allowed = true)
            }
            showResult(res, "Paiement encaissé et enregistré en comptabilité.")
        }
    }

    fun assignDelivery(order: PressingOrder, driverName: String, zone: String, markDelivered: Boolean = false) {
        if (!requirePermission(AccessPermission.DISPATCH_DELIVERIES, "Gérer livraison ${order.orderNumber}")) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.assignDelivery(currentUserId, order, driverName, zone, markDelivered)
            _isBusy.value = false
            if (res.isSuccess) {
                recordAudit(
                    if (markDelivered) "Livraison clôturée ${order.orderNumber}" else "Livreur $driverName affecté (${order.orderNumber})",
                    allowed = true
                )
            }
            showResult(res, if (markDelivered) "Livraison confirmée !" else "Livreur $driverName affecté.")
        }
    }

    fun addAccountingEntry(entry: AccountingEntry, onDone: () -> Unit = {}) {
        val session = _activeRoleSession.value
        val allowed = session.has(AccessPermission.ADD_EXPENSE) ||
            (entry.entryType != "EXPENSE" && session.has(AccessPermission.COLLECT_PAYMENT))
        if (!allowed) {
            recordAudit("Refus écriture comptable (${entry.entryType})", allowed = false)
            _feedbackMessage.value = "Privilège « Saisir dépenses & écritures » requis."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addAccountingEntry(currentUserId, entry)
            _isBusy.value = false
            if (res.isSuccess) {
                recordAudit("Écriture SYSCOHADA ${entry.syscohadaAccount} (${entry.entryType})", allowed = true)
            }
            showResult(res, "Écriture comptable SYSCOHADA enregistrée.")
            if (res.isSuccess) onDone()
        }
    }

    fun saveStockItem(item: StockItem, onDone: () -> Unit = {}) {
        if (!requirePermission(AccessPermission.EDIT_STOCKS, "Modifier le stock")) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveStockItem(currentUserId, item)
            _isBusy.value = false
            showResult(res, "Article / équipement mis à jour.")
            if (res.isSuccess) onDone()
        }
    }

    fun deleteStockItem(stockId: String) {
        if (!requirePermission(AccessPermission.EDIT_STOCKS, "Supprimer un article de stock")) return
        viewModelScope.launch {
            val res = repository.deleteStockItem(currentUserId, stockId)
            showResult(res, "Article supprimé du stock.")
        }
    }

    fun saveEmployee(emp: EmployeeRecord, onDone: () -> Unit = {}) {
        if (!requirePermission(AccessPermission.MANAGE_STAFF_AND_ROLES, "Gérer le personnel")) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveEmployee(currentUserId, emp)
            _isBusy.value = false
            showResult(res, "Fiche employé enregistrée.")
            if (res.isSuccess) onDone()
        }
    }

    fun deleteEmployee(empId: String) {
        if (!requirePermission(AccessPermission.MANAGE_STAFF_AND_ROLES, "Supprimer un employé")) return
        viewModelScope.launch {
            val res = repository.deleteEmployee(currentUserId, empId)
            showResult(res, "Employé supprimé.")
        }
    }

    fun createClaim(claim: ClaimRecord, onDone: () -> Unit = {}) {
        if (!requirePermission(AccessPermission.MANAGE_CLAIMS, "Créer une réclamation")) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createClaim(currentUserId, claim)
            _isBusy.value = false
            showResult(res, "Réclamation enregistrée.")
            if (res.isSuccess) onDone()
        }
    }

    fun updateClaimStatus(claimId: String, status: String, severity: String, resolutionNote: String) {
        if (!requirePermission(AccessPermission.MANAGE_CLAIMS, "Mettre à jour une réclamation")) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateClaimStatus(currentUserId, claimId, status, severity, resolutionNote)
            _isBusy.value = false
            showResult(res, "Réclamation mise à jour.")
        }
    }

    // --- FIREBASE AI LOGIC (GEMINI 3.8 FLASH) ---

    fun analyzeGarmentPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _garmentVisionState.value = GarmentVisionState.Loading
            val result = aiRepository.analyzeGarmentPhoto(bitmap)
            result.fold(
                onSuccess = { analysis ->
                    _garmentVisionState.value = GarmentVisionState.Success(analysis)
                },
                onFailure = { e ->
                    _garmentVisionState.value = GarmentVisionState.Error(
                        e.localizedMessage ?: "Échec de l'analyse IA de la photo."
                    )
                }
            )
        }
    }

    fun askAiAdvisor(question: String) {
        val session = _activeRoleSession.value
        val company = (companyState.value as? UiState.Success)?.data
        val rawOrders = (ordersState.value as? UiState.Success)?.data.orEmpty()
        val rawClients = (clientsState.value as? UiState.Success)?.data.orEmpty()
        val rawTransactions = (transactionsState.value as? UiState.Success)?.data.orEmpty()
        val rawStocks = (stocksState.value as? UiState.Success)?.data.orEmpty()
        val rawClaims = (claimsState.value as? UiState.Success)?.data.orEmpty()

        // Strictly scope AI prompt context to active role & isolated agency
        val orders = TenantDataScoper.scopeOrders(rawOrders, session)
        val transactions = TenantDataScoper.scopeTransactions(rawTransactions, session)
        val clients = TenantDataScoper.scopeClients(rawClients, orders, session, rawOrders)
        val stocks = TenantDataScoper.scopeStocks(rawStocks, session)
        val claims = TenantDataScoper.scopeClaims(rawClaims, orders, session)

        val currency = company?.currency ?: "FCFA"
        val lateCount = orders.count { it.isLate() }
        val lowStocks = stocks.filter { it.isLowStock() }
        val brokenMachines = stocks.filter { it.itemKind == "MACHINE" && it.machineStatus != "OPERATIONAL" }

        val contextSummary = buildString {
            appendLine("Entreprise : ${company?.companyName ?: "Pressing"} (${company?.city ?: "Abidjan"}, ${company?.country ?: "Côte d'Ivoire"})")
            appendLine("Rôle utilisateur : ${session.role.labelFr} · Périmètre isolé : ${session.scopeLabel()}")
            appendLine("Commandes du périmètre : ${orders.size} (En cours: ${orders.count { it.orderStatusEnum() !in listOf(OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.CANCELLED) }}, En retard: $lateCount)")
            if (session.has(AccessPermission.VIEW_CLIENTS)) {
                appendLine("Clients actifs : ${clients.size} (VIP: ${clients.count { it.category == "VIP" }})")
            }
            if (session.has(AccessPermission.VIEW_FINANCIAL_KPIS)) {
                val totalRevenue = transactions.filter { it.entryType == "REVENUE" }.sumOf { it.amount }
                val totalExpenses = transactions.filter { it.entryType == "EXPENSE" || it.entryType == "REFUND" }.sumOf { it.amount }
                val unpaidTotal = orders.filter { it.status != "CANCELLED" }.sumOf { it.remainingAmount }
                appendLine("Recettes encaissées : ${formatMoney(totalRevenue, currency)}")
                appendLine("Dépenses totales : ${formatMoney(totalExpenses, currency)}")
                appendLine("Bénéfice net estimé : ${formatMoney(totalRevenue - totalExpenses, currency)}")
                appendLine("Créances clients (impayés) : ${formatMoney(unpaidTotal, currency)}")
            } else {
                appendLine("Données financières : CONFIDENTIELLES (ne jamais mentionner de chiffres financiers à ce rôle).")
            }
            if (session.has(AccessPermission.VIEW_STOCKS)) {
                appendLine("Stocks critiques (${lowStocks.size}) : ${lowStocks.joinToString { "${it.name} (${it.quantity} ${it.unit})" }.ifBlank { "Aucun" }}")
                appendLine("Machines en maintenance/panne : ${brokenMachines.joinToString { it.name }.ifBlank { "Aucune" }}")
            }
            if (session.has(AccessPermission.MANAGE_CLAIMS)) {
                appendLine("Réclamations ouvertes : ${claims.count { it.status != "RESOLVED" }}")
            }
        }

        viewModelScope.launch {
            _aiAdvisorState.value = AiStreamState.Loading
            var finalText = ""
            try {
                aiRepository.streamAdvisorAnswer(contextSummary, question).collect { partial ->
                    finalText = partial
                    _aiAdvisorState.value = AiStreamState.Streaming(partial)
                }
                _aiAdvisorState.value = AiStreamState.Success(finalText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiAdvisorState.value = AiStreamState.Error(
                    e.localizedMessage ?: "Impossible de joindre l'assistant IA."
                )
            }
        }
    }
}
