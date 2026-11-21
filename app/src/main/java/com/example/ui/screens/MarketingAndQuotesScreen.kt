package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.escapeHtml
import com.example.core.formatMoney
import com.example.core.nowFormatted
import com.example.core.openWhatsAppIntent
import com.example.core.printHtmlDocument
import com.example.core.shareTextIntent
import com.example.data.ActiveRoleSession
import com.example.data.ClientRecord
import com.example.data.CompanyConfig
import com.example.data.OrderStatus
import com.example.data.PressingOrder
import com.example.data.ServiceItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.RoleBadgeChip
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.RoyalBlue

private data class QuoteLine(
    val serviceName: String,
    val treatment: String,
    val unitPrice: Long,
    val quantity: Long
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuotesScreen(
    company: CompanyConfig,
    clients: List<ClientRecord>,
    services: List<ServiceItem>,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currency = company.currency

    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("+225 ") }
    var expressMode by remember { mutableStateOf(false) }
    var deliveryFeeStr by remember { mutableStateOf("0") }
    var discountPercentStr by remember { mutableStateOf("0") }
    var validityDaysStr by remember { mutableStateOf("7") }
    val lines = remember { mutableStateListOf<QuoteLine>() }

    val subtotal = lines.sumOf { it.unitPrice * it.quantity }
    val expressFee = if (expressMode) (subtotal * company.expressSurchargePercent / 100.0).toLong() else 0L
    val discountPct = discountPercentStr.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
    val discountAmount = (subtotal * discountPct / 100.0).toLong()
    val deliveryFee = deliveryFeeStr.toLongOrNull() ?: 0L
    val taxable = (subtotal - discountAmount + expressFee + deliveryFee).coerceAtLeast(0L)
    val taxAmount = (taxable * company.taxRatePercent / 100.0).toLong()
    val grandTotal = taxable + taxAmount
    val quoteRef = remember { "DEV-${System.currentTimeMillis() % 100000}" }

    fun buildQuoteText(): String = buildString {
        appendLine("================================")
        appendLine("DEVIS COMMERCIAL · $quoteRef")
        appendLine(company.companyName.uppercase())
        appendLine("${company.address.ifBlank { company.commune }} · ${company.city}")
        appendLine("Tél : ${company.phone}")
        appendLine("Date : ${nowFormatted()} · Validité : ${validityDaysStr.ifBlank { "7" }} jours")
        if (clientName.isNotBlank()) appendLine("Client : $clientName")
        appendLine("--------------------------------")
        lines.forEach { l ->
            appendLine("${l.quantity}x ${l.serviceName} (${l.treatment}) : ${formatMoney(l.unitPrice * l.quantity, currency)}")
        }
        appendLine("--------------------------------")
        appendLine("Sous-total : ${formatMoney(subtotal, currency)}")
        if (expressFee > 0) appendLine("Express (+${company.expressSurchargePercent.toInt()}%) : ${formatMoney(expressFee, currency)}")
        if (discountAmount > 0) appendLine("Remise (-${discountPct.toInt()}%) : -${formatMoney(discountAmount, currency)}")
        if (deliveryFee > 0) appendLine("Collecte / Livraison : ${formatMoney(deliveryFee, currency)}")
        if (taxAmount > 0) appendLine("TVA (${company.taxRatePercent}%) : ${formatMoney(taxAmount, currency)}")
        appendLine("TOTAL ESTIMÉ : ${formatMoney(grandTotal, currency)}")
        appendLine("--------------------------------")
        appendLine("Délai estimé : ${if (expressMode) company.expressDelayHours else company.standardDelayHours} heures.")
        appendLine(company.receiptFooter)
    }

    fun buildQuoteHtml(): String = buildString {
        append("<html><body style='font-family:sans-serif;padding:24px;color:#081B4B;'>")
        append("<h2 style='margin:0;'>${escapeHtml(company.companyName)}</h2>")
        append("<p style='margin:4px 0;color:#4B5563;'>${escapeHtml(company.address.ifBlank { company.commune })}, ${escapeHtml(company.city)} · ${escapeHtml(company.phone)}</p>")
        append("<hr/><h3>DEVIS $quoteRef</h3>")
        append("<p><b>Client :</b> ${escapeHtml(clientName.ifBlank { "Client de passage" })}<br/><b>Date :</b> ${nowFormatted()} · <b>Validité :</b> ${validityDaysStr.ifBlank { "7" }} jours</p>")
        append("<table style='width:100%;border-collapse:collapse;margin-top:12px;'>")
        append("<tr style='background:#EEF2FF;text-align:left;'><th style='padding:8px;'>Article</th><th style='padding:8px;'>Qté</th><th style='padding:8px;'>P.U.</th><th style='padding:8px;'>Total</th></tr>")
        lines.forEach { l ->
            append("<tr><td style='padding:8px;border-bottom:1px solid #E5E7EB;'>${escapeHtml(l.serviceName)} (${escapeHtml(l.treatment)})</td><td style='padding:8px;border-bottom:1px solid #E5E7EB;'>${l.quantity}</td><td style='padding:8px;border-bottom:1px solid #E5E7EB;'>${formatMoney(l.unitPrice, currency)}</td><td style='padding:8px;border-bottom:1px solid #E5E7EB;'><b>${formatMoney(l.unitPrice * l.quantity, currency)}</b></td></tr>")
        }
        append("</table>")
        append("<h3 style='text-align:right;color:#155DFC;'>TOTAL ESTIMÉ : ${formatMoney(grandTotal, currency)}</h3>")
        append("<p style='font-size:12px;color:#6B7280;'>${escapeHtml(company.receiptFooter)}</p>")
        append("</body></html>")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Devis & Simulateur de Tarifs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy, titleContentColor = Color.White)
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
                            Text("Devis $quoteRef · ${company.companyName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightNavy)
                            Text(activeSession.scopeLabel(), style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                        }
                        RoleBadgeChip(role = activeSession.role)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. Destinataire du devis", fontWeight = FontWeight.Bold)
                        if (clients.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                clients.take(6).forEach { c ->
                                    FilterChip(
                                        selected = clientName == c.fullName,
                                        onClick = {
                                            clientName = c.fullName
                                            clientPhone = c.whatsapp.ifBlank { c.phone }
                                            if (c.discountPercent > 0) discountPercentStr = c.discountPercent.toInt().toString()
                                        },
                                        label = { Text(c.fullName) }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Nom client / Entreprise") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = clientPhone, onValueChange = { clientPhone = it }, label = { Text("WhatsApp / Tél") }, modifier = Modifier.weight(1.2f), singleLine = true)
                            OutlinedTextField(value = validityDaysStr, onValueChange = { validityDaysStr = it.filter { ch -> ch.isDigit() } }, label = { Text("Validité (j)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.8f), singleLine = true)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("2. Sélectionner les prestations", fontWeight = FontWeight.Bold)
                        if (services.isEmpty()) {
                            Text("Ajoutez d'abord des services dans le catalogue.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                services.filter { it.available }.forEach { srv ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            val idx = lines.indexOfFirst { it.serviceName == srv.name }
                                            if (idx >= 0) {
                                                val cur = lines[idx]
                                                lines[idx] = cur.copy(quantity = cur.quantity + 1)
                                            } else {
                                                lines.add(QuoteLine(srv.name, srv.treatmentType, srv.unitPrice, 1L))
                                            }
                                        },
                                        label = { Text("${srv.name} · ${srv.unitPrice} F") }
                                    )
                                }
                            }
                        }

                        lines.forEachIndexed { idx, line ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(line.serviceName, fontWeight = FontWeight.Bold)
                                        Text("${line.treatment} · ${formatMoney(line.unitPrice, currency)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            if (line.quantity > 1L) lines[idx] = line.copy(quantity = line.quantity - 1L)
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Rounded.Remove, contentDescription = "Moins", modifier = Modifier.size(16.dp))
                                        }
                                        Text("${line.quantity}", fontWeight = FontWeight.Bold)
                                        IconButton(onClick = {
                                            lines[idx] = line.copy(quantity = line.quantity + 1L)
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Rounded.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { lines.removeAt(idx) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Retirer", tint = CrimsonDanger, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("3. Options & Total du Devis", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Traitement Express (+${company.expressSurchargePercent.toInt()}%)")
                            Switch(checked = expressMode, onCheckedChange = { expressMode = it })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = discountPercentStr, onValueChange = { discountPercentStr = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Remise (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = deliveryFeeStr, onValueChange = { deliveryFeeStr = it.filter { ch -> ch.isDigit() } }, label = { Text("Livraison ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL DEVIS", fontWeight = FontWeight.ExtraBold)
                            Text(
                                formatMoney(grandTotal, currency),
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalBlue
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (clientPhone.filter { it.isDigit() }.length >= 8) {
                                        openWhatsAppIntent(context, clientPhone, buildQuoteText())
                                    } else {
                                        shareTextIntent(context, "Devis $quoteRef", buildQuoteText())
                                    }
                                },
                                enabled = lines.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quote_send_whatsapp_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("WhatsApp")
                            }
                            OutlinedButton(
                                onClick = { printHtmlDocument(context, "Devis_$quoteRef", buildQuoteHtml()) },
                                enabled = lines.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quote_print_pdf_button")
                            ) {
                                Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("PDF / Imprimer")
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketingScreen(
    company: CompanyConfig,
    clients: List<ClientRecord>,
    orders: List<PressingOrder>,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val currency = company.currency
    var selectedTab by remember { mutableIntStateOf(0) }
    var customPromoMessage by remember {
        mutableStateOf(
            "Offre spéciale ${company.companyName} : -15% sur tous les costumes et couettes cette semaine ! Répondez à ce message pour réserver une collecte."
        )
    }

    val topClients = remember(clients) { clients.sortedByDescending { it.loyaltyPoints } }
    val readyOrders = remember(orders) { orders.filter { it.orderStatusEnum() == OrderStatus.READY } }
    val unpaidOrders = remember(orders) { orders.filter { it.remainingAmount > 0L && it.status != "CANCELLED" } }

    val promoTemplates = listOf(
        "Promo Costumes -15%" to "Bonjour ! Cette semaine chez ${company.companyName}, profitez de -15% sur le nettoyage à sec de vos costumes et vestes. Contact : ${company.phone}.",
        "Relance Couettes & Draps" to "Bonjour ! Offrez une fraîcheur hôtel à vos couettes et draps chez ${company.companyName}. Collecte et livraison disponibles à ${company.commune}.",
        "Coupon Fidélité VIP" to "Merci pour votre fidélité chez ${company.companyName} ! Présentez le code VIP-10 pour bénéficier de 10% de remise sur votre prochain dépôt."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fidélité & Marketing WhatsApp", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy, titleContentColor = Color.White)
            )
        }
    ) { inner ->
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Relances") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Fidélité") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Promos") })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                                Text("Campagnes & Relances · ${clients.size} clients", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightNavy)
                                Text(activeSession.scopeLabel(), style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                            }
                            RoleBadgeChip(role = activeSession.role)
                        }
                    }
                }
                when (selectedTab) {
                    0 -> {
                        item {
                            Text("Commandes prêtes au retrait (${readyOrders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (readyOrders.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.Campaign,
                                    title = "Aucune relance de retrait en attente",
                                    subtitle = "Les commandes marquées « Prête » apparaîtront ici pour notifier le client en 1 clic."
                                )
                            }
                        } else {
                            items(readyOrders, key = { "ready_${it.id}" }) { order ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${order.orderNumber} · ${order.clientName}", fontWeight = FontWeight.Bold)
                                            Text("Reste à régler : ${formatMoney(order.remainingAmount, currency)}", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                                        }
                                        Button(
                                            onClick = {
                                                val msg = "Bonjour ${order.clientName}, votre commande ${order.orderNumber} chez ${company.companyName} est PRÊTE ! Horaires : ${company.openingHours}. Reste à régler : ${formatMoney(order.remainingAmount, currency)}."
                                                openWhatsAppIntent(context, order.clientPhone, msg)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                                        ) { Text("Notifier") }
                                    }
                                }
                            }
                        }

                        if (unpaidOrders.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(6.dp))
                                Text("Rappels d'impayés (${unpaidOrders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(unpaidOrders.take(10), key = { "unpaid_${it.id}" }) { order ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, AmberWarning)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${order.clientName} (${order.orderNumber})", fontWeight = FontWeight.Bold)
                                            Text("Solde dû : ${formatMoney(order.remainingAmount, currency)}", style = MaterialTheme.typography.bodySmall, color = CrimsonDanger, fontWeight = FontWeight.SemiBold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val msg = "Bonjour ${order.clientName}, rappel amical de ${company.companyName} concernant le solde de ${formatMoney(order.remainingAmount, currency)} sur la commande ${order.orderNumber}. Paiement possible par Orange Money, MTN MoMo, Wave ou Espèces."
                                                openWhatsAppIntent(context, order.clientPhone, msg)
                                            }
                                        ) { Text("Relancer") }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            Surface(shape = RoundedCornerShape(14.dp), color = EmeraldContainer, modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = EmeraldSuccess)
                                    Text("Règle fidélité active : 1 point cumulé par tranche de 1 000 $currency dépensée.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        if (topClients.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.Star,
                                    title = "Aucun client fidèle",
                                    subtitle = "Les points fidélité s'accumulent automatiquement à chaque commande."
                                )
                            }
                        } else {
                            items(topClients, key = { it.id }) { cli ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cli.fullName, fontWeight = FontWeight.Bold)
                                            Text("${cli.loyaltyPoints} points · Remise ${cli.discountPercent.toInt()}%", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val msg = "Bravo ${cli.fullName} ! Vous cumulez ${cli.loyaltyPoints} points fidélité chez ${company.companyName}. Profitez de votre récompense lors de votre prochain dépôt !"
                                                openWhatsAppIntent(context, cli.whatsapp.ifBlank { cli.phone }, msg)
                                            }
                                        ) { Text("Féliciter") }
                                    }
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
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Modèles de campagnes & coupons", fontWeight = FontWeight.Bold)
                                    promoTemplates.forEach { (title, body) ->
                                        OutlinedButton(
                                            onClick = { customPromoMessage = body },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text(title) }
                                    }
                                    OutlinedTextField(
                                        value = customPromoMessage,
                                        onValueChange = { customPromoMessage = it },
                                        label = { Text("Message promotionnel") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )
                                    Button(
                                        onClick = { shareTextIntent(context, "Campagne ${company.companyName}", customPromoMessage) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                                    ) {
                                        Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Diffuser la campagne (WhatsApp / SMS)")
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(
    company: CompanyConfig,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val faqs = listOf(
        "Pourquoi photographier chaque vêtement au dépôt ?" to "La photo horodatée avant traitement protège votre pressing en cas de réclamation pour tache préexistante ou déchirure. L'IA Gemini vous aide à repérer les tissus délicats (soie, laine, bazin), mais la validation humaine reste obligatoire.",
        "Comment fonctionne la comptabilité SYSCOHADA ?" to "Chaque encaissement client génère automatiquement une écriture immuable créditant le compte 706 (Prestations) et ventilée par trésorerie (571 Espèces, 5211 Orange Money, 5212 MTN MoMo, 5213 Wave). Aucune suppression silencieuse n'est autorisée pour garantir la piste d'audit.",
        "Comment imprimer un ticket thermique ou une facture PDF ?" to "Ouvrez une commande et appuyez sur « Ticket QR » ou l'icône Imprimante. Choisissez Ticket Thermique, Facture A4, Bon de Livraison ou Étiquettes, puis appuyez sur « Imprimer / PDF ».",
        "Comment gérer plusieurs agences et isoler les rôles ?" to "Créez vos agences depuis Super-Admin SaaS ou l'assistant Setup, puis ouvrez « Rôles, Droits & Isolation » pour configurer les 27 privilèges par rôle et isoler chaque entité (tableau de bord dédié, masquage des montants financiers et journal d'audit sécurité en temps réel).",
        "Comment sauvegarder les données de l'entreprise ?" to "Vos données sont synchronisées en temps réel sur Cloud Firestore. Vous pouvez aussi exporter un récapitulatif JSON depuis Super-Admin SaaS et un export CSV SYSCOHADA depuis Finances."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Centre d'aide & FAQ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy, titleContentColor = Color.White)
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MidnightNavy)) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.HelpOutline, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(32.dp))
                        Column {
                            Text("Guide Opérationnel Pressing Pro", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Support dédié pour ${company.companyName}", color = Color(0xFFCCD9FF), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            items(faqs) { (q, a) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(q, fontWeight = FontWeight.Bold, color = RoyalBlue)
                        Text(a, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
