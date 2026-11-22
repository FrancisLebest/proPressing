package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.BluetoothThermalPrinterManager
import com.example.core.PhysicalGarmentLabel
import com.example.core.ThermalLine
import com.example.core.ThermalLineStyle
import com.example.core.ThermalPaperWidth
import com.example.core.ThermalReceiptFormatter
import com.example.core.printCalibratedThermalHtml
import com.example.core.renderThermalBitmap
import com.example.core.shareThermalBitmapPng
import com.example.data.ActiveRoleSession
import com.example.data.CompanyConfig
import com.example.data.PressingOrder
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.RoyalBlue
import kotlinx.coroutines.launch

/**
 * Full responsive Thermal Receipt & Individual Physical Garment Labels Preview Dialog.
 * - Faithful white thermal paper rendering with serrated top & bottom edges (58mm / 80mm).
 * - Strict RBAC: workshop roles (Opérateur / Contrôle Qualité) only see Garment Labels without FCFA prices.
 * - 3 clear actions: Bluetooth ESC/POS, Android Print / PDF, and WhatsApp / Image PNG Share.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThermalPrintPreviewDialog(
    company: CompanyConfig,
    order: PressingOrder,
    activeSession: ActiveRoleSession,
    initialMode: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedPrefs = remember(company) { BluetoothThermalPrinterManager.loadPrefs(context, company) }

    val canShowClientReceipt = activeSession.canPrintFinancialReceipt
    val defaultMode = when {
        initialMode == "LABELS" -> "LABELS"
        !canShowClientReceipt -> "LABELS"
        else -> "RECEIPT"
    }

    var activeMode by remember(canShowClientReceipt, initialMode) { mutableStateOf(defaultMode) }
    var paperWidth by remember(savedPrefs) { mutableStateOf(savedPrefs.paperWidth) }
    var isPrintingBt by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    val receiptLines = remember(company, order, paperWidth, canShowClientReceipt) {
        ThermalReceiptFormatter.buildThermalReceiptLines(
            company = company,
            order = order,
            paperWidth = paperWidth,
            includeFinancials = canShowClientReceipt
        )
    }
    val garmentLabels = remember(order) {
        ThermalReceiptFormatter.buildPhysicalGarmentLabels(order)
    }
    val orderQrPayload = remember(order.orderNumber, order.trackingToken) {
        val tokenSegment = order.trackingToken.ifBlank { order.orderNumber }
        "PRESSINGPRO|${order.orderNumber}|$tokenSegment"
    }

    fun triggerBluetoothPrint() {
        val prefs = BluetoothThermalPrinterManager.loadPrefs(context, company)
        if (prefs.printerAddress.isBlank()) {
            statusIsError = true
            statusMessage = "Aucune imprimante Bluetooth enregistrée. Allez dans Paramètres > Imprimante Bluetooth pour en choisir une."
            return
        }
        scope.launch {
            isPrintingBt = true
            statusMessage = null
            val payload = if (activeMode == "RECEIPT" && canShowClientReceipt) {
                ThermalReceiptFormatter.encodeEscPosBytes(
                    lines = receiptLines,
                    qrContent = orderQrPayload,
                    barcodeContent = null,
                    paperWidth = paperWidth,
                    autoCut = prefs.autoCut
                )
            } else {
                val combined = java.io.ByteArrayOutputStream()
                garmentLabels.forEach { lbl ->
                    val lblLines = ThermalReceiptFormatter.buildPhysicalGarmentLabelLines(company, lbl, paperWidth)
                    combined.write(
                        ThermalReceiptFormatter.encodeEscPosBytes(
                            lines = lblLines,
                            qrContent = lbl.barcodeValue,
                            barcodeContent = lbl.barcodeValue,
                            paperWidth = paperWidth,
                            autoCut = prefs.autoCut
                        )
                    )
                }
                combined.toByteArray()
            }
            val result = BluetoothThermalPrinterManager.sendEscPosToPrinter(
                context = context,
                printerAddress = prefs.printerAddress,
                payload = payload
            )
            isPrintingBt = false
            result.fold(
                onSuccess = { msg ->
                    statusIsError = false
                    statusMessage = msg
                },
                onFailure = { err ->
                    statusIsError = true
                    statusMessage = err.localizedMessage ?: "Échec de connexion à l'imprimante Bluetooth."
                }
            )
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            triggerBluetoothPrint()
        } else {
            statusIsError = true
            statusMessage = "Permission Bluetooth requise pour imprimer directement sur l'imprimante thermique."
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(color = MidnightNavy, contentColor = Color.White) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Impression & Aperçu Thermique",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                softWrap = true
                            )
                            Text(
                                text = "${order.orderNumber} · ${garmentLabels.size} pièce(s) physique(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD5E2FF)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("thermal_preview_close_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Fermer l'aperçu", tint = Color.White)
                        }
                    }
                }

                // Mode & Paper Width Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (canShowClientReceipt) {
                            FilterChip(
                                selected = activeMode == "RECEIPT",
                                onClick = { activeMode = "RECEIPT" },
                                leadingIcon = { Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Reçu Client") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("thermal_tab_receipt")
                            )
                        }
                        FilterChip(
                            selected = activeMode == "LABELS",
                            onClick = { activeMode = "LABELS" },
                            leadingIcon = { Icon(Icons.Rounded.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Étiquettes (${garmentLabels.size})") },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("thermal_tab_labels")
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThermalPaperWidth.entries.forEach { widthOpt ->
                            FilterChip(
                                selected = paperWidth == widthOpt,
                                onClick = { paperWidth = widthOpt },
                                label = { Text(widthOpt.labelFr, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("thermal_width_${widthOpt.name}")
                            )
                        }
                    }

                    if (!canShowClientReceipt) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PastelBlueTint,
                            border = BorderStroke(1.dp, PastelBlueBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Mode Atelier (${activeSession.role.labelFr}) : réimpression limitée aux étiquettes vêtements sans aucun montant financier.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MidnightNavy,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    statusMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (statusIsError) CrimsonContainer else EmeraldContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (statusIsError) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = if (statusIsError) CrimsonDanger else EmeraldSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (statusIsError) CrimsonDanger else Color(0xFF065F46),
                                    softWrap = true
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Centered Scrollable Authentic Thermal Paper Sheet Preview
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFE2E8F0))
                ) {
                    val paperMaxWidth = if (paperWidth == ThermalPaperWidth.MM_58) 290.dp else 380.dp
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (activeMode == "RECEIPT" && canShowClientReceipt) {
                            SerratedThermalPaperCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = paperMaxWidth)
                                    .testTag("thermal_receipt_paper_card")
                            ) {
                                ThermalLinesPreviewBody(
                                    lines = receiptLines,
                                    paperWidth = paperWidth
                                )
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    QrCodeCanvas(
                                        content = orderQrPayload,
                                        size = if (paperWidth == ThermalPaperWidth.MM_58) 112.dp else 136.dp
                                    )
                                }
                                Text(
                                    text = order.orderNumber,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MidnightNavy,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            if (garmentLabels.isEmpty()) {
                                Text(
                                    text = "Aucun article à étiqueter sur cette commande.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MidnightNavy
                                )
                            } else {
                                garmentLabels.forEachIndexed { idx, lbl ->
                                    SerratedThermalPaperCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .widthIn(max = paperMaxWidth)
                                            .testTag("thermal_label_card_$idx")
                                    ) {
                                        GarmentLabelPreviewContent(
                                            companyName = company.companyName,
                                            label = lbl,
                                            paperWidth = paperWidth
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 3 Responsive Print / Export / Share Actions (Zero overflow, >= 48dp touch targets)
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (BluetoothThermalPrinterManager.hasBluetoothConnectPermission(context)) {
                                    triggerBluetoothPrint()
                                } else {
                                    val perms = BluetoothThermalPrinterManager.requiredBluetoothPermissions()
                                    if (perms.isNotEmpty()) {
                                        btPermissionLauncher.launch(perms)
                                    } else {
                                        triggerBluetoothPrint()
                                    }
                                }
                            },
                            enabled = !isPrintingBt,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightNavy),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("thermal_btn_bluetooth_print")
                        ) {
                            if (isPrintingBt) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AfricaGold, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Envoi ESC/POS en cours...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (savedPrefs.printerName.isNotBlank()) {
                                        "Imprimer Bluetooth (${savedPrefs.printerName})"
                                    } else {
                                        "Imprimer (Bluetooth / ESC-POS)"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    softWrap = true
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (activeMode == "RECEIPT" && canShowClientReceipt) {
                                        val html = ThermalReceiptFormatter.buildCalibratedThermalHtml(
                                            lines = receiptLines,
                                            qrContent = orderQrPayload,
                                            barcodeContent = null,
                                            paperWidth = paperWidth,
                                            documentTitle = "Ticket_${order.orderNumber}"
                                        )
                                        printCalibratedThermalHtml(context, "Ticket_${order.orderNumber}", html, paperWidth)
                                    } else {
                                        val html = ThermalReceiptFormatter.buildCalibratedLabelsHtml(
                                            company = company,
                                            labels = garmentLabels,
                                            paperWidth = paperWidth
                                        )
                                        printCalibratedThermalHtml(context, "Etiquettes_${order.orderNumber}", html, paperWidth)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .heightIn(min = 48.dp)
                                    .testTag("thermal_btn_android_pdf")
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Imprimer / PDF", fontWeight = FontWeight.Bold, softWrap = true)
                            }

                            Button(
                                onClick = {
                                    val bmp = if (activeMode == "RECEIPT" && canShowClientReceipt) {
                                        renderThermalBitmap(
                                            lines = receiptLines,
                                            qrContent = orderQrPayload,
                                            barcodeContent = null,
                                            paperWidth = paperWidth
                                        )
                                    } else {
                                        val firstLabel = garmentLabels.firstOrNull()
                                        val linesToRender = if (firstLabel != null) {
                                            ThermalReceiptFormatter.buildPhysicalGarmentLabelLines(company, firstLabel, paperWidth)
                                        } else {
                                            receiptLines
                                        }
                                        renderThermalBitmap(
                                            lines = linesToRender,
                                            qrContent = firstLabel?.barcodeValue,
                                            barcodeContent = firstLabel?.barcodeValue,
                                            paperWidth = paperWidth
                                        )
                                    }
                                    val res = shareThermalBitmapPng(
                                        context = context,
                                        bitmap = bmp,
                                        fileNamePrefix = if (activeMode == "RECEIPT") "Recu_${order.orderNumber}" else "Etiquette_${order.orderNumber}",
                                        caption = "Ticket ${order.orderNumber} · ${company.companyName}",
                                        preferWhatsApp = false
                                    )
                                    res.fold(
                                        onSuccess = {
                                            statusIsError = false
                                            statusMessage = it
                                        },
                                        onFailure = {
                                            statusIsError = true
                                            statusMessage = "Impossible de partager l'image du reçu."
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .heightIn(min = 48.dp)
                                    .testTag("thermal_btn_share_whatsapp")
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Partager (WhatsApp / Image)", fontWeight = FontWeight.Bold, softWrap = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Authentic white thermal receipt paper surface with serrated zigzag top & bottom borders.
 */
@Composable
fun SerratedThermalPaperCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        ThermalSerratedEdge(topEdge = true)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
        ThermalSerratedEdge(topEdge = false)
    }
}

@Composable
private fun ThermalSerratedEdge(topEdge: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
    ) {
        val toothW = 14.dp.toPx()
        val path = Path()
        if (topEdge) {
            path.moveTo(0f, size.height)
            var x = 0f
            while (x < size.width) {
                path.lineTo(x + toothW / 2f, 0f)
                path.lineTo(x + toothW, size.height)
                x += toothW
            }
            path.lineTo(size.width, size.height)
            path.close()
        } else {
            path.moveTo(0f, 0f)
            var x = 0f
            while (x < size.width) {
                path.lineTo(x + toothW / 2f, size.height)
                path.lineTo(x + toothW, 0f)
                x += toothW
            }
            path.lineTo(size.width, 0f)
            path.close()
        }
        drawPath(path = path, color = Color.White)
    }
}

@Composable
fun ThermalLinesPreviewBody(
    lines: List<ThermalLine>,
    paperWidth: ThermalPaperWidth
) {
    val baseSp = if (paperWidth == ThermalPaperWidth.MM_58) 10.5.sp else 11.5.sp
    val titleSp = if (paperWidth == ThermalPaperWidth.MM_58) 13.sp else 14.5.sp
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        lines.forEach { line ->
            val align = when (line.style) {
                ThermalLineStyle.TITLE, ThermalLineStyle.CENTER, ThermalLineStyle.CENTER_BOLD -> TextAlign.Center
                else -> TextAlign.Start
            }
            val weight = when (line.style) {
                ThermalLineStyle.TITLE -> FontWeight.ExtraBold
                ThermalLineStyle.BOLD, ThermalLineStyle.CENTER_BOLD -> FontWeight.Bold
                else -> FontWeight.Normal
            }
            Text(
                text = line.text,
                fontFamily = JetBrainsMonoFamily,
                fontSize = if (line.style == ThermalLineStyle.TITLE) titleSp else baseSp,
                lineHeight = if (line.style == ThermalLineStyle.TITLE) 17.sp else 14.5.sp,
                fontWeight = weight,
                color = MidnightNavy,
                textAlign = align,
                softWrap = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GarmentLabelPreviewContent(
    companyName: String,
    label: PhysicalGarmentLabel,
    paperWidth: ThermalPaperWidth
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = companyName.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
            color = Color(0xFF475569),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = label.orderNumber,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
            fontWeight = FontWeight.Black,
            color = MidnightNavy,
            textAlign = TextAlign.Center
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MidnightNavy
        ) {
            Text(
                text = label.pieceCounterLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        if (label.alertTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                label.alertTags.forEach { tag ->
                    val bg = when (tag) {
                        "URGENT" -> CrimsonContainer
                        "TACHE" -> AmberContainer
                        else -> PastelBlueTint
                    }
                    val fg = when (tag) {
                        "URGENT" -> CrimsonDanger
                        "TACHE" -> Color(0xFF92400E)
                        else -> RoyalBlue
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = bg, border = BorderStroke(1.dp, fg)) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = fg,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = Color(0xFFCBD5E1))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "CLIENT : ${label.clientName}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.Bold,
                color = MidnightNavy,
                softWrap = true
            )
            Text(
                text = "VÊTEMENT : ${label.garmentName}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.ExtraBold,
                color = RoyalBlue,
                softWrap = true
            )
            Text(
                text = "SOIN : ${label.serviceName}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = Color(0xFF334155),
                softWrap = true
            )
            Text(
                text = "RETRAIT : ${label.promisedDateLabel}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.Bold,
                color = MidnightNavy
            )
        }
        Spacer(Modifier.height(2.dp))
        Code128BarcodeCanvas(
            content = label.barcodeValue,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (paperWidth == ThermalPaperWidth.MM_58) 40.dp else 48.dp)
        )
        Text(
            text = label.barcodeValue,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
            fontWeight = FontWeight.Bold,
            color = MidnightNavy
        )
        QrCodeCanvas(
            content = label.barcodeValue,
            size = if (paperWidth == ThermalPaperWidth.MM_58) 76.dp else 88.dp
        )
    }
}

/**
 * Vector Canvas renderer for Code-128 style individual garment tracking barcodes.
 */
@Composable
fun Code128BarcodeCanvas(
    content: String,
    modifier: Modifier = Modifier
) {
    val bars = remember(content) { ThermalReceiptFormatter.generateCode128Bars(content) }
    Canvas(modifier = modifier.background(Color.White)) {
        if (bars.isEmpty()) return@Canvas
        val barWidth = size.width / bars.size.toFloat()
        bars.forEachIndexed { idx, isBlack ->
            if (isBlack) {
                drawRect(
                    color = MidnightNavy,
                    topLeft = Offset(idx * barWidth, 0f),
                    size = Size(barWidth + 0.5f, size.height)
                )
            }
        }
    }
}
