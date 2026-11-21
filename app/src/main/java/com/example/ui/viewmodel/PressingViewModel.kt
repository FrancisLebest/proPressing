package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.CashCloseSummary
import com.example.core.NetworkConnectivityObserver
import com.example.core.PressingNotificationManager
import com.example.core.formatMoney
import com.example.core.nowFormatted
import com.example.data.AccessPermission
import com.example.data.AccountingEntry
import com.example.data.ActiveRoleSession
import com.example.data.AiRepository
import com.example.data.AuditLogRecord
import com.example.data.ClaimRecord
import com.example.data.ClientRecord
import com.example.data.CompanyConfig
import com.example.data.DefaultCatalogTemplates
import com.example.data.EmployeeRecord
import com.example.data.FcmTokenRecord
import com.example.data.GarmentAiAnalysis
import com.example.data.GarmentWorkshopStatus
import com.example.data.OrderGarmentItem
import com.example.data.OrderPriority
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.PressingOrder
import com.example.data.PressingRepository
import com.example.data.PushNotificationRecord
import com.example.data.RoleAuditEvent
import com.example.data.RolePermissionMatrix
import com.example.data.RoleSessionResolver
import com.example.data.ServiceItem
import com.example.data.StockItem
import com.example.data.TenantDataScoper
import com.example.data.WorkspaceRole
import com.example.data.local.OfflineOrderEntity
import com.example.data.local.OfflineSyncRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    val currentUserName: String = "",
    private val appContext: Context? = null,
    private val offlineSyncRepository: OfflineSyncRepository? = null,
    private val connectivityObserver: NetworkConnectivityObserver? = null
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

    // --- ROOM OFFLINE CACHE & NETWORK CONNECTIVITY ---
    val isOnline: StateFlow<Boolean> =
        (connectivityObserver?.observeOnlineStatus() ?: flowOf(true))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = connectivityObserver?.isCurrentlyOnline() ?: true
            )

    val pendingOfflineOrders: StateFlow<List<OfflineOrderEntity>> =
        (offlineSyncRepository?.observePendingOrders(currentUserId) ?: flowOf(emptyList()))
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyList()
            )

    private val _isSyncingOffline = MutableStateFlow(false)
    val isSyncingOffline: StateFlow<Boolean> = _isSyncingOffline.asStateFlow()

    // --- FCM PUSH NOTIFICATIONS STATE ---
    private val _fcmTopics = MutableStateFlow<List<String>>(emptyList())
    val fcmTopics: StateFlow<List<String>> = _fcmTopics.asStateFlow()

    val pushNotificationsState: StateFlow<List<PushNotificationRecord>> =
        repository.observePushNotifications(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyList()
            )

    private val remoteOrdersState: StateFlow<UiState<List<PressingOrder>>> =
        repository.observeOrders(currentUserId).toUiState("commandes")

    /** Merges cloud Firestore orders with locally cached Room pending orders so cashiers see them immediately offline. */
    val ordersState: StateFlow<UiState<List<PressingOrder>>> =
        combine(remoteOrdersState, pendingOfflineOrders) { remoteUi, localPending ->
            val localAsOrders = localPending.map { it.toPressingOrder() }
            when (remoteUi) {
                is UiState.Success -> {
                    val remoteNumbers = remoteUi.data.map { it.orderNumber }.toSet()
                    val uniqueLocal = localAsOrders.filter { it.orderNumber !in remoteNumbers }
                    UiState.Success(uniqueLocal + remoteUi.data)
                }
                is UiState.Error -> {
                    if (localAsOrders.isNotEmpty()) UiState.Success(localAsOrders) else remoteUi
                }
                UiState.Loading -> {
                    if (localAsOrders.isNotEmpty()) UiState.Success(localAsOrders) else UiState.Loading
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

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

    private val _localAuditLog = MutableStateFlow(
        listOf(
            RoleAuditEvent(
                timestampLabel = nowFormatted(),
                actor = sanitizeOperatorName(currentUserName, "Directeur Général"),
                roleLabel = WorkspaceRole.DIRECTEUR.shortTitleFr,
                scopeLabel = "Multi-agences",
                action = "Ouverture de session sécurisée",
                allowed = true
            )
        )
    )

    private val remoteAuditLogsState: StateFlow<List<RoleAuditEvent>> =
        repository.observeAuditLogs(currentUserId)
            .map { records ->
                records.map { rec ->
                    val ts = rec.createdAt?.toDate()?.let {
                        java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.FRANCE).format(it)
                    } ?: nowFormatted()
                    RoleAuditEvent(
                        timestampLabel = ts,
                        actor = rec.actor.ifBlank { "Utilisateur" },
                        roleLabel = rec.roleLabel.ifBlank { "Opérateur" },
                        scopeLabel = rec.scopeLabel.ifBlank { "Agence" },
                        action = rec.action,
                        allowed = rec.allowed
                    )
                }
            }
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyList()
            )

    val auditLog: StateFlow<List<RoleAuditEvent>> = combine(_localAuditLog, remoteAuditLogsState) { local, remote ->
        (local + remote)
            .distinctBy { "${it.actor}|${it.action}|${it.allowed}" }
            .take(50)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = _localAuditLog.value
    )

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

        // 3. Automatic Room -> Firestore synchronization as soon as internet connectivity returns
        if (offlineSyncRepository != null) {
            viewModelScope.launch {
                combine(isOnline, pendingOfflineOrders) { online, pending -> online to pending.size }
                    .collect { (online, pendingCount) ->
                        if (online && pendingCount > 0 && !_isSyncingOffline.value) {
                            syncPendingOfflineOrders(autoTriggered = true)
                        }
                    }
            }
        }
    }

    private var lastRegisteredFcmKey: String = ""

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
        // Register FCM device token & role/agency topics once per resolved role+agency
        val fcmKey = "${cleanResolved.role.name}|${cleanResolved.isolatedAgency.orEmpty()}"
        if (appContext != null && fcmKey != lastRegisteredFcmKey) {
            lastRegisteredFcmKey = fcmKey
            PressingNotificationManager.registerFcmTokenAndTopics(
                context = appContext,
                role = cleanResolved.role,
                agencyName = cleanResolved.isolatedAgency
            ) { token, topics ->
                _fcmTopics.value = topics
                viewModelScope.launch {
                    repository.saveFcmToken(
                        currentUserId,
                        FcmTokenRecord(
                            token = token,
                            role = cleanResolved.role.name,
                            agencyName = cleanResolved.isolatedAgency.orEmpty(),
                            topics = topics
                        )
                    )
                }
            }
        }
    }

    fun syncPendingOfflineOrders(autoTriggered: Boolean = false) {
        val syncRepo = offlineSyncRepository ?: return
        if (_isSyncingOffline.value) return
        if (connectivityObserver != null && !connectivityObserver.isCurrentlyOnline()) {
            if (!autoTriggered) {
                _feedbackMessage.value = "Toujours hors-ligne : les commandes restent sécurisées dans le cache Room."
            }
            return
        }
        viewModelScope.launch {
            _isSyncingOffline.value = true
            val batch = runCatching { syncRepo.syncPendingOrders(currentUserId) }
                .getOrDefault(com.example.data.local.OfflineSyncBatchResult())
            _isSyncingOffline.value = false
            if (batch.syncedCount > 0) {
                recordAudit(
                    "Synchronisation auto Room → Cloud : ${batch.syncedCount} commande(s) transmise(s)",
                    allowed = true
                )
                appContext?.let {
                    PressingNotificationManager.showOfflineSyncCompletedNotification(it, batch.syncedCount)
                }
                repository.dispatchPushNotificationRecord(
                    currentUserId,
                    PushNotificationRecord(
                        eventType = PressingNotificationManager.TYPE_OFFLINE_SYNCED,
                        targetRole = "CAISSIER",
                        agencyName = _activeRoleSession.value.isolatedAgency.orEmpty(),
                        orderNumber = batch.syncedOrderNumbers.firstOrNull().orEmpty(),
                        title = "Synchronisation hors-ligne terminée",
                        body = "${batch.syncedCount} commande(s) enregistrée(s) hors-ligne synchronisée(s) vers Firestore."
                    )
                )
                _feedbackMessage.value = "${batch.syncedCount} commande(s) hors-ligne synchronisée(s) avec succès."
            } else if (!autoTriggered && batch.failedCount > 0) {
                _feedbackMessage.value = "Synchronisation partielle : ${batch.failedCount} commande(s) en attente de réseau stable."
            }
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

    private fun recordAudit(
        action: String,
        allowed: Boolean = true,
        route: String = "",
        persistToFirestore: Boolean = !allowed
    ) {
        val session = _activeRoleSession.value
        val event = RoleAuditEvent(
            timestampLabel = nowFormatted(),
            actor = session.operatorName,
            roleLabel = session.role.shortTitleFr,
            scopeLabel = session.isolatedAgency ?: "Multi-agences",
            action = action,
            allowed = allowed
        )
        _localAuditLog.update { (listOf(event) + it).take(40) }
        if (persistToFirestore) {
            viewModelScope.launch {
                repository.logAuditEvent(
                    currentUserId,
                    AuditLogRecord(
                        actor = session.operatorName,
                        roleLabel = session.role.shortTitleFr,
                        scopeLabel = session.isolatedAgency ?: "Multi-agences",
                        action = action,
                        route = route,
                        allowed = allowed
                    )
                )
            }
        }
    }

    /** Logs an unauthorized direct navigation attempt to Firestore audit_logs. */
    fun logUnauthorizedNavigation(route: String, requiredPermission: AccessPermission?) {
        val permLabel = requiredPermission?.labelFr ?: "Module restreint"
        recordAudit(
            action = "Accès bloqué vers « $route » (privilège requis : $permLabel)",
            allowed = false,
            route = route,
            persistToFirestore = true
        )
    }

    private fun requirePermission(permission: AccessPermission, actionLabel: String): Boolean {
        val session = _activeRoleSession.value
        if (session.has(permission)) return true
        recordAudit("Refus : $actionLabel (${permission.labelFr})", allowed = false, persistToFirestore = true)
        _feedbackMessage.value = "Action bloquée : privilège « ${permission.labelFr} » requis."
        return false
    }

    // Role session is strictly bound to the authenticated Firestore user profile — no client-side role switching.

    fun selectIsolatedAgency(agency: String?) {
        val current = _activeRoleSession.value
        if (current.role != WorkspaceRole.DIRECTEUR) {
            recordAudit("Tentative changement périmètre agence refusée", allowed = false, persistToFirestore = true)
            _feedbackMessage.value = "Ce rôle reste strictement isolé sur son agence d'affectation."
            return
        }
        _activeRoleSession.update { it.copy(isolatedAgency = agency?.takeIf { a -> a.isNotBlank() }) }
        recordAudit("Filtre agence Direction : ${agency?.takeIf { it.isNotBlank() } ?: "Toutes les agences"}", allowed = true)
        _feedbackMessage.value = if (agency.isNullOrBlank()) {
            "Vue multi-agences activée."
        } else {
            "Tableau de bord filtré sur : $agency"
        }
    }

    fun toggleRolePermission(role: WorkspaceRole, permission: AccessPermission) {
        if (!_activeRoleSession.value.has(AccessPermission.EDIT_PRIVILEGE_MATRIX)) {
            recordAudit("Modification matrice privilèges refusée (${role.shortTitleFr})", allowed = false, persistToFirestore = true)
            _feedbackMessage.value = "Seul le Directeur Général peut modifier la matrice des privilèges."
            return
        }
        if (role == WorkspaceRole.DIRECTEUR &&
            (permission == AccessPermission.EDIT_PRIVILEGE_MATRIX || permission == AccessPermission.MANAGE_USERS_AND_ROLES)
        ) {
            _feedbackMessage.value = "Ce privilège critique d'administration reste verrouillé pour le Directeur."
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
        val session = _activeRoleSession.value
        val allowed = session.canEditAgencySettingsAndTariffs || session.canManageLocalPrinter || !config.setupCompleted
        if (!allowed) {
            recordAudit("Refus modification paramètres entreprise", allowed = false)
            _feedbackMessage.value = "Action bloquée : accès réservé à la direction ou au gérant d'agence."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveCompanyConfig(currentUserId, config)
            _isBusy.value = false
            if (!silent) showResult(res, "Paramètres agence enregistrés.")
            if (res.isSuccess) onDone()
        }
    }

    fun recordDailyCashCloseZ(
        company: CompanyConfig,
        summary: CashCloseSummary,
        justification: String,
        onDone: () -> Unit = {}
    ) {
        val session = _activeRoleSession.value
        if (!session.canRunCashClose) {
            recordAudit("Refus clôture Z de caisse", allowed = false)
            _feedbackMessage.value = "Action bloquée : clôture de caisse réservée au Caissier, Gérant ou Directeur."
            return
        }
        if (summary.requiresJustification && justification.trim().length < 5) {
            _feedbackMessage.value = "Une justification détaillée est obligatoire lorsque l'écart de caisse est non nul."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val varianceText = when {
                summary.isBalanced -> "Écart 0"
                summary.isSurplus -> "Excédent +${summary.variance}"
                else -> "Manquant ${summary.variance}"
            }
            val label = buildString {
                append("Clôture Z ${summary.dateKey} (${summary.agencyName}) · $varianceText")
                if (justification.isNotBlank()) append(" · Justif: ${justification.trim()}")
            }.take(200)

            val entry = AccountingEntry(
                reference = "Z-${summary.dateKey.replace("-", "")}-${System.currentTimeMillis() % 1000}",
                entryType = "CASH_CLOSE",
                category = "Clôture Z Caisse",
                syscohadaAccount = "571",
                label = label,
                amount = summary.countedCash.coerceAtLeast(0L),
                paymentMethod = PaymentMethod.ESPECES.name,
                counterparty = session.operatorName,
                agencyName = summary.agencyName
            )
            val entryRes = repository.addAccountingEntry(currentUserId, entry)
            if (entryRes.isSuccess) {
                repository.saveCompanyConfig(currentUserId, company.copy(lastCashCloseDate = summary.dateKey))
                recordAudit("Clôture Z validée (${summary.dateKey} · $varianceText)", allowed = true)
            }
            _isBusy.value = false
            showResult(entryRes, "Clôture Z de caisse validée et journée verrouillée.")
            if (entryRes.isSuccess) onDone()
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
        if (!requirePermission(AccessPermission.MANAGE_CLIENTS, "Enregistrer un client")) return
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
        if (!requirePermission(AccessPermission.CANCEL_DELETE_ORDER, "Supprimer un client")) return
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
            val offlineNow = connectivityObserver != null && !connectivityObserver.isCurrentlyOnline()
            if (offlineNow && offlineSyncRepository != null) {
                val localEntity = offlineSyncRepository.enqueueOrder(
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
                recordAudit("Commande $orderNumber mise en cache local Room (hors-ligne)", allowed = true, persistToFirestore = false)
                _feedbackMessage.value = "Mode hors-ligne : $orderNumber enregistrée en cache local (sync auto au retour d'internet)."
                onCreated(localEntity.localId)
                return@launch
            }

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
                    val totalPieces = items.sumOf { it.quantity.coerceAtLeast(1L) }
                    val subtotal = items.sumOf { it.quantity.coerceAtLeast(1L) * it.unitPrice.coerceAtLeast(0L) }
                    val totalNet = (subtotal - discountAmount.coerceIn(0L, subtotal) + expressFee + deliveryFee + taxAmount).coerceAtLeast(0L)
                    val currency = (companyState.value as? UiState.Success)?.data?.currency ?: "FCFA"
                    val totalLabel = formatMoney(totalNet, currency)
                    // Dispatch FCM cashier notification outbox + local alert
                    repository.dispatchPushNotificationRecord(
                        currentUserId,
                        PushNotificationRecord(
                            eventType = PressingNotificationManager.TYPE_NEW_ORDER_CASHIER,
                            targetRole = "CAISSIER",
                            agencyName = agencyName,
                            orderNumber = orderNumber,
                            clientPhone = clientPhone,
                            title = "Nouvelle commande · $orderNumber",
                            body = "$clientName ($totalPieces pièce(s)) · $totalLabel · $agencyName"
                        )
                    )
                    appContext?.let {
                        PressingNotificationManager.showCashierNewOrderNotification(
                            context = it,
                            orderNumber = orderNumber,
                            clientName = clientName,
                            agencyName = agencyName,
                            itemCount = totalPieces,
                            totalLabel = totalLabel
                        )
                    }
                    _feedbackMessage.value = "Commande $orderNumber créée et comptabilisée."
                    onCreated(orderId)
                },
                onFailure = { e ->
                    val isNetworkIssue = e is FirebaseFirestoreException &&
                        (e.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                            e.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)
                    if (isNetworkIssue && offlineSyncRepository != null) {
                        val localEntity = offlineSyncRepository.enqueueOrder(
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
                        _feedbackMessage.value = "Réseau instable : $orderNumber sauvegardée en cache Room pour sync auto."
                        onCreated(localEntity.localId)
                    } else {
                        _feedbackMessage.value = sanitizeErrorMessage(e, "Échec de création de commande.")
                    }
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
                if (newStatus == OrderStatus.READY) {
                    notifyClientOrderReady(order)
                }
            }
            showResult(res, "Statut mis à jour : ${newStatus.labelFr}")
        }
    }

    private fun notifyClientOrderReady(order: PressingOrder) {
        val agencyLabel = order.agencyName.ifBlank { "votre agence Pressing Pro" }
        viewModelScope.launch {
            repository.dispatchPushNotificationRecord(
                currentUserId,
                PushNotificationRecord(
                    eventType = PressingNotificationManager.TYPE_ORDER_READY_CLIENT,
                    targetRole = "CLIENT",
                    agencyName = agencyLabel,
                    orderNumber = order.orderNumber,
                    clientPhone = order.clientPhone,
                    title = "Vêtements prêts · ${order.orderNumber}",
                    body = "Bonjour ${order.clientName}, vos vêtements sont prêts au retrait à $agencyLabel."
                )
            )
        }
        appContext?.let {
            PressingNotificationManager.showClientGarmentReadyNotification(
                context = it,
                orderNumber = order.orderNumber,
                clientName = order.clientName,
                agencyName = agencyLabel
            )
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
                if (newWorkshopStatus == GarmentWorkshopStatus.READY) {
                    val parsed = order.parsedItems()
                    val allReady = parsed.indices.all { idx ->
                        idx == garmentIndex || parsed[idx].workshopStatusEnum() == GarmentWorkshopStatus.READY
                    }
                    if (allReady) {
                        notifyClientOrderReady(order)
                    }
                }
            }
            showResult(res, "Article passé à : ${newWorkshopStatus.labelFr}")
        }
    }

    fun returnOrderToRework(
        order: PressingOrder,
        targetStage: GarmentWorkshopStatus,
        reason: String
    ) {
        if (!requirePermission(AccessPermission.RETURN_FOR_REWORK, "Renvoyer ${order.orderNumber} en reprise")) return
        val session = _activeRoleSession.value
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.returnOrderToRework(
                userId = currentUserId,
                order = order,
                targetStage = targetStage,
                reason = reason,
                operatorName = session.operatorName
            )
            _isBusy.value = false
            if (res.isSuccess) {
                recordAudit(
                    "Reprise qualité ${order.orderNumber} → ${targetStage.labelFr} (${reason.ifBlank { "Non conforme" }})",
                    allowed = true,
                    persistToFirestore = true
                )
            }
            showResult(res, "Commande renvoyée en reprise (${targetStage.labelFr}).")
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
        if (!requirePermission(AccessPermission.MANAGE_DELIVERY_RUNS, "Gérer livraison ${order.orderNumber}")) return
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

    fun confirmDeliveryWithProof(
        order: PressingOrder,
        recipientName: String,
        proofNote: String
    ) {
        if (!requirePermission(AccessPermission.MANAGE_DELIVERY_RUNS, "Valider livraison ${order.orderNumber}")) return
        val session = _activeRoleSession.value
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.confirmDeliveryWithProof(
                userId = currentUserId,
                order = order,
                driverName = session.operatorName,
                zone = order.deliveryZone.ifBlank { order.clientAddress.ifBlank { "Zone client" } },
                recipientName = recipientName,
                proofNote = proofNote
            )
            _isBusy.value = false
            if (res.isSuccess) {
                recordAudit(
                    "Livraison validée avec preuve ${order.orderNumber} (reçu par ${recipientName.ifBlank { order.clientName }})",
                    allowed = true,
                    persistToFirestore = true
                )
            }
            showResult(res, "Livraison confirmée et preuve enregistrée !")
        }
    }

    fun addAccountingEntry(entry: AccountingEntry, onDone: () -> Unit = {}) {
        val session = _activeRoleSession.value
        val allowed = session.has(AccessPermission.ADD_EXPENSE) ||
            (entry.entryType != "EXPENSE" && session.has(AccessPermission.COLLECT_PAYMENT))
        if (!allowed) {
            recordAudit("Refus écriture comptable (${entry.entryType})", allowed = false, persistToFirestore = true)
            _feedbackMessage.value = "Privilège « Saisir dépense caisse » requis."
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
        val session = _activeRoleSession.value
        val allowed = session.has(AccessPermission.ADVANCE_PRODUCTION) || session.has(AccessPermission.CONFIGURE_PRINTER_AND_AGENCY)
        if (!allowed) {
            recordAudit("Refus modification stock", allowed = false, persistToFirestore = true)
            _feedbackMessage.value = "Action bloquée : droits atelier ou gérant requis."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveStockItem(currentUserId, item)
            _isBusy.value = false
            showResult(res, "Article / équipement mis à jour.")
            if (res.isSuccess) onDone()
        }
    }

    fun deleteStockItem(stockId: String) {
        if (!requirePermission(AccessPermission.CONFIGURE_PRINTER_AND_AGENCY, "Supprimer un article de stock")) return
        viewModelScope.launch {
            val res = repository.deleteStockItem(currentUserId, stockId)
            showResult(res, "Article supprimé du stock.")
        }
    }

    fun saveEmployee(emp: EmployeeRecord, onDone: () -> Unit = {}) {
        if (!requirePermission(AccessPermission.MANAGE_USERS_AND_ROLES, "Gérer le personnel")) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveEmployee(currentUserId, emp)
            _isBusy.value = false
            showResult(res, "Fiche employé enregistrée.")
            if (res.isSuccess) onDone()
        }
    }

    fun deleteEmployee(empId: String) {
        if (!requirePermission(AccessPermission.MANAGE_USERS_AND_ROLES, "Supprimer un employé")) return
        viewModelScope.launch {
            val res = repository.deleteEmployee(currentUserId, empId)
            showResult(res, "Employé supprimé.")
        }
    }

    fun createClaim(claim: ClaimRecord, onDone: () -> Unit = {}) {
        if (!requirePermission(AccessPermission.HANDLE_SAV_CLAIMS, "Créer une réclamation")) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createClaim(currentUserId, claim)
            _isBusy.value = false
            showResult(res, "Réclamation enregistrée.")
            if (res.isSuccess) onDone()
        }
    }

    fun updateClaimStatus(claimId: String, status: String, severity: String, resolutionNote: String) {
        if (!requirePermission(AccessPermission.HANDLE_SAV_CLAIMS, "Mettre à jour une réclamation")) return
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
            if (session.has(AccessPermission.MANAGE_CLIENTS)) {
                appendLine("Clients actifs : ${clients.size} (VIP: ${clients.count { it.category == "VIP" }})")
            }
            if (session.has(AccessPermission.VIEW_AGENCY_REVENUE) || session.has(AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA)) {
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
            if (session.has(AccessPermission.ADVANCE_PRODUCTION) || session.has(AccessPermission.CONFIGURE_PRINTER_AND_AGENCY)) {
                appendLine("Stocks critiques (${lowStocks.size}) : ${lowStocks.joinToString { "${it.name} (${it.quantity} ${it.unit})" }.ifBlank { "Aucun" }}")
                appendLine("Machines en maintenance/panne : ${brokenMachines.joinToString { it.name }.ifBlank { "Aucune" }}")
            }
            if (session.has(AccessPermission.HANDLE_SAV_CLAIMS)) {
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
