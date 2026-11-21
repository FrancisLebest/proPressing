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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AccessPermission
import com.example.data.ActiveRoleSession
import com.example.data.CompanyConfig
import com.example.data.EmployeeRecord
import com.example.data.RoleAuditEvent
import com.example.data.RolePermissionMatrix
import com.example.data.WorkspaceRole
import com.example.ui.components.RoleBadgeChip
import com.example.ui.theme.AfricaGold
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
    onTogglePermission: (WorkspaceRole, AccessPermission) -> Unit,
    onResetRolePermissions: (WorkspaceRole) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var inspectedRole by remember { mutableStateOf(activeSession.role) }

    val currentPermissions = rolePermissionsMap[inspectedRole]
        ?: RolePermissionMatrix.defaultPermissionsFor(inspectedRole)
    val permissionsByDomain = remember { AccessPermission.entries.groupBy { it.domainFr } }
    val isDirector = activeSession.role == WorkspaceRole.DIRECTEUR

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rôles, Droits & Isolation", fontWeight = FontWeight.Bold)
                        Text(
                            "RBAC granulaire · Entités & données privées",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFBFD4FF)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Multi-tenant & Entity Isolation Status Hero Card
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
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = "Isolation multi-tenant",
                                        tint = AfricaGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Isolation Multi-Entités & Cloud Privé",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${company.companyName} · ${company.agencies.size} entité(s)/agence(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD5E1FF)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = EmeraldSuccess.copy(alpha = 0.24f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.VerifiedUser,
                                            contentDescription = null,
                                            tint = Color(0xFF6EE7B7),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Cloisonnement Firestore actif",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = activeSession.effectiveScope.badgeFr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AfricaGold,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Session active : ${activeSession.operatorName} (${activeSession.role.labelFr}) · ${activeSession.scopeLabel()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2ECFF)
                            )
                        }
                    }
                }
            }

            // Role Selector Pills
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sélectionner un rôle à inspecter ou activer",
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
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("rbac_role_chip_${role.name}")
                            )
                        }
                    }
                }
            }

            // Selected Role Overview & Instant Isolated Workspace Switcher
            item {
                val matchingStaff = employees.filter {
                    when (inspectedRole) {
                        WorkspaceRole.DIRECTEUR -> it.role == "DIRECTEUR"
                        WorkspaceRole.GERANT_AGENCE -> it.role == "DIRECTEUR"
                        WorkspaceRole.CAISSIER -> it.role == "CAISSIER"
                        WorkspaceRole.OPERATEUR -> it.role == "OPERATEUR"
                        WorkspaceRole.CONTROLE_QUALITE -> it.role == "CONTROLE_QUALITE"
                        WorkspaceRole.LIVREUR -> it.role == "LIVREUR"
                    }
                }

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
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SoftBlueContainer
                            ) {
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

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Business,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Isolation : ${inspectedRole.defaultScope.labelFr}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftBlueContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Cloisonnement strict : chaque collaborateur est automatiquement rattaché à son rôle et son agence lors de la connexion Firebase. Aucun changement de rôle n'est autorisé depuis l'interface.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MidnightNavy
                                )
                            }
                        }

                        if (isDirector) {
                            OutlinedButton(
                                onClick = { onResetRolePermissions(inspectedRole) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("rbac_reset_role_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Réinitialiser les privilèges par défaut (${inspectedRole.shortTitleFr})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Granular Permissions Matrix by Domain
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
                                Text(
                                    text = domain,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue
                                )
                                Text(
                                    text = "${perms.count { it in currentPermissions }}/${perms.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider()
                            perms.forEachIndexed { idx, perm ->
                                val granted = perm in currentPermissions
                                val locked = inspectedRole == WorkspaceRole.DIRECTEUR &&
                                    perm == AccessPermission.MANAGE_STAFF_AND_ROLES
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
                                            Text(
                                                text = perm.labelFr,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (locked) {
                                                Icon(
                                                    Icons.Rounded.Lock,
                                                    contentDescription = "Verrouillé",
                                                    tint = AfricaGold,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = perm.descriptionFr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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

            // Real-time Security Audit Log Card
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Journal d'audit sécurité",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SoftBlueContainer
                            ) {
                                Text(
                                    text = "${auditLog.size} événement(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "Traçabilité temps réel des bascules de rôles, changements d'entités isolées, modifications de droits et tentatives bloquées.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider()
                        if (auditLog.isEmpty()) {
                            Text(
                                text = "Aucun événement de sécurité enregistré sur cette session.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            auditLog.take(12).forEachIndexed { idx, ev ->
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
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${ev.actor} (${ev.roleLabel})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
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
                                        Text(
                                            text = ev.action,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${ev.timestampLabel} · ${ev.scopeLabel}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (idx < auditLog.take(12).lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
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
