package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.PressingNotificationManager
import com.example.core.formatMoney
import com.example.data.AccessPermission
import com.example.data.AuthUserProfile
import com.example.data.CompanyConfig
import com.example.data.OrderPriority
import com.example.data.RolePermissionMatrix
import com.example.data.SubscriptionPlan
import com.example.data.TenantDataScoper
import com.example.data.WorkspaceRole
import com.example.ui.components.GestionProBrandHeader
import com.example.ui.components.RoleBadgeChip
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SidebarNavyBottom
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.PressingViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainWorkspace(
    company: CompanyConfig,
    userProfile: AuthUserProfile,
    authUiState: AuthUiState,
    viewModel: PressingViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onReopenSetupWizard: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onRefreshSession: () -> Unit,
    onClearAuthMessages: () -> Unit,
    onSignOut: () -> Unit
) {
    val servicesState by viewModel.servicesState.collectAsStateWithLifecycle()
    val clientsState by viewModel.clientsState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val stocksState by viewModel.stocksState.collectAsStateWithLifecycle()
    val employeesState by viewModel.employeesState.collectAsStateWithLifecycle()
    val claimsState by viewModel.claimsState.collectAsStateWithLifecycle()
    val activeRoleSession by viewModel.activeRoleSession.collectAsStateWithLifecycle()
    val rolePermissionsMap by viewModel.rolePermissionsMap.collectAsStateWithLifecycle()
    val auditLog by viewModel.auditLog.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val garmentVisionState by viewModel.garmentVisionState.collectAsStateWithLifecycle()
    val aiAdvisorState by viewModel.aiAdvisorState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val pendingOfflineOrders by viewModel.pendingOfflineOrders.collectAsStateWithLifecycle()
    val isSyncingOffline by viewModel.isSyncingOffline.collectAsStateWithLifecycle()
    val pushNotifications by viewModel.pushNotificationsState.collectAsStateWithLifecycle()
    val fcmTopics by viewModel.fcmTopics.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var notifPermissionGranted by remember {
        mutableStateOf(PressingNotificationManager.hasNotificationPermission(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermissionGranted = granted
    }

    val rawServices = (servicesState as? UiState.Success)?.data.orEmpty()
    val rawClients = (clientsState as? UiState.Success)?.data.orEmpty()
    val rawOrders = (ordersState as? UiState.Success)?.data.orEmpty()
    val rawTransactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val rawStocks = (stocksState as? UiState.Success)?.data.orEmpty()
    val rawEmployees = (employeesState as? UiState.Success)?.data.orEmpty()
    val rawClaims = (claimsState as? UiState.Success)?.data.orEmpty()

    // Strict Role + Entity/Agency Data Isolation via TenantDataScoper
    val orders = remember(rawOrders, activeRoleSession) {
        TenantDataScoper.scopeOrders(rawOrders, activeRoleSession)
    }
    val transactions = remember(rawTransactions, activeRoleSession) {
        TenantDataScoper.scopeTransactions(rawTransactions, activeRoleSession)
    }
    val clients = remember(rawClients, orders, rawOrders, activeRoleSession) {
        TenantDataScoper.scopeClients(rawClients, orders, activeRoleSession, rawOrders)
    }
    val employees = remember(rawEmployees, activeRoleSession) {
        TenantDataScoper.scopeEmployees(rawEmployees, activeRoleSession)
    }
    val stocks = remember(rawStocks, activeRoleSession) {
        TenantDataScoper.scopeStocks(rawStocks, activeRoleSession)
    }
    val claims = remember(rawClaims, orders, activeRoleSession) {
        TenantDataScoper.scopeClaims(rawClaims, orders, activeRoleSession)
    }
    val services = rawServices

    var currentRoute by remember { mutableStateOf("dashboard") }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var autoOpenThermalPrintForOrderId by remember { mutableStateOf<String?>(null) }
    var showGlobalSearch by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }

    // Redirect back to isolated dashboard if a role switch revokes access to currentRoute
    LaunchedEffect(activeRoleSession) {
        if (!activeRoleSession.canAccessRoute(currentRoute)) {
            currentRoute = "dashboard"
        }
    }

    val lateOrders = remember(orders) { orders.filter { it.isLate() } }
    val lowStocks = remember(stocks) { stocks.filter { it.isLowStock() || it.machineStatus != "OPERATIONAL" } }
    val openClaims = remember(claims) { claims.filter { it.status != "RESOLVED" } }
    val alertCount = lateOrders.size + lowStocks.size + openClaims.size + pendingOfflineOrders.size

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // Strict Route Permission Enforcement with AccessDeniedScreen + Firestore Audit Logging
    val fullScreenRoutes = setOf(
        "new_order", "order_detail", "workshop_tracker", "ai_assistant", "catalog", "stocks",
        "deliveries", "staff", "roles_rbac", "claims", "client_portal",
        "pos_cash", "invoices", "reports", "saas_admin", "quotes",
        "marketing", "help", "settings"
    )
    if (currentRoute in fullScreenRoutes && !activeRoleSession.canAccessRoute(currentRoute)) {
        AccessDeniedScreen(
            session = activeRoleSession,
            attemptedRoute = currentRoute,
            requiredPermission = RolePermissionMatrix.requiredPermissionForRoute(currentRoute),
            onLogAccessDenied = { route, perm -> viewModel.logUnauthorizedNavigation(route, perm) },
            onBackToDashboard = { currentRoute = "dashboard" }
        )
        return
    }

    when (currentRoute) {
        "new_order" -> {
            NewOrderScreen(
                company = company,
                existingOrderCount = rawOrders.size,
                clients = clients,
                services = services,
                employees = employees,
                isBusy = isBusy,
                garmentVisionState = garmentVisionState,
                activeSession = activeRoleSession,
                onAnalyzeGarmentPhoto = { bmp -> viewModel.analyzeGarmentPhoto(bmp) },
                onResetVision = { viewModel.resetGarmentVision() },
                onQuickAddClient = { cli, cb -> viewModel.saveClient(cli, cb) },
                onSubmitOrder = { orderNum, agency, cliId, cliName, cliPhone, cliAddr, prio, items, disc, expFee, delFee, tax, paid, method, delReq, delZone, emp, sig, obs, due ->
                    val targetAgency = activeRoleSession.isolatedAgency ?: agency
                    viewModel.createOrder(
                        orderNum, targetAgency, cliId, cliName, cliPhone, cliAddr, prio, items,
                        disc, expFee, delFee, tax, paid, method, delReq, delZone, emp, sig, obs, due
                    ) { createdId ->
                        selectedOrderId = createdId
                        autoOpenThermalPrintForOrderId = createdId
                        currentRoute = "order_detail"
                    }
                },
                onBack = { currentRoute = "orders" }
            )
            return
        }

        "order_detail" -> {
            val order = orders.firstOrNull { it.id == selectedOrderId }
            if (order != null) {
                OrderDetailScreen(
                    company = company,
                    order = order,
                    employees = employees,
                    isBusy = isBusy,
                    activeSession = activeRoleSession,
                    autoOpenPrintPreview = autoOpenThermalPrintForOrderId == order.id,
                    onAutoPrintConsumed = { autoOpenThermalPrintForOrderId = null },
                    onUpdateStatus = { o, st, qc -> viewModel.updateOrderStatus(o, st, qualityChecked = qc) },
                    onRecordPayment = { o, amt, m -> viewModel.recordOrderPayment(o, amt, m) },
                    onAssignDelivery = { o, drv, zone, done -> viewModel.assignDelivery(o, drv, zone, done) },
                    onCreateClaim = { claim -> viewModel.createClaim(claim) },
                    onBack = { currentRoute = if (activeRoleSession.canAccessRoute("orders")) "orders" else "dashboard" }
                )
                return
            } else {
                currentRoute = if (activeRoleSession.canAccessRoute("orders")) "orders" else "dashboard"
            }
        }

        "workshop_tracker" -> {
            WorkshopTrackerScreen(
                orders = orders,
                activeSession = activeRoleSession,
                isBusy = isBusy,
                onUpdateGarmentStatus = { ord, idx, newStage ->
                    viewModel.updateGarmentWorkshopStatus(ord, idx, newStage)
                },
                onOpenOrder = { id -> selectedOrderId = id; currentRoute = "order_detail" },
                onBack = { currentRoute = "dashboard" }
            )
            return
        }

        "roles_rbac" -> {
            RolesPermissionsScreen(
                company = company,
                employees = rawEmployees,
                activeSession = activeRoleSession,
                rolePermissionsMap = rolePermissionsMap,
                auditLog = auditLog,
                onTogglePermission = { role, perm -> viewModel.toggleRolePermission(role, perm) },
                onResetRolePermissions = { role -> viewModel.resetRolePermissions(role) },
                onBack = { currentRoute = "dashboard" }
            )
            return
        }

        "ai_assistant" -> {
            AiAssistantScreen(
                aiState = aiAdvisorState,
                activeSession = activeRoleSession,
                onAskQuestion = { q -> viewModel.askAiAdvisor(q) },
                onBack = { currentRoute = "dashboard" }
            )
            return
        }

        "catalog" -> {
            CatalogScreen(
                company = company,
                services = services,
                activeSession = activeRoleSession,
                onSaveService = { viewModel.saveService(it) },
                onDeleteService = { viewModel.deleteService(it) },
                onSeedDefaults = { viewModel.seedStarterServices() },
                onBack = { currentRoute = "more" }
            )
            return
        }

        "stocks" -> {
            StocksScreen(
                company = company,
                stocks = stocks,
                activeSession = activeRoleSession,
                onSaveStock = { viewModel.saveStockItem(it) },
                onDeleteStock = { viewModel.deleteStockItem(it) },
                onBack = { currentRoute = "more" }
            )
            return
        }

        "deliveries" -> {
            DeliveriesScreen(
                company = company,
                orders = orders,
                employees = employees,
                activeSession = activeRoleSession,
                onAssignDelivery = { o, drv, z, done -> viewModel.assignDelivery(o, drv, z, done) },
                onOpenOrder = { id -> selectedOrderId = id; currentRoute = "order_detail" },
                onBack = { currentRoute = "dashboard" }
            )
            return
        }

        "staff" -> {
            StaffScreen(
                company = company,
                employees = employees,
                activeSession = activeRoleSession,
                onSaveEmployee = { viewModel.saveEmployee(it) },
                onDeleteEmployee = { viewModel.deleteEmployee(it) },
                onBack = { currentRoute = "more" }
            )
            return
        }

        "claims" -> {
            ClaimsScreen(
                claims = claims,
                activeSession = activeRoleSession,
                onCreateClaim = { viewModel.createClaim(it) },
                onUpdateClaim = { id, st, sev, note -> viewModel.updateClaimStatus(id, st, sev, note) },
                onBack = { currentRoute = "more" }
            )
            return
        }

        "client_portal" -> {
            ClientPortalScreen(
                company = company,
                orders = orders,
                services = services,
                onCreateClaim = { viewModel.createClaim(it) },
                onBack = { currentRoute = "dashboard" }
            )
            return
        }

        "pos_cash" -> {
            PosCashRegisterScreen(
                company = company,
                existingOrderCount = rawOrders.size,
                clients = clients,
                services = services,
                isBusy = isBusy,
                onCreatePosOrder = { orderNum, cliId, cliName, cliPhone, items, paidAmt, method ->
                    val due = System.currentTimeMillis() + company.standardDelayHours * 3_600_000L
                    viewModel.createOrder(
                        orderNumber = orderNum,
                        agencyName = activeRoleSession.isolatedAgency ?: company.agencies.firstOrNull() ?: "Agence Principale",
                        clientId = cliId,
                        clientName = cliName,
                        clientPhone = cliPhone,
                        clientAddress = company.commune,
                        priority = OrderPriority.STANDARD,
                        items = items,
                        discountAmount = 0L,
                        expressFee = 0L,
                        deliveryFee = 0L,
                        taxAmount = 0L,
                        paidAmount = paidAmt,
                        paymentMethod = method,
                        deliveryRequested = false,
                        deliveryZone = company.commune,
                        assignedEmployee = activeRoleSession.operatorName,
                        clientSignatureName = cliName,
                        observations = "Vente comptoir Caisse POS",
                        dueDateMillis = due
                    ) { createdId ->
                        selectedOrderId = createdId
                        autoOpenThermalPrintForOrderId = createdId
                        currentRoute = "order_detail"
                    }
                },
                onSeedDefaultServices = { viewModel.seedStarterServices() },
                onBack = { currentRoute = "dashboard" }
            )
            return
        }

        "invoices" -> {
            BillingInvoicesScreen(
                company = company,
                orders = orders,
                activeSession = activeRoleSession,
                onOpenOrder = { id -> selectedOrderId = id; currentRoute = "order_detail" },
                onBack = { currentRoute = "more" }
            )
            return
        }

        "reports" -> {
            ReportsAnalyticsScreen(
                company = company,
                orders = orders,
                clients = clients,
                transactions = transactions,
                stocks = stocks,
                activeSession = activeRoleSession,
                onBack = { currentRoute = "more" }
            )
            return
        }

        "saas_admin" -> {
            SaasAdminScreen(
                company = company,
                orders = orders,
                clients = clients,
                transactions = transactions,
                onUpdateCompany = { viewModel.saveCompanyConfig(it) },
                onBack = { currentRoute = "dashboard" }
            )
            return
        }

        "quotes" -> {
            QuotesScreen(
                company = company,
                clients = clients,
                services = services,
                activeSession = activeRoleSession,
                onBack = { currentRoute = "more" }
            )
            return
        }

        "marketing" -> {
            MarketingScreen(
                company = company,
                clients = clients,
                orders = orders,
                activeSession = activeRoleSession,
                onBack = { currentRoute = "more" }
            )
            return
        }

        "help" -> {
            HelpCenterScreen(
                company = company,
                onBack = { currentRoute = "dashboard" }
            )
            return
        }

        "settings" -> {
            AgencySettingsScreen(
                company = company,
                services = services,
                transactions = transactions,
                userProfile = userProfile,
                authUiState = authUiState,
                isDarkTheme = isDarkTheme,
                isBusy = isBusy,
                activeSession = activeRoleSession,
                onSaveCompanyConfig = { updated -> viewModel.saveCompanyConfig(updated) },
                onSaveService = { srv -> viewModel.saveService(srv) },
                onSeedDefaultCatalog = { viewModel.seedStarterServices() },
                onRecordDailyCashClose = { summary, justification, onDone ->
                    viewModel.recordDailyCashCloseZ(company, summary, justification, onDone)
                },
                onToggleDarkTheme = onToggleDarkTheme,
                onReopenSetupWizard = onReopenSetupWizard,
                onOpenRbacMatrix = { currentRoute = "roles_rbac" },
                onUpdateDisplayName = onUpdateDisplayName,
                onRefreshSession = onRefreshSession,
                onClearAuthMessages = onClearAuthMessages,
                onSignOut = onSignOut,
                onBack = { currentRoute = "dashboard" }
            )
            return
        }
    }

    data class NavTab(val route: String, val label: String, val icon: ImageVector)

    // Dynamic Role-Filtered Bottom Tabs
    val tabs = remember(activeRoleSession) {
        buildList {
            add(NavTab("dashboard", "Accueil", Icons.Rounded.Dashboard))
            if (activeRoleSession.canAccessRoute("orders")) add(NavTab("orders", "Commandes", Icons.Rounded.Checkroom))
            if (activeRoleSession.role == WorkspaceRole.OPERATEUR || activeRoleSession.role == WorkspaceRole.CONTROLE_QUALITE) {
                add(NavTab("workshop_tracker", "Atelier", Icons.Rounded.LocalLaundryService))
            }
            if (activeRoleSession.canAccessRoute("clients")) add(NavTab("clients", "Clients", Icons.Rounded.People))
            if (activeRoleSession.canAccessRoute("finance")) add(NavTab("finance", "Finances", Icons.Rounded.AccountBalance))
            else if (activeRoleSession.canAccessRoute("pos_cash")) add(NavTab("pos_cash", "Caisse", Icons.Rounded.PointOfSale))
            else if (activeRoleSession.canAccessRoute("deliveries")) add(NavTab("deliveries", "Livraisons", Icons.Rounded.LocalShipping))
            add(NavTab("more", "Modules", Icons.Rounded.GridView))
        }.take(5)
    }

    // Dynamic Role-Filtered Sidebar Drawer Items (faithful to mockup order)
    val drawerItems = remember(activeRoleSession) {
        listOf(
            NavTab("dashboard", "Tableau de bord", Icons.Rounded.Dashboard),
            NavTab("orders", "Commandes & Tickets", Icons.Rounded.Checkroom),
            NavTab("workshop_tracker", "Atelier & Suivi Vêtements", Icons.Rounded.LocalLaundryService),
            NavTab("pos_cash", "Caisse Enregistreuse", Icons.Rounded.PointOfSale),
            NavTab("invoices", "Facturation & Reçus", Icons.Rounded.ReceiptLong),
            NavTab("clients", "Clients & Fidélité", Icons.Rounded.People),
            NavTab("catalog", "Articles & Tarifs", Icons.Rounded.Checkroom),
            NavTab("stocks", "Stocks & Produits", Icons.Rounded.Inventory2),
            NavTab("deliveries", "Livraisons & GPS", Icons.Rounded.LocalShipping),
            NavTab("finance", "Comptabilité SYSCOHADA", Icons.Rounded.AccountBalance),
            NavTab("reports", "Rapports & Analyses", Icons.Rounded.BarChart),
            NavTab("quotes", "Devis & Simulateur", Icons.Rounded.RequestQuote),
            NavTab("marketing", "Relances & Marketing", Icons.Rounded.Campaign),
            NavTab("staff", "Personnel & Équipe", Icons.Rounded.Badge),
            NavTab("roles_rbac", "Rôles, Droits & Isolation", Icons.Rounded.Security),
            NavTab("claims", "Réclamations & Qualité", Icons.Rounded.ReportProblem),
            NavTab("ai_assistant", "Assistant IA Gemini", Icons.Rounded.AutoAwesome),
            NavTab("client_portal", "Portail Client", Icons.Rounded.Public),
            NavTab("saas_admin", "Super-Admin SaaS", Icons.Rounded.AdminPanelSettings),
            NavTab("help", "Aide & Support", Icons.Rounded.HelpOutline),
            NavTab("settings", "Paramètres", Icons.Rounded.Settings)
        ).filter { activeRoleSession.canAccessRoute(it.route) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val isWideScreen = screenWidth >= 700.dp
        val isCompactPhone = screenWidth < 380.dp
        val drawerSheetWidth = (screenWidth * 0.84f).coerceIn(256.dp, 312.dp)

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MidnightNavy,
                    drawerContentColor = Color.White,
                    modifier = Modifier.width(drawerSheetWidth)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(Brush.verticalGradient(listOf(MidnightNavy, SidebarNavyBottom)))
                            .systemBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp)
                    ) {
                        GestionProBrandHeader(
                            compact = true,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        // Read-only locked operator & agency card (no role switching from sidebar)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(RoyalBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userProfile.initials,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeRoleSession.operatorName,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        softWrap = true,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${activeRoleSession.role.shortTitleFr} · ${activeRoleSession.isolatedAgency ?: "Multi-agences"}",
                                        color = AfricaGold,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 2,
                                        softWrap = true,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Session rôle verrouillée",
                                    tint = AfricaGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.14f), modifier = Modifier.padding(vertical = 8.dp))

                        drawerItems.forEach { item ->
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = item.label,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        softWrap = true
                                    )
                                },
                                selected = currentRoute == item.route,
                                onClick = {
                                    currentRoute = item.route
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = RoyalBlue,
                                    unselectedContainerColor = Color.Transparent,
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color(0xFFDCE6FF),
                                    selectedIconColor = AfricaGold,
                                    unselectedIconColor = Color(0xFF9AB4F0)
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                                    .heightIn(min = 48.dp)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        if (activeRoleSession.has(AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA)) {
                            Card(
                                onClick = {
                                    currentRoute = "saas_admin"
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Forfait ${SubscriptionPlan.fromRaw(company.subscriptionPlan).labelFr} · ${company.subscriptionStatus}",
                                        color = AfricaGold,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        softWrap = true
                                    )
                                    Text(
                                        text = "Renouveler ou gérer l'abonnement →",
                                        color = Color(0xFFDCE6FF),
                                        style = MaterialTheme.typography.labelSmall,
                                        softWrap = true
                                    )
                                }
                            }
                        }

                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = "Déconnexion",
                                    fontWeight = FontWeight.SemiBold,
                                    softWrap = true
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onSignOut()
                            },
                            icon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Déconnexion") },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = Color(0xFFFCA5A5),
                                unselectedIconColor = Color(0xFFFCA5A5)
                            ),
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .heightIn(min = 48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Surface(
                                onClick = { showGlobalSearch = true },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("global_search_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Search,
                                        contentDescription = "Recherche globale",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isCompactPhone) "Rechercher" else "Rechercher...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag("open_drawer_button")
                            ) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Ouvrir menu")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showNotifications = true },
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag("notifications_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (alertCount > 0) {
                                            Badge(containerColor = CrimsonDanger, contentColor = Color.White) {
                                                Text(if (alertCount > 9) "9+" else "$alertCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Rounded.Notifications, contentDescription = "Notifications & Alertes")
                                }
                            }
                            // Read-only Top-Right Role Badge Pill (adapts to compact phones so search bar never clips)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SoftBlueContainer,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .heightIn(min = 38.dp)
                                    .testTag("topbar_role_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 6.dp,
                                        end = if (isCompactPhone) 8.dp else 10.dp,
                                        top = 5.dp,
                                        bottom = 5.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(RoyalBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = userProfile.initials,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!isCompactPhone) {
                                        Text(
                                            text = activeRoleSession.role.shortTitleFr,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MidnightNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 96.dp)
                                        )
                                    }
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = "Rôle verrouillé (${activeRoleSession.role.shortTitleFr})",
                                        tint = RoyalBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                floatingActionButton = {
                    if ((currentRoute == "dashboard" || currentRoute == "orders") &&
                        activeRoleSession.has(AccessPermission.CREATE_ORDER)
                    ) {
                        FloatingActionButton(
                            onClick = { currentRoute = "new_order" },
                            containerColor = RoyalBlue,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("fab_new_order")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Nouvelle commande")
                        }
                    }
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                            tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { currentRoute = tab.route },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = if (isCompactPhone) 10.sp else 11.sp
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                tabs.forEach { tab ->
                                    NavigationRailItem(
                                        selected = currentRoute == tab.route,
                                        onClick = { currentRoute = tab.route },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    )
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Room Offline Cache & Auto-Sync Banner (shown whenever offline or pending orders await sync)
                        if (!isOnline || pendingOfflineOrders.isNotEmpty() || isSyncingOffline) {
                            Surface(
                                color = if (!isOnline) AmberContainer else SoftBlueContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("offline_sync_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isSyncingOffline -> Icons.Rounded.CloudSync
                                            !isOnline -> Icons.Rounded.CloudOff
                                            else -> Icons.Rounded.CloudDone
                                        },
                                        contentDescription = null,
                                        tint = if (!isOnline) Color(0xFFB45309) else RoyalBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when {
                                                isSyncingOffline -> "Synchronisation Room → Cloud en cours..."
                                                !isOnline -> "Mode hors-ligne actif (Cache local Room)"
                                                else -> "${pendingOfflineOrders.size} commande(s) hors-ligne prête(s) à synchroniser"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MidnightNavy
                                        )
                                        Text(
                                            text = if (!isOnline) {
                                                "${pendingOfflineOrders.size} commande(s) en attente · Enregistrement continu activé"
                                            } else {
                                                "Synchronisation automatique dès connexion stable."
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MidnightNavy.copy(alpha = 0.8f)
                                        )
                                    }
                                    if (isOnline && pendingOfflineOrders.isNotEmpty() && !isSyncingOffline) {
                                        TextButton(
                                            onClick = { viewModel.syncPendingOfflineOrders() },
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("sync_offline_now_btn")
                                        ) {
                                            Text("Synchroniser", fontWeight = FontWeight.Bold, color = RoyalBlue)
                                        }
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (!activeRoleSession.canAccessRoute(currentRoute)) {
                                AccessDeniedScreen(
                                    session = activeRoleSession,
                                    attemptedRoute = currentRoute,
                                    requiredPermission = RolePermissionMatrix.requiredPermissionForRoute(currentRoute),
                                    onLogAccessDenied = { route, perm -> viewModel.logUnauthorizedNavigation(route, perm) },
                                    onBackToDashboard = { currentRoute = "dashboard" }
                                )
                            } else {
                            when (currentRoute) {
                                "dashboard" -> DashboardScreen(
                                    company = company,
                                    roleSession = activeRoleSession,
                                    orders = orders,
                                    allTenantOrders = rawOrders,
                                    clients = clients,
                                    transactions = transactions,
                                    stocks = stocks,
                                    employees = employees,
                                    claims = claims,
                                    auditLog = auditLog,
                                    onSelectAgencyFilter = { agency -> viewModel.selectIsolatedAgency(agency) },
                                    onNewOrder = { currentRoute = "new_order" },
                                    onOpenOrder = { id -> selectedOrderId = id; currentRoute = "order_detail" },
                                    onQuickAdvanceStatus = { o, next ->
                                        viewModel.updateOrderStatus(o, next, qualityChecked = next == com.example.data.OrderStatus.READY || o.qualityChecked)
                                    },
                                    onReturnToRework = { o, stage, reason ->
                                        viewModel.returnOrderToRework(o, stage, reason)
                                    },
                                    onRecordPayment = { o, amt, method ->
                                        viewModel.recordOrderPayment(o, amt, method)
                                    },
                                    onConfirmDeliveryWithProof = { o, recipient, note ->
                                        viewModel.confirmDeliveryWithProof(o, recipient, note)
                                    },
                                    onNavigateModule = { route -> currentRoute = route }
                                )
                                "orders" -> OrdersScreen(
                                    company = company,
                                    orders = orders,
                                    activeSession = activeRoleSession,
                                    onNewOrder = {
                                        if (activeRoleSession.has(AccessPermission.CREATE_ORDER)) currentRoute = "new_order"
                                    },
                                    onOpenOrder = { id -> selectedOrderId = id; currentRoute = "order_detail" },
                                    onQuickAdvanceStatus = { o, next -> viewModel.updateOrderStatus(o, next) }
                                )
                                "clients" -> ClientsScreen(
                                    company = company,
                                    clients = clients,
                                    orders = orders,
                                    activeSession = activeRoleSession,
                                    onOpenOrder = { id -> selectedOrderId = id; currentRoute = "order_detail" },
                                    onSaveClient = {
                                        if (activeRoleSession.has(AccessPermission.MANAGE_CLIENTS)) viewModel.saveClient(it)
                                    },
                                    onDeleteClient = {
                                        if (activeRoleSession.has(AccessPermission.CANCEL_DELETE_ORDER)) viewModel.deleteClient(it)
                                    }
                                )
                                "finance" -> FinanceScreen(
                                    company = company,
                                    orders = orders,
                                    transactions = transactions,
                                    activeSession = activeRoleSession,
                                    onAddEntry = { entry -> viewModel.addAccountingEntry(entry) },
                                    onOpenOrder = { id -> selectedOrderId = id; currentRoute = "order_detail" }
                                )
                                "more" -> MoreHubScreen(
                                    activeSession = activeRoleSession,
                                    onOpenModule = { route -> currentRoute = route }
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    // Scoped Global Search Dialog (strictly respects TenantDataScoper)
    if (showGlobalSearch) {
        var q by remember { mutableStateOf("") }
        val matchedModules = remember(q, drawerItems) {
            if (q.isBlank()) emptyList()
            else drawerItems.filter { it.label.contains(q, true) || it.route.contains(q, true) }.take(5)
        }
        val matchedOrders = remember(q, orders) {
            if (q.isBlank()) emptyList()
            else orders.filter { it.orderNumber.contains(q, true) || it.clientName.contains(q, true) || it.clientPhone.contains(q, true) }.take(5)
        }
        val matchedClients = remember(q, clients) {
            if (q.isBlank()) emptyList()
            else clients.filter { it.fullName.contains(q, true) || it.phone.contains(q, true) || it.address.contains(q, true) }.take(5)
        }
        val matchedServices = remember(q, services) {
            if (q.isBlank() || !activeRoleSession.canAccessRoute("catalog")) emptyList()
            else services.filter { it.name.contains(q, true) || it.treatmentType.contains(q, true) }.take(5)
        }
        AlertDialog(
            onDismissRequest = { showGlobalSearch = false },
            title = { Text("Recherche isolée (${activeRoleSession.role.shortTitleFr})", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = q,
                        onValueChange = { q = it },
                        placeholder = { Text("Fonctionnalité, commande, client, service...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (q.isBlank()) {
                        Text("Recherche limitée au périmètre autorisé : ${activeRoleSession.scopeLabel()}.", style = MaterialTheme.typography.bodySmall)
                    } else if (matchedModules.isEmpty() && matchedOrders.isEmpty() && matchedClients.isEmpty() && matchedServices.isEmpty()) {
                        Text("Aucun résultat dans votre périmètre pour « $q ».", style = MaterialTheme.typography.bodySmall)
                    } else {
                        if (matchedModules.isNotEmpty()) {
                            Text("Modules autorisés (${matchedModules.size})", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            matchedModules.forEach { mod ->
                                Card(
                                    onClick = {
                                        showGlobalSearch = false
                                        currentRoute = mod.route
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SoftBlueContainer.copy(alpha = 0.55f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(mod.icon, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                        Text(mod.label, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                        if (matchedOrders.isNotEmpty()) {
                            Text("Commandes (${matchedOrders.size})", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            matchedOrders.forEach { ord ->
                                Card(
                                    onClick = {
                                        showGlobalSearch = false
                                        selectedOrderId = ord.id
                                        currentRoute = "order_detail"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("${ord.orderNumber} · ${ord.clientName}", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            if (activeRoleSession.canViewFinancialAmounts)
                                                "${ord.orderStatusEnum().labelFr} · ${formatMoney(ord.totalAmount, company.currency)}"
                                            else ord.orderStatusEnum().labelFr,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                        if (matchedClients.isNotEmpty()) {
                            Text("Clients (${matchedClients.size})", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            matchedClients.forEach { cli ->
                                Card(
                                    onClick = {
                                        showGlobalSearch = false
                                        if (activeRoleSession.canAccessRoute("clients")) currentRoute = "clients"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(cli.fullName, fontWeight = FontWeight.SemiBold)
                                        Text("${cli.phone} · ${cli.address}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        if (matchedServices.isNotEmpty()) {
                            Text("Services & Tarifs (${matchedServices.size})", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            matchedServices.forEach { srv ->
                                Card(
                                    onClick = {
                                        showGlobalSearch = false
                                        currentRoute = "catalog"
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(srv.name, fontWeight = FontWeight.SemiBold)
                                        Text("${srv.treatmentType} · ${formatMoney(srv.unitPrice, company.currency)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showGlobalSearch = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Fermer")
                }
            }
        )
    }

    // Scoped Notifications, FCM Push Center & Operational Alerts
    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Centre d'alertes & Push FCM ($alertCount)", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // FCM Push Status & Android 13+ permission activator
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (notifPermissionGranted) EmeraldContainer.copy(alpha = 0.65f) else AmberContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (notifPermissionGranted) EmeraldSuccess else Color(0xFFB45309),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (notifPermissionGranted) "Notifications Push FCM actives" else "Activer les alertes Push FCM",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MidnightNavy
                                )
                            }
                            Text(
                                text = "Canaux : Nouvelles commandes caissiers & Vêtements prêts clients" +
                                    if (fcmTopics.isNotEmpty()) " (${fcmTopics.size} topic(s) abonnés)" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MidnightNavy.copy(alpha = 0.85f)
                            )
                            if (!notifPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Button(
                                    onClick = {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("enable_fcm_notifications_btn")
                                ) {
                                    Text("Autoriser les notifications", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Pending Room Offline Orders Queue
                    if (pendingOfflineOrders.isNotEmpty()) {
                        Text(
                            "File hors-ligne Room (${pendingOfflineOrders.size})",
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue
                        )
                        pendingOfflineOrders.take(5).forEach { off ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("${off.orderNumber} · ${off.clientName}", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "En cache local (${off.syncStatus}) · Sync auto au retour réseau",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RoyalBlue
                                    )
                                }
                            }
                        }
                    }

                    // Recent FCM Push Feed (Cashier new orders & Client garments ready)
                    if (pushNotifications.isNotEmpty()) {
                        Text(
                            "Alertes Push récentes (${pushNotifications.size})",
                            fontWeight = FontWeight.Bold,
                            color = MidnightNavy
                        )
                        pushNotifications.take(5).forEach { pn ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SoftBlueContainer.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(pn.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                                    Text(pn.body, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    if (alertCount == 0 && pushNotifications.isEmpty()) {
                        Text("Aucune alerte active dans votre périmètre (${activeRoleSession.scopeLabel()}).", style = MaterialTheme.typography.bodySmall)
                    } else {
                        if (lateOrders.isNotEmpty()) {
                            Text("Commandes en retard (${lateOrders.size})", fontWeight = FontWeight.Bold, color = CrimsonDanger)
                            lateOrders.take(5).forEach { ord ->
                                Card(
                                    onClick = {
                                        showNotifications = false
                                        selectedOrderId = ord.id
                                        currentRoute = "order_detail"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("${ord.orderNumber} · ${ord.clientName}", fontWeight = FontWeight.SemiBold)
                                        Text("Statut : ${ord.orderStatusEnum().labelFr}", style = MaterialTheme.typography.labelSmall, color = CrimsonDanger)
                                    }
                                }
                            }
                        }
                        if (lowStocks.isNotEmpty()) {
                            Text("Alertes Stocks & Machines (${lowStocks.size})", fontWeight = FontWeight.Bold, color = CrimsonDanger)
                            lowStocks.take(5).forEach { st ->
                                Card(
                                    onClick = {
                                        showNotifications = false
                                        currentRoute = "stocks"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(st.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            if (st.itemKind == "MACHINE") "Machine : ${st.machineStatus}" else "Stock bas : ${st.quantity} ${st.unit} (seuil ${st.minThreshold})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrimsonDanger
                                        )
                                    }
                                }
                            }
                        }
                        if (openClaims.isNotEmpty()) {
                            Text("Réclamations SAV ouvertes (${openClaims.size})", fontWeight = FontWeight.Bold, color = RoyalBlue)
                            openClaims.take(5).forEach { cl ->
                                Card(
                                    onClick = {
                                        showNotifications = false
                                        currentRoute = "claims"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(cl.subject, fontWeight = FontWeight.SemiBold)
                                        Text("${cl.clientName} · ${cl.orderNumber}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showNotifications = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Fermer")
                }
            }
        )
    }
}
