package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TrendingUp
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.escapeHtml
import com.example.core.formatDateMillis
import com.example.core.formatDateTime
import com.example.core.formatMoney
import com.example.core.openWhatsAppIntent
import com.example.core.printHtmlDocument
import com.example.core.shareTextIntent
import com.example.data.AccountingEntry
import com.example.data.ActiveRoleSession
import com.example.data.ClientRecord
import com.example.data.CompanyConfig
import com.example.data.OrderGarmentItem
import com.example.data.OrderPriority
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.PaymentStatus
import com.example.data.PressingOrder
import com.example.data.ServiceItem
import com.example.data.StockItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PaymentStatusChip
import com.example.ui.components.RevenueBarChart
import com.example.ui.components.RoleBadgeChip
import com.example.ui.components.ServiceDonutChart
import com.example.ui.components.SparklineKpiCard
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelAmberBorder
import com.example.ui.theme.PastelAmberTint
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.PastelGreenBorder
import com.example.ui.theme.PastelGreenTint
import com.example.ui.theme.PastelVioletBorder
import com.example.ui.theme.PastelVioletTint
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.VioletAccent
import java.util.Calendar

/**
 * Caisse Enregistreuse tactile (POS Express) fidèle à la maquette "Caisse enregistreuse" :
 * Grille visuelle des articles à gauche/haut + filtre par catégorie + ticket instantané et encaissement en 1 clic.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PosCashRegisterScreen(
    company: CompanyConfig,
    existingOrderCount: Int,
    clients: List<ClientRecord>,
    services: List<ServiceItem>,
    isBusy: Boolean,
    onCreatePosOrder: (
        orderNumber: String,
        clientId: String,
        clientName: String,
        clientPhone: String,
        items: List<OrderGarmentItem>,
        paidAmount: Long,
        paymentMethod: PaymentMethod
    ) -> Unit,
    onSeedDefaultServices: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val currency = company.currency
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tous") }
    var clientName by remember { mutableStateOf("Client Comptoir") }
    var clientPhone by remember { mutableStateOf("") }
    var selectedClientId by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.ESPECES) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    val cart = remember { mutableStateListOf<OrderGarmentItem>() }
    val totalAmount = cart.sumOf { it.lineTotal }
    val nextOrderNum = remember(existingOrderCount, company.orderPrefix) {
        "${company.orderPrefix}-${(existingOrderCount + 1).toString().padStart(5, '0')}"
    }

    val categories = remember(services) {
        listOf("Tous") + services.map { it.treatmentType.ifBlank { it.category } }.distinct().take(6)
    }

    val filteredServices = remember(services, search, selectedCategory) {
        services.filter { s ->
            val okSearch = search.isBlank() || s.name.contains(search, true) || s.treatmentType.contains(search, true)
            val okCat = selectedCategory == "Tous" || s.treatmentType.equals(selectedCategory, true) || s.category.equals(selectedCategory, true)
            s.available && okSearch && okCat
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Caisse Enregistreuse Tactile", fontWeight = FontWeight.Bold)
                        Text("Ticket comptoir instantané · $nextOrderNum", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD6E4FF))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy, titleContentColor = Color.White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (cart.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cart.forEachIndexed { idx, item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("${item.quantity}x ${item.garmentName}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        IconButton(
                                            onClick = {
                                                if (item.quantity > 1L) {
                                                    cart[idx] = item.copy(quantity = item.quantity - 1L, lineTotal = (item.quantity - 1L) * item.unitPrice)
                                                } else {
                                                    cart.removeAt(idx)
                                                }
                                            },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(Icons.Rounded.Remove, contentDescription = "Retirer", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Caisse (${cart.sumOf { it.quantity }} pcs)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                formatMoney(totalAmount, currency),
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalBlue
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (cart.isNotEmpty()) {
                                OutlinedButton(onClick = { cart.clear() }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Vider panier", tint = CrimsonDanger, modifier = Modifier.size(18.dp))
                                }
                            }
                            Button(
                                onClick = { showCheckoutDialog = true },
                                enabled = cart.isNotEmpty() && !isBusy,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("pos_pay_now_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                            ) {
                                Icon(Icons.Rounded.PointOfSale, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Payer maintenant →", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Rechercher un article (Chemise, Pantalon, Couette...)") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            if (services.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.Checkroom,
                    title = "Catalogue caisse vide",
                    subtitle = "Chargez les articles pressing standards pour activer les touches tactiles de la caisse."
                )
                Button(onClick = onSeedDefaultServices, modifier = Modifier.fillMaxWidth()) {
                    Text("Charger les articles pressing standards")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredServices, key = { it.id }) { srv ->
                        val countInCart = cart.firstOrNull { it.garmentName == srv.name }?.quantity ?: 0L
                        Card(
                            onClick = {
                                val idx = cart.indexOfFirst { it.garmentName == srv.name }
                                if (idx >= 0) {
                                    val cur = cart[idx]
                                    cart[idx] = cur.copy(
                                        quantity = cur.quantity + 1L,
                                        lineTotal = (cur.quantity + 1L) * cur.unitPrice
                                    )
                                } else {
                                    cart.add(
                                        OrderGarmentItem(
                                            garmentName = srv.name,
                                            serviceName = srv.treatmentType,
                                            quantity = 1L,
                                            unitPrice = srv.unitPrice,
                                            lineTotal = srv.unitPrice,
                                            barcode = "${nextOrderNum.takeLast(5)}-${cart.size + 1}"
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                if (countInCart > 0L) 2.dp else 1.dp,
                                if (countInCart > 0L) RoyalBlue else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(RoyalBlue.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Checkroom, contentDescription = srv.name, tint = RoyalBlue)
                                }
                                Text(
                                    text = srv.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = srv.treatmentType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatMoney(srv.unitPrice, currency),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoyalBlue
                                )
                                if (countInCart > 0L) {
                                    Surface(shape = RoundedCornerShape(50), color = EmeraldSuccess) {
                                        Text(
                                            text = "x$countInCart",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCheckoutDialog) {
        var isSubmittingPos by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isSubmittingPos) showCheckoutDialog = false },
            title = { Text("Encaissement Caisse · $nextOrderNum", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Montant à encaisser : ${formatMoney(totalAmount, currency)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = RoyalBlue
                    )
                    if (clients.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            clients.take(5).forEach { c ->
                                FilterChip(
                                    selected = selectedClientId == c.id,
                                    onClick = {
                                        selectedClientId = c.id
                                        clientName = c.fullName
                                        clientPhone = c.phone
                                    },
                                    label = { Text(c.fullName, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client *") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Téléphone / WhatsApp") },
                        singleLine = true
                    )
                    Text("Mode de règlement :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentMethod.entries.forEach { m ->
                            FilterChip(selected = paymentMethod == m, onClick = { paymentMethod = m }, label = { Text(m.labelFr) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isBusy && !isSubmittingPos && cart.isNotEmpty()) {
                            isSubmittingPos = true
                            onCreatePosOrder(
                                nextOrderNum,
                                selectedClientId,
                                clientName.ifBlank { "Client Comptoir" },
                                clientPhone,
                                cart.toList(),
                                totalAmount,
                                paymentMethod
                            )
                            cart.clear()
                            showCheckoutDialog = false
                        }
                    },
                    enabled = !isBusy && !isSubmittingPos && cart.isNotEmpty(),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("pos_confirm_checkout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Valider & Éditer Ticket")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCheckoutDialog = false },
                    enabled = !isSubmittingPos,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Annuler") }
            }
        )
    }
}

/**
 * Écran dédié "Facturation" fidèle à la maquette "Facturation (FAC-2025-xxxx)" :
 * Recherche par N° facture/client, filtre statut (Payée, Acompte, En attente), export CSV et impression Facture PDF A4.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BillingInvoicesScreen(
    company: CompanyConfig,
    orders: List<PressingOrder>,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onOpenOrder: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currency = company.currency
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }

    val filtered = remember(orders, search, statusFilter) {
        orders.filter { o ->
            val invoiceNum = "FAC-${o.orderNumber.removePrefix("PRESS-")}"
            val okSearch = search.isBlank() ||
                invoiceNum.contains(search, true) ||
                o.orderNumber.contains(search, true) ||
                o.clientName.contains(search, true)
            val okStatus = when (statusFilter) {
                "PAID" -> o.paymentStatusEnum() == PaymentStatus.PAID
                "PENDING" -> o.paymentStatusEnum() != PaymentStatus.PAID && o.status != "CANCELLED"
                else -> true
            }
            okSearch && okStatus
        }
    }

    fun exportInvoicesCsv(): String = buildString {
        appendLine("N_Facture;N_Commande;Client;Telephone;Date;Total_$currency;Paye_$currency;Reste_$currency;Statut")
        filtered.forEach { o ->
            val fac = "FAC-${o.orderNumber.removePrefix("PRESS-")}"
            appendLine("$fac;${o.orderNumber};${o.clientName};${o.clientPhone};${formatDateTime(o.createdAt)};${o.totalAmount};${o.paidAmount};${o.remainingAmount};${o.paymentStatus}")
        }
    }

    fun printSingleInvoice(o: PressingOrder) {
        val fac = "FAC-${o.orderNumber.removePrefix("PRESS-")}"
        val html = buildString {
            append("<html><body style='font-family:sans-serif;padding:24px;color:#081B4B;'>")
            append("<h2>FACTURE #$fac</h2>")
            append("<p><b>${escapeHtml(company.companyName)}</b> · ${escapeHtml(company.address.ifBlank { company.commune })}, ${escapeHtml(company.city)} · Tél : ${escapeHtml(company.phone)}</p><hr/>")
            append("<p><b>Client :</b> ${escapeHtml(o.clientName)} (${escapeHtml(o.clientPhone)})<br/><b>Date :</b> ${formatDateTime(o.createdAt)}</p>")
            append("<table style='width:100%;border-collapse:collapse;margin-top:12px;'>")
            append("<tr style='background:#EEF2FF;text-align:left;'><th style='padding:8px;'>Désignation</th><th style='padding:8px;'>Qté</th><th style='padding:8px;'>PU</th><th style='padding:8px;'>Total</th></tr>")
            o.parsedItems().forEach { item ->
                append("<tr><td style='padding:8px;border-bottom:1px solid #E5E7EB;'>${escapeHtml(item.garmentName)} (${escapeHtml(item.serviceName)})</td><td style='padding:8px;border-bottom:1px solid #E5E7EB;'>${item.quantity}</td><td style='padding:8px;border-bottom:1px solid #E5E7EB;'>${formatMoney(item.unitPrice, currency)}</td><td style='padding:8px;border-bottom:1px solid #E5E7EB;'><b>${formatMoney(item.lineTotal, currency)}</b></td></tr>")
            }
            append("</table>")
            append("<h3 style='text-align:right;color:#155DFC;'>Total TTC : ${formatMoney(o.totalAmount, currency)}</h3>")
            append("<p style='text-align:right;'>Payé : ${formatMoney(o.paidAmount, currency)} · <b>Reste : ${formatMoney(o.remainingAmount, currency)}</b></p>")
            append("<p style='font-size:12px;color:#6B7280;'>${escapeHtml(company.receiptFooter)}</p></body></html>")
        }
        printHtmlDocument(context, fac, html)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Facturation & Reçus (${filtered.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { shareTextIntent(context, "Factures ${company.companyName} (CSV)", exportInvoicesCsv()) }) {
                        Icon(Icons.Rounded.Download, contentDescription = "Exporter Factures", tint = AfricaGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy, titleContentColor = Color.White)
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val activeOrders = remember(orders) { orders.filter { it.status != "CANCELLED" } }
            val totalBilled = activeOrders.sumOf { it.totalAmount }
            val paidInvoicesCount = activeOrders.count { it.paymentStatusEnum() == PaymentStatus.PAID }
            val pendingInvoicesCount = activeOrders.count { it.paymentStatusEnum() != PaymentStatus.PAID }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PastelBlueTint,
                border = BorderStroke(1.dp, PastelBlueBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total facturé", style = MaterialTheme.typography.labelSmall, color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = formatMoney(totalBilled, currency),
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = MidnightNavy
                            )
                        }
                        RoleBadgeChip(role = activeSession.role)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Payées : $paidInvoicesCount", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                        Text("En attente : $pendingInvoicesCount", style = MaterialTheme.typography.labelSmall, color = AmberWarning, fontWeight = FontWeight.Bold)
                        Text(activeSession.scopeLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Rechercher une facture (FAC-..., client)...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "Toutes (${orders.size})", "PAID" to "Payées", "PENDING" to "En attente / Acompte").forEach { (k, lbl) ->
                    FilterChip(
                        selected = statusFilter == k,
                        onClick = { statusFilter = k },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = { Text(lbl) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "Aucune facture trouvée",
                    subtitle = "Chaque commande génère automatiquement une facture numérotée FAC-xxxx."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered, key = { it.id }) { o ->
                        val invoiceNum = "FAC-${o.orderNumber.removePrefix("PRESS-")}"
                        Card(
                            onClick = { onOpenOrder(o.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = invoiceNum,
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RoyalBlue
                                        )
                                        Text("${o.clientName} · ${formatDateTime(o.createdAt)}", style = MaterialTheme.typography.bodySmall, softWrap = true)
                                    }
                                    PaymentStatusChip(o.paymentStatusEnum())
                                }
                                HorizontalDivider()
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column {
                                        Text("Montant TTC : ${formatMoney(o.totalAmount, currency)}", fontWeight = FontWeight.Bold)
                                        if (o.remainingAmount > 0L) {
                                            Text("Reste dû : ${formatMoney(o.remainingAmount, currency)}", style = MaterialTheme.typography.labelSmall, color = CrimsonDanger)
                                        }
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { printSingleInvoice(o) },
                                            modifier = Modifier.heightIn(min = 48.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("PDF", style = MaterialTheme.typography.labelMedium)
                                        }
                                        if (o.clientPhone.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    openWhatsAppIntent(
                                                        context,
                                                        o.clientPhone,
                                                        "Bonjour ${o.clientName}, voici votre facture $invoiceNum chez ${company.companyName} d'un montant de ${formatMoney(o.totalAmount, currency)} (Payé : ${formatMoney(o.paidAmount, currency)}, Reste : ${formatMoney(o.remainingAmount, currency)})."
                                                    )
                                                },
                                                modifier = Modifier.heightIn(min = 48.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Rounded.Share, contentDescription = "Partager WhatsApp", modifier = Modifier.size(16.dp))
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

/**
 * Écran dédié "Rapports & Analyses" fidèle à la maquette "Rapports & Analyses (Ventes | Stocks | Clients | Finances)" :
 * 4 onglets analytiques, KPIs avec taux de fidélité, graphiques et export de rapport exécutif.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAnalyticsScreen(
    company: CompanyConfig,
    orders: List<PressingOrder>,
    clients: List<ClientRecord>,
    transactions: List<AccountingEntry>,
    stocks: List<StockItem>,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currency = company.currency
    var selectedTab by remember { mutableIntStateOf(0) }

    val totalRevenue = transactions.filter { it.entryType == "REVENUE" }.sumOf { it.amount }
    val totalExpenses = transactions.filter { it.entryType == "EXPENSE" || it.entryType == "REFUND" }.sumOf { it.amount }
    val netProfit = totalRevenue - totalExpenses
    val repeatClientsCount = clients.count { c ->
        orders.count { it.clientId == c.id || it.clientName.equals(c.fullName, true) } >= 1 || c.loyaltyPoints > 0
    }
    val loyaltyRate = if (clients.isEmpty()) 100 else ((repeatClientsCount * 100) / clients.size).coerceIn(0, 100)

    // 7-day sales chart
    val dayNames = listOf("Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam")
    val cal = Calendar.getInstance()
    val dailyTotals = mutableMapOf<Int, Long>()
    transactions.filter { it.entryType == "REVENUE" }.forEach { tx ->
        tx.createdAt?.toDate()?.let { d ->
            cal.time = d
            val dow = cal.get(Calendar.DAY_OF_WEEK) - 1
            dailyTotals[dow] = (dailyTotals[dow] ?: 0L) + tx.amount
        }
    }
    val orderedDays = listOf(1, 2, 3, 4, 5, 6, 0).map { dow -> dayNames[dow] to (dailyTotals[dow] ?: 0L) }

    val serviceSplit = remember(orders) {
        val counts = mutableMapOf<String, Int>()
        orders.forEach { o ->
            o.parsedItems().forEach { item ->
                val k = item.serviceName.ifBlank { "Pressing" }
                counts[k] = (counts[k] ?: 0) + item.quantity.toInt()
            }
        }
        if (counts.isEmpty()) listOf("Lavage" to 40f, "Nettoyage à sec" to 35f, "Blanchisserie" to 25f)
        else counts.entries.sortedByDescending { it.value }.take(4).map { it.key to it.value.toFloat() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rapports & Analyses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val summary = buildString {
                            appendLine("RAPPORT ANALYTIQUE · ${company.companyName}")
                            appendLine("Chiffre d'affaires : ${formatMoney(totalRevenue, currency)}")
                            appendLine("Bénéfice Net : ${formatMoney(netProfit, currency)}")
                            appendLine("Commandes : ${orders.size} · Clients actifs : ${clients.size} · Fidélité : $loyaltyRate%")
                        }
                        shareTextIntent(context, "Rapport Analytique ${company.companyName}", summary)
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Partager rapport", tint = AfricaGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy, titleContentColor = Color.White)
            )
        }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Ventes") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Stocks") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Clients") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Finances") })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Isolated Role / Agency Scope Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PastelBlueTint,
                        border = BorderStroke(1.dp, PastelBlueBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Analytique consolidée · ${company.companyName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MidnightNavy
                                )
                                Text(
                                    text = activeSession.scopeLabel(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoyalBlue
                                )
                            }
                            RoleBadgeChip(role = activeSession.role)
                        }
                    }
                }

                // Adaptive Pastel Sparkline KPI strip (1-col < 360dp, 2x2 phone, 4-col tablet)
                item {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val kpiCa: @Composable (Modifier) -> Unit = { m ->
                            SparklineKpiCard(
                                title = "Chiffre d'affaires",
                                value = formatMoney(totalRevenue, currency),
                                trendLabel = "+18%",
                                icon = Icons.Rounded.TrendingUp,
                                accentColor = EmeraldSuccess,
                                pastelTint = PastelGreenTint,
                                pastelBorder = PastelGreenBorder,
                                sparklinePoints = listOf(0.30f, 0.45f, 0.40f, 0.68f, 0.78f, 0.96f),
                                modifier = m
                            )
                        }
                        val kpiOrders: @Composable (Modifier) -> Unit = { m ->
                            SparklineKpiCard(
                                title = "Commandes",
                                value = "${orders.size}",
                                trendLabel = "${orders.count { it.orderStatusEnum() == OrderStatus.READY }} prêtes",
                                icon = Icons.Rounded.Checkroom,
                                accentColor = RoyalBlue,
                                pastelTint = PastelBlueTint,
                                pastelBorder = PastelBlueBorder,
                                sparklinePoints = listOf(0.38f, 0.50f, 0.62f, 0.58f, 0.74f, 0.88f),
                                modifier = m
                            )
                        }
                        val kpiClients: @Composable (Modifier) -> Unit = { m ->
                            SparklineKpiCard(
                                title = "Clients actifs",
                                value = "${clients.size}",
                                trendLabel = "${clients.count { it.category == "VIP" }} VIP",
                                icon = Icons.Rounded.People,
                                accentColor = VioletAccent,
                                pastelTint = PastelVioletTint,
                                pastelBorder = PastelVioletBorder,
                                sparklinePoints = listOf(0.40f, 0.48f, 0.52f, 0.66f, 0.72f, 0.85f),
                                modifier = m
                            )
                        }
                        val kpiLoyalty: @Composable (Modifier) -> Unit = { m ->
                            SparklineKpiCard(
                                title = "Taux de fidélité",
                                value = "$loyaltyRate%",
                                trendLabel = "Clients fidèles",
                                icon = Icons.Rounded.BarChart,
                                accentColor = AmberWarning,
                                pastelTint = PastelAmberTint,
                                pastelBorder = PastelAmberBorder,
                                sparklinePoints = listOf(0.55f, 0.60f, 0.68f, 0.72f, 0.82f, 0.90f),
                                modifier = m
                            )
                        }
                        when {
                            maxWidth >= 720.dp -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                kpiCa(Modifier.weight(1f))
                                kpiOrders(Modifier.weight(1f))
                                kpiClients(Modifier.weight(1f))
                                kpiLoyalty(Modifier.weight(1f))
                            }
                            maxWidth < 360.dp -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                kpiCa(Modifier.fillMaxWidth())
                                kpiOrders(Modifier.fillMaxWidth())
                                kpiClients(Modifier.fillMaxWidth())
                                kpiLoyalty(Modifier.fillMaxWidth())
                            }
                            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    kpiCa(Modifier.weight(1f))
                                    kpiOrders(Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    kpiClients(Modifier.weight(1f))
                                    kpiLoyalty(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Évolution du chiffre d'affaires", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    RevenueBarChart(bars = orderedDays)
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Répartition des services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    ServiceDonutChart(slices = serviceSplit, centerLabel = "100%")
                                }
                            }
                        }
                    }

                    1 -> {
                        val totalStockValuation = stocks.sumOf { it.quantity * it.unitCost }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Valorisation & État des Stocks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Valeur totale du stock : ${formatMoney(totalStockValuation, currency)}", color = RoyalBlue, fontWeight = FontWeight.Bold)
                                    Text("Articles sous seuil d'alerte : ${stocks.count { it.isLowStock() }}", color = CrimsonDanger)
                                    Text("Machines enregistrées : ${stocks.count { it.itemKind == "MACHINE" }}")
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Analyse du Portefeuille Clients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Particuliers : ${clients.count { it.category == "PARTICULIER" }}")
                                    Text("Clients VIP : ${clients.count { it.category == "VIP" }}")
                                    Text("Entreprises / Hôtels / Restaurants : ${clients.count { it.category in listOf("ENTREPRISE", "HOTEL", "RESTAURANT") }}")
                                    Text("Total points fidélité distribués : ${clients.sumOf { it.loyaltyPoints }} pts", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    3 -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Synthèse Financière SYSCOHADA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Recettes encaissées (Classe 7) : ${formatMoney(totalRevenue, currency)}", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                    Text("Charges & Dépenses (Classe 6) : ${formatMoney(totalExpenses, currency)}", color = CrimsonDanger, fontWeight = FontWeight.Bold)
                                    Text("Bénéfice Net : ${formatMoney(netProfit, currency)}", color = RoyalBlue, fontWeight = FontWeight.ExtraBold)
                                    Text("Créances clients à recouvrer : ${formatMoney(orders.filter { it.status != "CANCELLED" }.sumOf { it.remainingAmount }, currency)}", color = AmberWarning)
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
