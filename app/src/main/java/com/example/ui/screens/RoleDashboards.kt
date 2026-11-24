package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.DryCleaning
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Iron
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.decodeBase64ToBitmap
import com.example.core.formatMoney
import com.example.core.formatShortDate
import com.example.core.openMapIntent
import com.example.core.openWhatsAppIntent
import com.example.data.AccessPermission
import com.example.data.AccountingEntry
import com.example.data.ActiveRoleSession
import com.example.data.AgencyPerformanceSummary
import com.example.data.ClaimRecord
import com.example.data.CompanyConfig
import com.example.data.GarmentWorkshopStatus
import com.example.data.OrderGarmentItem
import com.example.data.OrderPriority
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.PaymentStatus
import com.example.data.PressingOrder
import com.example.data.RoleDashboardAnalytics
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.PaymentStatusChip
import com.example.ui.components.QrBarcodeScannerDialog
import com.example.ui.components.RoleBadgeChip
import com.example.ui.components.SparklineKpiCard
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BrightAzure
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
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

internal data class DashboardKpiSpec(
    val title: String,
    val value: String,
    val trend: String,
    val icon: ImageVector,
    val accent: Color,
    val pastelBg: Color,
    val pastelBorder: Color,
    val sparkline: List<Float> = listOf(0.3f, 0.48f, 0.42f, 0.7f, 0.65f, 0.9f),
    val onClick: (() -> Unit)? = null
)

@Composable
internal fun ResponsiveKpiGrid(
    items: List<DashboardKpiSpec>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = when {
            maxWidth >= 880.dp -> 4
            maxWidth >= 600.dp -> 3
            maxWidth >= 360.dp -> 2
            else -> 1
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { spec ->
                        SparklineKpiCard(
                            title = spec.title,
                            value = spec.value,
                            trendLabel = spec.trend,
                            icon = spec.icon,
                            accentColor = spec.accent,
                            pastelTint = spec.pastelBg,
                            pastelBorder = spec.pastelBorder,
                            sparklinePoints = spec.sparkline,
                            onClick = spec.onClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DashboardRoleHeroHeader(
    roleSession: ActiveRoleSession,
    title: String,
    subtitle: String,
    accentGradientEnd: Color = Color(0xFF1E3A8A),
    trailingContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(MidnightNavy, accentGradientEnd)))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RoleBadgeChip(role = roleSession.role)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Session verrouillée",
                                tint = AfricaGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = roleSession.scopeLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                softWrap = true
                            )
                        }
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    softWrap = true
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFDCE6FF),
                    softWrap = true
                )
                if (trailingContent != null) {
                    Spacer(Modifier.height(2.dp))
                    trailingContent()
                }
            }
        }
    }
}

/**
 * Dual-series trend curve showing Revenue (RoyalBlue) vs Net Collections (EmeraldSuccess).
 */
@Composable
internal fun DualCurveRevenueChart(
    labels: List<String>,
    revenueSeries: List<Long>,
    collectedSeries: List<Long>,
    modifier: Modifier = Modifier
) {
    val count = labels.size.coerceAtLeast(2)
    val rev = if (revenueSeries.size == count) revenueSeries else List(count) { 0L }
    val col = if (collectedSeries.size == count) collectedSeries else List(count) { 0L }
    val maxVal = (rev + col).maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(RoyalBlue))
                Text("CA facturé", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(EmeraldSuccess))
                Text("Encaissements nets", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
        ) {
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = size.height * i / gridLines
                drawLine(
                    color = Color(0x2264748B),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            fun drawCurve(values: List<Long>, strokeColor: Color, fillAlpha: Float) {
                val stepX = size.width / (values.size - 1).coerceAtLeast(1)
                val pts = values.mapIndexed { idx, v ->
                    val ratio = (v.toFloat() / maxVal.toFloat()).coerceIn(0.05f, 0.96f)
                    Offset(idx * stepX, size.height * (1f - ratio))
                }
                val linePath = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 1 until pts.size) {
                        val prev = pts[i - 1]
                        val curr = pts[i]
                        val cx = (prev.x + curr.x) / 2f
                        cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                    }
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(pts.last().x, size.height)
                    lineTo(pts.first().x, size.height)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        listOf(strokeColor.copy(alpha = fillAlpha), Color.Transparent)
                    )
                )
                drawPath(
                    path = linePath,
                    color = strokeColor,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )
                pts.forEach { p ->
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = p)
                    drawCircle(color = strokeColor, radius = 2.8.dp.toPx(), center = p)
                }
            }

            drawCurve(rev, RoyalBlue, 0.22f)
            drawCurve(col, EmeraldSuccess, 0.16f)
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { lbl ->
                Text(
                    text = lbl,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun AgencyComparisonChart(
    agencies: List<AgencyPerformanceSummary>,
    currency: String,
    selectedAgency: String?,
    onSelectAgency: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxRev = (agencies.maxOfOrNull { it.revenueFcfa } ?: 1L).coerceAtLeast(1L)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        agencies.forEach { ag ->
            val isSelected = selectedAgency != null && selectedAgency.equals(ag.agencyName, ignoreCase = true)
            val ratio = (ag.revenueFcfa.toFloat() / maxRev.toFloat()).coerceIn(0.06f, 1f)
            Card(
                onClick = { onSelectAgency(if (isSelected) null else ag.agencyName) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) SoftBlueContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = if (isSelected) BorderStroke(1.2.dp, RoyalBlue) else null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ag.agencyName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatMoney(ag.revenueFcfa, currency),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.ExtraBold,
                            color = RoyalBlue
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(RoyalBlue, BrightAzure)))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${ag.orderCount} cmd · ${ag.piecesCount} pièces",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (ag.lateCount > 0) {
                            Text(
                                text = "${ag.lateCount} retard(s)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonDanger
                            )
                        } else {
                            Text(
                                text = "100% à l'heure",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// C) TABLEAU DE BORD CAISSIER / ACCUEIL COMPTOIR
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CashierCounterDashboard(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    orders: List<PressingOrder>,
    transactions: List<AccountingEntry>,
    onNewOrder: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onRecordPayment: (PressingOrder, Long, PaymentMethod) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val context = LocalContext.current
    val currency = company.currency
    val startOfDayMillis = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todayTx = remember(transactions, startOfDayMillis) {
        transactions.filter {
            it.entryType == "REVENUE" &&
                ((it.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= startOfDayMillis)
        }
    }
    val todayCollected = remember(todayTx) { todayTx.sumOf { it.amount } }
    val channelTotals = remember(todayTx) {
        listOf(
            "Espèces" to todayTx.filter { it.paymentMethod == PaymentMethod.ESPECES.name }.sumOf { it.amount },
            "Wave" to todayTx.filter { it.paymentMethod == PaymentMethod.WAVE.name }.sumOf { it.amount },
            "Orange Money" to todayTx.filter { it.paymentMethod == PaymentMethod.ORANGE_MONEY.name }.sumOf { it.amount },
            "MTN MoMo" to todayTx.filter { it.paymentMethod == PaymentMethod.MTN_MOMO.name }.sumOf { it.amount }
        )
    }

    val todayDeposits = remember(orders, startOfDayMillis) {
        orders.count { (it.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= startOfDayMillis }
    }
    val todayPickupsDone = remember(orders) {
        orders.count { it.orderStatusEnum() == OrderStatus.PICKED_UP || it.orderStatusEnum() == OrderStatus.DELIVERED }
    }
    val unpaidOrders = remember(orders) {
        orders.filter { it.status != OrderStatus.CANCELLED.name && it.remainingAmount > 0L }
    }
    val imminentPickups = remember(orders) {
        orders.filter {
            it.orderStatusEnum() in listOf(OrderStatus.READY, OrderStatus.QUALITY_CHECK, OrderStatus.IRONING)
        }.sortedBy { it.dueDateMillis }
    }

    var showTicketScanner by remember { mutableStateOf(false) }
    var showQuickSettleDialog by remember { mutableStateOf(false) }
    var settlingOrder by remember { mutableStateOf<PressingOrder?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_cashier"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardRoleHeroHeader(
                roleSession = roleSession,
                title = "Comptoir Caisse & Accueil",
                subtitle = "Encaissements de votre session du jour · Aucun accès aux marges globales, paie RH ni SYSCOHADA.",
                accentGradientEnd = Color(0xFF065F46)
            )
        }

        // Session KPIs
        item {
            ResponsiveKpiGrid(
                items = listOf(
                    DashboardKpiSpec(
                        title = "Encaissé aujourd'hui",
                        value = formatMoney(todayCollected, currency),
                        trend = "${todayTx.size} encaissement(s)",
                        icon = Icons.Rounded.Payments,
                        accent = EmeraldSuccess,
                        pastelBg = PastelGreenTint,
                        pastelBorder = PastelGreenBorder
                    ),
                    DashboardKpiSpec(
                        title = "Dépôts du jour",
                        value = "$todayDeposits",
                        trend = "Tickets réceptionnés",
                        icon = Icons.Rounded.Checkroom,
                        accent = RoyalBlue,
                        pastelBg = PastelBlueTint,
                        pastelBorder = PastelBlueBorder,
                        onClick = { onNavigateModule("orders") }
                    ),
                    DashboardKpiSpec(
                        title = "Retraits effectués",
                        value = "$todayPickupsDone",
                        trend = "Clients servis",
                        icon = Icons.Rounded.Verified,
                        accent = VioletAccent,
                        pastelBg = PastelVioletTint,
                        pastelBorder = PastelVioletBorder
                    ),
                    DashboardKpiSpec(
                        title = "En attente de règlement",
                        value = "${unpaidOrders.size}",
                        trend = formatMoney(unpaidOrders.sumOf { it.remainingAmount }, currency),
                        icon = Icons.Rounded.Schedule,
                        accent = AfricaGold,
                        pastelBg = PastelAmberTint,
                        pastelBorder = PastelAmberBorder,
                        onClick = { showQuickSettleDialog = true }
                    )
                )
            )
        }

        // Channel Breakdown Card (Espèces, Wave, Orange Money, MTN MoMo)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ventilation de ma caisse du jour par canal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        channelTotals.forEach { (channel, amt) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SoftBlueContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.18f))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(channel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = formatMoney(amt, currency),
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MidnightNavy
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4 Large Priority Tactile Buttons
        item {
            Text("Actions prioritaires comptoir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cols = if (maxWidth >= 600.dp) 2 else 1
                val actions = listOf(
                    Triple("Nouvelle Commande (43 articles + Photo)", Icons.Rounded.AddCircle, RoyalBlue) to onNewOrder,
                    Triple("Scanner Ticket / Retrait Client", Icons.Rounded.QrCodeScanner, MidnightNavy) to { showTicketScanner = true },
                    Triple("Encaisser un Solde Mobile Money", Icons.Rounded.Payments, EmeraldSuccess) to { showQuickSettleDialog = true },
                    Triple("Clôture Z de ma Caisse", Icons.Rounded.PointOfSale, AfricaGold) to { onNavigateModule("settings") }
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    actions.chunked(cols).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { (meta, action) ->
                                val (label, icon, bg) = meta
                                Button(
                                    onClick = action,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp)
                                ) {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        softWrap = true,
                                        maxLines = 2
                                    )
                                }
                            }
                            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // Imminent Pickups with 1-Click WhatsApp Reminder
        item {
            Text(
                text = "Retraits imminents du jour (${imminentPickups.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (imminentPickups.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.CheckCircle,
                    title = "Aucun retrait en attente",
                    subtitle = "Les commandes prêtes ou en finition apparaîtront ici avec rappel WhatsApp en 1 clic."
                )
            }
        } else {
            items(imminentPickups.take(10), key = { it.id }) { order ->
                Card(
                    onClick = { onOpenOrder(order.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${order.orderNumber} · ${order.clientName}", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${order.itemCount} pièce(s) · Retrait prévu ${formatShortDate(order.dueDateMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OrderStatusChip(order.orderStatusEnum(), order.isLate())
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaymentStatusChip(order.paymentStatusEnum())
                            if (order.remainingAmount > 0L) {
                                Surface(shape = RoundedCornerShape(8.dp), color = CrimsonContainer) {
                                    Text(
                                        text = "Reste : ${formatMoney(order.remainingAmount, currency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonDanger,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (order.clientPhone.isNotBlank() && roleSession.has(AccessPermission.SEND_WHATSAPP_NOTIFICATION)) {
                                Button(
                                    onClick = {
                                        val msg = "Bonjour ${order.clientName}, votre commande ${order.orderNumber} chez ${company.companyName} est prête au retrait." +
                                            if (order.remainingAmount > 0L) " Solde restant : ${formatMoney(order.remainingAmount, currency)}." else ""
                                        openWhatsAppIntent(context, order.clientPhone, msg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Rappel WhatsApp", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (order.remainingAmount > 0L) {
                                OutlinedButton(
                                    onClick = { settlingOrder = order },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Encaisser solde", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTicketScanner) {
        QrBarcodeScannerDialog(
            title = "Scanner Ticket / Retrait Client",
            subtitle = "Visez le QR Code du reçu client ou le code-barres de commande",
            onCodeScanned = { code ->
                showTicketScanner = false
                val cleaned = code.trim()
                val extractedToken = com.example.data.QrCodeValidator.extractTrackingToken(cleaned)
                val matched = orders.firstOrNull { ord ->
                    ord.orderNumber.equals(cleaned, ignoreCase = true) ||
                        (extractedToken != null && ord.trackingToken.equals(extractedToken, ignoreCase = true)) ||
                        (ord.trackingToken.isNotBlank() && cleaned.contains(ord.trackingToken, ignoreCase = true)) ||
                        cleaned.contains(ord.orderNumber, ignoreCase = true)
                }
                if (matched != null) onOpenOrder(matched.id) else onNavigateModule("orders")
            },
            onDismiss = { showTicketScanner = false }
        )
    }

    if (showQuickSettleDialog) {
        AlertDialog(
            onDismissRequest = { showQuickSettleDialog = false },
            title = { Text("Encaisser un Solde Mobile Money", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (unpaidOrders.isEmpty()) {
                        Text("Tous les tickets de votre agence sont actuellement soldés.")
                    } else {
                        unpaidOrders.take(12).forEach { ord ->
                            Card(
                                onClick = {
                                    showQuickSettleDialog = false
                                    settlingOrder = ord
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                colors = CardDefaults.cardColors(containerColor = SoftBlueContainer.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${ord.orderNumber} · ${ord.clientName}", fontWeight = FontWeight.Bold)
                                        Text(ord.orderStatusEnum().labelFr, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(
                                        text = formatMoney(ord.remainingAmount, currency),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CrimsonDanger,
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickSettleDialog = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Fermer")
                }
            }
        )
    }

    settlingOrder?.let { ord ->
        var selectedMethod by remember(ord.id) { mutableStateOf(PaymentMethod.WAVE) }
        var amountInput by remember(ord.id) { mutableStateOf(ord.remainingAmount.toString()) }
        var isSubmittingSettle by remember(ord.id) { mutableStateOf(false) }
        val parsedAmount = amountInput.toLongOrNull() ?: 0L
        AlertDialog(
            onDismissRequest = { if (!isSubmittingSettle) settlingOrder = null },
            title = { Text("Solde ${ord.orderNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Client : ${ord.clientName} · Reste dû : ${formatMoney(ord.remainingAmount, currency)}")
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter(Char::isDigit) },
                        label = { Text("Montant encaissé ($currency)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Canal d'encaissement", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            PaymentMethod.WAVE,
                            PaymentMethod.ORANGE_MONEY,
                            PaymentMethod.MTN_MOMO,
                            PaymentMethod.MOOV_MONEY,
                            PaymentMethod.ESPECES
                        ).forEach { m ->
                            FilterChip(
                                selected = selectedMethod == m,
                                onClick = { selectedMethod = m },
                                label = { Text(m.labelFr) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isSubmittingSettle && parsedAmount > 0L) {
                            isSubmittingSettle = true
                            onRecordPayment(ord, parsedAmount, selectedMethod)
                            settlingOrder = null
                        }
                    },
                    enabled = !isSubmittingSettle && parsedAmount > 0L,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Valider l'encaissement")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { settlingOrder = null },
                    enabled = !isSubmittingSettle,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

// ============================================================================
// D) TABLEAU DE BORD OPÉRATEUR ATELIER (ZÉRO DONNÉE FINANCIÈRE)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WorkshopOperatorDashboard(
    roleSession: ActiveRoleSession,
    orders: List<PressingOrder>,
    onOpenOrder: (String) -> Unit,
    onQuickAdvanceStatus: (PressingOrder, OrderStatus) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val now = System.currentTimeMillis()
    val allGarments = remember(orders) {
        orders.filter {
            it.orderStatusEnum() !in listOf(OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.CANCELLED)
        }.flatMap { ord ->
            val parsed = ord.parsedItems()
            if (parsed.isEmpty()) listOf(ord to GarmentWorkshopStatus.RECEIVED)
            else parsed.map { ord to it.workshopStatusEnum() }
        }
    }

    val toSortPieces = remember(allGarments) { allGarments.count { it.second == GarmentWorkshopStatus.RECEIVED } }
    val washingPieces = remember(allGarments) { allGarments.count { it.second == GarmentWorkshopStatus.WASHING } }
    val dryingPieces = remember(allGarments) { allGarments.count { it.second == GarmentWorkshopStatus.DRYING } }
    val ironingPieces = remember(allGarments) { allGarments.count { it.second == GarmentWorkshopStatus.IRONING } }
    val expressOrdersCount = remember(orders) {
        orders.count {
            it.priorityEnum() == OrderPriority.EXPRESS &&
                it.orderStatusEnum() !in listOf(OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.CANCELLED)
        }
    }

    val smartQueue = remember(orders) {
        orders.filter {
            it.orderStatusEnum() in listOf(
                OrderStatus.PENDING,
                OrderStatus.ACCEPTED,
                OrderStatus.WASHING,
                OrderStatus.DRYING,
                OrderStatus.IRONING,
                OrderStatus.FINISHING
            )
        }.sortedWith(
            compareBy<PressingOrder> { if (it.dueDateMillis > 0L) it.dueDateMillis else Long.MAX_VALUE }
                .thenByDescending { it.priorityEnum() == OrderPriority.EXPRESS }
        )
    }

    var showLabelScanner by remember { mutableStateOf(false) }
    var inspectingOrderPhotos by remember { mutableStateOf<PressingOrder?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_operator"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardRoleHeroHeader(
                roleSession = roleSession,
                title = "Poste Production Atelier",
                subtitle = "File triée par délai promis · Zéro donnée financière affichée (conformité stricte atelier).",
                accentGradientEnd = Color(0xFF92400E)
            )
        }

        // Production KPIs (counts only — zero FCFA)
        item {
            ResponsiveKpiGrid(
                items = listOf(
                    DashboardKpiSpec(
                        title = "À trier (Réceptionnées)",
                        value = "$toSortPieces pcs",
                        trend = "Prêtes pour lavage",
                        icon = Icons.Rounded.Checkroom,
                        accent = RoyalBlue,
                        pastelBg = PastelBlueTint,
                        pastelBorder = PastelBlueBorder
                    ),
                    DashboardKpiSpec(
                        title = "En lavage",
                        value = "$washingPieces pcs",
                        trend = "Cycle tambour actif",
                        icon = Icons.Rounded.LocalLaundryService,
                        accent = BrightAzure,
                        pastelBg = PastelBlueTint,
                        pastelBorder = PastelBlueBorder
                    ),
                    DashboardKpiSpec(
                        title = "En séchage",
                        value = "$dryingPieces pcs",
                        trend = "Avant repassage",
                        icon = Icons.Rounded.DryCleaning,
                        accent = VioletAccent,
                        pastelBg = PastelVioletTint,
                        pastelBorder = PastelVioletBorder
                    ),
                    DashboardKpiSpec(
                        title = "En repassage",
                        value = "$ironingPieces pcs",
                        trend = "Finition vapeur",
                        icon = Icons.Rounded.Iron,
                        accent = EmeraldSuccess,
                        pastelBg = PastelGreenTint,
                        pastelBorder = PastelGreenBorder
                    ),
                    DashboardKpiSpec(
                        title = "Commandes Express",
                        value = "$expressOrdersCount",
                        trend = "Priorité 6h / 24h",
                        icon = Icons.Rounded.FlashOn,
                        accent = CrimsonDanger,
                        pastelBg = PastelAmberTint,
                        pastelBorder = PastelAmberBorder
                    )
                )
            )
        }

        // Quick Scan Action + Workshop Tracker link
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showLabelScanner = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scanner Étiquette Vêtement (1/N)", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onNavigateModule("workshop_tracker") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Icon(Icons.Rounded.Build, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Suivi pièce par pièce", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text(
                text = "File d'attente intelligente par délai promis (${smartQueue.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (smartQueue.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.CheckCircle,
                    title = "Atelier entièrement à jour",
                    subtitle = "Aucune pièce en attente de lavage, séchage ou repassage dans votre agence."
                )
            }
        } else {
            items(smartQueue, key = { it.id }) { order ->
                val remainingMs = if (order.dueDateMillis > 0L) order.dueDateMillis - now else Long.MAX_VALUE
                val (urgencyLabel, urgencyBg, urgencyFg) = when {
                    remainingMs <= 0L || order.isLate(now) -> Triple("URGENT / EN RETARD", CrimsonContainer, CrimsonDanger)
                    remainingMs <= 3 * 3_600_000L || order.priorityEnum() == OrderPriority.EXPRESS ->
                        Triple("< 3h / EXPRESS", AmberContainer, Color(0xFFB45309))
                    else -> Triple("À TEMPS", EmeraldContainer, Color(0xFF047857))
                }
                val nextStatus = when (order.orderStatusEnum()) {
                    OrderStatus.PENDING, OrderStatus.ACCEPTED -> OrderStatus.WASHING
                    OrderStatus.WASHING -> OrderStatus.DRYING
                    OrderStatus.DRYING -> OrderStatus.IRONING
                    OrderStatus.IRONING -> OrderStatus.QUALITY_CHECK
                    OrderStatus.FINISHING -> OrderStatus.QUALITY_CHECK
                    else -> null
                }
                val parsedItems = remember(order.items) { order.parsedItems() }
                val hasPhotosOrNotes = parsedItems.any { it.photoBase64.isNotBlank() || it.defects.isNotBlank() }

                Card(
                    onClick = { onOpenOrder(order.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, urgencyFg.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${order.orderNumber} · ${order.itemCount} pièce(s)", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Client : ${order.clientName} · Promis ${formatShortDate(order.dueDateMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(shape = RoundedCornerShape(50), color = urgencyBg) {
                                Text(
                                    text = urgencyLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = urgencyFg,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (order.observations.isNotBlank()) {
                            Text(
                                text = "Notes taches / consignes : ${order.observations}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OrderStatusChip(order.orderStatusEnum(), order.isLate(now))
                            if (hasPhotosOrNotes && roleSession.has(AccessPermission.VIEW_RECEPTION_PHOTOS)) {
                                OutlinedButton(
                                    onClick = { inspectingOrderPhotos = order },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Photos & taches", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (nextStatus != null && roleSession.has(AccessPermission.ADVANCE_PRODUCTION)) {
                                Button(
                                    onClick = { onQuickAdvanceStatus(order, nextStatus) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Passer → ${nextStatus.labelFr}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLabelScanner) {
        QrBarcodeScannerDialog(
            title = "Scanner Étiquette Vêtement (1/N)",
            subtitle = "Scannez le code-barres / QR de la pièce pour ouvrir ou avancer son étape",
            onCodeScanned = { code ->
                showLabelScanner = false
                val matched = orders.firstOrNull { ord ->
                    ord.orderNumber.equals(code.trim(), ignoreCase = true) ||
                        code.contains(ord.orderNumber, ignoreCase = true) ||
                        ord.parsedItems().any { it.barcode.equals(code.trim(), ignoreCase = true) }
                }
                if (matched != null) onOpenOrder(matched.id) else onNavigateModule("workshop_tracker")
            },
            onDismiss = { showLabelScanner = false }
        )
    }

    inspectingOrderPhotos?.let { ord ->
        ReceptionPhotosInspectionDialog(
            order = ord,
            onDismiss = { inspectingOrderPhotos = null }
        )
    }
}

// ============================================================================
// E) TABLEAU DE BORD CONTRÔLE QUALITÉ & SAV (ZÉRO DONNÉE FINANCIÈRE)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QualityControlDashboard(
    roleSession: ActiveRoleSession,
    orders: List<PressingOrder>,
    claims: List<ClaimRecord>,
    onOpenOrder: (String) -> Unit,
    onQuickAdvanceStatus: (PressingOrder, OrderStatus) -> Unit,
    onReturnToRework: (PressingOrder, GarmentWorkshopStatus, String) -> Unit,
    onNavigateModule: (String) -> Unit
) {
    val waitingQcOrders = remember(orders) {
        orders.filter {
            it.orderStatusEnum() in listOf(OrderStatus.QUALITY_CHECK, OrderStatus.FINISHING, OrderStatus.IRONING)
        }.sortedBy { it.dueDateMillis }
    }
    val pendingPieces = remember(waitingQcOrders) { waitingQcOrders.sumOf { it.itemCount.coerceAtLeast(1L) } }
    val conformityPct = remember(orders) { RoleDashboardAnalytics.computeQcConformityPercent(orders) }
    val reworkCount = remember(orders) {
        orders.count { ord -> ord.statusHistory.any { it.contains("Reprise", ignoreCase = true) } }
    }
    val openClaims = remember(claims) { claims.filter { it.status != "RESOLVED" } }

    var reworkTargetOrder by remember { mutableStateOf<PressingOrder?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_quality"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardRoleHeroHeader(
                roleSession = roleSession,
                title = "Contrôle Qualité & SAV",
                subtitle = "Comparateur visuel avant/après traitement · Zéro donnée financière exposée.",
                accentGradientEnd = Color(0xFF0369A1)
            )
        }

        item {
            ResponsiveKpiGrid(
                items = listOf(
                    DashboardKpiSpec(
                        title = "Pièces en attente contrôle",
                        value = "$pendingPieces pcs",
                        trend = "${waitingQcOrders.size} commande(s)",
                        icon = Icons.Rounded.Verified,
                        accent = RoyalBlue,
                        pastelBg = PastelBlueTint,
                        pastelBorder = PastelBlueBorder
                    ),
                    DashboardKpiSpec(
                        title = "Taux de conformité",
                        value = "$conformityPct%",
                        trend = "Qualité du jour",
                        icon = Icons.Rounded.CheckCircle,
                        accent = EmeraldSuccess,
                        pastelBg = PastelGreenTint,
                        pastelBorder = PastelGreenBorder
                    ),
                    DashboardKpiSpec(
                        title = "Renvoyées en reprise",
                        value = "$reworkCount",
                        trend = "Lavage / Repassage",
                        icon = Icons.Rounded.Replay,
                        accent = AfricaGold,
                        pastelBg = PastelAmberTint,
                        pastelBorder = PastelAmberBorder
                    ),
                    DashboardKpiSpec(
                        title = "Dossiers SAV ouverts",
                        value = "${openClaims.size}",
                        trend = "Réclamations actives",
                        icon = Icons.Rounded.ReportProblem,
                        accent = CrimsonDanger,
                        pastelBg = PastelVioletTint,
                        pastelBorder = PastelVioletBorder,
                        onClick = { onNavigateModule("claims") }
                    )
                )
            )
        }

        item {
            Text(
                text = "Comparateur visuel Avant / Après (${waitingQcOrders.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (waitingQcOrders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Verified,
                    title = "Aucune pièce en attente d'inspection",
                    subtitle = "Toutes les commandes contrôlées ont été certifiées Prêtes pour le client."
                )
            }
        } else {
            items(waitingQcOrders, key = { it.id }) { order ->
                QualityComparatorOrderCard(
                    order = order,
                    canValidate = roleSession.has(AccessPermission.VALIDATE_QUALITY),
                    canRework = roleSession.canReworkGarment,
                    onOpenOrder = { onOpenOrder(order.id) },
                    onApproveReady = { onQuickAdvanceStatus(order, OrderStatus.READY) },
                    onRequestRework = { reworkTargetOrder = order }
                )
            }
        }
    }

    reworkTargetOrder?.let { ord ->
        var targetStage by remember(ord.id) { mutableStateOf(GarmentWorkshopStatus.WASHING) }
        var reason by remember(ord.id) { mutableStateOf("Tache résiduelle détectée au contrôle final") }
        AlertDialog(
            onDismissRequest = { reworkTargetOrder = null },
            title = { Text("Renvoyer en reprise · ${ord.orderNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choisissez l'étape atelier de reprise :", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = targetStage == GarmentWorkshopStatus.WASHING,
                            onClick = { targetStage = GarmentWorkshopStatus.WASHING },
                            label = { Text("Lavage / Détachage") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                        FilterChip(
                            selected = targetStage == GarmentWorkshopStatus.IRONING,
                            onClick = { targetStage = GarmentWorkshopStatus.IRONING },
                            label = { Text("Repassage vapeur") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Motif de non-conformité") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReturnToRework(ord, targetStage, reason)
                        reworkTargetOrder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Confirmer la reprise")
                }
            },
            dismissButton = {
                TextButton(onClick = { reworkTargetOrder = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QualityComparatorOrderCard(
    order: PressingOrder,
    canValidate: Boolean,
    canRework: Boolean,
    onOpenOrder: () -> Unit,
    onApproveReady: () -> Unit,
    onRequestRework: () -> Unit
) {
    val parsedItems = remember(order.items) { order.parsedItems() }
    val photoGarment = remember(parsedItems) { parsedItems.firstOrNull { it.photoBase64.isNotBlank() } }
    val receptionBitmap = remember(photoGarment?.photoBase64) {
        photoGarment?.photoBase64?.takeIf { it.isNotBlank() }?.let { decodeBase64ToBitmap(it) }
    }
    var stainsRemoved by remember(order.id) { mutableStateOf(true) }
    var ironingClean by remember(order.id) { mutableStateOf(true) }
    var accessoriesIntact by remember(order.id) { mutableStateOf(true) }

    Card(
        onClick = onOpenOrder,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${order.orderNumber} · ${order.clientName}", fontWeight = FontWeight.Bold)
                    Text(
                        text = "${order.itemCount} pièce(s) · Échéance ${formatShortDate(order.dueDateMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OrderStatusChip(order.orderStatusEnum(), order.isLate())
            }

            // Before / After visual comparator row (stacks on narrow phones)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val sideBySide = maxWidth >= 460.dp
                val beforeBox: @Composable (Modifier) -> Unit = { m ->
                    Surface(
                        modifier = m,
                        shape = RoundedCornerShape(12.dp),
                        color = SoftBlueContainer.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("AVANT · Photo réception", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RoyalBlue)
                            if (receptionBitmap != null) {
                                Image(
                                    bitmap = receptionBitmap.asImageBitmap(),
                                    contentDescription = "Photo réception ${photoGarment?.garmentName}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Text(
                                    text = "Aucune photo réception attachée.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val defectSummary = parsedItems.mapNotNull { it.defects.takeIf { d -> d.isNotBlank() } }.joinToString(" · ")
                            Text(
                                text = if (defectSummary.isNotBlank()) "Défauts signalés : $defectSummary" else "Défauts signalés : Aucun",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
                val afterBox: @Composable (Modifier) -> Unit = { m ->
                    Surface(
                        modifier = m,
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldContainer.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("APRÈS · Checklist Qualité", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = stainsRemoved, onCheckedChange = { stainsRemoved = it })
                                Text("Taches / odeurs traitées", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = ironingClean, onCheckedChange = { ironingClean = it })
                                Text("Repassage & plis impeccables", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = accessoriesIntact, onCheckedChange = { accessoriesIntact = it })
                                Text("Boutons, cintre & housse OK", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (sideBySide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        beforeBox(Modifier.weight(1f))
                        afterBox(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        beforeBox(Modifier.fillMaxWidth())
                        afterBox(Modifier.fillMaxWidth())
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canValidate) {
                    Button(
                        onClick = onApproveReady,
                        enabled = stainsRemoved && ironingClean && accessoriesIntact,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Valider qualité → Prêt", fontWeight = FontWeight.Bold)
                    }
                }
                if (canRework) {
                    OutlinedButton(
                        onClick = onRequestRework,
                        border = BorderStroke(1.dp, CrimsonDanger),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDanger),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Renvoyer en reprise", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// F) TABLEAU DE BORD LIVREUR GPS (COD UNIQUEMENT SUR COLIS NON SOLDÉS)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DeliveryDriverDashboard(
    company: CompanyConfig,
    roleSession: ActiveRoleSession,
    orders: List<PressingOrder>,
    onOpenOrder: (String) -> Unit,
    onConfirmDeliveryWithProof: (PressingOrder, String, String) -> Unit
) {
    val context = LocalContext.current
    val currency = company.currency

    val pickupRuns = remember(orders) {
        orders.filter {
            it.deliveryRequested && it.orderStatusEnum() in listOf(OrderStatus.PENDING, OrderStatus.ACCEPTED)
        }
    }
    val readyToDeliver = remember(orders) {
        orders.filter { it.orderStatusEnum() == OrderStatus.READY }
    }
    val deliveredToday = remember(orders) {
        orders.count { it.orderStatusEnum() == OrderStatus.DELIVERED }
    }
    val codTotal = remember(orders) { RoleDashboardAnalytics.computeDriverCodTotal(orders) }

    // Runs sorted by neighborhood / commune
    val activeRuns = remember(orders) {
        orders.filter {
            it.orderStatusEnum() !in listOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED)
        }.sortedBy { (it.deliveryZone.ifBlank { it.clientAddress }).lowercase() }
    }

    var proofOrder by remember { mutableStateOf<PressingOrder?>(null) }
    var showQrDeliveryScanner by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_driver"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardRoleHeroHeader(
                roleSession = roleSession,
                title = "Tournée Livreur GPS",
                subtitle = "Collectes et livraisons classées par quartier · Solde COD visible uniquement pour vos colis non soldés.",
                accentGradientEnd = Color(0xFF991B1B)
            )
        }

        item {
            ResponsiveKpiGrid(
                items = listOf(
                    DashboardKpiSpec(
                        title = "Colis à récupérer",
                        value = "${pickupRuns.size}",
                        trend = "Collectes domicile",
                        icon = Icons.Rounded.LocalShipping,
                        accent = RoyalBlue,
                        pastelBg = PastelBlueTint,
                        pastelBorder = PastelBlueBorder
                    ),
                    DashboardKpiSpec(
                        title = "Prêtes à livrer",
                        value = "${readyToDeliver.size}",
                        trend = "En attente remise",
                        icon = Icons.Rounded.Checkroom,
                        accent = AfricaGold,
                        pastelBg = PastelAmberTint,
                        pastelBorder = PastelAmberBorder
                    ),
                    DashboardKpiSpec(
                        title = "Livrées aujourd'hui",
                        value = "$deliveredToday",
                        trend = "Remises validées",
                        icon = Icons.Rounded.CheckCircle,
                        accent = EmeraldSuccess,
                        pastelBg = PastelGreenTint,
                        pastelBorder = PastelGreenBorder
                    ),
                    DashboardKpiSpec(
                        title = "Total COD à encaisser",
                        value = formatMoney(codTotal, currency),
                        trend = "Colis non soldés uniquement",
                        icon = Icons.Rounded.Payments,
                        accent = CrimsonDanger,
                        pastelBg = PastelVioletTint,
                        pastelBorder = PastelVioletBorder
                    )
                )
            )
        }

        item {
            Button(
                onClick = { showQrDeliveryScanner = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scanner QR Ticket & Valider une Livraison", fontWeight = FontWeight.Bold)
            }
        }

        item {
            Text(
                text = "Courses ordonnées par quartier / commune (${activeRuns.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (activeRuns.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.LocalShipping,
                    title = "Aucune course active",
                    subtitle = "Vos collectes et livraisons assignées apparaîtront ici par quartier."
                )
            }
        } else {
            items(activeRuns, key = { it.id }) { order ->
                val zoneLabel = order.deliveryZone.ifBlank { order.clientAddress.ifBlank { company.commune } }
                val isCod = order.paymentStatusEnum() != PaymentStatus.PAID && order.remainingAmount > 0L

                Card(
                    onClick = { onOpenOrder(order.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(shape = RoundedCornerShape(8.dp), color = SoftBlueContainer) {
                                    Text(
                                        text = "Quartier : $zoneLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("${order.orderNumber} · ${order.clientName}", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${order.itemCount} pièce(s) · ${order.clientAddress.ifBlank { zoneLabel }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OrderStatusChip(order.orderStatusEnum(), order.isLate())
                        }

                        if (isCod) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CrimsonContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("COD à encaisser à la livraison", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CrimsonDanger)
                                    Text(
                                        text = formatMoney(order.remainingAmount, currency),
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CrimsonDanger
                                    )
                                }
                            }
                        } else {
                            Surface(shape = RoundedCornerShape(8.dp), color = EmeraldContainer) {
                                Text(
                                    text = "Commande déjà soldée · Remise simple",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (order.clientPhone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.clientPhone}"))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Appeler")
                                }
                                OutlinedButton(
                                    onClick = {
                                        openWhatsAppIntent(
                                            context,
                                            order.clientPhone,
                                            "Bonjour ${order.clientName}, votre livreur ${company.companyName} est en route pour votre commande ${order.orderNumber}."
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("WhatsApp")
                                }
                            }
                            OutlinedButton(
                                onClick = { openMapIntent(context, company.latitude, company.longitude, zoneLabel) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Itinéraire GPS")
                            }
                            if (roleSession.has(AccessPermission.MANAGE_DELIVERY_RUNS)) {
                                Button(
                                    onClick = { proofOrder = order },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Valider Livraison", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrDeliveryScanner) {
        QrBarcodeScannerDialog(
            title = "Scanner QR Colis à Livrer",
            subtitle = "Scannez le QR du ticket pour valider la remise au client",
            onCodeScanned = { code ->
                showQrDeliveryScanner = false
                val matched = activeRuns.firstOrNull { ord ->
                    ord.orderNumber.equals(code.trim(), ignoreCase = true) ||
                        code.contains(ord.orderNumber, ignoreCase = true)
                }
                if (matched != null) proofOrder = matched
            },
            onDismiss = { showQrDeliveryScanner = false }
        )
    }

    proofOrder?.let { ord ->
        var recipientName by remember(ord.id) { mutableStateOf(ord.clientName) }
        var proofNote by remember(ord.id) {
            mutableStateOf(
                if (ord.remainingAmount > 0L) "COD ${formatMoney(ord.remainingAmount, currency)} encaissé · Remis en main propre"
                else "Colis remis en main propre avec signature"
            )
        }
        AlertDialog(
            onDismissRequest = { proofOrder = null },
            title = { Text("Preuve de Livraison · ${ord.orderNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Client : ${ord.clientName} · ${ord.itemCount} pièce(s)")
                    if (ord.remainingAmount > 0L) {
                        Text(
                            text = "Montant COD à confirmer : ${formatMoney(ord.remainingAmount, currency)}",
                            fontWeight = FontWeight.Bold,
                            color = CrimsonDanger
                        )
                    }
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("Nom / Signature du réceptionnaire") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = proofNote,
                        onValueChange = { proofNote = it },
                        label = { Text("Note de remise / preuve") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmDeliveryWithProof(ord, recipientName, proofNote)
                        proofOrder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Confirmer remise", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { proofOrder = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun ReceptionPhotosInspectionDialog(
    order: PressingOrder,
    onDismiss: () -> Unit
) {
    val garments = remember(order.items) { order.parsedItems() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photos & Taches · ${order.orderNumber}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                garments.forEachIndexed { idx, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SoftBlueContainer.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${idx + 1}. ${item.garmentName} (×${item.quantity})", fontWeight = FontWeight.Bold)
                            if (item.defects.isNotBlank()) {
                                Text("Défauts / Taches : ${item.defects}", style = MaterialTheme.typography.bodySmall, color = CrimsonDanger)
                            }
                            if (item.aiDiagnosis.isNotBlank()) {
                                Text("Diagnostic IA : ${item.aiDiagnosis}", style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                            }
                            val bmp = remember(item.photoBase64) {
                                item.photoBase64.takeIf { it.isNotBlank() }?.let { decodeBase64ToBitmap(it) }
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Photo ${item.garmentName}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Fermer")
            }
        }
    )
}
