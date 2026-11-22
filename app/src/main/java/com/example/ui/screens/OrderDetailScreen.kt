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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.core.formatDateMillis
import com.example.core.formatDateTime
import com.example.core.formatMoney
import com.example.core.openWhatsAppIntent
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
import com.example.ui.components.RoleBadgeChip
import com.example.ui.components.ThermalPrintPreviewDialog
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
    autoOpenPrintPreview: Boolean = false,
    onAutoPrintConsumed: () -> Unit = {},
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
    val canDispatchDelivery = activeSession.has(AccessPermission.MANAGE_DELIVERY_RUNS)
    val canCreateClaims = activeSession.has(AccessPermission.HANDLE_SAV_CLAIMS)

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeliveryDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showClaimDialog by remember { mutableStateOf(false) }
    var previewPhotoGarment by remember { mutableStateOf<OrderGarmentItem?>(null) }

    LaunchedEffect(autoOpenPrintPreview) {
        if (autoOpenPrintPreview) {
            showReceiptDialog = true
            onAutoPrintConsumed()
        }
    }

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
        var isSubmittingPayment by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isSubmittingPayment) showPaymentDialog = false },
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
                val parsedAmount = amountStr.toLongOrNull() ?: 0L
                Button(
                    onClick = {
                        if (!isBusy && !isSubmittingPayment && parsedAmount > 0L) {
                            isSubmittingPayment = true
                            onRecordPayment(order, parsedAmount, method)
                            showPaymentDialog = false
                        }
                    },
                    enabled = !isBusy && !isSubmittingPayment && parsedAmount > 0L,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("order_detail_confirm_payment_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) { Text("Confirmer l'encaissement") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPaymentDialog = false },
                    enabled = !isSubmittingPayment,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Annuler") }
            }
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

    // Responsive Thermal Receipt (58mm / 80mm) & Individual Physical Garment Labels Preview
    if (showReceiptDialog) {
        ThermalPrintPreviewDialog(
            company = company,
            order = order,
            activeSession = activeSession,
            initialMode = if (company.autoPrintLabelsOnPayment && order.remainingAmount == 0L && !activeSession.canPrintFinancialReceipt) {
                "LABELS"
            } else {
                null
            },
            onDismiss = { showReceiptDialog = false }
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
