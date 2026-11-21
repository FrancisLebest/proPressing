package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.decodeBase64ToBitmap
import com.example.core.dialPhoneIntent
import com.example.core.escapeHtml
import com.example.core.formatDateMillis
import com.example.core.formatDateTime
import com.example.core.formatMoney
import com.example.core.openWhatsAppIntent
import com.example.core.printHtmlDocument
import com.example.core.shareTextIntent
import com.example.data.AccessPermission
import com.example.data.ActiveRoleSession
import com.example.data.ClaimRecord
import com.example.data.CompanyConfig
import com.example.data.EmployeeRecord
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.PressingOrder
import com.example.ui.components.HorizontalOrderTimeline
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.PaymentStatusChip
import com.example.ui.components.QrCodeCanvas
import com.example.ui.components.RoleBadgeChip
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.OrderGarmentItem
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderDetailScreen(
    company: CompanyConfig,
    order: PressingOrder,
    employees: List<EmployeeRecord>,
    isBusy: Boolean,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onUpdateStatus: (PressingOrder, OrderStatus, Boolean) -> Unit,
    onRecordPayment: (PressingOrder, Long, PaymentMethod) -> Unit,
    onAssignDelivery: (PressingOrder, String, String, Boolean) -> Unit,
    onCreateClaim: (ClaimRecord) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val currency = company.currency
    val currentStatus = order.orderStatusEnum()
    val parsedItems = remember(order.items) { order.parsedItems() }
    val showFinancials = activeSession.canViewFinancialAmounts
    val canCollectPayment = activeSession.has(AccessPermission.COLLECT_PAYMENT)
    val canDispatchDelivery = activeSession.has(AccessPermission.DISPATCH_DELIVERIES)
    val canCreateClaims = activeSession.has(AccessPermission.MANAGE_CLAIMS)

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeliveryDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showClaimDialog by remember { mutableStateOf(false) }
    var previewPhotoGarment by remember { mutableStateOf<OrderGarmentItem?>(null) }

    fun buildReceiptText(): String = buildString {
        appendLine("================================")
        appendLine(company.companyName.uppercase())
        appendLine("${company.address.ifBlank { company.commune }} · ${company.city}")
        appendLine("Tél : ${company.phone}")
        appendLine("================================")
        appendLine(if (showFinancials) "REÇU / TICKET : ${order.orderNumber}" else "FICHE ATELIER / BL : ${order.orderNumber}")
        appendLine("Date dépôt : ${formatDateTime(order.createdAt)}")
        appendLine("Retrait prévu : ${formatDateMillis(order.dueDateMillis)}")
        appendLine("Client : ${order.clientName} (${order.clientPhone})")
        appendLine("Statut : ${currentStatus.labelFr}")
        appendLine("--------------------------------")
        parsedItems.forEach { item ->
            if (showFinancials) {
                appendLine("${item.quantity}x ${item.garmentName} (${item.serviceName}) : ${formatMoney(item.lineTotal, currency)}")
            } else {
                appendLine("${item.quantity}x ${item.garmentName} (${item.serviceName}) [${item.barcode.ifBlank { order.orderNumber }}]")
            }
            if (item.defects.isNotBlank()) appendLine("   Défaut signalé : ${item.defects}")
        }
        if (showFinancials) {
            appendLine("--------------------------------")
            appendLine("TOTAL : ${formatMoney(order.totalAmount, currency)}")
            appendLine("PAYÉ (${order.paymentMethodEnum().labelFr}) : ${formatMoney(order.paidAmount, currency)}")
            appendLine("RESTE À PAYER : ${formatMoney(order.remainingAmount, currency)}")
        }
        appendLine("--------------------------------")
        appendLine(company.receiptFooter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(order.orderNumber, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(order.clientName, style = MaterialTheme.typography.labelSmall, color = Color(0xFFD6E4FF), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        onClick = { showReceiptDialog = true },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = "Ticket & QR Code", tint = Color.White)
                    }
                    IconButton(
                        onClick = { shareTextIntent(context, "Reçu ${order.orderNumber}", buildReceiptText()) },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = "Partager reçu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightNavy,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, PastelBlueBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OrderStatusChip(currentStatus, order.isLate())
                        if (showFinancials) {
                            PaymentStatusChip(order.paymentStatusEnum())
                        } else {
                            RoleBadgeChip(role = activeSession.role)
                        }
                    }
                    Text("Client : ${order.clientName}", fontWeight = FontWeight.Bold, softWrap = true)
                    if (order.clientPhone.isNotBlank()) {
                        Text("Tél / WhatsApp : ${order.clientPhone}", style = MaterialTheme.typography.bodySmall, softWrap = true)
                    }
                    Text("Retrait estimé : ${formatDateMillis(order.dueDateMillis)}", style = MaterialTheme.typography.bodySmall, color = RoyalBlue, softWrap = true)
                    HorizontalDivider()
                    if (showFinancials) {
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Total commande", fontWeight = FontWeight.SemiBold)
                            Text(
                                formatMoney(order.totalAmount, currency),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalBlue
                            )
                        }
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Déjà payé (${order.paymentMethodEnum().labelFr})", style = MaterialTheme.typography.bodySmall)
                            Text(formatMoney(order.paidAmount, currency), color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                        }
                        if (order.remainingAmount > 0L) {
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Reste à payer", style = MaterialTheme.typography.bodySmall)
                                Text(formatMoney(order.remainingAmount, currency), color = CrimsonDanger, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PastelBlueTint,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Informations tarifaires masquées pour le rôle ${activeSession.role.labelFr} (données financières privées).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MidnightNavy,
                                softWrap = true,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (canCollectPayment && order.remainingAmount > 0L && currentStatus != OrderStatus.CANCELLED) {
                            Button(
                                onClick = { showPaymentDialog = true },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("order_settle_payment_button"),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                            ) {
                                Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Encaisser", fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = { showReceiptDialog = true },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("order_show_receipt_button")
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (showFinancials) "Ticket QR" else "Étiquettes / BL")
                        }
                        if (order.clientPhone.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val msg = if (currentStatus == OrderStatus.READY) {
                                        val remainInfo = if (showFinancials) " Reste à payer : ${formatMoney(order.remainingAmount, currency)}." else ""
                                        "Bonjour ${order.clientName}, votre commande ${order.orderNumber} chez ${company.companyName} est PRÊTE !$remainInfo"
                                    } else {
                                        "Bonjour ${order.clientName}, suivi de votre commande ${order.orderNumber} chez ${company.companyName} : statut ${currentStatus.labelFr}."
                                    }
                                    openWhatsAppIntent(context, order.clientPhone, msg)
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("WhatsApp Client")
                            }
                            OutlinedButton(
                                onClick = { dialPhoneIntent(context, order.clientPhone) },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Appeler")
                            }
                        }
                    }
                    if (canCreateClaims) {
                        OutlinedButton(
                            onClick = { showClaimDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("order_report_claim_button")
                        ) {
                            Icon(Icons.Rounded.ReportProblem, contentDescription = null, tint = CrimsonDanger, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ouvrir une réclamation SAV / Retouche", color = CrimsonDanger, softWrap = true)
                        }
                    }
                }
            }

            // Production Workflow Progression Card
            if (currentStatus != OrderStatus.CANCELLED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Suivi Atelier & Contrôle Qualité",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                softWrap = true
                            )
                            if (order.qualityChecked) {
                                Icon(Icons.Rounded.Verified, contentDescription = "Contrôle qualité validé", tint = EmeraldSuccess)
                            }
                        }
                        HorizontalOrderTimeline(currentStepIndex = currentStatus.stepIndex)
                        HorizontalDivider()
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OrderStatus.productionSteps.forEach { st ->
                                val selected = currentStatus == st
                                val allowed = activeSession.canTransitionOrderTo(st)
                                FilterChip(
                                    selected = selected,
                                    enabled = !isBusy && allowed,
                                    onClick = { onUpdateStatus(order, st, st == OrderStatus.READY || order.qualityChecked) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    label = { Text(st.labelFr) }
                                )
                            }
                        }
                        if (canDispatchDelivery || activeSession.canTransitionOrderTo(OrderStatus.CANCELLED)) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (canDispatchDelivery) {
                                    OutlinedButton(
                                        onClick = { showDeliveryDialog = true },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Livraison")
                                    }
                                }
                                if (activeSession.canTransitionOrderTo(OrderStatus.CANCELLED)) {
                                    TextButton(
                                        onClick = { onUpdateStatus(order, OrderStatus.CANCELLED, false) },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                        colors = ButtonDefaults.textButtonColors(contentColor = CrimsonDanger)
                                    ) {
                                        Text("Annuler la commande")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Garments List with Tappable Photos, QR Tags & Per-Item Workshop Stage
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Vêtements enregistrés (${parsedItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    parsedItems.forEach { item ->
                        val itemStage = item.workshopStatusEnum()
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    val bmp = remember(item.photoBase64) { decodeBase64ToBitmap(item.photoBase64) }
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = item.garmentName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { previewPhotoGarment = item }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SoftBlueContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Checkroom, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(26.dp))
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "${item.quantity}× ${item.garmentName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MidnightNavy,
                                            softWrap = true
                                        )
                                        Text(
                                            "${item.serviceName} · ${item.fabric}${if (item.color.isNotBlank()) " · ${item.color}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            softWrap = true
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(shape = RoundedCornerShape(50), color = SoftBlueContainer) {
                                                Text(
                                                    text = "Atelier : ${itemStage.labelFr}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = RoyalBlue,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            if (item.barcode.isNotBlank()) {
                                                Surface(shape = RoundedCornerShape(8.dp), color = PastelBlueTint) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(13.dp))
                                                        Text(
                                                            text = item.barcode,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                                            color = MidnightNavy
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (showFinancials) {
                                            Text(
                                                text = formatMoney(item.lineTotal, currency),
                                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = RoyalBlue
                                            )
                                        }
                                    }
                                }
                                if (item.defects.isNotBlank()) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = AmberContainer, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "Défaut signalé : ${item.defects}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF92400E),
                                            softWrap = true,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                if (item.aiDiagnosis.isNotBlank()) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = PastelBlueTint, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "Note IA : ${item.aiDiagnosis}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MidnightNavy,
                                            softWrap = true,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Timestamped Audit History
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Traçabilité & Historique horodaté", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    order.statusHistory.forEach { entry ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Text(entry, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Payment Settlement Dialog
    if (showPaymentDialog) {
        var amountStr by remember { mutableStateOf(order.remainingAmount.toString()) }
        var method by remember { mutableStateOf(order.paymentMethodEnum()) }
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Encaisser un règlement", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reste dû : ${formatMoney(order.remainingAmount, currency)}", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Montant reçu ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentMethod.entries.forEach { m ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m.labelFr) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toLongOrNull() ?: 0L
                        if (amt > 0L) {
                            onRecordPayment(order, amt, method)
                            showPaymentDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) { Text("Confirmer l'encaissement") }
            },
            dismissButton = { TextButton(onClick = { showPaymentDialog = false }) { Text("Annuler") } }
        )
    }

    // Delivery Assignment Dialog
    if (showDeliveryDialog) {
        val drivers = employees.filter { it.role == "LIVREUR" }
        var driverName by remember { mutableStateOf(order.deliveryDriver.ifBlank { drivers.firstOrNull()?.fullName ?: "" }) }
        var zone by remember { mutableStateOf(order.deliveryZone.ifBlank { company.commune }) }
        AlertDialog(
            onDismissRequest = { showDeliveryDialog = false },
            title = { Text("Affectation Livraison", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = driverName, onValueChange = { driverName = it }, label = { Text("Nom du livreur") }, singleLine = true)
                    OutlinedTextField(value = zone, onValueChange = { zone = it }, label = { Text("Zone / Quartier") }, singleLine = true)
                }
            },
            confirmButton = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            onAssignDelivery(order, driverName.ifBlank { "Livreur" }, zone, false)
                            showDeliveryDialog = false
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Affecter") }
                    Button(
                        onClick = {
                            onAssignDelivery(order, driverName.ifBlank { "Livreur" }, zone, true)
                            showDeliveryDialog = false
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) { Text("Marquer Livrée") }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeliveryDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Fermer") }
            }
        )
    }

    // Visual Documents & Printing Center Dialog (Ticket, Facture A4, Bon de Livraison, Étiquettes)
    if (showReceiptDialog) {
        val availableDocModes = if (showFinancials) {
            listOf(
                "TICKET" to "Ticket QR",
                "INVOICE" to "Facture A4",
                "DELIVERY" to "Bon Livraison",
                "LABELS" to "Étiquettes"
            )
        } else {
            listOf(
                "LABELS" to "Étiquettes",
                "DELIVERY" to "Bon Livraison"
            )
        }
        var docMode by remember(showFinancials) { mutableStateOf(availableDocModes.first().first) }

        fun buildDocumentHtml(): String = when (docMode) {
            "INVOICE" -> buildString {
                append("<html><body style='font-family:sans-serif;padding:24px;color:#081B4B;'>")
                append("<h2 style='margin:0;'>FACTURE PRO · ${escapeHtml(order.orderNumber)}</h2>")
                append("<p style='margin:4px 0;color:#4B5563;'><b>${escapeHtml(company.companyName)}</b> (${escapeHtml(company.legalName)}) · RCCM ${escapeHtml(company.rccm.ifBlank { "-" })} · IFU ${escapeHtml(company.nif.ifBlank { "-" })}<br/>${escapeHtml(company.address.ifBlank { company.commune })}, ${escapeHtml(company.city)} · ${escapeHtml(company.phone)}</p><hr/>")
                append("<p><b>Client :</b> ${escapeHtml(order.clientName)} (${escapeHtml(order.clientPhone)})<br/><b>Date dépôt :</b> ${formatDateTime(order.createdAt)} · <b>Retrait prévu :</b> ${formatDateMillis(order.dueDateMillis)}</p>")
                append("<table style='width:100%;border-collapse:collapse;margin-top:12px;'>")
                append("<tr style='background:#EEF2FF;text-align:left;'><th style='padding:8px;'>Désignation</th><th style='padding:8px;'>Service</th><th style='padding:8px;'>Qté</th><th style='padding:8px;'>Montant</th></tr>")
                parsedItems.forEach { item ->
                    append("<tr><td style='padding:8px;border-bottom:1px solid #E5E7EB;'>${escapeHtml(item.garmentName)}</td><td style='padding:8px;border-bottom:1px solid #E5E7EB;'>${escapeHtml(item.serviceName)}</td><td style='padding:8px;border-bottom:1px solid #E5E7EB;'>${item.quantity}</td><td style='padding:8px;border-bottom:1px solid #E5E7EB;'><b>${formatMoney(item.lineTotal, currency)}</b></td></tr>")
                }
                append("</table>")
                append("<div style='text-align:right;margin-top:14px;'>")
                append("<p>Sous-total : ${formatMoney(order.subtotal, currency)}<br/>")
                if (order.discountAmount > 0) append("Remise : -${formatMoney(order.discountAmount, currency)}<br/>")
                if (order.expressFee > 0) append("Express : +${formatMoney(order.expressFee, currency)}<br/>")
                if (order.deliveryFee > 0) append("Livraison : +${formatMoney(order.deliveryFee, currency)}<br/>")
                if (order.taxAmount > 0) append("TVA : +${formatMoney(order.taxAmount, currency)}<br/>")
                append("<b>TOTAL TTC : ${formatMoney(order.totalAmount, currency)}</b><br/>")
                append("Payé (${escapeHtml(order.paymentMethodEnum().labelFr)}) : ${formatMoney(order.paidAmount, currency)}<br/>")
                append("<b>RESTE À PAYER : ${formatMoney(order.remainingAmount, currency)}</b></p></div>")
                append("<p style='font-size:11px;color:#6B7280;'>${escapeHtml(company.receiptFooter)}</p></body></html>")
            }
            "DELIVERY" -> buildString {
                append("<html><body style='font-family:sans-serif;padding:24px;color:#081B4B;'>")
                append("<h2>BON DE LIVRAISON · ${escapeHtml(order.orderNumber)}</h2>")
                append("<p><b>${escapeHtml(company.companyName)}</b> · ${escapeHtml(company.phone)}</p><hr/>")
                append("<p><b>Destinataire :</b> ${escapeHtml(order.clientName)} (${escapeHtml(order.clientPhone)})<br/><b>Zone / Adresse :</b> ${escapeHtml(order.deliveryZone.ifBlank { order.clientAddress })}<br/><b>Livreur :</b> ${escapeHtml(order.deliveryDriver.ifBlank { "À affecter" })}</p>")
                append("<ul>")
                parsedItems.forEach { item -> append("<li><b>${item.quantity}x ${escapeHtml(item.garmentName)}</b> (${escapeHtml(item.barcode)})</li>") }
                append("</ul>")
                if (showFinancials) {
                    append("<p><b>Reste à encaisser à la livraison : ${formatMoney(order.remainingAmount, currency)}</b></p>")
                }
                append("<p style='margin-top:32px;'>Signature client à la réception : ________________________</p></body></html>")
            }
            "LABELS" -> buildString {
                append("<html><body style='font-family:monospace;padding:12px;'>")
                append("<h3>ÉTIQUETTES VÊTEMENTS · ${escapeHtml(order.orderNumber)}</h3>")
                parsedItems.forEach { item ->
                    append("<div style='border:1px dashed #111;padding:10px;margin-bottom:10px;'>")
                    append("<div><b>${escapeHtml(company.companyName)}</b></div>")
                    append("<div>CMD: ${escapeHtml(order.orderNumber)} | TAG: <b>${escapeHtml(item.barcode.ifBlank { order.orderNumber })}</b></div>")
                    append("<div>CLIENT: ${escapeHtml(order.clientName)}</div>")
                    append("<div>ARTICLE: ${item.quantity}x ${escapeHtml(item.garmentName)} (${escapeHtml(item.serviceName)})</div>")
                    if (item.defects.isNotBlank()) append("<div>DEF: ${escapeHtml(item.defects)}</div>")
                    append("</div>")
                }
                append("</body></html>")
            }
            else -> buildString {
                val widthPx = if (company.receiptFormat == "THERMAL_58") "220px" else "300px"
                append("<html><body style='font-family:monospace;width:$widthPx;margin:0 auto;padding:10px;font-size:12px;'>")
                append("<div style='text-align:center;'><b>${escapeHtml(company.companyName.uppercase())}</b><br/>${escapeHtml(company.address.ifBlank { company.commune })}<br/>Tél: ${escapeHtml(company.phone)}</div><hr/>")
                append("<div><b>TICKET: ${escapeHtml(order.orderNumber)}</b><br/>Client: ${escapeHtml(order.clientName)}<br/>Retrait: ${formatDateMillis(order.dueDateMillis)}</div><hr/>")
                parsedItems.forEach { item ->
                    append("<div>${item.quantity}x ${escapeHtml(item.garmentName)} ... <b>${formatMoney(item.lineTotal, currency)}</b></div>")
                }
                append("<hr/><div><b>TOTAL: ${formatMoney(order.totalAmount, currency)}</b><br/>PAYÉ: ${formatMoney(order.paidAmount, currency)}<br/><b>RESTE: ${formatMoney(order.remainingAmount, currency)}</b></div><hr/>")
                append("<div style='text-align:center;font-size:10px;'>${escapeHtml(company.receiptFooter)}</div></body></html>")
            }
        }

        AlertDialog(
            onDismissRequest = { showReceiptDialog = false },
            title = { Text("Documents & Impression (${company.receiptFormat})", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableDocModes.forEach { (code, label) ->
                            FilterChip(selected = docMode == code, onClick = { docMode = code }, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    HorizontalDivider()
                    Text(company.companyName.uppercase(), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Text("${company.address.ifBlank { company.commune }} · ${company.phone}", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                        fontWeight = FontWeight.Bold
                    )
                    if (docMode == "TICKET" || docMode == "INVOICE") {
                        QrCodeCanvas(content = "${company.companyName}|${order.orderNumber}|${order.totalAmount}", size = 116.dp)
                        Text("Scannez pour vérifier la commande", style = MaterialTheme.typography.labelSmall)
                    }
                    HorizontalDivider()
                    parsedItems.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                if (docMode == "LABELS") "[${item.barcode}] ${item.quantity}x ${item.garmentName}" else "${item.quantity}x ${item.garmentName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (showFinancials) {
                                Text(formatMoney(item.lineTotal, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (showFinancials) {
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL", fontWeight = FontWeight.ExtraBold)
                            Text(formatMoney(order.totalAmount, currency), fontWeight = FontWeight.ExtraBold, color = RoyalBlue)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PAYÉ", style = MaterialTheme.typography.bodySmall)
                            Text(formatMoney(order.paidAmount, currency), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RESTE", style = MaterialTheme.typography.bodySmall)
                            Text(formatMoney(order.remainingAmount, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(company.receiptFooter, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { shareTextIntent(context, "Document ${order.orderNumber}", buildReceiptText()) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Partager")
                    }
                    Button(
                        onClick = { printHtmlDocument(context, "${docMode}_${order.orderNumber}", buildDocumentHtml()) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Text("Imprimer / PDF")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReceiptDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Fermer") }
            }
        )
    }

    // Quick SAV Claim Creation Dialog pre-linked to this Order
    if (showClaimDialog) {
        var subject by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var severity by remember { mutableStateOf("MEDIUM") }
        AlertDialog(
            onDismissRequest = { showClaimDialog = false },
            title = { Text("Réclamation SAV · ${order.orderNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Client : ${order.clientName}", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Objet (ex: Retouche, Tache persistante) *") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Détails du signalement *") }, minLines = 2)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("LOW" to "Faible", "MEDIUM" to "Moyenne", "HIGH" to "Urgente").forEach { (c, l) ->
                            FilterChip(
                                selected = severity == c,
                                onClick = { severity = c },
                                modifier = Modifier.heightIn(min = 48.dp),
                                label = { Text(l) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subject.isNotBlank() && desc.isNotBlank()) {
                            onCreateClaim(
                                ClaimRecord(
                                    orderNumber = order.orderNumber,
                                    clientName = order.clientName,
                                    subject = subject.trim(),
                                    description = desc.trim(),
                                    severity = severity
                                )
                            )
                            showClaimDialog = false
                        }
                    },
                    enabled = subject.isNotBlank() && desc.isNotBlank(),
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClaimDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Annuler") }
            }
        )
    }

    previewPhotoGarment?.let { item ->
        val fullBmp = remember(item.photoBase64) { decodeBase64ToBitmap(item.photoBase64) }
        if (fullBmp != null) {
            AlertDialog(
                onDismissRequest = { previewPhotoGarment = null },
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${item.quantity}× ${item.garmentName}", fontWeight = FontWeight.Bold, softWrap = true)
                        Text(
                            "Étiquette : ${item.barcode.ifBlank { order.orderNumber }} · ${item.workshopStatusEnum().labelFr}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Image(
                        bitmap = fullBmp.asImageBitmap(),
                        contentDescription = item.garmentName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 340.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { previewPhotoGarment = null },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) { Text("Fermer") }
                }
            )
        }
    }
}
