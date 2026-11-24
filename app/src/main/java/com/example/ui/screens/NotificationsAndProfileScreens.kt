package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.formatMoney
import com.example.data.AccessPermission
import com.example.data.AccountingEntry
import com.example.data.ActiveRoleSession
import com.example.data.ApprovalRequestRecord
import com.example.data.ApprovalStatus
import com.example.data.AuthUserProfile
import com.example.data.ClaimRecord
import com.example.data.CompanyConfig
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.PressingOrder
import com.example.data.PushNotificationRecord
import com.example.data.StockItem
import com.example.data.SubscriptionPlan
import com.example.data.local.OfflineOrderEntity
import com.example.ui.components.GPAConfirmationDialog
import com.example.ui.components.GPAEmptyState
import com.example.ui.components.GPAOfflineBanner
import com.example.ui.components.RoleBadgeChip
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.AppThemeMode
import com.example.ui.viewmodel.AuthUiState

private enum class NotificationFeedCategory(val labelFr: String) {
    ALL("Toutes"),
    ORDERS("Commandes"),
    PAYMENTS("Caisse"),
    STOCKS("Stocks"),
    APPROVALS("Validations")
}

private data class NotificationFeedEntry(
    val id: String,
    val title: String,
    val body: String,
    val timeLabel: String,
    val isToday: Boolean,
    val unread: Boolean,
    val category: NotificationFeedCategory,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val onClickAction: () -> Unit
)

/**
 * Full-screen Notifications & Firebase FCM Center faithful to Screen #14 of the
 * Gestion Pro Africa — Pressing Pro mockup (Today / Yesterday grouped timeline,
 * unread dots, FCM permission banner, Room offline queue, direct click-through).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsCenterScreen(
    company: CompanyConfig,
    activeSession: ActiveRoleSession,
    orders: List<PressingOrder>,
    transactions: List<AccountingEntry>,
    stocks: List<StockItem>,
    claims: List<ClaimRecord>,
    approvalRequests: List<ApprovalRequestRecord>,
    pushNotifications: List<PushNotificationRecord>,
    pendingOfflineOrders: List<OfflineOrderEntity>,
    isOnline: Boolean,
    isSyncingOffline: Boolean,
    notifPermissionGranted: Boolean,
    fcmTopics: List<String>,
    onRequestNotifPermission: () -> Unit,
    onSyncOfflineNow: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onNavigateRoute: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedCategory by remember { mutableStateOf(NotificationFeedCategory.ALL) }
    var readIds by remember { mutableStateOf(emptySet<String>()) }

    val feedEntries = remember(
        orders, transactions, stocks, claims, approvalRequests, pushNotifications, readIds, activeSession
    ) {
        val list = mutableListOf<NotificationFeedEntry>()

        // 1. Live FCM Push events
        pushNotifications.forEachIndexed { idx, pn ->
            val id = "fcm_${pn.id.ifBlank { idx.toString() }}"
            list.add(
                NotificationFeedEntry(
                    id = id,
                    title = pn.title,
                    body = pn.body,
                    timeLabel = "FCM · En direct",
                    isToday = true,
                    unread = id !in readIds,
                    category = NotificationFeedCategory.ORDERS,
                    icon = Icons.Rounded.NotificationsActive,
                    iconBg = SoftBlueContainer,
                    iconTint = RoyalBlue,
                    onClickAction = {
                        readIds = readIds + id
                        val matchId = orders.firstOrNull { it.orderNumber.equals(pn.orderNumber, ignoreCase = true) }?.id
                        if (!matchId.isNullOrBlank()) onOpenOrder(matchId)
                    }
                )
            )
        }

        // 2. Ready orders ("Commande prête")
        orders.filter { it.orderStatusEnum() == OrderStatus.READY }.take(5).forEach { ord ->
            val id = "ready_${ord.id}"
            list.add(
                NotificationFeedEntry(
                    id = id,
                    title = "Commande prête",
                    body = "${ord.orderNumber} (${ord.clientName}) est prête à être livrée ou retirée.",
                    timeLabel = "Aujourd'hui",
                    isToday = true,
                    unread = id !in readIds,
                    category = NotificationFeedCategory.ORDERS,
                    icon = Icons.Rounded.CheckCircle,
                    iconBg = EmeraldContainer,
                    iconTint = EmeraldSuccess,
                    onClickAction = {
                        readIds = readIds + id
                        onOpenOrder(ord.id)
                    }
                )
            )
        }

        // 3. Late orders alert
        orders.filter { it.isLate() }.take(5).forEach { ord ->
            val id = "late_${ord.id}"
            list.add(
                NotificationFeedEntry(
                    id = id,
                    title = "Commande en retard",
                    body = "${ord.orderNumber} (${ord.clientName}) · Statut : ${ord.orderStatusEnum().labelFr}",
                    timeLabel = "Urgent",
                    isToday = true,
                    unread = id !in readIds,
                    category = NotificationFeedCategory.ORDERS,
                    icon = Icons.Rounded.ReportProblem,
                    iconBg = CrimsonContainer,
                    iconTint = CrimsonDanger,
                    onClickAction = {
                        readIds = readIds + id
                        onOpenOrder(ord.id)
                    }
                )
            )
        }

        // 4. Recent incoming payments (only if authorized to view financial amounts)
        if (activeSession.canViewFinancialAmounts) {
            transactions.filter { it.entryType == "REVENUE" }.take(4).forEachIndexed { idx, tx ->
                val id = "pay_${tx.id.ifBlank { idx.toString() }}"
                list.add(
                    NotificationFeedEntry(
                        id = id,
                        title = "Paiement reçu",
                        body = "${formatMoney(tx.amount, company.currency)} via ${PaymentMethod.fromRaw(tx.paymentMethod).labelFr} · ${tx.label}",
                        timeLabel = "Aujourd'hui",
                        isToday = idx < 2,
                        unread = id !in readIds && idx == 0,
                        category = NotificationFeedCategory.PAYMENTS,
                        icon = Icons.Rounded.AccountBalanceWallet,
                        iconBg = AmberContainer,
                        iconTint = Color(0xFFB45309),
                        onClickAction = {
                            readIds = readIds + id
                            if (activeSession.canAccessRoute("pos_cash")) onNavigateRoute("pos_cash")
                        }
                    )
                )
            }
        }

        // 5. Low stocks & machine incidents
        stocks.filter { it.isLowStock() || it.machineStatus != "OPERATIONAL" }.take(5).forEach { st ->
            val id = "stock_${st.id}"
            list.add(
                NotificationFeedEntry(
                    id = id,
                    title = if (st.itemKind == "MACHINE") "Incident machine atelier" else "Stock faible",
                    body = if (st.itemKind == "MACHINE") "${st.name} : état ${st.machineStatus}" else "${st.name} : ${st.quantity} ${st.unit} restant(s) (seuil ${st.minThreshold}).",
                    timeLabel = "Alerte stock",
                    isToday = true,
                    unread = id !in readIds,
                    category = NotificationFeedCategory.STOCKS,
                    icon = Icons.Rounded.Inventory2,
                    iconBg = CrimsonContainer,
                    iconTint = CrimsonDanger,
                    onClickAction = {
                        readIds = readIds + id
                        if (activeSession.canAccessRoute("stocks")) onNavigateRoute("stocks")
                    }
                )
            )
        }

        // 6. Pending RBAC+ABAC approval requests
        approvalRequests.filter { it.statusEnum() == ApprovalStatus.PENDING }.take(5).forEach { req ->
            val id = "approval_${req.id}"
            list.add(
                NotificationFeedEntry(
                    id = id,
                    title = "Validation administrative requise",
                    body = "${req.operationEnum().labelFr} (${req.resourceRef}) · ${formatMoney(req.amountFcfa, company.currency)} par ${req.requestedBy}",
                    timeLabel = "En attente",
                    isToday = true,
                    unread = id !in readIds,
                    category = NotificationFeedCategory.APPROVALS,
                    icon = Icons.Rounded.Gavel,
                    iconBg = Color(0xFFEDE9FE),
                    iconTint = Color(0xFF7C3AED),
                    onClickAction = {
                        readIds = readIds + id
                        if (activeSession.canAccessRoute("roles_rbac")) onNavigateRoute("roles_rbac")
                    }
                )
            )
        }

        // 7. Recent standard orders ("Hier / Historique récent")
        orders.filter { !it.isLate() && it.orderStatusEnum() != OrderStatus.READY }.take(4).forEach { ord ->
            val id = "ord_${ord.id}"
            list.add(
                NotificationFeedEntry(
                    id = id,
                    title = "Nouvelle commande enregistrée",
                    body = "${ord.orderNumber} · ${ord.clientName} (${ord.orderStatusEnum().labelFr})",
                    timeLabel = "Récent",
                    isToday = false,
                    unread = false,
                    category = NotificationFeedCategory.ORDERS,
                    icon = Icons.Rounded.ReceiptLong,
                    iconBg = SoftBlueContainer,
                    iconTint = RoyalBlue,
                    onClickAction = {
                        readIds = readIds + id
                        onOpenOrder(ord.id)
                    }
                )
            )
        }

        list
    }

    val filtered = remember(feedEntries, selectedCategory) {
        if (selectedCategory == NotificationFeedCategory.ALL) feedEntries
        else feedEntries.filter { it.category == selectedCategory }
    }
    val todayEntries = remember(filtered) { filtered.filter { it.isToday } }
    val earlierEntries = remember(filtered) { filtered.filter { !it.isToday } }
    val unreadCount = remember(feedEntries) { feedEntries.count { it.unread } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notifications", fontWeight = FontWeight.Bold)
                        Text(
                            if (unreadCount > 0) "$unreadCount non lue(s) · Firebase FCM" else "Toutes les alertes sont à jour",
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
                actions = {
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = { readIds = readIds + feedEntries.map { it.id } },
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("notifications_mark_all_read")
                        ) {
                            Icon(Icons.Rounded.DoneAll, contentDescription = "Tout marquer comme lu", tint = AfricaGold)
                        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isOnline || pendingOfflineOrders.isNotEmpty()) {
                item {
                    GPAOfflineBanner(
                        isOffline = !isOnline,
                        pendingCount = pendingOfflineOrders.size,
                        isSyncing = isSyncingOffline,
                        onSyncNow = onSyncOfflineNow
                    )
                }
            }

            // FCM Status Banner
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (notifPermissionGranted) EmeraldContainer.copy(alpha = 0.65f) else AmberContainer.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, if (notifPermissionGranted) EmeraldSuccess.copy(alpha = 0.35f) else AmberWarning.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = if (notifPermissionGranted) EmeraldSuccess else Color(0xFFB45309)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (notifPermissionGranted) "Alertes Push Firebase FCM actives" else "Autoriser les notifications Push FCM",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MidnightNavy
                                )
                                Text(
                                    text = "Nouvelles commandes, vêtements prêts, stocks critiques et validations (${fcmTopics.size} canal/canaux).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MidnightNavy.copy(alpha = 0.8f)
                                )
                            }
                        }
                        if (!notifPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = onRequestNotifPermission,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("notifications_request_permission_btn")
                            ) {
                                Text("Activer les notifications Android", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotificationFeedCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.labelFr) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    GPAEmptyState(
                        title = "Aucune notification dans cette catégorie",
                        description = "Les alertes commandes, paiements, stocks faibles et validations administratives apparaîtront ici en temps réel.",
                        icon = Icons.Rounded.Notifications
                    )
                }
            } else {
                if (todayEntries.isNotEmpty()) {
                    item {
                        Text(
                            text = "Aujourd'hui",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(todayEntries, key = { it.id }) { entry ->
                        NotificationTimelineCard(entry = entry)
                    }
                }
                if (earlierEntries.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Plus tôt",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(earlierEntries, key = { it.id }) { entry ->
                        NotificationTimelineCard(entry = entry)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun NotificationTimelineCard(entry: NotificationFeedEntry) {
    Card(
        onClick = entry.onClickAction,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (entry.unread) RoyalBlue.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(entry.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = entry.title,
                    tint = entry.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = entry.timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = entry.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.unread) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CrimsonDanger)
                )
            }
        }
    }
}

/**
 * Mockup-faithful "Mon profil" hub screen (Screen #13 & Module U) with centered circular avatar,
 * role/scope identity, 3-way Theme Mode selector (System / Light / Dark), Firebase sync status,
 * structured navigation rows, About dialog, and confirmed Sign-Out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileHubScreen(
    company: CompanyConfig,
    userProfile: AuthUserProfile,
    authUiState: AuthUiState,
    activeSession: ActiveRoleSession,
    themeMode: AppThemeMode,
    isOnline: Boolean,
    pendingOfflineCount: Int,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onRefreshSession: () -> Unit,
    onSyncOfflineNow: () -> Unit,
    onOpenCompanyWizard: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember(userProfile.displayName) { mutableStateOf(userProfile.displayLabel) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon profil", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Centered Mockup-Faithful Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(66.dp)
                                .background(Brush.horizontalGradient(listOf(MidnightNavy, DeepNavySurface, RoyalBlue)))
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 0.dp)
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(RoyalBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.initials,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = userProfile.displayLabel,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            IconButton(
                                onClick = {
                                    editedName = userProfile.displayLabel
                                    showEditNameDialog = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("profile_edit_name_btn")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Modifier mon nom", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(
                            text = userProfile.effectiveContactLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        RoleBadgeChip(role = activeSession.role)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${company.companyName} · ${activeSession.scopeLabel()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // Appearance: 3-way Theme Mode Selector (Système / Clair / Sombre)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Apparence de l'application",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(
                                Triple(AppThemeMode.SYSTEM, "Système", Icons.Rounded.SettingsBrightness),
                                Triple(AppThemeMode.LIGHT, "Clair", Icons.Rounded.LightMode),
                                Triple(AppThemeMode.DARK, "Sombre", Icons.Rounded.DarkMode)
                            )
                            options.forEach { (mode, label, icon) ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { onSelectThemeMode(mode) },
                                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("theme_mode_${mode.name}")
                                )
                            }
                        }
                    }
                }
            }

            // Structured Menu Card (Screen #13 items)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        if (activeSession.canEditAgencySettingsAndTariffs) {
                            ProfileMenuRow(
                                icon = Icons.Rounded.Business,
                                title = "Mon entreprise",
                                subtitle = "${company.companyName} · ${company.agencies.size.coerceAtLeast(1)} agence(s)",
                                onClick = onOpenCompanyWizard
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        if (activeSession.canAccessRoute("roles_rbac")) {
                            ProfileMenuRow(
                                icon = Icons.Rounded.Security,
                                title = "Utilisateurs & rôles",
                                subtitle = "RBAC + ABAC · Plafonds & validations",
                                onClick = { onNavigateRoute("roles_rbac") }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        if (activeSession.canAccessRoute("settings")) {
                            ProfileMenuRow(
                                icon = Icons.Rounded.Settings,
                                title = "Paramètres & Imprimante",
                                subtitle = "Catalogue, Bluetooth 58/80mm, Clôture Z",
                                onClick = { onNavigateRoute("settings") }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        if (activeSession.canAccessRoute("saas_admin")) {
                            ProfileMenuRow(
                                icon = Icons.Rounded.CreditCard,
                                title = "Abonnement",
                                subtitle = "Forfait ${SubscriptionPlan.fromRaw(company.subscriptionPlan).labelFr} · Gestion SaaS",
                                onClick = { onNavigateRoute("saas_admin") }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        ProfileMenuRow(
                            icon = if (isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                            title = "Sauvegarde (Firebase)",
                            subtitle = if (isOnline) {
                                if (pendingOfflineCount > 0) "En ligne · $pendingOfflineCount en attente de sync" else "Synchronisation cloud temps réel active"
                            } else {
                                "Hors ligne · Cache Room actif ($pendingOfflineCount)"
                            },
                            onClick = {
                                onRefreshSession()
                                if (pendingOfflineCount > 0 && isOnline) onSyncOfflineNow()
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProfileMenuRow(
                            icon = Icons.Rounded.Notifications,
                            title = "Notifications & Alertes FCM",
                            subtitle = "Commandes prêtes, stocks et validations",
                            onClick = { onNavigateRoute("notifications") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProfileMenuRow(
                            icon = Icons.Rounded.HelpOutline,
                            title = "Aide & Support",
                            subtitle = "FAQ, WhatsApp support et guides métier",
                            onClick = { onNavigateRoute("help") }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ProfileMenuRow(
                            icon = Icons.Rounded.Info,
                            title = "À propos",
                            subtitle = "GESTION PRO AFRICA · Pressing Pro",
                            onClick = { showAboutDialog = true }
                        )
                    }
                }
            }

            if (authUiState.infoMessage != null) {
                item {
                    Surface(shape = RoundedCornerShape(12.dp), color = EmeraldContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = authUiState.infoMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Confirmed Sign Out
            item {
                OutlinedButton(
                    onClick = { showSignOutConfirm = true },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CrimsonDanger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDanger),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("profile_sign_out_btn")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Se déconnecter", fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Modifier mon profil", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it.take(80) },
                    label = { Text("Nom complet") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedName.isNotBlank()) onUpdateDisplayName(editedName.trim())
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = RoyalBlue) },
            title = { Text("GESTION PRO AFRICA", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Module : Pressing Pro Mobile (Android natif Kotlin + Jetpack Compose).", style = MaterialTheme.typography.bodyMedium)
                    Text("Slogan : La gestion professionnelle, simplement.", style = MaterialTheme.typography.bodySmall, color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                    Text("Sécurité : Authentification Firebase Auth, App Check, isolation multi-tenant Firestore et matrice RBAC+ABAC DENY BY DEFAULT.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showSignOutConfirm) {
        GPAConfirmationDialog(
            title = "Confirmer la déconnexion",
            message = "Voulez-vous fermer votre session sur cet appareil ? Vos données synchronisées restent sécurisées dans votre espace cloud.",
            confirmLabel = "Se déconnecter",
            isDestructive = true,
            onConfirm = {
                showSignOutConfirm = false
                onSignOut()
            },
            onDismiss = { showSignOutConfirm = false }
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SoftBlueContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
