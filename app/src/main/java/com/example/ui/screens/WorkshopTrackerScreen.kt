package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.decodeBase64ToBitmap
import com.example.core.formatShortDate
import com.example.data.AccessPermission
import com.example.data.ActiveRoleSession
import com.example.data.GarmentWorkshopStatus
import com.example.data.OrderGarmentItem
import com.example.data.OrderStatus
import com.example.data.PressingOrder
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.QrBarcodeScannerDialog
import com.example.ui.components.RoleBadgeChip
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelAmberTint
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.PastelGreenTint
import com.example.ui.theme.PastelVioletTint
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.VioletAccent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.data.OrderPriority

enum class WorkshopUrgencyFilter(val labelFr: String) {
    ALL("Tous délais"),
    LATE("En retard"),
    EXPRESS("Express"),
    DUE_24H("< 24h"),
    DUE_48H("Sous 48h")
}

internal fun matchesWorkshopUrgency(
    order: PressingOrder,
    filter: WorkshopUrgencyFilter,
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    val isClosed = order.orderStatusEnum() in listOf(
        OrderStatus.PICKED_UP,
        OrderStatus.DELIVERED,
        OrderStatus.CANCELLED
    )
    val isLate = !isClosed && order.dueDateMillis > 0L && order.dueDateMillis < nowMillis
    val horizon24h = nowMillis + 24L * 3_600_000L
    val horizon48h = nowMillis + 48L * 3_600_000L
    return when (filter) {
        WorkshopUrgencyFilter.ALL -> true
        WorkshopUrgencyFilter.LATE -> isLate
        WorkshopUrgencyFilter.EXPRESS -> order.priority == OrderPriority.EXPRESS.name
        WorkshopUrgencyFilter.DUE_24H -> !isLate && order.dueDateMillis in nowMillis..horizon24h
        WorkshopUrgencyFilter.DUE_48H -> !isLate && order.dueDateMillis in (horizon24h + 1L)..horizon48h
    }
}

internal fun workshopUrgencyRank(
    order: PressingOrder,
    nowMillis: Long = System.currentTimeMillis()
): Int {
    val isClosed = order.orderStatusEnum() in listOf(
        OrderStatus.PICKED_UP,
        OrderStatus.DELIVERED,
        OrderStatus.CANCELLED
    )
    val isLate = !isClosed && order.dueDateMillis > 0L && order.dueDateMillis < nowMillis
    val horizon24h = nowMillis + 24L * 3_600_000L
    val horizon48h = nowMillis + 48L * 3_600_000L
    return when {
        isLate -> 0
        order.priority == OrderPriority.EXPRESS.name -> 1
        order.dueDateMillis in nowMillis..horizon24h -> 2
        order.dueDateMillis in (horizon24h + 1L)..horizon48h -> 3
        else -> 4
    }
}

internal fun formatRemainingDeadlineFr(
    dueDateMillis: Long,
    nowMillis: Long = System.currentTimeMillis()
): String {
    if (dueDateMillis <= 0L) return "Délai non fixé"
    val diffMillis = dueDateMillis - nowMillis
    return when {
        diffMillis < 0L -> {
            val overdueH = ((-diffMillis) / 3_600_000L).coerceAtLeast(1L)
            if (overdueH >= 24L) "Retard ${overdueH / 24L}j" else "Retard ${overdueH}h"
        }
        diffMillis < 3_600_000L -> "Échéance < 1h"
        diffMillis < 24L * 3_600_000L -> "Dans ${diffMillis / 3_600_000L}h"
        diffMillis < 48L * 3_600_000L -> "Demain (${diffMillis / 3_600_000L}h)"
        else -> "Dans ${diffMillis / (24L * 3_600_000L)}j"
    }
}

private data class TrackedGarmentEntry(
    val order: PressingOrder,
    val itemIndex: Int,
    val garment: OrderGarmentItem,
    val stage: GarmentWorkshopStatus
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkshopTrackerScreen(
    orders: List<PressingOrder>,
    activeSession: ActiveRoleSession,
    isBusy: Boolean,
    onUpdateGarmentStatus: (PressingOrder, Int, GarmentWorkshopStatus) -> Unit,
    onOpenOrder: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedStage by remember { mutableStateOf<GarmentWorkshopStatus?>(null) }
    var selectedUrgency by remember { mutableStateOf(WorkshopUrgencyFilter.ALL) }
    var showQrScanner by remember { mutableStateOf(false) }
    var previewEntry by remember { mutableStateOf<TrackedGarmentEntry?>(null) }

    val canAdvanceProduction = activeSession.has(AccessPermission.ADVANCE_PRODUCTION)
    val canValidateQuality = activeSession.has(AccessPermission.VALIDATE_QUALITY)
    val nowMillis = remember(orders) { System.currentTimeMillis() }

    val allTrackedItems = remember(orders) {
        orders
            .filter {
                it.orderStatusEnum() !in listOf(
                    OrderStatus.PICKED_UP,
                    OrderStatus.DELIVERED,
                    OrderStatus.CANCELLED
                )
            }
            .flatMap { order ->
                order.parsedItems().mapIndexed { index, garment ->
                    TrackedGarmentEntry(
                        order = order,
                        itemIndex = index,
                        garment = garment,
                        stage = garment.workshopStatusEnum()
                    )
                }
            }
    }

    val stageCounts = remember(allTrackedItems) {
        GarmentWorkshopStatus.entries.associateWith { st ->
            allTrackedItems.count { it.stage == st }
        }
    }

    val urgencyCounts = remember(allTrackedItems, nowMillis) {
        WorkshopUrgencyFilter.entries.associateWith { u ->
            allTrackedItems.count { matchesWorkshopUrgency(it.order, u, nowMillis) }
        }
    }

    val filteredItems = remember(allTrackedItems, selectedStage, selectedUrgency, searchQuery, nowMillis) {
        allTrackedItems
            .filter { entry ->
                val matchesStage = selectedStage == null || entry.stage == selectedStage
                val matchesUrgency = matchesWorkshopUrgency(entry.order, selectedUrgency, nowMillis)
                val q = searchQuery.trim()
                val matchesQuery = q.isBlank() ||
                    entry.garment.garmentName.contains(q, ignoreCase = true) ||
                    entry.garment.barcode.contains(q, ignoreCase = true) ||
                    entry.order.orderNumber.contains(q, ignoreCase = true) ||
                    entry.order.clientName.contains(q, ignoreCase = true) ||
                    entry.garment.serviceName.contains(q, ignoreCase = true)
                matchesStage && matchesUrgency && matchesQuery
            }
            .sortedWith(
                compareBy<TrackedGarmentEntry>(
                    { workshopUrgencyRank(it.order, nowMillis) },
                    { if (it.order.dueDateMillis > 0L) it.order.dueDateMillis else Long.MAX_VALUE }
                )
            )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Atelier & Suivi Vêtements",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${allTrackedItems.size} article(s) en traitement · Sync Firestore",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD6E4FF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    IconButton(
                        onClick = { showQrScanner = true },
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("workshop_scan_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Scanner une étiquette QR ou code-barres",
                            tint = AfricaGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightNavy,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Role & Entity Isolation Banner (zero financial info)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PastelBlueTint,
                    border = BorderStroke(1.dp, PastelBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Suivi pièce par pièce en atelier",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MidnightNavy,
                                softWrap = true
                            )
                            Text(
                                text = "${activeSession.scopeLabel()} · Montants masqués",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalBlue,
                                softWrap = true
                            )
                        }
                        RoleBadgeChip(role = activeSession.role)
                    }
                }
            }

            // Pipeline KPI Summary Strip (Responsive: 2x2 on phone, 4 columns on tablet+)
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWideKpi = maxWidth >= 600.dp
                    if (isWideKpi) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WorkshopKpiBadge("En lavage", stageCounts[GarmentWorkshopStatus.WASHING] ?: 0, RoyalBlue, PastelBlueTint, Modifier.weight(1f))
                            WorkshopKpiBadge("Séchage", stageCounts[GarmentWorkshopStatus.DRYING] ?: 0, AmberWarning, PastelAmberTint, Modifier.weight(1f))
                            WorkshopKpiBadge("Repassage", stageCounts[GarmentWorkshopStatus.IRONING] ?: 0, VioletAccent, PastelVioletTint, Modifier.weight(1f))
                            WorkshopKpiBadge("Prêt", stageCounts[GarmentWorkshopStatus.READY] ?: 0, EmeraldSuccess, PastelGreenTint, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                WorkshopKpiBadge("En lavage", stageCounts[GarmentWorkshopStatus.WASHING] ?: 0, RoyalBlue, PastelBlueTint, Modifier.weight(1f))
                                WorkshopKpiBadge("Séchage", stageCounts[GarmentWorkshopStatus.DRYING] ?: 0, AmberWarning, PastelAmberTint, Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                WorkshopKpiBadge("Repassage", stageCounts[GarmentWorkshopStatus.IRONING] ?: 0, VioletAccent, PastelVioletTint, Modifier.weight(1f))
                                WorkshopKpiBadge("Prêt", stageCounts[GarmentWorkshopStatus.READY] ?: 0, EmeraldSuccess, PastelGreenTint, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Search + Scan QR row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Code QR, vêtement, N° commande, client...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Effacer")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .testTag("workshop_search_input")
                    )
                    Button(
                        onClick = { showQrScanner = true },
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .testTag("workshop_scan_qr_inline_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scanner")
                    }
                }
            }

            // Quick Filter by Urgency & Promised Delivery Deadline
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Priorité & délai de livraison promis",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MidnightNavy
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(WorkshopUrgencyFilter.entries) { urgency ->
                            val count = urgencyCounts[urgency] ?: 0
                            FilterChip(
                                selected = selectedUrgency == urgency,
                                onClick = {
                                    selectedUrgency = if (selectedUrgency == urgency && urgency != WorkshopUrgencyFilter.ALL) {
                                        WorkshopUrgencyFilter.ALL
                                    } else {
                                        urgency
                                    }
                                },
                                leadingIcon = {
                                    when (urgency) {
                                        WorkshopUrgencyFilter.LATE -> Icon(
                                            Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = CrimsonDanger,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        WorkshopUrgencyFilter.EXPRESS -> Icon(
                                            Icons.Rounded.Bolt,
                                            contentDescription = null,
                                            tint = AfricaGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        WorkshopUrgencyFilter.DUE_24H, WorkshopUrgencyFilter.DUE_48H -> Icon(
                                            Icons.Rounded.Schedule,
                                            contentDescription = null,
                                            tint = RoyalBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        WorkshopUrgencyFilter.ALL -> Unit
                                    }
                                },
                                label = { Text("${urgency.labelFr} ($count)") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("workshop_urgency_${urgency.name}")
                            )
                        }
                    }
                }
            }

            // Stage Filter Chips (6 steps)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Filtrer par étape atelier",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MidnightNavy
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedStage == null,
                                onClick = { selectedStage = null },
                                label = { Text("Toutes (${allTrackedItems.size})") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("workshop_filter_ALL")
                            )
                        }
                        items(GarmentWorkshopStatus.entries) { st ->
                            val count = stageCounts[st] ?: 0
                            FilterChip(
                                selected = selectedStage == st,
                                onClick = { selectedStage = if (selectedStage == st) null else st },
                                label = { Text("${st.labelFr} ($count)") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("workshop_filter_${st.name}")
                            )
                        }
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.LocalLaundryService,
                        title = "Aucun vêtement dans cette sélection",
                        subtitle = if (searchQuery.isNotBlank()) {
                            "Aucun vêtement ne correspond au filtre ou code scanné « $searchQuery »."
                        } else if (selectedUrgency != WorkshopUrgencyFilter.ALL) {
                            "Aucun article dans la catégorie « ${selectedUrgency.labelFr} » pour le moment."
                        } else {
                            "Les vêtements des commandes actives apparaîtront ici pour le suivi en temps réel (Réceptionné → Prêt)."
                        }
                    )
                }
            } else {
                items(
                    items = filteredItems,
                    key = { "${it.order.id}_${it.itemIndex}_${it.garment.barcode}" }
                ) { entry ->
                    WorkshopGarmentCard(
                        entry = entry,
                        nowMillis = nowMillis,
                        isBusy = isBusy,
                        canAdvanceProduction = canAdvanceProduction,
                        canValidateQuality = canValidateQuality,
                        onSelectStage = { target ->
                            onUpdateGarmentStatus(entry.order, entry.itemIndex, target)
                        },
                        onPreviewPhoto = { previewEntry = entry },
                        onOpenOrder = { onOpenOrder(entry.order.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showQrScanner) {
        QrBarcodeScannerDialog(
            title = "Scanner étiquette atelier",
            subtitle = "Scannez le code-barres ou QR d'un vêtement pour afficher son avancement.",
            onCodeScanned = { scanned ->
                searchQuery = scanned
                selectedStage = null
                selectedUrgency = WorkshopUrgencyFilter.ALL
                showQrScanner = false
            },
            onDismiss = { showQrScanner = false }
        )
    }

    previewEntry?.let { entry ->
        val fullBmp = remember(entry.garment.photoBase64) { decodeBase64ToBitmap(entry.garment.photoBase64) }
        if (fullBmp != null) {
            AlertDialog(
                onDismissRequest = { previewEntry = null },
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${entry.garment.quantity}× ${entry.garment.garmentName}", fontWeight = FontWeight.Bold, softWrap = true)
                        Text(
                            "${entry.order.orderNumber} · ${entry.garment.barcode.ifBlank { "Sans étiquette" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Image(
                        bitmap = fullBmp.asImageBitmap(),
                        contentDescription = entry.garment.garmentName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 340.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { previewEntry = null },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) { Text("Fermer") }
                }
            )
        }
    }
}

@Composable
private fun WorkshopKpiBadge(
    label: String,
    count: Int,
    fg: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.22f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.ExtraBold,
                color = fg
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MidnightNavy,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkshopGarmentCard(
    entry: TrackedGarmentEntry,
    nowMillis: Long,
    isBusy: Boolean,
    canAdvanceProduction: Boolean,
    canValidateQuality: Boolean,
    onSelectStage: (GarmentWorkshopStatus) -> Unit,
    onPreviewPhoto: () -> Unit,
    onOpenOrder: () -> Unit
) {
    val garment = entry.garment
    val stage = entry.stage
    val nextStage = stage.nextStage()
    val stageColor = workshopStageColor(stage)
    val isLate = entry.order.isLate()
    val isExpress = entry.order.priority == OrderPriority.EXPRESS.name
    val deadlineLabel = formatRemainingDeadlineFr(entry.order.dueDateMillis, nowMillis)

    val canMoveToNext = nextStage != null && when (nextStage) {
        GarmentWorkshopStatus.READY -> canValidateQuality
        else -> canAdvanceProduction
    }

    Card(
        onClick = onOpenOrder,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workshop_item_card_${entry.order.orderNumber}_${entry.itemIndex}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isLate) 1.5.dp else 1.dp,
            color = when {
                isLate -> CrimsonDanger
                isExpress -> AfricaGold
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                val bmp = remember(garment.photoBase64) { decodeBase64ToBitmap(garment.photoBase64) }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = garment.garmentName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onPreviewPhoto)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Checkroom,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${garment.quantity}× ${garment.garmentName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MidnightNavy,
                            maxLines = 2,
                            softWrap = true
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = stageColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = stage.labelFr,
                                style = MaterialTheme.typography.labelSmall,
                                color = stageColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "${entry.order.orderNumber} · ${entry.order.clientName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalBlue,
                        maxLines = 2,
                        softWrap = true
                    )

                    Text(
                        text = "${garment.serviceName} · Retrait ${formatShortDate(entry.order.dueDateMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        softWrap = true
                    )

                    // Urgency, Deadline & QR Tag Badges
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isExpress) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MidnightNavy
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(12.dp))
                                    Text("EXPRESS", style = MaterialTheme.typography.labelSmall, color = AfricaGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = when {
                                isLate -> CrimsonDanger.copy(alpha = 0.14f)
                                matchesWorkshopUrgency(entry.order, WorkshopUrgencyFilter.DUE_24H, nowMillis) -> AmberContainer
                                else -> PastelBlueTint
                            }
                        ) {
                            Text(
                                text = deadlineLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isLate -> CrimsonDanger
                                    matchesWorkshopUrgency(entry.order, WorkshopUrgencyFilter.DUE_24H, nowMillis) -> Color(0xFF92400E)
                                    else -> RoyalBlue
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (garment.barcode.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCode2,
                                        contentDescription = null,
                                        tint = MidnightNavy,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = garment.barcode,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MidnightNavy
                                    )
                                }
                            }
                        }
                    }

                    if (garment.defects.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Attention : ${garment.defects}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E),
                                softWrap = true,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Portrait-Readable 6-Stage Progress Tracker
            WorkshopSixStageProgressBar(
                currentStage = stage,
                isLate = isLate,
                deadlineLabel = deadlineLabel
            )

            // Direct stage selector chips (RBAC-filtered, 48dp touch target)
            if (canAdvanceProduction || canValidateQuality) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GarmentWorkshopStatus.entries.forEach { st ->
                        val permitted = if (st == GarmentWorkshopStatus.READY) canValidateQuality else canAdvanceProduction
                        if (permitted) {
                            FilterChip(
                                selected = stage == st,
                                enabled = !isBusy,
                                onClick = { if (stage != st) onSelectStage(st) },
                                modifier = Modifier.heightIn(min = 48.dp),
                                label = {
                                    Text(
                                        text = st.labelFr,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Primary 1-tap progression CTA
            if (canMoveToNext && nextStage != null) {
                Button(
                    onClick = { onSelectStage(nextStage) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("workshop_advance_${entry.order.orderNumber}_${entry.itemIndex}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (nextStage == GarmentWorkshopStatus.READY) EmeraldSuccess else RoyalBlue
                    )
                ) {
                    Icon(
                        imageVector = if (nextStage == GarmentWorkshopStatus.READY) Icons.Rounded.Verified else Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (nextStage == GarmentWorkshopStatus.READY) {
                            "Valider Qualité → Prêt"
                        } else {
                            "Étape suivante : ${nextStage.labelFr}"
                        },
                        fontWeight = FontWeight.Bold,
                        softWrap = true
                    )
                }
            } else if (stage == GarmentWorkshopStatus.READY) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Vêtement prêt pour retrait ou livraison",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857),
                            softWrap = true
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkshopSixStageProgressBar(
    currentStage: GarmentWorkshopStatus,
    isLate: Boolean,
    deadlineLabel: String
) {
    val stages = GarmentWorkshopStatus.entries
    val activeColor = workshopStageColor(currentStage)
    val nextStage = currentStage.nextStage()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PastelBlueTint.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, PastelBlueBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workshop_six_stage_bar")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Unabbreviated active step summary header (always readable in portrait)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = buildString {
                        append("Étape ${currentStage.stepIndex + 1}/6 · ${currentStage.labelFr}")
                        if (nextStage != null) append(" → ${nextStage.labelFr}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = activeColor,
                    softWrap = true
                )
                if (isLate) {
                    Text(
                        text = deadlineLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrimsonDanger
                    )
                }
            }

            // 6 segmented progress bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stages.forEach { st ->
                    val completed = st.stepIndex < currentStage.stepIndex
                    val isCurrent = st.stepIndex == currentStage.stepIndex
                    val barColor = when {
                        isCurrent -> activeColor
                        completed -> activeColor.copy(alpha = 0.65f)
                        else -> PastelBlueBorder
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isCurrent) 8.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(barColor)
                    )
                }
            }

            // 6 numbered nodes + responsive labels (compact in portrait < 420dp, full on wider screens)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isPortraitCompact = maxWidth < 420.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    stages.forEach { st ->
                        val completed = st.stepIndex < currentStage.stepIndex
                        val isCurrent = st.stepIndex == currentStage.stepIndex
                        val nodeColor = when {
                            isCurrent -> activeColor
                            completed -> EmeraldSuccess
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 20.dp else 18.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent || completed) nodeColor else MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (completed) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${st.stepIndex + 1}",
                                        fontSize = 10.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = if (isPortraitCompact) st.shortPortraitLabel() else st.labelFr,
                                fontSize = if (isPortraitCompact) 9.5.sp else 11.sp,
                                lineHeight = if (isPortraitCompact) 11.sp else 13.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                color = when {
                                    isCurrent -> activeColor
                                    completed -> MidnightNavy
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                                maxLines = if (isPortraitCompact) 1 else 2,
                                softWrap = !isPortraitCompact,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun GarmentWorkshopStatus.shortPortraitLabel(): String = when (this) {
    GarmentWorkshopStatus.RECEIVED -> "Récep."
    GarmentWorkshopStatus.WASHING -> "Lavage"
    GarmentWorkshopStatus.DRYING -> "Séch."
    GarmentWorkshopStatus.IRONING -> "Repas."
    GarmentWorkshopStatus.QUALITY_CHECK -> "Qualité"
    GarmentWorkshopStatus.READY -> "Prêt"
}

private fun workshopStageColor(stage: GarmentWorkshopStatus): Color = when (stage) {
    GarmentWorkshopStatus.RECEIVED -> RoyalBlue
    GarmentWorkshopStatus.WASHING -> Color(0xFF0284C7)
    GarmentWorkshopStatus.DRYING -> AmberWarning
    GarmentWorkshopStatus.IRONING -> VioletAccent
    GarmentWorkshopStatus.QUALITY_CHECK -> Color(0xFFEA580C)
    GarmentWorkshopStatus.READY -> EmeraldSuccess
}
