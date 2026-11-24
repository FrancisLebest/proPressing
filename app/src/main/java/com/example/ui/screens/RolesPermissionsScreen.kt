package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.formatMoney
import com.example.data.AccessPermission
import com.example.data.ActiveRoleSession
import com.example.data.ApprovalRequestRecord
import com.example.data.ApprovalStatus
import com.example.data.AtomicPermission
import com.example.data.AtomicPermissionRegistry
import com.example.data.AuthorizationEngine
import com.example.data.CompanyConfig
import com.example.data.EmployeeRecord
import com.example.data.ManagerAccessPolicy
import com.example.data.ManagerPermissionProfile
import com.example.data.PermissionRiskLevel
import com.example.data.RoleAuditEvent
import com.example.data.RolePermissionMatrix
import com.example.data.SensitiveOperationType
import com.example.data.WorkspaceRole
import com.example.ui.components.GPAApprovalStatusChip
import com.example.ui.components.GPAFinancialLimitSlider
import com.example.ui.components.RoleBadgeChip
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RolesPermissionsScreen(
    company: CompanyConfig,
    employees: List<EmployeeRecord>,
    activeSession: ActiveRoleSession,
    rolePermissionsMap: Map<WorkspaceRole, Set<AccessPermission>>,
    auditLog: List<RoleAuditEvent> = emptyList(),
    managerPolicies: List<ManagerAccessPolicy> = emptyList(),
    approvalRequests: List<ApprovalRequestRecord> = emptyList(),
    onTogglePermission: (WorkspaceRole, AccessPermission) -> Unit,
    onResetRolePermissions: (WorkspaceRole) -> Unit,
    onSaveManagerPolicy: (ManagerAccessPolicy) -> Unit = {},
    onSubmitApprovalRequest: (SensitiveOperationType, String, Long, Long, String) -> Unit = { _, _, _, _, _ -> },
    onReviewApprovalRequest: (ApprovalRequestRecord, Boolean, String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var inspectedRole by remember { mutableStateOf(activeSession.role) }

    val currentPermissions = rolePermissionsMap[inspectedRole]
        ?: RolePermissionMatrix.defaultPermissionsFor(inspectedRole)
    val permissionsByDomain = remember { AccessPermission.entries.groupBy { it.domainFr } }
    val isDirector = activeSession.role == WorkspaceRole.DIRECTEUR || activeSession.isSuperAdmin
    val pendingApprovalsCount = remember(approvalRequests) {
        approvalRequests.count { it.statusEnum() == ApprovalStatus.PENDING }
    }

    // Manager Access Configurator state
    val configurableManagers = remember(employees) {
        employees.filter { it.role != "DIRECTEUR" }.ifEmpty {
            listOf(
                EmployeeRecord(
                    id = "default_manager",
                    fullName = "Gestionnaire — Compte agence",
                    role = "CAISSIER",
                    agencyName = company.agencies.firstOrNull() ?: "Agence Principale"
                )
            )
        }
    }
    var selectedManagerIdx by remember { mutableIntStateOf(0) }
    val selectedManager = configurableManagers.getOrElse(selectedManagerIdx) { configurableManagers.first() }
    val existingPolicy = remember(selectedManager.id, managerPolicies) {
        managerPolicies.firstOrNull {
            it.employeeId == selectedManager.id || it.employeeName.equals(selectedManager.fullName, ignoreCase = true)
        }
    }

    var selectedProfile by remember { mutableStateOf(ManagerPermissionProfile.CAISSIER) }
    var allowedAgencies by remember { mutableStateOf(setOf(selectedManager.agencyName.ifBlank { company.agencies.firstOrNull() ?: "Agence Principale" })) }
    var maxDiscountPct by remember { mutableIntStateOf(5) }
    var maxRefundText by remember { mutableStateOf("5000") }
    var maxExpenseText by remember { mutableStateOf("10000") }
    var maxDisbursementText by remember { mutableStateOf("20000") }
    var startHourText by remember { mutableStateOf("7") }
    var endHourText by remember { mutableStateOf("21") }
    var allowDeferred by remember { mutableStateOf(false) }
    var allowCashClose by remember { mutableStateOf(true) }
    var customGranted by remember { mutableStateOf(ManagerPermissionProfile.CAISSIER.defaultPermissions) }
    var atomicGrantedCodes by remember { mutableStateOf(AtomicPermissionRegistry.defaultAtomicCodesForProfile(ManagerPermissionProfile.CAISSIER)) }
    var atomicDeniedCodes by remember { mutableStateOf(emptySet<String>()) }
    var atomicSearchQuery by remember { mutableStateOf("") }
    var selectedAtomicModule by remember { mutableStateOf("Commandes") }
    var pendingSensitivePermission by remember { mutableStateOf<AtomicPermission?>(null) }
    var configCopiedFeedback by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val atomicModules = remember { AtomicPermissionRegistry.byModule.keys.toList() }

    LaunchedEffect(selectedManager.id, existingPolicy) {
        configCopiedFeedback = false
        if (existingPolicy != null) {
            selectedProfile = ManagerPermissionProfile.fromRaw(existingPolicy.profileCode)
            allowedAgencies = existingPolicy.allowedAgencies.toSet()
                .ifEmpty { setOf(selectedManager.agencyName.ifBlank { company.agencies.firstOrNull() ?: "Agence Principale" }) }
            maxDiscountPct = existingPolicy.maxDiscountPercent.coerceIn(0, 30)
            maxRefundText = existingPolicy.maxRefundFcfa.toString()
            maxExpenseText = existingPolicy.maxExpenseFcfa.toString()
            maxDisbursementText = existingPolicy.maxDisbursementFcfa.toString()
            startHourText = existingPolicy.accessStartHour.toString()
            endHourText = existingPolicy.accessEndHour.toString()
            allowDeferred = existingPolicy.allowDeferredPayment
            allowCashClose = existingPolicy.allowCashRegisterClose
            customGranted = existingPolicy.grantedPermissions
                .mapNotNull { code -> AccessPermission.entries.firstOrNull { it.name == code } }
                .toSet()
                .ifEmpty { selectedProfile.defaultPermissions }
            val savedAtomic = existingPolicy.grantedPermissions
                .filter { it.contains('.') && AtomicPermissionRegistry.byCode.containsKey(it) }
                .toSet()
            atomicGrantedCodes = savedAtomic.ifEmpty {
                AtomicPermissionRegistry.defaultAtomicCodesForProfile(selectedProfile)
            }
            atomicDeniedCodes = existingPolicy.deniedPermissions
                .filter { it.contains('.') && AtomicPermissionRegistry.byCode.containsKey(it) }
                .toSet()
        } else {
            val defaultProf = when (selectedManager.role) {
                "OPERATEUR" -> ManagerPermissionProfile.AGENT_ATELIER
                "LIVREUR" -> ManagerPermissionProfile.LIVREUR
                "CONTROLE_QUALITE" -> ManagerPermissionProfile.POLYVALENT
                else -> ManagerPermissionProfile.CAISSIER
            }
            selectedProfile = defaultProf
            allowedAgencies = setOf(selectedManager.agencyName.ifBlank { company.agencies.firstOrNull() ?: "Agence Principale" })
            maxDiscountPct = defaultProf.defaultMaxDiscountPercent
            maxRefundText = defaultProf.defaultMaxRefundFcfa.toString()
            maxExpenseText = defaultProf.defaultMaxExpenseFcfa.toString()
            maxDisbursementText = defaultProf.defaultMaxDisbursementFcfa.toString()
            customGranted = defaultProf.defaultPermissions
            atomicGrantedCodes = AtomicPermissionRegistry.defaultAtomicCodesForProfile(defaultProf)
            atomicDeniedCodes = emptySet()
        }
    }

    val inheritedProfileAtomic = remember(selectedProfile) {
        AtomicPermissionRegistry.defaultAtomicCodesForProfile(selectedProfile)
    }
    val effectiveAtomicCodes = remember(atomicGrantedCodes, atomicDeniedCodes) {
        (atomicGrantedCodes - atomicDeniedCodes).filter { code ->
            AtomicPermissionRegistry.byCode[code]?.isDelegable == true
        }.toSet()
    }
    val filteredAtomicPermissions = remember(selectedAtomicModule, atomicSearchQuery) {
        val q = atomicSearchQuery.trim()
        val source = if (q.isNotBlank()) {
            AtomicPermissionRegistry.allPermissions
        } else {
            AtomicPermissionRegistry.byModule[selectedAtomicModule].orEmpty()
        }
        if (q.isBlank()) {
            source
        } else {
            source.filter { perm ->
                perm.labelFr.contains(q, ignoreCase = true) ||
                    perm.code.contains(q, ignoreCase = true) ||
                    perm.moduleFr.contains(q, ignoreCase = true)
            }
        }
    }

    // Approval submission form state
    var reqOpType by remember { mutableStateOf(SensitiveOperationType.REFUND) }
    var reqRef by remember { mutableStateOf("CMD-001") }
    var reqAmount by remember { mutableStateOf("15000") }
    var reqReason by remember { mutableStateOf("") }

    val availableAgencies = remember(company.agencies) {
        company.agencies.filter { it.isNotBlank() }.ifEmpty { listOf("Agence Principale") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rôles, Accès & Validations", fontWeight = FontWeight.Bold)
                        Text(
                            "RBAC + ABAC · Plafonds & Isolation Multi-Agences",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFBFD4FF)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightNavy,
                    titleContentColor = Color.White
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = RoyalBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Gestionnaires", maxLines = 1) },
                    icon = { Icon(Icons.Rounded.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("rbac_tab_managers")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            if (pendingApprovalsCount > 0) "Validations ($pendingApprovalsCount)" else "Validations",
                            maxLines = 1
                        )
                    },
                    icon = { Icon(Icons.Rounded.Gavel, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("rbac_tab_approvals")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Matrice & Audit", maxLines = 1) },
                    icon = { Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("rbac_tab_matrix")
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Multi-tenant Hero Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightNavy)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.horizontalGradient(listOf(MidnightNavy, DeepNavySurface, RoyalBlue)))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Security,
                                            contentDescription = "Isolation multi-tenant",
                                            tint = AfricaGold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "GESTION PRO AFRICA · Pressing Pro",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${company.companyName} · ${availableAgencies.size} agence(s) · DENY BY DEFAULT",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFD5E1FF)
                                        )
                                    }
                                }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(50), color = EmeraldSuccess.copy(alpha = 0.24f)) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(14.dp))
                                            Text("RBAC + ABAC Firestore actif", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.14f)) {
                                        Text(
                                            text = activeSession.effectiveScope.badgeFr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AfricaGold,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                when (selectedTab) {
                    // ================================================================
                    // TAB 0: CONFIGURATEUR GESTIONNAIRE (6 PROFILS + PLAFONDS FINANCIERS)
                    // ================================================================
                    0 -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Configurer les accès d'un gestionnaire",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        configurableManagers.forEachIndexed { idx, emp ->
                                            FilterChip(
                                                selected = selectedManagerIdx == idx,
                                                onClick = { selectedManagerIdx = idx },
                                                label = { Text("${emp.fullName} (${emp.role})") },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider()

                                    Text(
                                        text = "Profil de permissions prédéfini (PRESSING_MANAGER)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ManagerPermissionProfile.entries.forEach { profile ->
                                            FilterChip(
                                                selected = selectedProfile == profile,
                                                onClick = {
                                                    selectedProfile = profile
                                                    configCopiedFeedback = false
                                                    if (profile != ManagerPermissionProfile.PERSONNALISE) {
                                                        customGranted = profile.defaultPermissions
                                                        atomicGrantedCodes = AtomicPermissionRegistry.defaultAtomicCodesForProfile(profile)
                                                        atomicDeniedCodes = emptySet()
                                                        maxDiscountPct = profile.defaultMaxDiscountPercent
                                                        maxRefundText = profile.defaultMaxRefundFcfa.toString()
                                                        maxExpenseText = profile.defaultMaxExpenseFcfa.toString()
                                                        maxDisbursementText = profile.defaultMaxDisbursementFcfa.toString()
                                                    }
                                                },
                                                label = { Text(profile.labelFr) },
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("profile_chip_${profile.name}")
                                            )
                                        }
                                    }
                                    Text(
                                        text = selectedProfile.summaryFr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    HorizontalDivider()

                                    // Authorized Agencies Multi-Select
                                    Text(
                                        text = "Agences autorisées",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        availableAgencies.forEach { agency ->
                                            val checked = agency in allowedAgencies
                                            FilterChip(
                                                selected = checked,
                                                enabled = isDirector,
                                                onClick = {
                                                    allowedAgencies = if (checked && allowedAgencies.size > 1) {
                                                        allowedAgencies - agency
                                                    } else {
                                                        allowedAgencies + agency
                                                    }
                                                },
                                                leadingIcon = { Icon(Icons.Rounded.Business, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                label = { Text(agency) },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider()

                                    // Financial Restrictions Block (Interactive Slider + FCFA Ceilings)
                                    Text(
                                        text = "Restrictions financières & Horaires d'accès",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue
                                    )

                                    GPAFinancialLimitSlider(
                                        discountPercent = maxDiscountPct,
                                        enabled = isDirector,
                                        onValueChange = { maxDiscountPct = it }
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = maxRefundText,
                                            onValueChange = { maxRefundText = it.filter { c -> c.isDigit() }.take(8) },
                                            label = { Text("Plafond remb. (${company.currency})") },
                                            enabled = isDirector,
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = maxExpenseText,
                                            onValueChange = { maxExpenseText = it.filter { c -> c.isDigit() }.take(8) },
                                            label = { Text("Plafond dépense (${company.currency})") },
                                            enabled = isDirector,
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = maxDisbursementText,
                                            onValueChange = { maxDisbursementText = it.filter { c -> c.isDigit() }.take(8) },
                                            label = { Text("Plafond décaissement") },
                                            enabled = isDirector,
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = startHourText,
                                            onValueChange = { startHourText = it.filter { c -> c.isDigit() }.take(2) },
                                            label = { Text("Début (h)") },
                                            enabled = isDirector,
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.width(86.dp)
                                        )
                                        OutlinedTextField(
                                            value = endHourText,
                                            onValueChange = { endHourText = it.filter { c -> c.isDigit() }.take(2) },
                                            label = { Text("Fin (h)") },
                                            enabled = isDirector,
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.width(86.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Autoriser paiement différé", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = allowDeferred, enabled = isDirector, onCheckedChange = { allowDeferred = it })
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Autoriser clôture Z de caisse", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = allowCashClose, enabled = isDirector, onCheckedChange = { allowCashClose = it })
                                    }

                                    HorizontalDivider()

                                    // Granular Per-Module Atomic Permissions Matrix (Section 7 & 19)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Permissions atomiques par module",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = RoyalBlue
                                            )
                                            Text(
                                                text = "Registre v${AtomicPermissionRegistry.REGISTRY_VERSION} · DENY explicite prioritaire",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = SoftBlueContainer
                                        ) {
                                            Text(
                                                text = "${effectiveAtomicCodes.size} / ${AtomicPermissionRegistry.allPermissions.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = RoyalBlue,
                                                modifier = Modifier
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                                    .testTag("effective_atomic_count_badge")
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = atomicSearchQuery,
                                        onValueChange = { atomicSearchQuery = it.take(60) },
                                        label = { Text("Rechercher une permission (ex. orders.cancel, caisse)") },
                                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                        trailingIcon = {
                                            if (atomicSearchQuery.isNotBlank()) {
                                                IconButton(onClick = { atomicSearchQuery = "" }) {
                                                    Icon(Icons.Rounded.Close, contentDescription = "Effacer")
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("atomic_permission_search")
                                    )

                                    if (atomicSearchQuery.isBlank()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            atomicModules.forEach { mod ->
                                                val modPerms = AtomicPermissionRegistry.byModule[mod].orEmpty()
                                                val activeCount = modPerms.count { it.code in effectiveAtomicCodes }
                                                FilterChip(
                                                    selected = selectedAtomicModule == mod,
                                                    onClick = { selectedAtomicModule = mod },
                                                    label = { Text("$mod ($activeCount/${modPerms.size})") },
                                                    modifier = Modifier.heightIn(min = 48.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        filteredAtomicPermissions.take(24).forEach { perm ->
                                            val isInherited = perm.code in inheritedProfileAtomic
                                            val isExplicitDeny = perm.code in atomicDeniedCodes
                                            val isEffectiveAllow = perm.isDelegable && !isExplicitDeny && perm.code in atomicGrantedCodes
                                            val isCustomAdded = isEffectiveAllow && !isInherited
                                            val isSensitive = perm.riskLevel == PermissionRiskLevel.HIGH || perm.riskLevel == PermissionRiskLevel.CRITICAL

                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = when {
                                                    !perm.isDelegable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                                    isExplicitDeny -> CrimsonContainer.copy(alpha = 0.45f)
                                                    isEffectiveAllow -> SoftBlueContainer.copy(alpha = 0.45f)
                                                    else -> MaterialTheme.colorScheme.surface
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    when {
                                                        isExplicitDeny -> CrimsonDanger.copy(alpha = 0.55f)
                                                        isEffectiveAllow -> RoyalBlue.copy(alpha = 0.4f)
                                                        else -> MaterialTheme.colorScheme.outlineVariant
                                                    }
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = perm.labelFr,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "${perm.code} · ${perm.actionType.labelFr} · Forfait ${perm.minPlan.labelFr}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontFamily = JetBrainsMonoFamily,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        if (!perm.isDelegable) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Lock,
                                                                contentDescription = "Non délégable",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        } else {
                                                            Switch(
                                                                checked = isEffectiveAllow,
                                                                enabled = isDirector,
                                                                onCheckedChange = { enable ->
                                                                    if (enable) {
                                                                        if (isSensitive) {
                                                                            pendingSensitivePermission = perm
                                                                        } else {
                                                                            atomicDeniedCodes = atomicDeniedCodes - perm.code
                                                                            atomicGrantedCodes = atomicGrantedCodes + perm.code
                                                                            selectedProfile = ManagerPermissionProfile.PERSONNALISE
                                                                        }
                                                                    } else {
                                                                        atomicGrantedCodes = atomicGrantedCodes - perm.code
                                                                        selectedProfile = ManagerPermissionProfile.PERSONNALISE
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }

                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        if (!perm.isDelegable) {
                                                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                                                                Text(
                                                                    text = "Verrouillée · Admin uniquement",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                )
                                                            }
                                                        }
                                                        if (isInherited && !isExplicitDeny && isEffectiveAllow) {
                                                            Surface(shape = RoundedCornerShape(50), color = EmeraldContainer) {
                                                                Text(
                                                                    text = "Héritée",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = EmeraldSuccess,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                )
                                                            }
                                                        }
                                                        if (isCustomAdded) {
                                                            Surface(shape = RoundedCornerShape(50), color = SoftBlueContainer) {
                                                                Text(
                                                                    text = "Personnalisée",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = RoyalBlue,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                )
                                                            }
                                                        }
                                                        if (isSensitive) {
                                                            Surface(shape = RoundedCornerShape(50), color = AmberContainer) {
                                                                Text(
                                                                    text = if (perm.riskLevel == PermissionRiskLevel.CRITICAL) "Critique" else "Sensible",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF92400E),
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                )
                                                            }
                                                        }
                                                        if (isExplicitDeny) {
                                                            Surface(shape = RoundedCornerShape(50), color = CrimsonContainer) {
                                                                Text(
                                                                    text = "DENY explicite",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = CrimsonDanger,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                )
                                                            }
                                                        }
                                                        if (perm.isDelegable && isDirector) {
                                                            FilterChip(
                                                                selected = isExplicitDeny,
                                                                onClick = {
                                                                    if (isExplicitDeny) {
                                                                        atomicDeniedCodes = atomicDeniedCodes - perm.code
                                                                    } else {
                                                                        atomicDeniedCodes = atomicDeniedCodes + perm.code
                                                                        atomicGrantedCodes = atomicGrantedCodes - perm.code
                                                                        selectedProfile = ManagerPermissionProfile.PERSONNALISE
                                                                    }
                                                                },
                                                                leadingIcon = {
                                                                    Icon(
                                                                        Icons.Rounded.Block,
                                                                        contentDescription = null,
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                },
                                                                label = {
                                                                    Text(
                                                                        if (isExplicitDeny) "Retirer DENY" else "Forcer DENY",
                                                                        style = MaterialTheme.typography.labelSmall
                                                                    )
                                                                },
                                                                colors = FilterChipDefaults.filterChipColors(
                                                                    selectedContainerColor = CrimsonDanger,
                                                                    selectedLabelColor = Color.White,
                                                                    selectedLeadingIconColor = Color.White
                                                                ),
                                                                modifier = Modifier.heightIn(min = 32.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (filteredAtomicPermissions.size > 24) {
                                            Text(
                                                text = "Affichage limité aux 24 premiers résultats — précisez votre recherche.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = AmberContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Les permissions non délégables (salaires RH, SYSCOHADA global, audit sécurité, matrice RBAC, plateforme SaaS) restent strictement bloquées pour tout gestionnaire.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF92400E),
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val summary = buildString {
                                                appendLine("=== CONFIGURATION GESTIONNAIRE — ${company.companyName} ===")
                                                appendLine("Gestionnaire : ${selectedManager.fullName} (${selectedManager.role})")
                                                appendLine("Profil : ${selectedProfile.labelFr}")
                                                appendLine("Agences autorisées : ${allowedAgencies.joinToString(", ")}")
                                                appendLine("Plafonds : Remise <= $maxDiscountPct% | Remb. <= ${maxRefundText} ${company.currency} | Dépense <= ${maxExpenseText} ${company.currency} | Décaissement <= ${maxDisbursementText} ${company.currency}")
                                                appendLine("Horaires : ${startHourText}h - ${endHourText}h | Paiement différé: ${if (allowDeferred) "Oui" else "Non"} | Clôture Z: ${if (allowCashClose) "Oui" else "Non"}")
                                                appendLine("Permissions atomiques effectives (${effectiveAtomicCodes.size}) : ${effectiveAtomicCodes.sorted().joinToString(", ")}")
                                                if (atomicDeniedCodes.isNotEmpty()) {
                                                    appendLine("Interdictions explicites DENY (${atomicDeniedCodes.size}) : ${atomicDeniedCodes.sorted().joinToString(", ")}")
                                                }
                                            }
                                            clipboardManager.setText(AnnotatedString(summary))
                                            configCopiedFeedback = true
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, RoyalBlue),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("copy_manager_config_button")
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = RoyalBlue)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (configCopiedFeedback) "Configuration copiée dans le presse-papiers" else "Copier la configuration",
                                            fontWeight = FontWeight.SemiBold,
                                            color = RoyalBlue
                                        )
                                    }

                                    if (isDirector) {
                                        Button(
                                            onClick = {
                                                val safeLegacy = (customGranted - AuthorizationEngine.nonDelegablePermissions)
                                                    .map { it.name }
                                                val safeAtomic = effectiveAtomicCodes.sorted()
                                                val combinedGranted = (safeLegacy + safeAtomic).distinct()
                                                onSaveManagerPolicy(
                                                    ManagerAccessPolicy(
                                                        id = existingPolicy?.id.orEmpty(),
                                                        employeeId = selectedManager.id.ifBlank { selectedManager.fullName },
                                                        employeeName = selectedManager.fullName,
                                                        profileCode = selectedProfile.name,
                                                        allowedAgencies = allowedAgencies.toList(),
                                                        grantedPermissions = combinedGranted,
                                                        deniedPermissions = atomicDeniedCodes.sorted(),
                                                        maxDiscountPercent = maxDiscountPct,
                                                        maxRefundFcfa = maxRefundText.toLongOrNull() ?: 5_000L,
                                                        maxExpenseFcfa = maxExpenseText.toLongOrNull() ?: 10_000L,
                                                        maxDisbursementFcfa = maxDisbursementText.toLongOrNull() ?: 20_000L,
                                                        dailyOperationCapFcfa = 100_000L,
                                                        allowDeferredPayment = allowDeferred,
                                                        allowCashRegisterClose = allowCashClose,
                                                        accessStartHour = (startHourText.toIntOrNull() ?: 7).coerceIn(0, 23),
                                                        accessEndHour = (endHourText.toIntOrNull() ?: 21).coerceIn(1, 24),
                                                        active = true
                                                    )
                                                )
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 50.dp)
                                                .testTag("save_manager_policy_button")
                                        ) {
                                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Enregistrer la politique (${selectedManager.fullName})", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ================================================================
                    // TAB 1: WORKFLOW DE VALIDATION ADMINISTRATIVE (OPÉRATIONS SENSIBLES)
                    // ================================================================
                    1 -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Soumettre une demande de validation",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue
                                    )
                                    Text(
                                        text = "Toute opération dépassant un plafond gestionnaire (remise exceptionnelle, remboursement, dépense, annulation) passe par ce workflow avec séparation des responsabilités.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SensitiveOperationType.entries.forEach { op ->
                                            FilterChip(
                                                selected = reqOpType == op,
                                                onClick = { reqOpType = op },
                                                label = { Text(op.labelFr) },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = reqRef,
                                            onValueChange = { reqRef = it.take(40) },
                                            label = { Text("Référence") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = reqAmount,
                                            onValueChange = { reqAmount = it.filter { c -> c.isDigit() }.take(8) },
                                            label = { Text("Montant (${company.currency})") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    OutlinedTextField(
                                        value = reqReason,
                                        onValueChange = { reqReason = it.take(250) },
                                        label = { Text("Motif obligatoire") },
                                        placeholder = { Text("Ex. Remboursement tache persistante validé avec client") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            onSubmitApprovalRequest(
                                                reqOpType,
                                                reqRef,
                                                reqAmount.toLongOrNull() ?: 0L,
                                                maxRefundText.toLongOrNull() ?: 5_000L,
                                                reqReason
                                            )
                                            reqReason = ""
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("submit_approval_request_button")
                                    ) {
                                        Text("Envoyer à l'Admin Pressing", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (approvalRequests.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SoftBlueContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Aucune demande de validation enregistrée pour le moment.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MidnightNavy,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(approvalRequests, key = { it.id.ifBlank { it.payloadHash + it.createdAt } }) { req ->
                                val status = req.statusEnum()
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = req.operationEnum().labelFr,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            GPAApprovalStatusChip(status = status)
                                        }
                                        Text(
                                            text = "${formatMoney(req.amountFcfa, company.currency)} · Réf. ${req.resourceRef} · ${req.agencyName}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalBlue
                                        )
                                        Text(
                                            text = "Demandé par ${req.requestedBy} (${req.requestedByRole}) — Motif : ${req.reason}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (req.reviewedBy.isNotBlank()) {
                                            Text(
                                                text = "Décision par ${req.reviewedBy} : ${req.reviewNote}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (status == ApprovalStatus.REJECTED) CrimsonDanger else EmeraldSuccess
                                            )
                                        }
                                        if (isDirector && status == ApprovalStatus.PENDING) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Button(
                                                    onClick = { onReviewApprovalRequest(req, true, "Approuvée et exécutée") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                ) {
                                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Approuver", fontWeight = FontWeight.Bold)
                                                }
                                                OutlinedButton(
                                                    onClick = { onReviewApprovalRequest(req, false, "Plafond dépassé sans justificatif") },
                                                    border = BorderStroke(1.dp, CrimsonDanger),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Close, contentDescription = null, tint = CrimsonDanger, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Rejeter", color = CrimsonDanger, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ================================================================
                    // TAB 2: MATRICE DES 27 PRIVILÈGES & JOURNAL D'AUDIT SÉCURITÉ
                    // ================================================================
                    else -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Inspecter la matrice des 27 privilèges par rôle",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    WorkspaceRole.entries.forEach { role ->
                                        FilterChip(
                                            selected = inspectedRole == role,
                                            onClick = { inspectedRole = role },
                                            label = { Text(role.shortTitleFr, fontWeight = FontWeight.SemiBold) },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                            },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("rbac_role_chip_${role.name}")
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RoleBadgeChip(role = inspectedRole)
                                        Surface(shape = RoundedCornerShape(50), color = SoftBlueContainer) {
                                            Text(
                                                text = "${currentPermissions.size} / ${AccessPermission.entries.size} droits",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RoyalBlue,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = inspectedRole.descriptionFr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isDirector) {
                                        OutlinedButton(
                                            onClick = { onResetRolePermissions(inspectedRole) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("rbac_reset_role_button"),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Réinitialiser (${inspectedRole.shortTitleFr})", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        permissionsByDomain.forEach { (domain, perms) ->
                            item(key = "domain_$domain") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(domain, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RoyalBlue)
                                            Text(
                                                "${perms.count { it in currentPermissions }}/${perms.size}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        HorizontalDivider()
                                        perms.forEachIndexed { idx, perm ->
                                            val granted = perm in currentPermissions
                                            val locked = inspectedRole == WorkspaceRole.DIRECTEUR &&
                                                (perm == AccessPermission.EDIT_PRIVILEGE_MATRIX || perm == AccessPermission.MANAGE_USERS_AND_ROLES)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(perm.labelFr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                        if (locked) {
                                                            Icon(Icons.Rounded.Lock, contentDescription = "Verrouillé", tint = AfricaGold, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                    Text(perm.descriptionFr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Switch(
                                                    checked = granted,
                                                    enabled = isDirector && !locked,
                                                    onCheckedChange = { onTogglePermission(inspectedRole, perm) },
                                                    modifier = Modifier.testTag("rbac_switch_${inspectedRole.name}_${perm.name}")
                                                )
                                            }
                                            if (idx < perms.lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "security_audit_log") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rbac_audit_log_card"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(20.dp))
                                            Text("Journal d'audit sécurité", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RoyalBlue)
                                        }
                                        Surface(shape = RoundedCornerShape(50), color = SoftBlueContainer) {
                                            Text(
                                                "${auditLog.size} événement(s)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RoyalBlue,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                    if (auditLog.isEmpty()) {
                                        Text("Aucun événement enregistré.", style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        auditLog.take(15).forEachIndexed { idx, ev ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 5.dp)
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(if (ev.allowed) EmeraldSuccess else CrimsonDanger)
                                                )
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("${ev.actor} (${ev.roleLabel})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                                        Surface(
                                                            shape = RoundedCornerShape(50),
                                                            color = if (ev.allowed) EmeraldContainer else CrimsonContainer
                                                        ) {
                                                            Text(
                                                                text = if (ev.allowed) "AUTORISÉ" else "BLOQUÉ",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (ev.allowed) Color(0xFF047857) else CrimsonDanger,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(ev.action, style = MaterialTheme.typography.bodySmall)
                                                    Text(
                                                        "${ev.timestampLabel} · ${ev.scopeLabel}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (idx < auditLog.take(15).lastIndex) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    pendingSensitivePermission?.let { perm ->
        AlertDialog(
            onDismissRequest = { pendingSensitivePermission = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = "Avertissement permission sensible",
                    tint = AfricaGold
                )
            },
            title = {
                Text(
                    text = "Activer une permission sensible ?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Vous êtes sur le point d'accorder à ${selectedManager.fullName} la permission « ${perm.labelFr} » (${perm.code}).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Niveau de risque : ${perm.riskLevel.labelFr}. Toute action exécutée avec cette permission sera journalisée dans l'audit sécurité et restera soumise aux plafonds financiers et à l'isolation par agence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        atomicDeniedCodes = atomicDeniedCodes - perm.code
                        atomicGrantedCodes = atomicGrantedCodes + perm.code
                        selectedProfile = ManagerPermissionProfile.PERSONNALISE
                        pendingSensitivePermission = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier.testTag("confirm_sensitive_permission_button")
                ) {
                    Text("Confirmer l'activation")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSensitivePermission = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

