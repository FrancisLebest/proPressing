package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Discount
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.PriceChange
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.formatMoney
import com.example.data.AccessPermission
import com.example.data.AccountingEntry
import com.example.data.ActiveRoleSession
import com.example.data.ClaimRecord
import com.example.data.ClientRecord
import com.example.data.CompanyConfig
import com.example.data.EmployeeRecord
import com.example.data.GarmentWorkshopStatus
import com.example.data.OrderPriority
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.PressingOrder
import com.example.data.RoleAuditEvent
import com.example.data.RoleDashboardAnalytics
import com.example.data.StockItem
import com.example.data.WorkspaceRole
import com.example.ui.components.ServiceDonutChart
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelAmberTint
import com.example.ui.theme.PastelAmberBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelGreenTint
import com.example.ui.theme.PastelGreenBorder
import com.example.ui.theme.PastelVioletTint
import com.example.ui.theme.PastelVioletBorder
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.VioletAccent

private data class TimeWindowOption(val label: String, val days: Int)

private val DASHBOARD_TIME_WINDOWS = listOf(
    TimeWindowOption("Aujourd'hui", 1),
    TimeWindowOption("7j", 7),
    TimeWindowOption("30j", 30),
    TimeWindowOption("90j", 90),
    TimeWindowOption("1 an", 365)
)

/**
 * Strictly locked Multi-Dashboard entry point.
 * Automatically dispatches the authenticated user to the exact dashboard of their Firestore role
 * with zero client-side role switching controls.
 */
@Composable
fun DashboardScreen(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    orders: List<PressingOrder>,
    allTenantOrders: List<PressingOrder>,
    clients: List<ClientRecord>,
    transactions: List<AccountingEntry>,
    stocks: List<StockItem>,
    employees: List<EmployeeRecord>,
    claims: List<ClaimRecord>,
    auditLog: List<RoleAuditEvent>,
    onSelectAgencyFilter: (String?) -> Unit,
    onNewOrder: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onQuickAdvanceStatus: (PressingOrder, OrderStatus) -> Unit,
    onReturnToRework: (PressingOrder, GarmentWorkshopStatus, String) -> Unit,
    onRecordPayment: (PressingOrder, Long, PaymentMethod) -> Unit,
    onConfirmDeliveryWithProof: (PressingOrder, String, String) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    when (roleSession.role) {
        WorkspaceRole.DIRECTEUR -> DirecteurGeneralDashboard(
            company = company,
            roleSession = roleSession,
            scopedOrders = orders,
            allTenantOrders = allTenantOrders,
            transactions = transactions,
            claims = claims,
            auditLog = auditLog,
            onSelectAgencyFilter = onSelectAgencyFilter,
            onNewOrder = onNewOrder,
            onNavigateModule = onNavigateModule
        )

        WorkspaceRole.GERANT_AGENCE -> AgencyManagerDashboard(
            company = company,
            roleSession = roleSession,
            orders = orders,
            transactions = transactions,
            stocks = stocks,
            employees = employees,
            claims = claims,
            onNewOrder = onNewOrder,
            onOpenOrder = onOpenOrder,
            onNavigateModule = onNavigateModule
        )

        WorkspaceRole.CAISSIER -> CashierCounterDashboard(
            company = company,
            roleSession = roleSession,
            orders = orders,
            transactions = transactions,
            onNewOrder = onNewOrder,
            onOpenOrder = onOpenOrder,
            onRecordPayment = onRecordPayment,
            onNavigateModule = onNavigateModule
        )

        WorkspaceRole.OPERATEUR -> WorkshopOperatorDashboard(
            roleSession = roleSession,
            orders = orders,
            onOpenOrder = onOpenOrder,
            onQuickAdvanceStatus = onQuickAdvanceStatus,
            onNavigateModule = onNavigateModule
        )

        WorkspaceRole.CONTROLE_QUALITE -> QualityControlDashboard(
            roleSession = roleSession,
            orders = orders,
            claims = claims,
            onOpenOrder = onOpenOrder,
            onQuickAdvanceStatus = onQuickAdvanceStatus,
            onReturnToRework = onReturnToRework,
            onNavigateModule = onNavigateModule
        )

        WorkspaceRole.LIVREUR -> DeliveryDriverDashboard(
            company = company,
            roleSession = roleSession,
            orders = orders,
            onOpenOrder = onOpenOrder,
            onConfirmDeliveryWithProof = onConfirmDeliveryWithProof
        )
    }
}

// ============================================================================
// A) TABLEAU DE BORD DIRECTEUR GÉNÉRAL (VUE MULTI-AGENCES & STRATÉGIQUE)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DirecteurGeneralDashboard(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    scopedOrders: List<PressingOrder>,
    allTenantOrders: List<PressingOrder>,
    transactions: List<AccountingEntry>,
    claims: List<ClaimRecord>,
    auditLog: List<RoleAuditEvent>,
    onSelectAgencyFilter: (String?) -> Unit,
    onNewOrder: () -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val currency = company.currency
    var periodIdx by remember { mutableIntStateOf(2) } // 30j by default
    val selectedWindow = DASHBOARD_TIME_WINDOWS[periodIdx]
    val cutoffMillis = remember(selectedWindow) {
        System.currentTimeMillis() - selectedWindow.days * 86_400_000L
    }

    val filteredOrders = remember(scopedOrders, cutoffMillis) {
        scopedOrders.filter {
            it.orderStatusEnum() != OrderStatus.CANCELLED &&
                ((it.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= cutoffMillis)
        }
    }
    val filteredTx = remember(transactions, cutoffMillis) {
        transactions.filter {
            (it.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= cutoffMillis
        }
    }

    val globalRevenue = remember(filteredOrders) { filteredOrders.sumOf { it.totalAmount } }
    val netCollected = remember(filteredTx) {
        filteredTx.filter { it.entryType == "REVENUE" }.sumOf { it.amount }
    }
    val receivables = remember(filteredOrders) { filteredOrders.sumOf { it.remainingAmount } }
    val expenses = remember(filteredTx) {
        filteredTx.filter { it.entryType == "EXPENSE" || it.entryType == "REFUND" }.sumOf { it.amount }
    }
    val netMargin = netCollected - expenses
    val totalPieces = remember(filteredOrders) { filteredOrders.sumOf { it.itemCount.coerceAtLeast(1L) } }
    val punctualityPct = remember(filteredOrders) { RoleDashboardAnalytics.computePunctualityRatePercent(filteredOrders) }

    // 7-bucket curve data for Revenue vs Collections
    val curveData = remember(filteredOrders, filteredTx, selectedWindow) {
        val buckets = 6
        val now = System.currentTimeMillis()
        val span = (selectedWindow.days * 86_400_000L).coerceAtLeast(86_400_000L)
        val step = span / buckets
        val labels = (0 until buckets).map { idx ->
            if (selectedWindow.days == 1) "${(idx + 1) * 4}h"
            else "P${idx + 1}"
        }
        val revBuckets = MutableList(buckets) { 0L }
        val colBuckets = MutableList(buckets) { 0L }
        filteredOrders.forEach { ord ->
            val t = ord.createdAt?.toDate()?.time ?: now
            val idx = (((t - (now - span)).coerceAtLeast(0L)) / step).toInt().coerceIn(0, buckets - 1)
            revBuckets[idx] += ord.totalAmount
        }
        filteredTx.filter { it.entryType == "REVENUE" }.forEach { tx ->
            val t = tx.createdAt?.toDate()?.time ?: now
            val idx = (((t - (now - span)).coerceAtLeast(0L)) / step).toInt().coerceIn(0, buckets - 1)
            colBuckets[idx] += tx.amount
        }
        Triple(labels, revBuckets, colBuckets)
    }

    // Payment method donut slices (Espèces, Wave, Orange Money, MTN MoMo, Moov Money)
    val paymentSlices = remember(filteredTx) {
        val revTx = filteredTx.filter { it.entryType == "REVENUE" }
        val raw = listOf(
            "Espèces" to revTx.filter { it.paymentMethod == PaymentMethod.ESPECES.name }.sumOf { it.amount }.toFloat(),
            "Wave" to revTx.filter { it.paymentMethod == PaymentMethod.WAVE.name }.sumOf { it.amount }.toFloat(),
            "Orange Money" to revTx.filter { it.paymentMethod == PaymentMethod.ORANGE_MONEY.name }.sumOf { it.amount }.toFloat(),
            "MTN MoMo" to revTx.filter { it.paymentMethod == PaymentMethod.MTN_MOMO.name }.sumOf { it.amount }.toFloat(),
            "Moov Money" to revTx.filter { it.paymentMethod == PaymentMethod.MOOV_MONEY.name }.sumOf { it.amount }.toFloat()
        ).filter { it.second > 0f }
        raw.ifEmpty { listOf("Espèces" to 1f, "Wave" to 1f, "Orange Money" to 1f) }
    }

    // Agency comparison built on all tenant orders so Directeur can compare & click to filter
    val agencyPerformance = remember(allTenantOrders, company.agencies) {
        RoleDashboardAnalytics.computeAgencyComparison(allTenantOrders, company.agencies)
    }

    val governanceAlerts = remember(scopedOrders, transactions, claims, auditLog, company.maxCashierDiscountPercent) {
        RoleDashboardAnalytics.detectGovernanceAlerts(
            orders = scopedOrders,
            transactions = transactions,
            claims = claims,
            auditLog = auditLog,
            maxCashierDiscountPercent = company.maxCashierDiscountPercent
        )
    }

    val agenciesList = remember(company.agencies, allTenantOrders) {
        (company.agencies + allTenantOrders.map { it.agencyName })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf("Agence Principale") }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_director"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardRoleHeroHeader(
                roleSession = roleSession,
                title = if (roleSession.isSuperAdmin) {
                    "Super-Admin & Direction Générale · ${company.companyName}"
                } else {
                    "Direction Générale · ${company.companyName}"
                },
                subtitle = "Pilotage stratégique multi-agences, trésorerie SYSCOHADA, gouvernance SaaS et sécurité.",
                trailingContent = {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNewOrder,
                            colors = ButtonDefaults.buttonColors(containerColor = AfricaGold, contentColor = MidnightNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Commande", fontWeight = FontWeight.ExtraBold)
                        }
                        OutlinedButton(
                            onClick = { onNavigateModule("finance") },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("SYSCOHADA", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onNavigateModule("saas_admin") },
                            border = BorderStroke(1.dp, AfricaGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AfricaGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("dashboard_open_saas_admin_button")
                        ) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Super-Admin SaaS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        // Agency Selector + Dynamic Time Window Filter
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = roleSession.isolatedAgency == null,
                            onClick = { onSelectAgencyFilter(null) },
                            leadingIcon = { Icon(Icons.Rounded.Business, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Toutes les agences") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                    items(agenciesList) { agency ->
                        FilterChip(
                            selected = roleSession.isolatedAgency.equals(agency, ignoreCase = true),
                            onClick = { onSelectAgencyFilter(agency) },
                            label = { Text(agency) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DASHBOARD_TIME_WINDOWS.indices.toList()) { idx ->
                        val opt = DASHBOARD_TIME_WINDOWS[idx]
                        FilterChip(
                            selected = periodIdx == idx,
                            onClick = { periodIdx = idx },
                            label = { Text(opt.label) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }
        }

        // 7 Strategic KPIs
        item {
            ResponsiveKpiGrid(
                items = listOf(
                    DashboardKpiSpec(
                        title = "CA Global (${selectedWindow.label})",
                        value = formatMoney(globalRevenue, currency),
                        trend = "${filteredOrders.size} commande(s)",
                        icon = Icons.Rounded.TrendingUp,
                        accent = RoyalBlue,
                        pastelBg = PastelBlueTint,
                        pastelBorder = PastelBlueBorder,
                        onClick = { onNavigateModule("reports") }
                    ),
                    DashboardKpiSpec(
                        title = "Encaissements nets",
                        value = formatMoney(netCollected, currency),
                        trend = "Trésorerie réelle",
                        icon = Icons.Rounded.Payments,
                        accent = EmeraldSuccess,
                        pastelBg = PastelGreenTint,
                        pastelBorder = PastelGreenBorder,
                        onClick = { onNavigateModule("finance") }
                    ),
                    DashboardKpiSpec(
                        title = "Reste à encaisser",
                        value = formatMoney(receivables, currency),
                        trend = "Créances clients",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        accent = AfricaGold,
                        pastelBg = PastelAmberTint,
                        pastelBorder = PastelAmberBorder,
                        onClick = { onNavigateModule("orders") }
                    ),
                    DashboardKpiSpec(
                        title = "Dépenses exploitation",
                        value = formatMoney(expenses, currency),
                        trend = "Charges période",
                        icon = Icons.Rounded.TrendingDown,
                        accent = CrimsonDanger,
                        pastelBg = PastelVioletTint,
                        pastelBorder = PastelVioletBorder,
                        onClick = { onNavigateModule("finance") }
                    ),
                    DashboardKpiSpec(
                        title = "Marge nette",
                        value = formatMoney(netMargin, currency),
                        trend = if (netMargin >= 0L) "Excédent net" else "Déficit",
                        icon = Icons.Rounded.Verified,
                        accent = if (netMargin >= 0L) EmeraldSuccess else CrimsonDanger,
                        pastelBg = PastelGreenTint,
                        pastelBorder = PastelGreenBorder
                    ),
                    DashboardKpiSpec(
                        title = "Pièces traitées",
                        value = "$totalPieces pcs",
                        trend = "Production cumulée",
                        icon = Icons.Rounded.Checkroom,
                        accent = VioletAccent,
                        pastelBg = PastelVioletTint,
                        pastelBorder = PastelVioletBorder
                    ),
                    DashboardKpiSpec(
                        title = "Ponctualité livraisons",
                        value = "$punctualityPct%",
                        trend = "Respect délais promis",
                        icon = Icons.Rounded.Schedule,
                        accent = if (punctualityPct >= 90) EmeraldSuccess else AfricaGold,
                        pastelBg = PastelBlueTint,
                        pastelBorder = PastelBlueBorder
                    )
                )
            )
        }

        // Interactive Charts (Side-by-side on wide screens >= 840dp)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 840.dp
                val curveCard: @Composable (Modifier) -> Unit = { m ->
                    Card(
                        modifier = m,
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Évolution CA & Encaissements (${selectedWindow.label})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            DualCurveRevenueChart(
                                labels = curveData.first,
                                revenueSeries = curveData.second,
                                collectedSeries = curveData.third
                            )
                        }
                    }
                }
                val donutCard: @Composable (Modifier) -> Unit = { m ->
                    Card(
                        modifier = m,
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Répartition par mode de paiement", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            ServiceDonutChart(
                                slices = paymentSlices,
                                centerLabel = "Canaux\n${selectedWindow.label}"
                            )
                        }
                    }
                }

                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        curveCard(Modifier.weight(1.25f))
                        donutCard(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        curveCard(Modifier.fillMaxWidth())
                        donutCard(Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Agency Performance Comparison + Governance Alerts (side-by-side on >= 840dp)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 840.dp
                val agencyCard: @Composable (Modifier) -> Unit = { m ->
                    Card(
                        modifier = m,
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Comparatif de performance par agence", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Touchez une agence pour filtrer les indicateurs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AgencyComparisonChart(
                                agencies = agencyPerformance,
                                currency = currency,
                                selectedAgency = roleSession.isolatedAgency,
                                onSelectAgency = onSelectAgencyFilter
                            )
                        }
                    }
                }
                val governanceCard: @Composable (Modifier) -> Unit = { m ->
                    Card(
                        modifier = m,
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, CrimsonDanger.copy(alpha = 0.28f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Alertes de Gouvernance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (governanceAlerts.totalAlertCount > 0) CrimsonContainer else EmeraldContainer
                                ) {
                                    Text(
                                        text = "${governanceAlerts.totalAlertCount} alerte(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (governanceAlerts.totalAlertCount > 0) CrimsonDanger else Color(0xFF047857),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            GovernanceAlertRow(
                                icon = Icons.Rounded.ErrorOutline,
                                label = "Écarts de caisse détectés (Clôture Z)",
                                count = governanceAlerts.cashVariances.size,
                                color = CrimsonDanger
                            )
                            GovernanceAlertRow(
                                icon = Icons.Rounded.Discount,
                                label = "Remises exceptionnelles (> ${company.maxCashierDiscountPercent.toInt()}%)",
                                count = governanceAlerts.exceptionalDiscountOrders.size,
                                color = AfricaGold
                            )
                            GovernanceAlertRow(
                                icon = Icons.Rounded.ReportProblem,
                                label = "Litiges SAV ouverts",
                                count = governanceAlerts.openClaims.size,
                                color = VioletAccent
                            )
                            GovernanceAlertRow(
                                icon = Icons.Rounded.Security,
                                label = "Tentatives d'accès refusées (Audit)",
                                count = governanceAlerts.deniedAuditEventsCount,
                                color = RoyalBlue
                            )
                            HorizontalDivider()
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateModule("roles_rbac") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MidnightNavy),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Audit & Matrice 27 Privilèges", fontWeight = FontWeight.Bold)
                                }
                                if (roleSession.has(AccessPermission.VIEW_MULTI_AGENCY_SYSCOHADA)) {
                                    OutlinedButton(
                                        onClick = { onNavigateModule("saas_admin") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Super-Admin", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        agencyCard(Modifier.weight(1f))
                        governanceCard(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        agencyCard(Modifier.fillMaxWidth())
                        governanceCard(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun GovernanceAlertRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            softWrap = true
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (count > 0) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// ============================================================================
// B) TABLEAU DE BORD GÉRANT D'AGENCE (PILOTAGE OPÉRATIONNEL & CAISSE LOCALE)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgencyManagerDashboard(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    orders: List<PressingOrder>,
    transactions: List<AccountingEntry>,
    stocks: List<StockItem>,
    employees: List<EmployeeRecord>,
    claims: List<ClaimRecord>,
    onNewOrder: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val currency = company.currency
    var periodIdx by remember { mutableIntStateOf(1) } // 7j default
    val selectedWindow = DASHBOARD_TIME_WINDOWS[periodIdx]
    val cutoffMillis = remember(selectedWindow) {
        System.currentTimeMillis() - selectedWindow.days * 86_400_000L
    }
    val startOfDayMillis = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val periodOrders = remember(orders, cutoffMillis) {
        orders.filter {
            it.orderStatusEnum() != OrderStatus.CANCELLED &&
                ((it.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= cutoffMillis)
        }
    }
    val todayRevenue = remember(orders, startOfDayMillis) {
        orders.filter {
            it.orderStatusEnum() != OrderStatus.CANCELLED &&
                ((it.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= startOfDayMillis)
        }.sumOf { it.totalAmount }
    }
    val periodRevenue = remember(periodOrders) { periodOrders.sumOf { it.totalAmount } }
    val deliveredOrders = remember(periodOrders) {
        periodOrders.count { it.orderStatusEnum() in listOf(OrderStatus.PICKED_UP, OrderStatus.DELIVERED) }
    }
    val urgentOrLateCount = remember(orders) {
        orders.count {
            it.orderStatusEnum() !in listOf(OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.CANCELLED) &&
                (it.isLate() || it.priorityEnum() == OrderPriority.EXPRESS)
        }
    }
    val inWorkshopOrders = remember(orders) {
        orders.count {
            it.orderStatusEnum() in listOf(
                OrderStatus.PENDING,
                OrderStatus.ACCEPTED,
                OrderStatus.WASHING,
                OrderStatus.DRYING,
                OrderStatus.IRONING,
                OrderStatus.FINISHING,
                OrderStatus.QUALITY_CHECK
            )
        }
    }
    val workshopFillRatePct = remember(inWorkshopOrders) {
        ((inWorkshopOrders.toDouble() / 25.0) * 100.0).toInt().coerceIn(0, 100)
    }
    val criticalConsumables = remember(stocks) {
        stocks.filter { it.isLowStock() || it.machineStatus != "OPERATIONAL" }
    }
    val openClaimsCount = remember(claims) { claims.count { it.status != "RESOLVED" } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_manager"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardRoleHeroHeader(
                roleSession = roleSession,
                title = "Gérant · ${roleSession.isolatedAgency ?: company.companyName}",
                subtitle = "Périmètre strictement isolé à votre agence · Pilotage caisse, atelier, stocks et équipe.",
                accentGradientEnd = Color(0xFF4C1D95)
            )
        }

        // Dynamic period selector (no multi-agency toggle for Gérant!)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DASHBOARD_TIME_WINDOWS.indices.toList()) { idx ->
                    FilterChip(
                        selected = periodIdx == idx,
                        onClick = { periodIdx = idx },
                        label = { Text(DASHBOARD_TIME_WINDOWS[idx].label) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }

        // Agency KPIs
        item {
            ResponsiveKpiGrid(
                items = listOf(
                    DashboardKpiSpec(
                        title = "CA Jour / ${selectedWindow.label}",
                        value = formatMoney(periodRevenue, currency),
                        trend = "Jour : ${formatMoney(todayRevenue, currency)}",
                        icon = Icons.Rounded.TrendingUp,
                        accent = RoyalBlue,
                        pastelBg = PastelBlueTint,
                        pastelBorder = PastelBlueBorder
                    ),
                    DashboardKpiSpec(
                        title = "Déposées vs Livrées",
                        value = "${periodOrders.size} / $deliveredOrders",
                        trend = "Flux commandes agence",
                        icon = Icons.Rounded.Checkroom,
                        accent = EmeraldSuccess,
                        pastelBg = PastelGreenTint,
                        pastelBorder = PastelGreenBorder,
                        onClick = { onNavigateModule("orders") }
                    ),
                    DashboardKpiSpec(
                        title = "Urgentes / Retards",
                        value = "$urgentOrLateCount",
                        trend = "Express 6h/24h & retards",
                        icon = Icons.Rounded.Schedule,
                        accent = CrimsonDanger,
                        pastelBg = PastelAmberTint,
                        pastelBorder = PastelAmberBorder,
                        onClick = { onNavigateModule("workshop_tracker") }
                    ),
                    DashboardKpiSpec(
                        title = "Remplissage atelier",
                        value = "$workshopFillRatePct%",
                        trend = "$inWorkshopOrders cmd en cours",
                        icon = Icons.Rounded.LocalLaundryService,
                        accent = VioletAccent,
                        pastelBg = PastelVioletTint,
                        pastelBorder = PastelVioletBorder,
                        onClick = { onNavigateModule("workshop_tracker") }
                    ),
                    DashboardKpiSpec(
                        title = "Stocks critiques",
                        value = "${criticalConsumables.size}",
                        trend = "Lessive, cintres, housses",
                        icon = Icons.Rounded.Inventory2,
                        accent = if (criticalConsumables.isEmpty()) EmeraldSuccess else AfricaGold,
                        pastelBg = PastelAmberTint,
                        pastelBorder = PastelAmberBorder,
                        onClick = { onNavigateModule("stocks") }
                    )
                )
            )
        }

        // Agency Quick Actions (4 key managerial actions)
        item {
            Text("Actions rapides Gérant d'Agence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNewOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Valider / Créer commande", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onNavigateModule("settings") },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Superviser Clôture Z", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onNavigateModule("claims") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SAV ($openClaimsCount)", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onNavigateModule("catalog") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.PriceChange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tarifs & Promos", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Agency Team Presence & Productivity
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Groups, contentDescription = null, tint = RoyalBlue)
                            Text("Équipe agence en temps réel (${employees.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Surface(shape = RoundedCornerShape(50), color = EmeraldContainer) {
                            Text(
                                text = "${employees.count { it.presentToday }} présent(s)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (employees.isEmpty()) {
                        Text(
                            text = "Aucun collaborateur enregistré pour cette agence.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        employees.forEach { emp ->
                            val handledCount = orders.count {
                                it.assignedEmployee.equals(emp.fullName, ignoreCase = true) ||
                                    it.deliveryDriver.equals(emp.fullName, ignoreCase = true)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (emp.presentToday) EmeraldSuccess else CrimsonDanger)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(emp.fullName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = "${WorkspaceRole.fromRaw(emp.role).labelFr} · ${if (emp.presentToday) "En poste" else "Absent"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = SoftBlueContainer) {
                                    Text(
                                        text = "$handledCount dossier(s)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Validation des remises exceptionnelles & Clôtures Z de l'agence
        item {
            val exceptionalDiscounts = remember(orders, company.maxCashierDiscountPercent) {
                orders.filter { ord ->
                    ord.subtotal > 0L &&
                        ((ord.discountAmount.toDouble() / ord.subtotal.toDouble()) * 100.0) > company.maxCashierDiscountPercent
                }
            }
            val recentCashCloses = remember(transactions) {
                transactions.filter { it.entryType == "CASH_CLOSE" }.take(3)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manager_validation_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = RoyalBlue)
                            Text(
                                "Validation Remises & Clôtures Z",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = { onNavigateModule("settings") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Clôture Z", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (exceptionalDiscounts.isEmpty()) {
                        Text(
                            text = "Aucune remise exceptionnelle au-delà du plafond caissier (${company.maxCashierDiscountPercent.toInt()}%).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Remises > ${company.maxCashierDiscountPercent.toInt()}% à contrôler (${exceptionalDiscounts.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonDanger
                        )
                        exceptionalDiscounts.take(3).forEach { ord ->
                            Surface(
                                onClick = { onOpenOrder(ord.id) },
                                shape = RoundedCornerShape(10.dp),
                                color = AmberContainer.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${ord.orderNumber} · ${ord.clientName}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "Remise : -${formatMoney(ord.discountAmount, currency)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CrimsonDanger
                                        )
                                    }
                                    Text("Contrôler", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RoyalBlue)
                                }
                            }
                        }
                    }

                    if (recentCashCloses.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            "Dernières clôtures Z d'agence (${recentCashCloses.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MidnightNavy
                        )
                        recentCashCloses.forEach { z ->
                            val hasVariance = z.label.contains("Manquant", true) || z.label.contains("Excédent", true)
                            Text(
                                text = "• ${z.label} (${formatMoney(z.amount, currency)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasVariance) CrimsonDanger else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (hasVariance) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Critical Consumables Watchlist
        if (criticalConsumables.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberContainer.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, AfricaGold.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Alertes consommables & équipements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightNavy)
                        criticalConsumables.take(5).forEach { st ->
                            Text(
                                text = "• ${st.name} : ${if (st.itemKind == "MACHINE") st.machineStatus else "${st.quantity} ${st.unit} (seuil ${st.minThreshold})"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
