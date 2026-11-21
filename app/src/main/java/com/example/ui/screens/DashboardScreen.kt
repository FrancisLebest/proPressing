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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.dialPhoneIntent
import com.example.core.formatMoney
import com.example.core.formatShortDate
import com.example.core.openMapIntent
import com.example.data.AccessPermission
import com.example.data.AccountingEntry
import com.example.data.ActiveRoleSession
import com.example.data.ClaimRecord
import com.example.data.ClientRecord
import com.example.data.CompanyConfig
import com.example.data.OrderStatus
import com.example.data.PaymentStatus
import com.example.data.PressingOrder
import com.example.data.StockItem
import com.example.data.SubscriptionPlan
import com.example.data.WorkspaceRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MapPreviewCanvas
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.PaymentStatusChip
import com.example.ui.components.RevenueBarChart
import com.example.ui.components.RoleBadgeChip
import com.example.ui.components.ServiceDonutChart
import com.example.ui.components.SparklineKpiCard
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AfricaGoldBright
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelAmberBorder
import com.example.ui.theme.PastelAmberTint
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.PastelGreenBorder
import com.example.ui.theme.PastelGreenTint
import com.example.ui.theme.PastelRoseBorder
import com.example.ui.theme.PastelRoseTint
import com.example.ui.theme.PastelVioletBorder
import com.example.ui.theme.PastelVioletTint
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.VioletAccent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    orders: List<PressingOrder>,
    clients: List<ClientRecord>,
    transactions: List<AccountingEntry>,
    stocks: List<StockItem>,
    claims: List<ClaimRecord>,
    onNewOrder: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onQuickAdvanceStatus: (PressingOrder, OrderStatus) -> Unit,
    onConfirmDelivery: (PressingOrder) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val todayLabel = remember {
        SimpleDateFormat("d MMM yyyy", Locale.FRENCH).format(Date())
    }

    when (roleSession.role) {
        WorkspaceRole.DIRECTEUR,
        WorkspaceRole.GERANT_AGENCE -> ExecutiveDashboardView(
            company = company,
            roleSession = roleSession,
            todayLabel = todayLabel,
            orders = orders,
            clients = clients,
            transactions = transactions,
            stocks = stocks,
            onNewOrder = onNewOrder,
            onOpenOrder = onOpenOrder,
            onNavigateModule = onNavigateModule
        )

        WorkspaceRole.CAISSIER -> CashierIsolatedDashboard(
            company = company,
            roleSession = roleSession,
            todayLabel = todayLabel,
            orders = orders,
            clients = clients,
            transactions = transactions,
            onNewOrder = onNewOrder,
            onOpenOrder = onOpenOrder,
            onNavigateModule = onNavigateModule
        )

        WorkspaceRole.OPERATEUR -> WorkshopOperatorIsolatedDashboard(
            company = company,
            roleSession = roleSession,
            todayLabel = todayLabel,
            orders = orders,
            stocks = stocks,
            onOpenOrder = onOpenOrder,
            onQuickAdvanceStatus = onQuickAdvanceStatus,
            onNavigateModule = onNavigateModule
        )

        WorkspaceRole.CONTROLE_QUALITE -> QualityIsolatedDashboard(
            company = company,
            roleSession = roleSession,
            todayLabel = todayLabel,
            orders = orders,
            claims = claims,
            onOpenOrder = onOpenOrder,
            onQuickAdvanceStatus = onQuickAdvanceStatus,
            onNavigateModule = onNavigateModule
        )

        WorkspaceRole.LIVREUR -> DriverIsolatedDashboard(
            company = company,
            roleSession = roleSession,
            todayLabel = todayLabel,
            orders = orders,
            onOpenOrder = onOpenOrder,
            onConfirmDelivery = onConfirmDelivery,
            onNavigateModule = onNavigateModule
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardGreetingHeader(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    todayLabel: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Bonjour, ${roleSession.operatorName} !",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = EmeraldContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Entreprise Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Aujourd'hui • $todayLabel",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Read-only Entity & Role Isolation Strip (strictly locked — no role switching from dashboard)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_role_isolation_bar")
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(shape = RoundedCornerShape(50), color = EmeraldContainer) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Rôle verrouillé",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Isolé",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Business,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = roleSession.scopeLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        softWrap = true
                    )
                }
                RoleBadgeChip(role = roleSession.role)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExecutiveDashboardView(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    todayLabel: String,
    orders: List<PressingOrder>,
    clients: List<ClientRecord>,
    transactions: List<AccountingEntry>,
    stocks: List<StockItem>,
    onNewOrder: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val currency = company.currency
    var chartPeriod by remember { mutableStateOf("7j") }

    val totalRevenue = transactions.filter { it.entryType == "REVENUE" }.sumOf { it.amount }
    val activeOrders = orders.count {
        it.orderStatusEnum() !in listOf(OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.CANCELLED)
    }
    val lateOrders = orders.count { it.isLate() }
    val lowStocks = stocks.count { it.isLowStock() }

    val revenueBars = remember(transactions, chartPeriod) {
        val nowMs = System.currentTimeMillis()
        val revEntries = transactions.filter { it.entryType == "REVENUE" }
        val cal = Calendar.getInstance()
        when (chartPeriod) {
            "30j" -> {
                val buckets = LongArray(4)
                val weekMs = 7L * 24L * 3_600_000L
                revEntries.forEach { tx ->
                    val ts = tx.createdAt?.toDate()?.time ?: nowMs
                    val ageWeeks = ((nowMs - ts).coerceAtLeast(0L) / weekMs).toInt()
                    if (ageWeeks in 0..3) {
                        buckets[3 - ageWeeks] += tx.amount
                    }
                }
                listOf("S-3", "S-2", "S-1", "Act.").mapIndexed { idx, lbl -> lbl to buckets[idx] }
            }
            "90j" -> {
                val monthNames = listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc")
                val curMonth = cal.get(Calendar.MONTH)
                val curYear = cal.get(Calendar.YEAR)
                val targets = (2 downTo 0).map { offset ->
                    val c = Calendar.getInstance().apply {
                        set(Calendar.YEAR, curYear)
                        set(Calendar.MONTH, curMonth)
                        add(Calendar.MONTH, -offset)
                    }
                    Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH), monthNames[c.get(Calendar.MONTH)])
                }
                targets.map { (yr, mo, lbl) ->
                    val sum = revEntries.filter { tx ->
                        val d = tx.createdAt?.toDate() ?: return@filter false
                        cal.time = d
                        cal.get(Calendar.YEAR) == yr && cal.get(Calendar.MONTH) == mo
                    }.sumOf { it.amount }
                    lbl to sum
                }
            }
            "1an" -> {
                val curYear = cal.get(Calendar.YEAR)
                val quarters = LongArray(4)
                revEntries.forEach { tx ->
                    val d = tx.createdAt?.toDate() ?: return@forEach
                    cal.time = d
                    if (cal.get(Calendar.YEAR) == curYear) {
                        val q = (cal.get(Calendar.MONTH) / 3).coerceIn(0, 3)
                        quarters[q] += tx.amount
                    }
                }
                listOf("T1", "T2", "T3", "T4").mapIndexed { idx, lbl -> lbl to quarters[idx] }
            }
            else -> { // 7j
                val dayNames = listOf("Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam")
                val dailyTotals = mutableMapOf<Int, Long>()
                val sevenDaysAgo = nowMs - 7L * 24L * 3_600_000L
                revEntries.forEach { tx ->
                    val d = tx.createdAt?.toDate()
                    if (d != null && d.time >= sevenDaysAgo) {
                        cal.time = d
                        val dow = cal.get(Calendar.DAY_OF_WEEK) - 1
                        dailyTotals[dow] = (dailyTotals[dow] ?: 0L) + tx.amount
                    }
                }
                listOf(1, 2, 3, 4, 5, 6, 0).map { dow ->
                    dayNames[dow] to (dailyTotals[dow] ?: 0L)
                }
            }
        }
    }

    val serviceCounts = mutableMapOf<String, Int>()
    orders.forEach { order ->
        order.parsedItems().forEach { item ->
            val key = item.serviceName.ifBlank { "Pressing" }
            serviceCounts[key] = (serviceCounts[key] ?: 0) + item.quantity.toInt()
        }
    }
    val donutSlices = if (serviceCounts.isEmpty()) {
        listOf("Lavage + Repassage" to 45f, "Nettoyage à sec" to 35f, "Blanchisserie" to 20f)
    } else {
        serviceCounts.entries.sortedByDescending { it.value }.take(4).map { it.key to it.value.toFloat() }
    }

    val revSpark = revenueBars.map { it.second.toFloat().coerceAtLeast(10f) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Mockup Greeting Header + Active Entity & Role Strip
        item {
            DashboardGreetingHeader(
                company = company,
                roleSession = roleSession,
                todayLabel = todayLabel,
                subtitle = "Voici un aperçu général de ${company.companyName} aujourd'hui."
            )
        }

        // 2. Signature 4 Pastel Sparkline KPI Cards (1 column < 600.dp, 2 columns >= 600.dp)
        item {
            data class KpiSpec(
                val title: String,
                val value: String,
                val trendLabel: String,
                val icon: ImageVector,
                val accentColor: Color,
                val pastelTint: Color,
                val pastelBorder: Color,
                val sparklinePoints: List<Float>,
                val onClick: (() -> Unit)?
            )
            val kpiItems = listOf(
                KpiSpec(
                    title = "Chiffre d'affaires",
                    value = formatMoney(totalRevenue, currency),
                    trendLabel = "${transactions.count { it.entryType == "REVENUE" }} enc.",
                    icon = Icons.Rounded.TrendingUp,
                    accentColor = RoyalBlue,
                    pastelTint = PastelBlueTint,
                    pastelBorder = PastelBlueBorder,
                    sparklinePoints = revSpark,
                    onClick = { if (roleSession.has(AccessPermission.VIEW_ACCOUNTING_SYSCOHADA)) onNavigateModule("finance") }
                ),
                KpiSpec(
                    title = "Commandes",
                    value = "${orders.size}",
                    trendLabel = "$activeOrders en cours",
                    icon = Icons.Rounded.ShoppingCart,
                    accentColor = EmeraldSuccess,
                    pastelTint = PastelGreenTint,
                    pastelBorder = PastelGreenBorder,
                    sparklinePoints = listOf(0.25f, 0.42f, 0.38f, 0.68f, 0.74f, 0.92f),
                    onClick = { onNavigateModule("orders") }
                ),
                KpiSpec(
                    title = "Clients CRM",
                    value = "${clients.size}",
                    trendLabel = "${clients.count { it.category == "VIP" }} VIP",
                    icon = Icons.Rounded.People,
                    accentColor = VioletAccent,
                    pastelTint = PastelVioletTint,
                    pastelBorder = PastelVioletBorder,
                    sparklinePoints = listOf(0.30f, 0.45f, 0.55f, 0.62f, 0.80f, 0.95f),
                    onClick = { onNavigateModule("clients") }
                ),
                KpiSpec(
                    title = "Stock & Alertes",
                    value = "${lowStocks + lateOrders}",
                    trendLabel = if (lowStocks + lateOrders > 0) "À surveiller" else "Optimal",
                    icon = Icons.Rounded.Inventory2,
                    accentColor = if (lowStocks + lateOrders > 0) AmberWarning else EmeraldSuccess,
                    pastelTint = PastelAmberTint,
                    pastelBorder = PastelAmberBorder,
                    sparklinePoints = listOf(0.55f, 0.35f, 0.60f, 0.45f, 0.70f, 0.50f),
                    onClick = { onNavigateModule("stocks") }
                )
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cols = if (maxWidth < 600.dp) 1 else 2
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    kpiItems.chunked(cols).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { kpi ->
                                SparklineKpiCard(
                                    title = kpi.title,
                                    value = kpi.value,
                                    trendLabel = kpi.trendLabel,
                                    icon = kpi.icon,
                                    accentColor = kpi.accentColor,
                                    pastelTint = kpi.pastelTint,
                                    pastelBorder = kpi.pastelBorder,
                                    sparklinePoints = kpi.sparklinePoints,
                                    modifier = Modifier.weight(1f),
                                    onClick = kpi.onClick
                                )
                            }
                            repeat(cols - rowItems.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 3 & 4. Revenue Bar Chart + Service Split Donut (side-by-side on wide screens, stacked on phone)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWideCharts = maxWidth >= 780.dp
                val revenueChartCard: @Composable (Modifier) -> Unit = { mod ->
                    Card(
                        modifier = mod,
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.BarChart, contentDescription = null, tint = RoyalBlue)
                                    Text(
                                        "Évolution des ventes",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        softWrap = true
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("7j", "30j", "90j", "1an").forEach { p ->
                                        val sel = chartPeriod == p
                                        Surface(
                                            onClick = { chartPeriod = p },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (sel) RoyalBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .widthIn(min = 48.dp)
                                                .testTag("chart_period_$p")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            ) {
                                                Text(
                                                    text = p,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            RevenueBarChart(bars = revenueBars)
                        }
                    }
                }
                val donutCard: @Composable (Modifier) -> Unit = { mod ->
                    Card(
                        modifier = mod,
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Répartition par prestation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            ServiceDonutChart(
                                slices = donutSlices,
                                centerLabel = if (orders.isEmpty()) "100%" else "${orders.sumOf { it.itemCount }} pcs"
                            )
                        }
                    }
                }

                if (isWideCharts) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        revenueChartCard(Modifier.weight(1.35f))
                        donutCard(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        revenueChartCard(Modifier.fillMaxWidth())
                        donutCard(Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // 5. Pastel Quick Access Grid (Accès rapide — strictly filtered by active role permissions)
        item {
            data class QuickItem(val route: String, val label: String, val icon: ImageVector, val fg: Color, val bg: Color)
            val allQuick = listOf(
                QuickItem("new_order", "Nouvelle Cmd", Icons.Rounded.AddShoppingCart, RoyalBlue, PastelBlueTint),
                QuickItem("workshop_tracker", "Suivi Atelier", Icons.Rounded.LocalLaundryService, EmeraldSuccess, PastelGreenTint),
                QuickItem("pos_cash", "Caisse POS", Icons.Rounded.PointOfSale, RoyalBlue, PastelBlueTint),
                QuickItem("stocks", "Stocks", Icons.Rounded.Inventory2, AmberWarning, PastelAmberTint),
                QuickItem("finance", "SYSCOHADA", Icons.Rounded.AccountBalance, RoyalBlue, PastelBlueTint),
                QuickItem("reports", "Rapports", Icons.Rounded.BarChart, EmeraldSuccess, PastelGreenTint),
                QuickItem("roles_rbac", "Rôles & Droits", Icons.Rounded.Security, VioletAccent, PastelVioletTint),
                QuickItem("ai_assistant", "IA Gemini", Icons.Rounded.AutoAwesome, MidnightNavy, SoftBlueContainer)
            ).filter { roleSession.canAccessRoute(it.route) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "⚡ Accès rapide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val cols = if (maxWidth < 600.dp) 1 else 2
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            allQuick.chunked(cols).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        PastelQuickTile(
                                            label = item.label,
                                            icon = item.icon,
                                            fg = item.fg,
                                            bg = item.bg,
                                            horizontalLayout = cols == 1,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("quick_tile_${item.route}"),
                                            onClick = {
                                                if (item.route == "new_order") onNewOrder()
                                                else onNavigateModule(item.route)
                                            }
                                        )
                                    }
                                    repeat(cols - rowItems.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Dark Navy + Gold Executive Banner + Subscription Card (matching mockup bottom-right)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightNavy)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(MidnightNavy, DeepNavySurface, Color(0xFF143C9F))))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Pilotez toutes vos activités avec GESTION PRO AFRICA",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("✓ Multi-agences isolées", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD5E1FF))
                            Text("✓ RBAC granulaire", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD5E1FF))
                            Text("✓ IA Gemini", style = MaterialTheme.typography.labelSmall, color = AfricaGoldBright)
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (roleSession.has(AccessPermission.CREATE_ORDER)) {
                                Button(
                                    onClick = onNewOrder,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("dashboard_new_order_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AfricaGold,
                                        contentColor = MidnightNavy
                                    )
                                ) {
                                    Text("Nouvelle commande", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (roleSession.has(AccessPermission.USE_AI_ASSISTANT)) {
                                Button(
                                    onClick = { onNavigateModule("ai_assistant") },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("dashboard_ai_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.16f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("IA Gemini")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subscription Status Card (Directeur only) + Besoin d'aide support card (matching mockup bottom-right)
        if (roleSession.has(AccessPermission.MANAGE_SAAS_ADMIN)) {
            item {
                val plan = SubscriptionPlan.fromRaw(company.subscriptionPlan)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "👑 Votre abonnement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                softWrap = true
                            )
                            Surface(shape = RoundedCornerShape(50), color = EmeraldContainer) {
                                Text(
                                    text = "Actif",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "Forfait : ${plan.labelFr} · ${formatMoney(plan.monthlyFcfa, currency)} / mois",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            softWrap = true
                        )
                        LinearProgressIndicator(
                            progress = { 0.72f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50)),
                            color = RoyalBlue,
                            trackColor = SoftBlueContainer
                        )
                        Button(
                            onClick = { onNavigateModule("saas_admin") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                        ) {
                            Text("Gérer l'abonnement & agences", fontWeight = FontWeight.Bold, softWrap = true)
                        }
                    }
                }
            }
        }

        // Compact Support Card matching mockup ("Besoin d'aide ? Notre équipe est disponible 24h/7j")
        item {
            Card(
                onClick = { onNavigateModule("help") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("dashboard_support_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PastelBlueTint),
                border = BorderStroke(1.dp, PastelBlueBorder)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(RoyalBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.HelpOutline, contentDescription = null, tint = RoyalBlue)
                        }
                        Column {
                            Text(
                                text = "Besoin d'aide ?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MidnightNavy,
                                softWrap = true
                            )
                            Text(
                                text = "Guide multi-agences, RBAC & support 24h/7j",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true
                            )
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = RoyalBlue) {
                        Text(
                            text = "Support",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 7. Recent Orders / Activities
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Activités & Commandes récentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    softWrap = true
                )
                TextButton(
                    onClick = { onNavigateModule("orders") },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Voir tout →", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (orders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Checkroom,
                    title = "Aucune commande dans ce périmètre",
                    subtitle = "Appuyez sur « Nouvelle commande » pour enregistrer vos premiers vêtements."
                )
            }
        } else {
            items(orders.take(5), key = { it.id }) { order ->
                OrderSummaryCard(
                    order = order,
                    currency = currency,
                    showFinancials = true,
                    onClick = { onOpenOrder(order.id) }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CashierIsolatedDashboard(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    todayLabel: String,
    orders: List<PressingOrder>,
    clients: List<ClientRecord>,
    transactions: List<AccountingEntry>,
    onNewOrder: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val currency = company.currency
    val collected = transactions.filter { it.entryType == "REVENUE" }.sumOf { it.amount }
    val pendingPayment = orders.filter { it.paymentStatusEnum() != PaymentStatus.PAID && it.status != "CANCELLED" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardGreetingHeader(
                company = company,
                roleSession = roleSession,
                todayLabel = todayLabel,
                subtitle = "Poste Caisse & Réception isolé · Encaissements comptoir uniquement."
            )
        }
        item {
            AdaptiveTwoKpiPair(
                first = { mod ->
                    SparklineKpiCard(
                        title = "Encaissé comptoir",
                        value = formatMoney(collected, currency),
                        trendLabel = "${transactions.size} op.",
                        icon = Icons.Rounded.PointOfSale,
                        accentColor = RoyalBlue,
                        pastelTint = PastelBlueTint,
                        pastelBorder = PastelBlueBorder,
                        sparklinePoints = listOf(0.3f, 0.5f, 0.45f, 0.75f, 0.9f),
                        modifier = mod,
                        onClick = { onNavigateModule("pos_cash") }
                    )
                },
                second = { mod ->
                    SparklineKpiCard(
                        title = "À encaisser",
                        value = "${pendingPayment.size}",
                        trendLabel = formatMoney(pendingPayment.sumOf { it.remainingAmount }, currency),
                        icon = Icons.Rounded.ReceiptLong,
                        accentColor = AmberWarning,
                        pastelTint = PastelAmberTint,
                        pastelBorder = PastelAmberBorder,
                        sparklinePoints = listOf(0.6f, 0.5f, 0.7f, 0.4f, 0.55f),
                        modifier = mod,
                        onClick = { onNavigateModule("invoices") }
                    )
                }
            )
        }
        item {
            data class CashierTile(val label: String, val icon: ImageVector, val fg: Color, val bg: Color, val action: () -> Unit)
            val tiles = listOf(
                CashierTile("Nouvelle Cmd", Icons.Rounded.AddShoppingCart, RoyalBlue, PastelBlueTint, onNewOrder),
                CashierTile("Caisse POS", Icons.Rounded.PointOfSale, EmeraldSuccess, PastelGreenTint) { onNavigateModule("pos_cash") },
                CashierTile("Factures", Icons.Rounded.ReceiptLong, VioletAccent, PastelVioletTint) { onNavigateModule("invoices") },
                CashierTile("Clients (${clients.size})", Icons.Rounded.People, AmberWarning, PastelAmberTint) { onNavigateModule("clients") }
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cols = if (maxWidth < 600.dp) 1 else 2
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tiles.chunked(cols).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { t ->
                                PastelQuickTile(
                                    label = t.label,
                                    icon = t.icon,
                                    fg = t.fg,
                                    bg = t.bg,
                                    horizontalLayout = cols == 1,
                                    modifier = Modifier.weight(1f),
                                    onClick = t.action
                                )
                            }
                            repeat(cols - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        item {
            Text("Commandes en attente de règlement (${pendingPayment.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (pendingPayment.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.CheckCircle,
                    title = "Caisse à jour",
                    subtitle = "Toutes les commandes actives de votre agence sont réglées."
                )
            }
        } else {
            items(pendingPayment.take(8), key = { it.id }) { ord ->
                OrderSummaryCard(order = ord, currency = currency, showFinancials = true) { onOpenOrder(ord.id) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun WorkshopOperatorIsolatedDashboard(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    todayLabel: String,
    orders: List<PressingOrder>,
    stocks: List<StockItem>,
    onOpenOrder: (String) -> Unit,
    onQuickAdvanceStatus: (PressingOrder, OrderStatus) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val workshopQueue = orders.filter {
        it.orderStatusEnum() in listOf(
            OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.WASHING,
            OrderStatus.DRYING, OrderStatus.IRONING, OrderStatus.FINISHING
        )
    }
    val lowStocks = stocks.count { it.isLowStock() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardGreetingHeader(
                company = company,
                roleSession = roleSession,
                todayLabel = todayLabel,
                subtitle = "Atelier Production isolé · Données financières masquées par sécurité."
            )
        }
        item {
            AdaptiveTwoKpiPair(
                first = { mod ->
                    SparklineKpiCard(
                        title = "File Atelier",
                        value = "${workshopQueue.size}",
                        trendLabel = "${workshopQueue.sumOf { it.itemCount }} vêtements",
                        icon = Icons.Rounded.Checkroom,
                        accentColor = RoyalBlue,
                        pastelTint = PastelBlueTint,
                        pastelBorder = PastelBlueBorder,
                        sparklinePoints = listOf(0.4f, 0.6f, 0.55f, 0.8f, 0.72f),
                        modifier = mod,
                        onClick = { onNavigateModule("workshop_tracker") }
                    )
                },
                second = { mod ->
                    SparklineKpiCard(
                        title = "Stocks & Machines",
                        value = "${stocks.size}",
                        trendLabel = if (lowStocks > 0) "$lowStocks alertes" else "OK",
                        icon = Icons.Rounded.Inventory2,
                        accentColor = if (lowStocks > 0) CrimsonDanger else EmeraldSuccess,
                        pastelTint = if (lowStocks > 0) PastelRoseTint else PastelGreenTint,
                        pastelBorder = if (lowStocks > 0) PastelRoseBorder else PastelGreenBorder,
                        sparklinePoints = listOf(0.6f, 0.5f, 0.55f, 0.45f, 0.65f),
                        modifier = mod,
                        onClick = { onNavigateModule("stocks") }
                    )
                }
            )
        }
        item {
            Button(
                onClick = { onNavigateModule("workshop_tracker") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("operator_open_workshop_tracker_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MidnightNavy)
            ) {
                Icon(Icons.Rounded.LocalLaundryService, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ouvrir le Suivi Vêtements & Scanner QR", fontWeight = FontWeight.Bold, softWrap = true)
            }
        }
        item {
            Text("File de traitement active (${workshopQueue.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (workshopQueue.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.CheckCircle,
                    title = "Atelier à jour",
                    subtitle = "Aucun vêtement en attente de lavage, séchage ou repassage."
                )
            }
        } else {
            items(workshopQueue.take(10), key = { it.id }) { order ->
                val current = order.orderStatusEnum()
                val nextStatus = when (current) {
                    OrderStatus.PENDING -> OrderStatus.ACCEPTED
                    OrderStatus.ACCEPTED -> OrderStatus.WASHING
                    OrderStatus.WASHING -> OrderStatus.DRYING
                    OrderStatus.DRYING -> OrderStatus.IRONING
                    OrderStatus.IRONING -> OrderStatus.FINISHING
                    OrderStatus.FINISHING -> OrderStatus.QUALITY_CHECK
                    else -> null
                }
                Card(
                    onClick = { onOpenOrder(order.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(order.orderNumber, fontWeight = FontWeight.Bold, softWrap = true)
                                Text(
                                    "${order.itemCount} article(s) · Retrait ${formatShortDate(order.dueDateMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    softWrap = true
                                )
                            }
                            OrderStatusChip(current, order.isLate())
                        }
                        if (nextStatus != null && roleSession.has(AccessPermission.ADVANCE_PRODUCTION)) {
                            Button(
                                onClick = { onQuickAdvanceStatus(order, nextStatus) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                            ) {
                                Text("Passer à : ${nextStatus.labelFr}", fontWeight = FontWeight.Bold, softWrap = true)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun QualityIsolatedDashboard(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    todayLabel: String,
    orders: List<PressingOrder>,
    claims: List<ClaimRecord>,
    onOpenOrder: (String) -> Unit,
    onQuickAdvanceStatus: (PressingOrder, OrderStatus) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val qcQueue = orders.filter { it.orderStatusEnum() in listOf(OrderStatus.FINISHING, OrderStatus.QUALITY_CHECK) }
    val openClaims = claims.filter { it.status != "RESOLVED" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardGreetingHeader(
                company = company,
                roleSession = roleSession,
                todayLabel = todayLabel,
                subtitle = "Espace Contrôle Qualité & SAV isolé · Validation finale obligatoire."
            )
        }
        item {
            AdaptiveTwoKpiPair(
                first = { mod ->
                    SparklineKpiCard(
                        title = "À contrôler",
                        value = "${qcQueue.size}",
                        trendLabel = "Qualité requise",
                        icon = Icons.Rounded.Verified,
                        accentColor = RoyalBlue,
                        pastelTint = PastelBlueTint,
                        pastelBorder = PastelBlueBorder,
                        sparklinePoints = listOf(0.3f, 0.5f, 0.7f, 0.6f, 0.85f),
                        modifier = mod,
                        onClick = { onNavigateModule("workshop_tracker") }
                    )
                },
                second = { mod ->
                    SparklineKpiCard(
                        title = "Réclamations SAV",
                        value = "${openClaims.size}",
                        trendLabel = if (openClaims.isEmpty()) "0 ouverte" else "À traiter",
                        icon = Icons.Rounded.ReportProblem,
                        accentColor = if (openClaims.isEmpty()) EmeraldSuccess else CrimsonDanger,
                        pastelTint = if (openClaims.isEmpty()) PastelGreenTint else PastelRoseTint,
                        pastelBorder = if (openClaims.isEmpty()) PastelGreenBorder else PastelRoseBorder,
                        sparklinePoints = listOf(0.5f, 0.4f, 0.3f, 0.25f, 0.2f),
                        modifier = mod,
                        onClick = { onNavigateModule("claims") }
                    )
                }
            )
        }
        item {
            Button(
                onClick = { onNavigateModule("workshop_tracker") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("quality_open_workshop_tracker_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MidnightNavy)
            ) {
                Icon(Icons.Rounded.LocalLaundryService, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Suivi pièce par pièce & Contrôle QR", fontWeight = FontWeight.Bold, softWrap = true)
            }
        }
        item {
            Text("Vêtements prêts pour contrôle qualité (${qcQueue.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, softWrap = true)
        }
        if (qcQueue.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Verified,
                    title = "Aucun lot en attente de contrôle",
                    subtitle = "Les commandes en finition apparaîtront ici dès que l'atelier les aura terminées."
                )
            }
        } else {
            items(qcQueue, key = { it.id }) { order ->
                Card(
                    onClick = { onOpenOrder(order.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${order.orderNumber} · ${order.itemCount} pcs",
                                fontWeight = FontWeight.Bold,
                                softWrap = true,
                                modifier = Modifier.weight(1f)
                            )
                            OrderStatusChip(order.orderStatusEnum(), order.isLate())
                        }
                        if (roleSession.has(AccessPermission.VALIDATE_QUALITY)) {
                            Button(
                                onClick = { onQuickAdvanceStatus(order, OrderStatus.READY) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                            ) {
                                Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Certifier Qualité → Prête", fontWeight = FontWeight.Bold, softWrap = true)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriverIsolatedDashboard(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    todayLabel: String,
    orders: List<PressingOrder>,
    onOpenOrder: (String) -> Unit,
    onConfirmDelivery: (PressingOrder) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val context = LocalContext.current
    val activeDeliveries = orders.filter { it.orderStatusEnum() != OrderStatus.DELIVERED && it.orderStatusEnum() != OrderStatus.CANCELLED }
    val completedCount = orders.count { it.orderStatusEnum() == OrderStatus.DELIVERED }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardGreetingHeader(
                company = company,
                roleSession = roleSession,
                todayLabel = todayLabel,
                subtitle = "Espace Livreur GPS isolé · Uniquement vos tournées assignées."
            )
        }
        item {
            AdaptiveTwoKpiPair(
                first = { mod ->
                    SparklineKpiCard(
                        title = "Tournées actives",
                        value = "${activeDeliveries.size}",
                        trendLabel = "En cours",
                        icon = Icons.Rounded.LocalShipping,
                        accentColor = RoyalBlue,
                        pastelTint = PastelBlueTint,
                        pastelBorder = PastelBlueBorder,
                        sparklinePoints = listOf(0.4f, 0.6f, 0.5f, 0.75f, 0.8f),
                        modifier = mod,
                        onClick = { onNavigateModule("deliveries") }
                    )
                },
                second = { mod ->
                    SparklineKpiCard(
                        title = "Livrées",
                        value = "$completedCount",
                        trendLabel = "Confirmées",
                        icon = Icons.Rounded.CheckCircle,
                        accentColor = EmeraldSuccess,
                        pastelTint = PastelGreenTint,
                        pastelBorder = PastelGreenBorder,
                        sparklinePoints = listOf(0.2f, 0.4f, 0.6f, 0.75f, 0.95f),
                        modifier = mod
                    )
                }
            )
        }
        item {
            MapPreviewCanvas(
                centerTitle = "${company.city} · ${roleSession.isolatedAgency ?: company.commune}",
                pinLabels = activeDeliveries.map { it.deliveryZone.ifBlank { it.clientAddress.ifBlank { company.commune } } }
            )
        }
        item {
            Text("Mes livraisons assignées (${activeDeliveries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, softWrap = true)
        }
        if (activeDeliveries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.LocalShipping,
                    title = "Aucune tournée assignée",
                    subtitle = "Vos prochaines collectes et livraisons apparaîtront automatiquement ici."
                )
            }
        } else {
            items(activeDeliveries, key = { it.id }) { order ->
                Card(
                    onClick = { onOpenOrder(order.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${order.orderNumber} · ${order.clientName}", fontWeight = FontWeight.Bold, softWrap = true)
                                Text(
                                    order.deliveryZone.ifBlank { order.clientAddress.ifBlank { company.commune } },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    softWrap = true
                                )
                            }
                            OrderStatusChip(order.orderStatusEnum(), order.isLate())
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (order.clientPhone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { dialPhoneIntent(context, order.clientPhone) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Appeler")
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    openMapIntent(
                                        context,
                                        company.latitude,
                                        company.longitude,
                                        "${order.clientName} - ${order.deliveryZone.ifBlank { company.city }}"
                                    )
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Itinéraire GPS")
                            }
                        }
                        if (roleSession.has(AccessPermission.DISPATCH_DELIVERIES)) {
                            Button(
                                onClick = { onConfirmDelivery(order) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                            ) {
                                Text("Confirmer la livraison remise", fontWeight = FontWeight.Bold, softWrap = true)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AdaptiveTwoKpiPair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 600.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderSummaryCard(
    order: PressingOrder,
    currency: String,
    showFinancials: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${order.orderNumber} · ${order.clientName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        softWrap = true
                    )
                    Text(
                        text = "${order.itemCount} article(s) · ${order.agencyName} · Retrait ${formatShortDate(order.dueDateMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true
                    )
                }
                OrderStatusChip(order.orderStatusEnum(), order.isLate())
            }
            if (showFinancials) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentStatusChip(order.paymentStatusEnum())
                    Text(
                        text = formatMoney(order.totalAmount, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = RoyalBlue,
                        softWrap = true
                    )
                }
            }
        }
    }
}

@Composable
private fun PastelQuickTile(
    label: String,
    icon: ImageVector,
    fg: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    horizontalLayout: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.16f))
    ) {
        if (horizontalLayout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(fg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MidnightNavy,
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(fg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MidnightNavy,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    softWrap = true
                )
            }
        }
    }
}
