package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.GARMENT_PHOTO_JPEG_QUALITY
import com.example.core.GARMENT_PHOTO_MAX_DIM
import com.example.core.compressBitmapToBase64
import com.example.core.decodeBase64ToBitmap
import com.example.core.decodeUriToBitmap
import com.example.core.estimateBase64SizeKb
import com.example.core.formatDateMillis
import com.example.core.formatMoney
import com.example.data.ActiveRoleSession
import com.example.data.ClientRecord
import com.example.data.CompanyConfig
import com.example.data.DefaultCatalogTemplates
import com.example.data.EmployeeRecord
import com.example.data.OrderGarmentItem
import com.example.data.OrderPriority
import com.example.data.PaymentMethod
import com.example.data.ServiceItem
import com.example.ui.components.MockupStepIndicator
import com.example.ui.components.QrBarcodeScannerDialog
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.viewmodel.GarmentVisionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewOrderScreen(
    company: CompanyConfig,
    existingOrderCount: Int,
    clients: List<ClientRecord>,
    services: List<ServiceItem>,
    employees: List<EmployeeRecord>,
    isBusy: Boolean,
    garmentVisionState: GarmentVisionState,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onAnalyzeGarmentPhoto: (Bitmap) -> Unit,
    onResetVision: () -> Unit,
    onQuickAddClient: (ClientRecord, (String) -> Unit) -> Unit,
    onSubmitOrder: (
        orderNumber: String,
        agencyName: String,
        clientId: String,
        clientName: String,
        clientPhone: String,
        clientAddress: String,
        priority: OrderPriority,
        items: List<OrderGarmentItem>,
        discountAmount: Long,
        expressFee: Long,
        deliveryFee: Long,
        taxAmount: Long,
        paidAmount: Long,
        paymentMethod: PaymentMethod,
        deliveryRequested: Boolean,
        deliveryZone: String,
        assignedEmployee: String,
        clientSignatureName: String,
        observations: String,
        dueDateMillis: Long
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val currency = company.currency
    val orderNumber = remember(existingOrderCount, company.orderPrefix) {
        "${company.orderPrefix}-${(existingOrderCount + 1).toString().padStart(5, '0')}"
    }
    var selectedAgency by remember(activeSession.isolatedAgency, company.agencies) {
        mutableStateOf(activeSession.isolatedAgency ?: company.agencies.firstOrNull() ?: "Agence Principale")
    }

    // Client selection (rememberSaveable survives screen rotation & incoming calls)
    var clientSearch by rememberSaveable { mutableStateOf("") }
    var selectedClientId by rememberSaveable { mutableStateOf("") }
    var selectedClientCategory by rememberSaveable { mutableStateOf("STANDARD") }
    var clientName by rememberSaveable { mutableStateOf("") }
    var clientPhone by rememberSaveable { mutableStateOf("") }
    var clientAddress by rememberSaveable { mutableStateOf("") }
    var showNewClientDialog by rememberSaveable { mutableStateOf(false) }

    // Order options
    var priorityName by rememberSaveable { mutableStateOf(OrderPriority.STANDARD.name) }
    val priority = OrderPriority.fromRaw(priorityName)
    var deliveryRequested by rememberSaveable { mutableStateOf(false) }
    var deliveryZone by rememberSaveable { mutableStateOf(company.commune) }
    var deliveryFeeStr by rememberSaveable { mutableStateOf("0") }
    var discountPercentStr by rememberSaveable { mutableStateOf("0") }
    var paymentModeChoice by rememberSaveable { mutableStateOf("PAID") } // PAID, PARTIAL, UNPAID
    var paymentMethodName by rememberSaveable { mutableStateOf(PaymentMethod.ESPECES.name) }
    val paymentMethod = PaymentMethod.fromRaw(paymentMethodName)
    var partialAmountStr by rememberSaveable { mutableStateOf("") }
    var assignedEmployee by rememberSaveable(employees, activeSession.operatorName) {
        mutableStateOf(employees.firstOrNull()?.fullName ?: activeSession.operatorName)
    }
    var clientSignatureName by rememberSaveable { mutableStateOf("") }
    var observations by rememberSaveable { mutableStateOf("") }
    var termsAccepted by rememberSaveable { mutableStateOf(true) }
    var isSubmittingOrder by remember { mutableStateOf(false) }

    LaunchedEffect(isBusy) {
        if (!isBusy) isSubmittingOrder = false
    }

    // Garments list & exhaustive pressing catalog
    val garments = remember { mutableStateListOf<OrderGarmentItem>() }
    var showAddGarmentDialog by remember { mutableStateOf(false) }
    var prefilledBarcodeForDialog by remember { mutableStateOf<String?>(null) }

    // Exhaustive catalog (merges Firestore custom services + 43 standard pressing garments)
    val exhaustiveCatalog = remember(services) {
        val existingNames = services.map { it.name.trim().lowercase() }.toSet()
        val defaults = DefaultCatalogTemplates.ivorianStarterServices()
            .filter { it.name.trim().lowercase() !in existingNames }
        (services.filter { it.available } + defaults)
    }
    val catalogCategories = remember(exhaustiveCatalog) {
        listOf("Tous") + exhaustiveCatalog.map { it.category }.distinct()
    }
    var selectedCatalogCategory by rememberSaveable { mutableStateOf("Tous") }
    var catalogSearchQuery by rememberSaveable { mutableStateOf("") }
    var showFullCatalog by rememberSaveable { mutableStateOf(false) }

    // Direct per-garment photo capture, AI analysis, zoom preview, and QR scanner state
    var pendingPhotoGarmentIdx by remember { mutableIntStateOf(-1) }
    var pendingVisionGarmentIdx by remember { mutableIntStateOf(-1) }
    var qrScanTargetGarmentIdx by remember { mutableIntStateOf(-1) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var previewPhotoGarment by remember { mutableStateOf<OrderGarmentItem?>(null) }

    val inlineCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        val idx = pendingPhotoGarmentIdx
        if (bmp != null && idx in garments.indices) {
            garments[idx] = garments[idx].copy(
                photoBase64 = compressBitmapToBase64(
                    bitmap = bmp,
                    maxDim = GARMENT_PHOTO_MAX_DIM,
                    quality = GARMENT_PHOTO_JPEG_QUALITY
                )
            )
        }
        pendingPhotoGarmentIdx = -1
    }

    val inlineCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingPhotoGarmentIdx in garments.indices) {
            inlineCameraLauncher.launch(null)
        } else {
            pendingPhotoGarmentIdx = -1
        }
    }

    val inlinePhotoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val idx = pendingPhotoGarmentIdx
        if (uri != null && idx in garments.indices) {
            decodeUriToBitmap(context, uri, maxDim = GARMENT_PHOTO_MAX_DIM)?.let { bmp ->
                garments[idx] = garments[idx].copy(
                    photoBase64 = compressBitmapToBase64(
                        bitmap = bmp,
                        maxDim = GARMENT_PHOTO_MAX_DIM,
                        quality = GARMENT_PHOTO_JPEG_QUALITY
                    )
                )
            }
        }
        pendingPhotoGarmentIdx = -1
    }

    // Bind inline Gemini Vision analysis back onto the targeted checked garment
    LaunchedEffect(garmentVisionState, pendingVisionGarmentIdx) {
        val idx = pendingVisionGarmentIdx
        if (!showAddGarmentDialog && idx in garments.indices && garmentVisionState is GarmentVisionState.Success) {
            val a = garmentVisionState.analysis
            val cur = garments[idx]
            garments[idx] = cur.copy(
                fabric = a.detectedFabric.ifBlank { cur.fabric },
                color = a.detectedColor.ifBlank { cur.color },
                soilLevel = a.soilLevel.ifBlank { cur.soilLevel },
                defects = a.visibleDefects.ifBlank { cur.defects },
                aiDiagnosis = a.careWarning
            )
            pendingVisionGarmentIdx = -1
        }
    }

    // Pricing Engine
    val subtotal = garments.sumOf { it.quantity * it.unitPrice }
    val expressFee = if (priority == OrderPriority.EXPRESS) {
        (subtotal * company.expressSurchargePercent / 100.0).toLong()
    } else 0L
    val isCashierCapped = activeSession.role == com.example.data.WorkspaceRole.CAISSIER
    val maxAllowedDiscount = if (isCashierCapped) company.maxCashierDiscountPercent.coerceIn(0.0, 100.0) else 100.0
    val rawDiscountPct = discountPercentStr.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
    val discountPct = rawDiscountPct.coerceAtMost(maxAllowedDiscount)
    val discountExceededCap = isCashierCapped && rawDiscountPct > maxAllowedDiscount
    val discountAmount = (subtotal * discountPct / 100.0).toLong()
    val deliveryFee = if (deliveryRequested) (deliveryFeeStr.toLongOrNull() ?: 0L) else 0L
    val taxableBase = (subtotal - discountAmount + expressFee + deliveryFee).coerceAtLeast(0L)
    val taxAmount = (taxableBase * company.taxRatePercent / 100.0).toLong()
    val totalAmount = taxableBase + taxAmount
    val paidAmount = when (paymentModeChoice) {
        "PAID" -> totalAmount
        "PARTIAL" -> (partialAmountStr.toLongOrNull() ?: 0L).coerceIn(0L, totalAmount)
        else -> 0L
    }
    val delayHours = when (priority) {
        OrderPriority.EXPRESS -> company.expressDelayHours
        OrderPriority.ECONOMY -> company.standardDelayHours + 24L
        OrderPriority.STANDARD -> company.standardDelayHours
    }
    val dueDateMillis = remember(priority, company.standardDelayHours, company.expressDelayHours) {
        System.currentTimeMillis() + delayHours * 3_600_000L
    }

    var formError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nouvelle commande", fontWeight = FontWeight.Bold)
                        Text(orderNumber, style = MaterialTheme.typography.labelSmall, color = Color(0xFFD6E4FF))
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightNavy,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total à payer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                formatMoney(totalAmount, currency),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (isBusy || isSubmittingOrder) return@Button
                                when {
                                    clientName.isBlank() -> formError = "Sélectionnez ou saisissez le nom du client."
                                    garments.isEmpty() -> formError = "Ajoutez au moins un vêtement à la commande."
                                    !termsAccepted -> formError = "Le client doit valider les conditions de dépôt."
                                    else -> {
                                        formError = null
                                        isSubmittingOrder = true
                                        onSubmitOrder(
                                            orderNumber,
                                            activeSession.isolatedAgency ?: selectedAgency,
                                            selectedClientId,
                                            clientName,
                                            clientPhone,
                                            clientAddress,
                                            priority,
                                            garments.toList(),
                                            discountAmount,
                                            expressFee,
                                            deliveryFee,
                                            taxAmount,
                                            paidAmount,
                                            paymentMethod,
                                            deliveryRequested,
                                            deliveryZone,
                                            assignedEmployee,
                                            clientSignatureName.ifBlank { clientName },
                                            observations,
                                            dueDateMillis
                                        )
                                    }
                                }
                            },
                            enabled = !isBusy && !isSubmittingOrder,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("submit_order_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                        ) {
                            if (isBusy || isSubmittingOrder) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Envoi...", fontWeight = FontWeight.Bold, maxLines = 1)
                            } else {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Enregistrer", fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val activeStepIndex = when {
            clientName.isBlank() -> 0
            garments.isEmpty() -> 1
            clientSignatureName.isBlank() && paymentModeChoice == "PARTIAL" && partialAmountStr.isBlank() -> 2
            else -> 3
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Pressing Pro mockup hero banner + 4-step workflow bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, PastelBlueBorder)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(MidnightNavy, RoyalBlue)))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nouvelle commande – Pressing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Enregistrez une commande de lavage, nettoyage et repassage.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD6E4FF)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AfricaGold.copy(alpha = 0.20f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Checkroom, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(15.dp))
                                Text("Pressing Pro", style = MaterialTheme.typography.labelSmall, color = AfricaGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    MockupStepIndicator(
                        steps = listOf("Client", "Articles", "Traitement", "Paiement"),
                        currentStepIndex = activeStepIndex,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }

            if (formError != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(formError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                }
            }

            // Isolated Agency Assignment Banner / Selector
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PastelBlueTint,
                border = BorderStroke(1.dp, PastelBlueBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Business, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (activeSession.isolatedAgency != null) {
                                "Entité isolée verrouillée : ${activeSession.isolatedAgency}"
                            } else {
                                "Agence d'affectation : $selectedAgency"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MidnightNavy
                        )
                    }
                    if (activeSession.isolatedAgency == null && company.agencies.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            company.agencies.forEach { ag ->
                                FilterChip(
                                    selected = selectedAgency == ag,
                                    onClick = { selectedAgency = ag },
                                    label = { Text(ag) }
                                )
                            }
                        }
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWideForm = maxWidth >= 780.dp
                val leftColumnContent: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Section 1: Client Info
                        val filteredClients = remember(clients, clientSearch) {
                            if (clientSearch.isBlank()) clients.take(6)
                            else clients.filter {
                                it.fullName.contains(clientSearch, true) || it.phone.contains(clientSearch, true)
                            }.take(8)
                        }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "1. Informations du client",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                softWrap = true
                            )
                            if (selectedClientId.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(50), color = EmeraldContainer) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(13.dp))
                                        Text(
                                            text = when (selectedClientCategory) {
                                                "VIP" -> "VIP"
                                                "CORPORATE" -> "Entreprise"
                                                else -> "Fidèle"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF047857),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = { showNewClientDialog = true },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nouveau client")
                        }
                    }
                    if (clients.isNotEmpty()) {
                        if (clients.size > 5) {
                            OutlinedTextField(
                                value = clientSearch,
                                onValueChange = { clientSearch = it },
                                placeholder = { Text("Filtrer un client existant (nom ou téléphone)...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            filteredClients.forEach { c ->
                                FilterChip(
                                    selected = selectedClientId == c.id,
                                    onClick = {
                                        selectedClientId = c.id
                                        selectedClientCategory = c.category
                                        clientName = c.fullName
                                        clientPhone = c.phone
                                        clientAddress = c.address
                                        if (c.discountPercent > 0) discountPercentStr = c.discountPercent.toInt().toString()
                                    },
                                    label = { Text(c.fullName) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nom du client *") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("order_client_name_input"),
                        singleLine = true
                    )
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth >= 420.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = clientPhone,
                                    onValueChange = { clientPhone = it },
                                    label = { Text("Téléphone / WhatsApp") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = clientAddress,
                                    onValueChange = { clientAddress = it },
                                    label = { Text("Quartier / Adresse") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp),
                                    singleLine = true
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = clientPhone,
                                    onValueChange = { clientPhone = it },
                                    label = { Text("Téléphone / WhatsApp") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = clientAddress,
                                    onValueChange = { clientAddress = it },
                                    label = { Text("Quartier / Adresse") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Exhaustive Garment Checklist + Direct Photo per Checked Garment + QR Scanner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "2. Vêtements & Photos (${garments.size} coché(s))",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cochez les vêtements dans la liste exhaustive, scannez un code QR et ajoutez une photo sur chaque vêtement coché.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    qrScanTargetGarmentIdx = -1
                                    showQrScannerDialog = true
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("order_scan_qr_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightNavy)
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Scanner QR", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    onResetVision()
                                    prefilledBarcodeForDialog = null
                                    showAddGarmentDialog = true
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("order_add_garment_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Article libre", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Exhaustive Categorized Pressing Garment Checklist
                    val filteredCatalog = remember(exhaustiveCatalog, selectedCatalogCategory, catalogSearchQuery) {
                        exhaustiveCatalog.filter { srv ->
                            val matchesCat = selectedCatalogCategory == "Tous" || srv.category == selectedCatalogCategory
                            val q = catalogSearchQuery.trim()
                            val matchesQ = q.isBlank() ||
                                srv.name.contains(q, ignoreCase = true) ||
                                srv.category.contains(q, ignoreCase = true) ||
                                srv.treatmentType.contains(q, ignoreCase = true)
                            matchesCat && matchesQ
                        }
                    }
                    val visibleCatalog = if (showFullCatalog || catalogSearchQuery.isNotBlank() || selectedCatalogCategory != "Tous") {
                        filteredCatalog
                    } else {
                        filteredCatalog.take(12)
                    }

                    OutlinedTextField(
                        value = catalogSearchQuery,
                        onValueChange = { catalogSearchQuery = it },
                        placeholder = { Text("Rechercher parmi ${exhaustiveCatalog.size} vêtements (bazin, costume, couette...)") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (catalogSearchQuery.isNotBlank()) {
                                IconButton(onClick = { catalogSearchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Effacer")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("order_catalog_search_input")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(catalogCategories) { cat ->
                            FilterChip(
                                selected = selectedCatalogCategory == cat,
                                onClick = { selectedCatalogCategory = cat },
                                modifier = Modifier.heightIn(min = 48.dp),
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Text(
                        text = "Liste exhaustive du pressing (${filteredCatalog.size} articles) — cochez pour ajouter :",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        visibleCatalog.forEach { srv ->
                            val checkedIdx = garments.indexOfFirst { it.garmentName.equals(srv.name, ignoreCase = true) }
                            val isChecked = checkedIdx >= 0
                            Surface(
                                onClick = {
                                    if (isChecked) {
                                        garments.removeAt(checkedIdx)
                                    } else {
                                        garments.add(
                                            OrderGarmentItem(
                                                garmentName = srv.name,
                                                serviceName = srv.treatmentType,
                                                quantity = 1L,
                                                unitPrice = srv.unitPrice,
                                                lineTotal = srv.unitPrice,
                                                barcode = "${orderNumber.takeLast(5)}-${garments.size + 1}"
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChecked) SoftBlueContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isChecked) RoyalBlue else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("catalog_check_${srv.name}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked && !isChecked) {
                                                garments.add(
                                                    OrderGarmentItem(
                                                        garmentName = srv.name,
                                                        serviceName = srv.treatmentType,
                                                        quantity = 1L,
                                                        unitPrice = srv.unitPrice,
                                                        lineTotal = srv.unitPrice,
                                                        barcode = "${orderNumber.takeLast(5)}-${garments.size + 1}"
                                                    )
                                                )
                                            } else if (!checked && isChecked) {
                                                garments.removeAt(checkedIdx)
                                            }
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = srv.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 2,
                                            softWrap = true,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${srv.category} · ${srv.treatmentType}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = formatMoney(srv.unitPrice, currency),
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue
                                    )
                                }
                            }
                        }

                        if (!showFullCatalog && filteredCatalog.size > visibleCatalog.size) {
                            OutlinedButton(
                                onClick = { showFullCatalog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("show_all_garments_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Afficher les ${filteredCatalog.size - visibleCatalog.size} autres vêtements du pressing",
                                    softWrap = true
                                )
                            }
                        } else if (showFullCatalog && selectedCatalogCategory == "Tous" && catalogSearchQuery.isBlank()) {
                            TextButton(
                                onClick = { showFullCatalog = false },
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("Réduire la liste")
                            }
                        }
                    }

                    // Checked Garments Section — Field-Optimized Card (Photo Thumbnail + QR + Stepper + IA)
                    if (garments.isNotEmpty()) {
                        HorizontalDivider()
                        val photosCount = garments.count { it.photoBase64.isNotBlank() }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Vêtements cochés (${garments.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MidnightNavy
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (photosCount == garments.size) EmeraldContainer else AmberContainer
                            ) {
                                Text(
                                    text = "$photosCount / ${garments.size} avec photo (1024px · 80%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (photosCount == garments.size) Color(0xFF047857) else Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        garments.forEachIndexed { idx, item ->
                            val bmp = remember(item.photoBase64) { decodeBase64ToBitmap(item.photoBase64) }
                            val photoSizeKb = remember(item.photoBase64) { estimateBase64SizeKb(item.photoBase64) }
                            val isAnalyzingThis = pendingVisionGarmentIdx == idx && garmentVisionState is GarmentVisionState.Loading
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = 1.2.dp,
                                    color = if (bmp != null) EmeraldSuccess.copy(alpha = 0.55f) else PastelBlueBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checked_garment_card_$idx")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Top Row: Captured Thumbnail + Title/Service/QR + Remove Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (bmp != null) Color.Black.copy(alpha = 0.06f) else SoftBlueContainer)
                                                .clickable {
                                                    if (bmp != null) {
                                                        previewPhotoGarment = item
                                                    } else {
                                                        pendingPhotoGarmentIdx = idx
                                                        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                                        if (ok) inlineCameraLauncher.launch(null)
                                                        else inlineCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                    }
                                                }
                                                .testTag("garment_photo_thumb_$idx"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Photo de ${item.garmentName}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                                                    color = MidnightNavy.copy(alpha = 0.78f),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "${photoSizeKb} Ko",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                                    modifier = Modifier.padding(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.CameraAlt,
                                                        contentDescription = "Ajouter une photo",
                                                        tint = RoyalBlue,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Text(
                                                        text = "Photo",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = RoyalBlue,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = item.garmentName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MidnightNavy,
                                                maxLines = 2,
                                                softWrap = true,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.serviceName} · ${formatMoney(item.unitPrice, currency)}/u",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                softWrap = true
                                            )
                                            Surface(
                                                onClick = {
                                                    qrScanTargetGarmentIdx = idx
                                                    showQrScannerDialog = true
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = PastelBlueTint,
                                                border = BorderStroke(1.dp, PastelBlueBorder),
                                                modifier = Modifier.testTag("garment_qr_badge_$idx")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.QrCode2,
                                                        contentDescription = null,
                                                        tint = RoyalBlue,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = item.barcode.ifBlank { "QR non assigné" },
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                                        color = MidnightNavy,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { garments.removeAt(idx) },
                                            modifier = Modifier
                                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                .testTag("garment_remove_btn_$idx")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Retirer ${item.garmentName}",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Defects / AI Diagnosis Banners (Full-width, never squeezed)
                                    if (item.defects.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = AmberContainer,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Défaut détecté : ${item.defects}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF92400E),
                                                fontWeight = FontWeight.SemiBold,
                                                softWrap = true,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                    if (item.aiDiagnosis.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SoftBlueContainer,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Diagnostic IA (${item.fabric}${if (item.color.isNotBlank()) " · ${item.color}" else ""}) : ${item.aiDiagnosis}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MidnightNavy,
                                                softWrap = true,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    // Dedicated Quantity Stepper (+/-) & Subtotal Row (Zero Collision)
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        if (item.quantity > 1L) {
                                                            garments[idx] = item.copy(
                                                                quantity = item.quantity - 1L,
                                                                lineTotal = (item.quantity - 1L) * item.unitPrice
                                                            )
                                                        }
                                                    },
                                                    enabled = item.quantity > 1L,
                                                    modifier = Modifier
                                                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                        .testTag("garment_qty_minus_$idx")
                                                ) {
                                                    Icon(Icons.Rounded.Remove, contentDescription = "Diminuer quantité", modifier = Modifier.size(18.dp))
                                                }
                                                Text(
                                                    text = "${item.quantity}",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MidnightNavy,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        garments[idx] = item.copy(
                                                            quantity = item.quantity + 1L,
                                                            lineTotal = (item.quantity + 1L) * item.unitPrice
                                                        )
                                                    },
                                                    modifier = Modifier
                                                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                        .testTag("garment_qty_plus_$idx")
                                                ) {
                                                    Icon(Icons.Rounded.Add, contentDescription = "Augmenter quantité", modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Sous-total ligne",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatMoney(item.lineTotal, currency),
                                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = RoyalBlue
                                            )
                                        }
                                    }

                                    // Direct Field Action Bar: Camera, Gallery, QR Scan, Inspecter IA (48dp touch targets)
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                pendingPhotoGarmentIdx = idx
                                                val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                                if (ok) inlineCameraLauncher.launch(null)
                                                else inlineCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("garment_camera_btn_$idx")
                                        ) {
                                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                if (bmp == null) "Caméra" else "Reprendre",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                pendingPhotoGarmentIdx = idx
                                                inlinePhotoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("garment_gallery_btn_$idx")
                                        ) {
                                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Galerie", style = MaterialTheme.typography.labelMedium)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                qrScanTargetGarmentIdx = idx
                                                showQrScannerDialog = true
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("garment_qr_btn_$idx")
                                        ) {
                                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Scanner QR", style = MaterialTheme.typography.labelMedium)
                                        }
                                        Button(
                                            onClick = {
                                                if (bmp != null) {
                                                    pendingVisionGarmentIdx = idx
                                                    onAnalyzeGarmentPhoto(bmp)
                                                }
                                            },
                                            enabled = bmp != null && !isAnalyzingThis,
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MidnightNavy,
                                                contentColor = Color.White,
                                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("garment_ai_btn_$idx")
                                        ) {
                                            if (isAnalyzingThis) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AfricaGold, strokeWidth = 2.dp)
                                                Spacer(Modifier.width(6.dp))
                                                Text("Analyse IA...", style = MaterialTheme.typography.labelMedium)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (bmp != null) AfricaGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Inspecter IA", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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

                val rightColumnContent: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Section 3: Priority, Delivery & Pricing Breakdown
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("3. Délai, Livraison & Tarification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OrderPriority.entries.forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priorityName = p.name },
                                modifier = Modifier.heightIn(min = 48.dp),
                                label = {
                                    Text(
                                        if (p == OrderPriority.EXPRESS) "${p.labelFr} (+${company.expressSurchargePercent.toInt()}%)"
                                        else p.labelFr
                                    )
                                }
                            )
                        }
                    }
                    Text(
                        "Date estimée de disponibilité : ${formatDateMillis(dueDateMillis)} (${delayHours}h)",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoyalBlue,
                        fontWeight = FontWeight.SemiBold,
                        softWrap = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Livraison à domicile demandée", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), softWrap = true)
                        Switch(checked = deliveryRequested, onCheckedChange = { deliveryRequested = it })
                    }
                    if (deliveryRequested) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            if (maxWidth >= 420.dp) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = deliveryZone,
                                        onValueChange = { deliveryZone = it },
                                        label = { Text("Zone de livraison") },
                                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = deliveryFeeStr,
                                        onValueChange = { deliveryFeeStr = it.filter { c -> c.isDigit() } },
                                        label = { Text("Frais ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.8f).heightIn(min = 56.dp),
                                        singleLine = true
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = deliveryZone,
                                        onValueChange = { deliveryZone = it },
                                        label = { Text("Zone de livraison") },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = deliveryFeeStr,
                                        onValueChange = { deliveryFeeStr = it.filter { c -> c.isDigit() } },
                                        label = { Text("Frais ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    if (employees.isNotEmpty()) {
                        Text("Opérateur / Réceptionniste assigné :", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            employees.take(6).forEach { emp ->
                                FilterChip(
                                    selected = assignedEmployee == emp.fullName,
                                    onClick = { assignedEmployee = emp.fullName },
                                    label = { Text(emp.fullName) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = discountPercentStr,
                        onValueChange = { discountPercentStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Remise commerciale (%)") },
                        supportingText = if (isCashierCapped) {
                            {
                                Text(
                                    text = if (discountExceededCap) {
                                        "Plafond Caissier appliqué : ${maxAllowedDiscount.toInt()}% maximum sans validation Gérant."
                                    } else {
                                        "Plafond Caissier autorisé : max ${maxAllowedDiscount.toInt()}% (remise entreprises : ${company.corporateDiscountPercent.toInt()}%)."
                                    },
                                    color = if (discountExceededCap) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    softWrap = true
                                )
                            }
                        } else null,
                        isError = discountExceededCap,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        singleLine = true
                    )

                    HorizontalDivider()
                    SummaryLine("Nombre d'articles", "${garments.sumOf { it.quantity }}")
                    SummaryLine("Sous-total", formatMoney(subtotal, currency))
                    if (expressFee > 0) SummaryLine("Supplément Express", "+ ${formatMoney(expressFee, currency)}")
                    if (discountAmount > 0) SummaryLine("Remise", "- ${formatMoney(discountAmount, currency)}")
                    if (deliveryFee > 0) SummaryLine("Frais de livraison", "+ ${formatMoney(deliveryFee, currency)}")
                    if (taxAmount > 0) SummaryLine("TVA (${company.taxRatePercent}%)", "+ ${formatMoney(taxAmount, currency)}")

                    // Highlighted Total à payer callout matching the Pressing Pro mockup
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SoftBlueContainer,
                        border = BorderStroke(1.dp, PastelBlueBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total à payer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MidnightNavy
                            )
                            Text(
                                text = formatMoney(totalAmount, currency),
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalBlue
                            )
                        }
                    }
                }
            }

            // Section 4: Payment & Electronic Signature
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("4. Encaissement & Signature client", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PAID" to "Payé intégralement", "PARTIAL" to "Acompte", "UNPAID" to "À payer au retrait").forEach { (k, lbl) ->
                            FilterChip(
                                selected = paymentModeChoice == k,
                                onClick = { paymentModeChoice = k },
                                label = { Text(lbl) }
                            )
                        }
                    }
                    if (paymentModeChoice == "PARTIAL") {
                        OutlinedTextField(
                            value = partialAmountStr,
                            onValueChange = { partialAmountStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Montant de l'acompte reçu ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Text("Mode de paiement :", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethodName = method.name },
                                label = { Text(method.labelFr) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = clientSignatureName,
                        onValueChange = { clientSignatureName = it },
                        label = { Text("Signature électronique client (Nom / Initiales)") },
                        placeholder = { Text(clientName.ifBlank { "Nom du déposant" }) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = observations,
                        onValueChange = { observations = it },
                        label = { Text("Observations générales (facultatif)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = termsAccepted, onCheckedChange = { termsAccepted = it })
                        Text(
                            text = "Le client valide l'état initial des vêtements et les conditions de dépôt.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
                    }
                }

                if (isWideForm) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(1.25f)) { leftColumnContent() }
                        Box(modifier = Modifier.weight(1f)) { rightColumnContent() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        leftColumnContent()
                        rightColumnContent()
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAddGarmentDialog) {
        AddGarmentWithPhotoDialog(
            services = exhaustiveCatalog,
            currency = currency,
            barcodePrefix = prefilledBarcodeForDialog ?: "${orderNumber.takeLast(5)}-${garments.size + 1}",
            visionState = garmentVisionState,
            onAnalyzePhoto = onAnalyzeGarmentPhoto,
            onDismiss = {
                showAddGarmentDialog = false
                prefilledBarcodeForDialog = null
            },
            onConfirm = { newItem ->
                garments.add(newItem)
                showAddGarmentDialog = false
                prefilledBarcodeForDialog = null
            }
        )
    }

    if (showQrScannerDialog) {
        QrBarcodeScannerDialog(
            title = if (qrScanTargetGarmentIdx in garments.indices) {
                "Associer étiquette QR au vêtement"
            } else {
                "Scanner QR / Code-barres article"
            },
            subtitle = "Scannez une étiquette pressing ou un code-barres pour enregistrer l'article.",
            onCodeScanned = { code ->
                showQrScannerDialog = false
                val targetIdx = qrScanTargetGarmentIdx
                if (targetIdx in garments.indices) {
                    garments[targetIdx] = garments[targetIdx].copy(barcode = code)
                    qrScanTargetGarmentIdx = -1
                } else {
                    // Check if scanned code matches a catalog garment name, else open AddGarment dialog pre-filled with scanned barcode
                    val catalogMatch = exhaustiveCatalog.firstOrNull {
                        it.name.equals(code, ignoreCase = true) || code.contains(it.name, ignoreCase = true)
                    }
                    if (catalogMatch != null) {
                        garments.add(
                            OrderGarmentItem(
                                garmentName = catalogMatch.name,
                                serviceName = catalogMatch.treatmentType,
                                quantity = 1L,
                                unitPrice = catalogMatch.unitPrice,
                                lineTotal = catalogMatch.unitPrice,
                                barcode = code
                            )
                        )
                    } else {
                        onResetVision()
                        prefilledBarcodeForDialog = code
                        showAddGarmentDialog = true
                    }
                }
            },
            onDismiss = {
                showQrScannerDialog = false
                qrScanTargetGarmentIdx = -1
            }
        )
    }

    if (showNewClientDialog) {
        var cName by remember { mutableStateOf("") }
        var cPhone by remember { mutableStateOf("+225 ") }
        var cAddr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewClientDialog = false },
            title = { Text("Nouveau client rapide", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cName, onValueChange = { cName = it }, label = { Text("Nom complet *") }, singleLine = true)
                    OutlinedTextField(value = cPhone, onValueChange = { cPhone = it }, label = { Text("Téléphone / WhatsApp *") }, singleLine = true)
                    OutlinedTextField(value = cAddr, onValueChange = { cAddr = it }, label = { Text("Adresse / Quartier") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cName.isNotBlank() && cPhone.isNotBlank()) {
                            onQuickAddClient(
                                ClientRecord(fullName = cName.trim(), phone = cPhone.trim(), whatsapp = cPhone.trim(), address = cAddr.trim())
                            ) { createdId ->
                                selectedClientId = createdId
                                clientName = cName.trim()
                                clientPhone = cPhone.trim()
                                clientAddress = cAddr.trim()
                                showNewClientDialog = false
                            }
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewClientDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Annuler") }
            }
        )
    }

    previewPhotoGarment?.let { item ->
        val fullBmp = remember(item.photoBase64) { decodeBase64ToBitmap(item.photoBase64) }
        val sizeKb = remember(item.photoBase64) { estimateBase64SizeKb(item.photoBase64) }
        if (fullBmp != null) {
            AlertDialog(
                onDismissRequest = { previewPhotoGarment = null },
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.garmentName, fontWeight = FontWeight.Bold, softWrap = true)
                        Text(
                            "Étiquette : ${item.barcode.ifBlank { "—" }} · ${fullBmp.width}×${fullBmp.height}px (${sizeKb} Ko)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Image(
                        bitmap = fullBmp.asImageBitmap(),
                        contentDescription = "Aperçu photo de ${item.garmentName}",
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

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddGarmentWithPhotoDialog(
    services: List<ServiceItem>,
    currency: String,
    barcodePrefix: String,
    visionState: GarmentVisionState,
    onAnalyzePhoto: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (OrderGarmentItem) -> Unit
) {
    val context = LocalContext.current
    var garmentName by remember { mutableStateOf(services.firstOrNull()?.name ?: "Chemise") }
    var serviceName by remember { mutableStateOf(services.firstOrNull()?.treatmentType ?: "Lavage + Repassage") }
    var unitPriceStr by remember { mutableStateOf((services.firstOrNull()?.unitPrice ?: 1000L).toString()) }
    var quantityStr by remember { mutableStateOf("1") }
    var fabric by remember { mutableStateOf("Coton") }
    var color by remember { mutableStateOf("") }
    var soilLevel by remember { mutableStateOf("Normal") }
    var defects by remember { mutableStateOf("") }
    var aiDiagnosis by remember { mutableStateOf("") }
    var humanValidated by remember { mutableStateOf(true) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(visionState) {
        if (visionState is GarmentVisionState.Success) {
            val a = visionState.analysis
            garmentName = a.garmentType.ifBlank { garmentName }
            color = a.detectedColor
            fabric = a.detectedFabric
            soilLevel = a.soilLevel
            defects = a.visibleDefects
            serviceName = a.recommendedTreatment
            aiDiagnosis = "${a.careWarning} (Validation humaine requise)"
            humanValidated = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) capturedBitmap = bmp
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            decodeUriToBitmap(context, uri, maxDim = GARMENT_PHOTO_MAX_DIM)?.let { capturedBitmap = it }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enregistrer un vêtement + Photo", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Camera & Zero-Permission Photo Picker Row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (ok) cameraLauncher.launch(null)
                            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Caméra", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Galerie", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (capturedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Photo du vêtement",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Button(
                        onClick = { onAnalyzePhoto(capturedBitmap!!) },
                        enabled = visionState !is GarmentVisionState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightNavy)
                    ) {
                        if (visionState is GarmentVisionState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AfricaGold, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Inspection IA Gemini en cours...")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Inspecter IA (défauts & tissu)")
                        }
                    }
                }

                if (visionState is GarmentVisionState.Error) {
                    Text(visionState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (aiDiagnosis.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = AmberContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Diagnostic IA (indicatif) :", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text(aiDiagnosis, style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = humanValidated, onCheckedChange = { humanValidated = it })
                                Text("J'ai vérifié physiquement le vêtement *", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                OutlinedTextField(value = garmentName, onValueChange = { garmentName = it }, label = { Text("Vêtement *") }, singleLine = true)
                OutlinedTextField(value = serviceName, onValueChange = { serviceName = it }, label = { Text("Prestation") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Qté") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.6f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unitPriceStr,
                        onValueChange = { unitPriceStr = it.filter { c -> c.isDigit() } },
                        label = { Text("PU ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fabric, onValueChange = { fabric = it }, label = { Text("Tissu") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Couleur") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Léger", "Normal", "Élevé").forEach { lvl ->
                        FilterChip(
                            selected = soilLevel == lvl,
                            onClick = { soilLevel = lvl },
                            modifier = Modifier.heightIn(min = 48.dp),
                            label = { Text(lvl) }
                        )
                    }
                }
                OutlinedTextField(
                    value = defects,
                    onValueChange = { defects = it },
                    label = { Text("Taches, déchirures, boutons manquants") },
                    singleLine = false
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = (quantityStr.toLongOrNull() ?: 1L).coerceAtLeast(1L)
                    val pu = (unitPriceStr.toLongOrNull() ?: 1000L).coerceAtLeast(0L)
                    val encodedPhoto = capturedBitmap?.let {
                        compressBitmapToBase64(
                            bitmap = it,
                            maxDim = GARMENT_PHOTO_MAX_DIM,
                            quality = GARMENT_PHOTO_JPEG_QUALITY
                        )
                    }.orEmpty()
                    onConfirm(
                        OrderGarmentItem(
                            garmentName = garmentName.trim().ifBlank { "Vêtement" },
                            serviceName = serviceName.trim().ifBlank { "Lavage + Repassage" },
                            fabric = fabric,
                            color = color,
                            soilLevel = soilLevel,
                            defects = defects,
                            quantity = q,
                            unitPrice = pu,
                            lineTotal = q * pu,
                            barcode = barcodePrefix,
                            photoBase64 = encodedPhoto,
                            aiDiagnosis = aiDiagnosis
                        )
                    )
                },
                enabled = garmentName.isNotBlank() && humanValidated,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
            ) { Text("Ajouter à la commande") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Annuler") }
        }
    )
}
