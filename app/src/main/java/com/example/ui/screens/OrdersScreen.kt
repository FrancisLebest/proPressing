package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewKanban
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.formatMoney
import com.example.core.formatShortDate
import com.example.data.AccessPermission
import com.example.data.ActiveRoleSession
import com.example.data.CompanyConfig
import com.example.data.OrderStatus
import com.example.data.PaymentStatus
import com.example.data.PressingOrder
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.PaymentStatusChip
import com.example.ui.components.RoleBadgeChip
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.RoyalBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrdersScreen(
    company: CompanyConfig,
    orders: List<PressingOrder>,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onNewOrder: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onQuickAdvanceStatus: (PressingOrder, OrderStatus) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterKey by remember { mutableStateOf("ALL") }
    var isKanbanMode by remember { mutableStateOf(false) }
    val showFinancials = activeSession.canViewFinancialAmounts
    val canCreate = activeSession.has(AccessPermission.CREATE_ORDER)

    val filteredOrders = remember(orders, searchQuery, filterKey) {
        orders.filter { o ->
            val matchesQuery = searchQuery.isBlank() ||
                o.orderNumber.contains(searchQuery, ignoreCase = true) ||
                o.clientName.contains(searchQuery, ignoreCase = true) ||
                o.clientPhone.contains(searchQuery, ignoreCase = true)
            val st = o.orderStatusEnum()
            val matchesFilter = when (filterKey) {
                "ACTIVE" -> st !in listOf(OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.CANCELLED)
                "EXPRESS" -> o.priority == "EXPRESS" && st != OrderStatus.CANCELLED
                "READY" -> st == OrderStatus.READY
                "LATE" -> o.isLate()
                "UNPAID" -> o.paymentStatusEnum() != PaymentStatus.PAID && st != OrderStatus.CANCELLED
                "DONE" -> st == OrderStatus.PICKED_UP || st == OrderStatus.DELIVERED
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Controls + Role Isolation Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                    RoleBadgeChip(role = activeSession.role)
                    Text(
                        text = activeSession.scopeLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MidnightNavy,
                        fontWeight = FontWeight.SemiBold,
                        softWrap = true
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("N° commande, client, tél...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Effacer recherche")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .testTag("orders_search_input")
                )
                if (canCreate) {
                    Button(
                        onClick = onNewOrder,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .testTag("orders_add_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Nouvelle commande")
                    }
                }
            }

            // Full-width scrollable filter chips (48dp touch targets)
            val filterItems = buildList {
                add("ALL" to "Toutes (${orders.size})")
                add("ACTIVE" to "Atelier")
                add("EXPRESS" to "Express")
                add("LATE" to "Retards")
                add("READY" to "Prêtes")
                if (showFinancials) add("UNPAID" to "Impayées")
                add("DONE" to "Clôturées")
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterItems) { (key, label) ->
                    FilterChip(
                        selected = filterKey == key,
                        onClick = { filterKey = key },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(label) }
                    )
                }
            }

            // Result count + View Mode Toggle (Liste / Kanban) without squeezing filters
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${filteredOrders.size} commande(s) affichée(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
                OutlinedButton(
                    onClick = { isKanbanMode = !isKanbanMode },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("orders_kanban_toggle")
                ) {
                    Icon(
                        imageVector = if (isKanbanMode) Icons.Rounded.ViewList else Icons.Rounded.ViewKanban,
                        contentDescription = "Changer de vue",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isKanbanMode) "Vue Liste" else "Vue Kanban")
                }
            }
        }

        if (isKanbanMode) {
            val kanbanColumns = listOf(
                OrderStatus.PENDING to OrderStatus.WASHING,
                OrderStatus.WASHING to OrderStatus.IRONING,
                OrderStatus.IRONING to OrderStatus.QUALITY_CHECK,
                OrderStatus.QUALITY_CHECK to OrderStatus.READY,
                OrderStatus.READY to OrderStatus.PICKED_UP
            )
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val colWidth = (maxWidth * 0.82f).coerceIn(240.dp, 300.dp)
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(kanbanColumns) { (colStatus, nextStatus) ->
                        val colOrders = filteredOrders.filter {
                            when (colStatus) {
                                OrderStatus.PENDING -> it.orderStatusEnum() in listOf(OrderStatus.PENDING, OrderStatus.ACCEPTED)
                                OrderStatus.WASHING -> it.orderStatusEnum() in listOf(OrderStatus.WASHING, OrderStatus.DRYING)
                                OrderStatus.IRONING -> it.orderStatusEnum() in listOf(OrderStatus.IRONING, OrderStatus.FINISHING)
                                else -> it.orderStatusEnum() == colStatus
                            }
                        }
                        Card(
                            modifier = Modifier
                                .width(colWidth)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = colStatus.labelFr,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        softWrap = true
                                    )
                                    Surface(shape = RoundedCornerShape(50), color = RoyalBlue) {
                                        Text(
                                            text = "${colOrders.size}",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (colOrders.isEmpty()) {
                                    Text(
                                        "Aucune commande à cette étape",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(colOrders, key = { it.id }) { order ->
                                            Card(
                                                onClick = { onOpenOrder(order.id) },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = if (order.isLate()) BorderStroke(1.dp, CrimsonDanger) else null
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    FlowRow(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            order.orderNumber,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily)
                                                        )
                                                        if (order.priority == "EXPRESS") {
                                                            Surface(shape = RoundedCornerShape(50), color = MidnightNavy) {
                                                                Text(
                                                                    "EXPRESS",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = AfricaGold,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(order.clientName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(
                                                        if (showFinancials) "${order.itemCount} pcs · ${formatMoney(order.totalAmount, company.currency)}"
                                                        else "${order.itemCount} pièce(s) · ${order.agencyName.ifBlank { "Atelier" }}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = RoyalBlue,
                                                        softWrap = true
                                                    )
                                                    if (activeSession.canTransitionOrderTo(nextStatus)) {
                                                        Spacer(Modifier.height(4.dp))
                                                        OutlinedButton(
                                                            onClick = { onQuickAdvanceStatus(order, nextStatus) },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .heightIn(min = 48.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                        ) {
                                                            Text("Passer → ${nextStatus.labelFr}", style = MaterialTheme.typography.labelSmall, softWrap = true)
                                                            Spacer(Modifier.width(4.dp))
                                                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredOrders.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Checkroom,
                        title = "Aucune commande trouvée",
                        subtitle = "Créez une nouvelle commande pressing ou ajustez votre filtre de recherche."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        val parsedItems = remember(order.items) { order.parsedItems() }
                        val photoCount = remember(parsedItems) { parsedItems.count { it.photoBase64.isNotBlank() } }
                        Card(
                            onClick = { onOpenOrder(order.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                width = if (order.isLate()) 1.5.dp else 1.dp,
                                color = when {
                                    order.isLate() -> CrimsonDanger
                                    order.priority == "EXPRESS" -> AfricaGold
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = order.orderNumber,
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MidnightNavy
                                        )
                                        if (order.priority == "EXPRESS") {
                                            Surface(shape = RoundedCornerShape(50), color = MidnightNavy) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(12.dp))
                                                    Text(
                                                        "EXPRESS",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = AfricaGold,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    OrderStatusChip(order.orderStatusEnum(), order.isLate())
                                }
                                Text(
                                    text = "${order.clientName} · ${order.clientPhone.ifBlank { "Sans tél" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    softWrap = true
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${order.itemCount} pièce(s) · Retrait : ${formatShortDate(order.dueDateMillis)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (order.isLate()) CrimsonDanger else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (order.isLate()) FontWeight.SemiBold else FontWeight.Normal,
                                        softWrap = true
                                    )
                                    if (photoCount > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(13.dp))
                                            Text(
                                                text = "$photoCount photo(s)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RoyalBlue,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                if (showFinancials) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            PaymentStatusChip(order.paymentStatusEnum())
                                            if (order.remainingAmount > 0L) {
                                                Text(
                                                    "Reste ${formatMoney(order.remainingAmount, company.currency)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Text(
                                            text = formatMoney(order.totalAmount, company.currency),
                                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RoyalBlue
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PastelBlueTint
                                    ) {
                                        Text(
                                            text = "Montants confidentiels · Vue ${activeSession.role.shortTitleFr}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RoyalBlue,
                                            fontWeight = FontWeight.SemiBold,
                                            softWrap = true,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}
