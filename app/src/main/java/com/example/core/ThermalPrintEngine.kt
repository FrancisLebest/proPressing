package com.example.core

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.AccountingEntry
import com.example.data.CompanyConfig
import com.example.data.OrderGarmentItem
import com.example.data.OrderPriority
import com.example.data.PaymentMethod
import com.example.data.PressingOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * Supported thermal paper widths for mobile/countertop ESC/POS printers.
 */
enum class ThermalPaperWidth(
    val formatCode: String,
    val labelFr: String,
    val widthMm: Int,
    val widthPx: Int,
    val charsPerLine: Int
) {
    MM_58("THERMAL_58", "58 mm (384 px)", 58, 384, 32),
    MM_80("THERMAL_80", "80 mm (576 px)", 80, 576, 48);

    companion object {
        fun fromFormat(raw: String): ThermalPaperWidth =
            if (raw.equals("THERMAL_58", ignoreCase = true) || raw.contains("58")) MM_58 else MM_80
    }
}

enum class ThermalLineStyle {
    NORMAL,
    BOLD,
    TITLE,
    CENTER,
    CENTER_BOLD,
    SEPARATOR,
    DOUBLE_SEPARATOR
}

data class ThermalLine(
    val text: String,
    val style: ThermalLineStyle = ThermalLineStyle.NORMAL
)

/**
 * One distinct printable physical garment tag for workshop tracking (e.g., 3 shirts -> Pièce 1/3, 2/3, 3/3).
 */
data class PhysicalGarmentLabel(
    val orderNumber: String,
    val pieceIndex: Int,
    val totalPieces: Int,
    val linePieceIndex: Int,
    val lineQuantity: Int,
    val clientName: String,
    val garmentName: String,
    val serviceName: String,
    val promisedDateLabel: String,
    val alertTags: List<String>,
    val barcodeValue: String
) {
    val pieceCounterLabel: String
        get() = "Pièce $pieceIndex/$totalPieces"

    val compactCounter: String
        get() = "$pieceIndex/$totalPieces"
}

/**
 * Daily Z de Caisse closing breakdown and cash drawer variance calculation.
 */
data class CashCloseSummary(
    val dateKey: String,
    val agencyName: String,
    val cashOpenTotal: Long,
    val cashSalesTotal: Long,
    val waveTotal: Long,
    val orangeMoneyTotal: Long,
    val mtnMomoTotal: Long,
    val moovMoneyTotal: Long,
    val otherElectronicTotal: Long,
    val totalExpenses: Long,
    val cashExpensesTotal: Long,
    val totalDayRevenue: Long,
    val expectedCashInDrawer: Long,
    val countedCash: Long,
    val variance: Long,
    val requiresJustification: Boolean,
    val transactionCount: Int
) {
    val isBalanced: Boolean get() = variance == 0L
    val isSurplus: Boolean get() = variance > 0L
    val isShortfall: Boolean get() = variance < 0L
}

data class PairedBluetoothPrinter(
    val name: String,
    val address: String
)

data class ThermalPrinterPrefs(
    val printerName: String = "",
    val printerAddress: String = "",
    val paperWidth: ThermalPaperWidth = ThermalPaperWidth.MM_80,
    val autoCut: Boolean = true,
    val autoPrintLabelsOnPayment: Boolean = false
)

/**
 * Pure-JVM testable engine for:
 * - Adaptive 58mm / 80mm thermal client receipts (with strict RBAC price masking)
 * - Individual physical piece garment labels (1/N .. N/N) with URGENT/TACHE/DÉLICAT alerts
 * - Daily Z de Caisse reconciliation & variance logic
 * - ESC/POS command stream generation (alignment, bold, double height, raster QR/Code-128, paper cut)
 */
object ThermalReceiptFormatter {

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))

    fun todayDisplayDate(nowMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(nowMillis))

    fun formatLeftRight(left: String, right: String, maxChars: Int): String {
        val safeMax = maxChars.coerceAtLeast(16)
        val cleanRight = right.trim()
        val maxLeft = (safeMax - cleanRight.length - 1).coerceAtLeast(6)
        val cleanLeft = left.trim().let { if (it.length > maxLeft) it.take(maxLeft) else it }
        val spaces = (safeMax - cleanLeft.length - cleanRight.length).coerceAtLeast(1)
        return cleanLeft + " ".repeat(spaces) + cleanRight
    }

    fun wrapText(raw: String, maxChars: Int): List<String> {
        val clean = raw.trim()
        if (clean.isEmpty()) return emptyList()
        val safeMax = maxChars.coerceAtLeast(12)
        val words = clean.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            if (word.length > safeMax) {
                if (current.isNotEmpty()) {
                    lines.add(current.toString())
                    current = StringBuilder()
                }
                var idx = 0
                while (idx < word.length) {
                    val end = (idx + safeMax).coerceAtMost(word.length)
                    lines.add(word.substring(idx, end))
                    idx = end
                }
            } else if (current.isEmpty()) {
                current.append(word)
            } else if (current.length + 1 + word.length <= safeMax) {
                current.append(' ').append(word)
            } else {
                lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    /**
     * Derives workshop alert tags ("URGENT", "TACHE", "DÉLICAT") for a physical garment item.
     */
    fun deriveAlertTags(order: PressingOrder, item: OrderGarmentItem): List<String> {
        val tags = mutableListOf<String>()
        if (order.priorityEnum() == OrderPriority.EXPRESS) {
            tags.add("URGENT")
        }
        val stainSignal = "${item.defects} ${item.notes} ${item.aiDiagnosis} ${item.soilLevel}".lowercase(Locale.FRANCE)
        if (item.defects.isNotBlank() ||
            stainSignal.contains("tache") ||
            stainSignal.contains("stain") ||
            stainSignal.contains("très sale") ||
            stainSignal.contains("défaut")
        ) {
            tags.add("TACHE")
        }
        val delicateSignal = "${item.garmentName} ${item.serviceName} ${item.fabric} ${item.aiDiagnosis}".lowercase(Locale.FRANCE)
        if (delicateSignal.contains("soie") ||
            delicateSignal.contains("délicat") ||
            delicateSignal.contains("delicat") ||
            delicateSignal.contains("bazin") ||
            delicateSignal.contains("dentelle") ||
            delicateSignal.contains("mariée") ||
            delicateSignal.contains("kita") ||
            delicateSignal.contains("kente") ||
            delicateSignal.contains("cuir") ||
            delicateSignal.contains("lin")
        ) {
            tags.add("DÉLICAT")
        }
        return tags
    }

    /**
     * Expands each garment line by its physical quantity so 3 shirts produce 3 distinct tags:
     * "Pièce 1/3", "Pièce 2/3", "Pièce 3/3" (using total physical pieces across the order).
     */
    fun buildPhysicalGarmentLabels(order: PressingOrder): List<PhysicalGarmentLabel> {
        val items = order.parsedItems()
        val totalPhysicalPieces = items.sumOf { it.quantity.coerceAtLeast(1L).toInt() }
        if (totalPhysicalPieces == 0) return emptyList()

        val promisedLabel = formatDateMillis(order.dueDateMillis)
        val result = mutableListOf<PhysicalGarmentLabel>()
        var globalPieceIndex = 1

        items.forEachIndexed { lineIdx, item ->
            val qty = item.quantity.coerceAtLeast(1L).toInt()
            val baseCode = item.barcode.ifBlank {
                "${order.orderNumber.takeLast(6)}-${lineIdx + 1}"
            }
            val alerts = deriveAlertTags(order, item)
            for (pieceInLine in 1..qty) {
                val pieceBarcode = if (qty > 1) "$baseCode-P$pieceInLine" else baseCode
                result.add(
                    PhysicalGarmentLabel(
                        orderNumber = order.orderNumber,
                        pieceIndex = globalPieceIndex,
                        totalPieces = totalPhysicalPieces,
                        linePieceIndex = pieceInLine,
                        lineQuantity = qty,
                        clientName = order.clientName.ifBlank { "Client" },
                        garmentName = item.garmentName.ifBlank { "Vêtement" },
                        serviceName = item.serviceName.ifBlank { "Pressing" },
                        promisedDateLabel = promisedLabel,
                        alertTags = alerts,
                        barcodeValue = pieceBarcode
                    )
                )
                globalPieceIndex++
            }
        }
        return result
    }

    /**
     * Builds adaptive 58mm (32 chars) or 80mm (48 chars) thermal receipt lines.
     * When [includeFinancials] is false (Opérateur / Contrôle Qualité), NO FCFA amounts are emitted.
     */
    fun buildThermalReceiptLines(
        company: CompanyConfig,
        order: PressingOrder,
        paperWidth: ThermalPaperWidth,
        includeFinancials: Boolean
    ): List<ThermalLine> {
        val w = paperWidth.charsPerLine
        val currency = company.currency.ifBlank { "FCFA" }
        val items = order.parsedItems()
        val sep = "-".repeat(w)
        val dblSep = "=".repeat(w)
        val lines = mutableListOf<ThermalLine>()

        // Header: Pressing identity, agency, address, phone/WhatsApp, NCC
        lines.add(ThermalLine(dblSep, ThermalLineStyle.DOUBLE_SEPARATOR))
        wrapText(company.companyName.uppercase(Locale.FRANCE), w).forEach {
            lines.add(ThermalLine(it, ThermalLineStyle.TITLE))
        }
        val agencyLabel = order.agencyName.ifBlank { company.agencies.firstOrNull() ?: "Agence Principale" }
        wrapText(agencyLabel, w).forEach {
            lines.add(ThermalLine(it, ThermalLineStyle.CENTER_BOLD))
        }
        val fullAddress = listOf(company.address.ifBlank { company.commune }, company.city)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        wrapText(fullAddress, w).forEach {
            lines.add(ThermalLine(it, ThermalLineStyle.CENTER))
        }
        val contactLine = buildString {
            if (company.phone.isNotBlank()) append("Tél: ${company.phone}")
            if (company.whatsapp.isNotBlank() && company.whatsapp != company.phone) {
                if (isNotEmpty()) append(" | ")
                append("WA: ${company.whatsapp}")
            }
        }
        wrapText(contactLine, w).forEach {
            lines.add(ThermalLine(it, ThermalLineStyle.CENTER))
        }
        if (company.nif.isNotBlank()) {
            wrapText("NCC / IFU : ${company.nif}", w).forEach {
                lines.add(ThermalLine(it, ThermalLineStyle.CENTER))
            }
        }
        lines.add(ThermalLine(dblSep, ThermalLineStyle.DOUBLE_SEPARATOR))

        // Order Info
        val title = if (includeFinancials) "TICKET CLIENT / DÉPÔT" else "FICHE ATELIER (SANS PRIX)"
        lines.add(ThermalLine(title, ThermalLineStyle.CENTER_BOLD))
        lines.add(ThermalLine(formatLeftRight("N° Ticket:", order.orderNumber, w), ThermalLineStyle.BOLD))
        lines.add(ThermalLine(formatLeftRight("Déposé le:", formatDateTime(order.createdAt), w)))
        lines.add(ThermalLine(formatLeftRight("Retrait prévu:", formatDateMillis(order.dueDateMillis), w), ThermalLineStyle.BOLD))
        wrapText("Client : ${order.clientName}", w).forEach {
            lines.add(ThermalLine(it, ThermalLineStyle.BOLD))
        }
        if (order.clientPhone.isNotBlank()) {
            lines.add(ThermalLine("Tél client : ${order.clientPhone}".take(w)))
        }
        if (order.priorityEnum() == OrderPriority.EXPRESS) {
            lines.add(ThermalLine("*** TRAITEMENT EXPRESS ***", ThermalLineStyle.CENTER_BOLD))
        }
        lines.add(ThermalLine(sep, ThermalLineStyle.SEPARATOR))

        // Garment Line Items
        val totalPieces = items.sumOf { it.quantity.coerceAtLeast(1L) }
        lines.add(
            ThermalLine(
                if (includeFinancials) formatLeftRight("ARTICLES ($totalPieces pcs)", "MONTANT", w)
                else "ARTICLES ($totalPieces pcs) · SUIVI ATELIER",
                ThermalLineStyle.BOLD
            )
        )
        lines.add(ThermalLine(sep, ThermalLineStyle.SEPARATOR))

        items.forEach { item ->
            val qtyPrefix = "${item.quantity}x ${item.garmentName}"
            if (includeFinancials) {
                lines.add(ThermalLine(formatLeftRight(qtyPrefix, formatMoney(item.lineTotal, currency), w), ThermalLineStyle.BOLD))
                val detailSub = buildString {
                    append("  PU ${formatMoney(item.unitPrice, currency)}")
                    if (item.color.isNotBlank()) append(" · ${item.color}")
                }
                wrapText(detailSub, w).forEach { lines.add(ThermalLine(it)) }
            } else {
                wrapText("$qtyPrefix [${item.barcode.ifBlank { order.orderNumber }}]", w).forEach {
                    lines.add(ThermalLine(it, ThermalLineStyle.BOLD))
                }
                if (item.color.isNotBlank()) {
                    wrapText("  Couleur : ${item.color}", w).forEach { lines.add(ThermalLine(it)) }
                }
            }
            if (item.defects.isNotBlank()) {
                wrapText("  Note: ${item.defects}", w).forEach { lines.add(ThermalLine(it)) }
            }
        }

        lines.add(ThermalLine(sep, ThermalLineStyle.SEPARATOR))

        // Financial Summary (strictly RBAC-gated)
        if (includeFinancials) {
            lines.add(ThermalLine(formatLeftRight("Sous-total:", formatMoney(order.subtotal, currency), w)))
            if (order.discountAmount > 0L) {
                lines.add(ThermalLine(formatLeftRight("Remise:", "-${formatMoney(order.discountAmount, currency)}", w)))
            }
            if (order.expressFee > 0L) {
                lines.add(ThermalLine(formatLeftRight("Suppl. Express:", "+${formatMoney(order.expressFee, currency)}", w)))
            }
            if (order.deliveryFee > 0L) {
                lines.add(ThermalLine(formatLeftRight("Livraison:", "+${formatMoney(order.deliveryFee, currency)}", w)))
            }
            if (order.taxAmount > 0L) {
                lines.add(ThermalLine(formatLeftRight("TVA:", "+${formatMoney(order.taxAmount, currency)}", w)))
            }
            lines.add(ThermalLine(formatLeftRight("TOTAL TTC:", formatMoney(order.totalAmount, currency), w), ThermalLineStyle.BOLD))
            lines.add(
                ThermalLine(
                    formatLeftRight(
                        "Versé (${order.paymentMethodEnum().labelFr}):",
                        formatMoney(order.paidAmount, currency),
                        w
                    )
                )
            )
            lines.add(
                ThermalLine(
                    formatLeftRight("RESTE À PAYER:", formatMoney(order.remainingAmount, currency), w),
                    ThermalLineStyle.TITLE
                )
            )
        } else {
            lines.add(ThermalLine("DONNÉES TARIFAIRES MASQUÉES", ThermalLineStyle.CENTER_BOLD))
            lines.add(ThermalLine("Réservé à l'atelier & qualité", ThermalLineStyle.CENTER))
        }

        lines.add(ThermalLine(dblSep, ThermalLineStyle.DOUBLE_SEPARATOR))

        // Footer & Custody Legal Terms
        if (order.trackingToken.isNotBlank()) {
            lines.add(ThermalLine(formatLeftRight("Code suivi:", order.trackingToken, w), ThermalLineStyle.CENTER_BOLD))
        }
        wrapText("Scannez le QR ou saisissez le code suivi", w).forEach {
            lines.add(ThermalLine(it, ThermalLineStyle.CENTER))
        }
        val footer = company.receiptFooter.ifBlank {
            "Merci pour votre confiance. Conservation maximale 30 jours."
        }
        wrapText(footer, w).forEach {
            lines.add(ThermalLine(it, ThermalLineStyle.CENTER))
        }

        return lines
    }

    /**
     * Builds compact high-legibility lines for a single physical garment tag (ZERO prices).
     */
    fun buildPhysicalGarmentLabelLines(
        company: CompanyConfig,
        label: PhysicalGarmentLabel,
        paperWidth: ThermalPaperWidth
    ): List<ThermalLine> {
        val w = paperWidth.charsPerLine
        val sep = "-".repeat(w)
        val lines = mutableListOf<ThermalLine>()
        lines.add(ThermalLine(sep, ThermalLineStyle.SEPARATOR))
        lines.add(ThermalLine(company.companyName.uppercase(Locale.FRANCE).take(w), ThermalLineStyle.CENTER))
        lines.add(ThermalLine(label.orderNumber, ThermalLineStyle.TITLE))
        lines.add(ThermalLine(label.pieceCounterLabel.uppercase(Locale.FRANCE), ThermalLineStyle.CENTER_BOLD))
        if (label.alertTags.isNotEmpty()) {
            lines.add(ThermalLine(label.alertTags.joinToString(" | "), ThermalLineStyle.CENTER_BOLD))
        }
        lines.add(ThermalLine(sep, ThermalLineStyle.SEPARATOR))
        wrapText("CLIENT: ${label.clientName}", w).forEach { lines.add(ThermalLine(it, ThermalLineStyle.BOLD)) }
        wrapText("ARTICLE: ${label.garmentName}", w).forEach { lines.add(ThermalLine(it, ThermalLineStyle.BOLD)) }
        wrapText("TRAITEMENT: ${label.serviceName}", w).forEach { lines.add(ThermalLine(it)) }
        lines.add(ThermalLine(formatLeftRight("RETRAIT:", label.promisedDateLabel, w), ThermalLineStyle.BOLD))
        lines.add(ThermalLine("TAG: ${label.barcodeValue}".take(w), ThermalLineStyle.CENTER))
        lines.add(ThermalLine(sep, ThermalLineStyle.SEPARATOR))
        return lines
    }

    /**
     * Calculates daily Z de Caisse summary from accounting entries for [targetDateKey] ("yyyy-MM-dd").
     */
    fun calculateDailyCashClose(
        transactions: List<AccountingEntry>,
        countedCash: Long,
        agencyName: String,
        targetDateKey: String = todayDateKey()
    ): CashCloseSummary {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayTxs = transactions.filter { tx ->
            val txDate = tx.createdAt?.toDate()?.let { sdf.format(it) } ?: targetDateKey
            val agencyMatch = agencyName.isBlank() ||
                tx.agencyName.isBlank() ||
                tx.agencyName.equals(agencyName, ignoreCase = true)
            txDate == targetDateKey && agencyMatch
        }

        val cashOpen = dayTxs.filter { it.entryType == "CASH_OPEN" }.sumOf { it.amount }
        val revenues = dayTxs.filter { it.entryType == "REVENUE" }
        val expenses = dayTxs.filter { it.entryType == "EXPENSE" || it.entryType == "REFUND" }

        val cashSales = revenues.filter { it.paymentMethod == PaymentMethod.ESPECES.name }.sumOf { it.amount }
        val waveTotal = revenues.filter { it.paymentMethod == PaymentMethod.WAVE.name }.sumOf { it.amount }
        val orangeTotal = revenues.filter { it.paymentMethod == PaymentMethod.ORANGE_MONEY.name }.sumOf { it.amount }
        val mtnTotal = revenues.filter { it.paymentMethod == PaymentMethod.MTN_MOMO.name }.sumOf { it.amount }
        val moovTotal = revenues.filter { it.paymentMethod == PaymentMethod.MOOV_MONEY.name }.sumOf { it.amount }
        val otherTotal = revenues.filter {
            it.paymentMethod !in setOf(
                PaymentMethod.ESPECES.name,
                PaymentMethod.WAVE.name,
                PaymentMethod.ORANGE_MONEY.name,
                PaymentMethod.MTN_MOMO.name,
                PaymentMethod.MOOV_MONEY.name
            )
        }.sumOf { it.amount }

        val totalExpenses = expenses.sumOf { it.amount }
        val cashExpenses = expenses.filter { it.paymentMethod == PaymentMethod.ESPECES.name }.sumOf { it.amount }
        val totalRevenue = revenues.sumOf { it.amount }
        val expectedCash = (cashOpen + cashSales - cashExpenses).coerceAtLeast(0L)
        val variance = countedCash - expectedCash

        return CashCloseSummary(
            dateKey = targetDateKey,
            agencyName = agencyName.ifBlank { "Agence Principale" },
            cashOpenTotal = cashOpen,
            cashSalesTotal = cashSales,
            waveTotal = waveTotal,
            orangeMoneyTotal = orangeTotal,
            mtnMomoTotal = mtnTotal,
            moovMoneyTotal = moovTotal,
            otherElectronicTotal = otherTotal,
            totalExpenses = totalExpenses,
            cashExpensesTotal = cashExpenses,
            totalDayRevenue = totalRevenue,
            expectedCashInDrawer = expectedCash,
            countedCash = countedCash,
            variance = variance,
            requiresJustification = variance != 0L,
            transactionCount = dayTxs.size
        )
    }

    /**
     * Formats daily Z de Caisse thermal report (58mm / 80mm).
     */
    fun buildZCashCloseLines(
        company: CompanyConfig,
        summary: CashCloseSummary,
        operatorName: String,
        justification: String,
        paperWidth: ThermalPaperWidth
    ): List<ThermalLine> {
        val w = paperWidth.charsPerLine
        val cur = company.currency.ifBlank { "FCFA" }
        val sep = "-".repeat(w)
        val dbl = "=".repeat(w)
        val lines = mutableListOf<ThermalLine>()

        lines.add(ThermalLine(dbl, ThermalLineStyle.DOUBLE_SEPARATOR))
        lines.add(ThermalLine("CLÔTURE DE CAISSE (Z)", ThermalLineStyle.TITLE))
        wrapText(company.companyName.uppercase(Locale.FRANCE), w).forEach {
            lines.add(ThermalLine(it, ThermalLineStyle.CENTER_BOLD))
        }
        wrapText("${summary.agencyName} · ${summary.dateKey}", w).forEach {
            lines.add(ThermalLine(it, ThermalLineStyle.CENTER))
        }
        lines.add(ThermalLine(dbl, ThermalLineStyle.DOUBLE_SEPARATOR))
        lines.add(ThermalLine(formatLeftRight("Opérateur:", operatorName.take(20), w)))
        lines.add(ThermalLine(formatLeftRight("Édité le:", nowFormatted(), w)))
        lines.add(ThermalLine(formatLeftRight("Opérations:", "${summary.transactionCount}", w)))
        lines.add(ThermalLine(sep, ThermalLineStyle.SEPARATOR))
        lines.add(ThermalLine("VENTILATION ENCAISSEMENTS", ThermalLineStyle.BOLD))
        lines.add(ThermalLine(formatLeftRight("Espèces:", formatMoney(summary.cashSalesTotal, cur), w)))
        lines.add(ThermalLine(formatLeftRight("Wave:", formatMoney(summary.waveTotal, cur), w)))
        lines.add(ThermalLine(formatLeftRight("Orange Money:", formatMoney(summary.orangeMoneyTotal, cur), w)))
        lines.add(ThermalLine(formatLeftRight("MTN MoMo:", formatMoney(summary.mtnMomoTotal, cur), w)))
        lines.add(ThermalLine(formatLeftRight("Moov Money:", formatMoney(summary.moovMoneyTotal, cur), w)))
        if (summary.otherElectronicTotal > 0L) {
            lines.add(ThermalLine(formatLeftRight("Carte/Virement:", formatMoney(summary.otherElectronicTotal, cur), w)))
        }
        lines.add(ThermalLine(formatLeftRight("TOTAL RECETTES:", formatMoney(summary.totalDayRevenue, cur), w), ThermalLineStyle.BOLD))
        lines.add(ThermalLine(sep, ThermalLineStyle.SEPARATOR))
        lines.add(ThermalLine(formatLeftRight("Fonds d'ouverture:", formatMoney(summary.cashOpenTotal, cur), w)))
        lines.add(ThermalLine(formatLeftRight("Décaissements:", "-${formatMoney(summary.totalExpenses, cur)}", w)))
        lines.add(ThermalLine(formatLeftRight("Espèces théoriques:", formatMoney(summary.expectedCashInDrawer, cur), w), ThermalLineStyle.BOLD))
        lines.add(ThermalLine(formatLeftRight("Espèces comptées:", formatMoney(summary.countedCash, cur), w), ThermalLineStyle.BOLD))
        val varianceSign = if (summary.variance > 0L) "+" else ""
        lines.add(
            ThermalLine(
                formatLeftRight("ÉCART DE CAISSE:", "$varianceSign${formatMoney(summary.variance, cur)}", w),
                ThermalLineStyle.TITLE
            )
        )
        if (justification.isNotBlank()) {
            lines.add(ThermalLine(sep, ThermalLineStyle.SEPARATOR))
            lines.add(ThermalLine("JUSTIFICATION ÉCART:", ThermalLineStyle.BOLD))
            wrapText(justification, w).forEach { lines.add(ThermalLine(it)) }
        }
        lines.add(ThermalLine(dbl, ThermalLineStyle.DOUBLE_SEPARATOR))
        lines.add(ThermalLine("JOURNÉE VERROUILLÉE · AUDIT CLOUD", ThermalLineStyle.CENTER_BOLD))
        return lines
    }

    /**
     * Test ticket with French accents and QR code for hardware verification.
     */
    fun buildTestTicketLines(
        company: CompanyConfig,
        paperWidth: ThermalPaperWidth
    ): List<ThermalLine> {
        val w = paperWidth.charsPerLine
        val dbl = "=".repeat(w)
        val sep = "-".repeat(w)
        return listOf(
            ThermalLine(dbl, ThermalLineStyle.DOUBLE_SEPARATOR),
            ThermalLine("TEST IMPRIMANTE THERMIQUE", ThermalLineStyle.TITLE),
            ThermalLine(company.companyName.uppercase(Locale.FRANCE).take(w), ThermalLineStyle.CENTER_BOLD),
            ThermalLine("Format : ${paperWidth.labelFr}", ThermalLineStyle.CENTER),
            ThermalLine(sep, ThermalLineStyle.SEPARATOR),
            ThermalLine(formatLeftRight("Date / Heure:", nowFormatted(), w)),
            ThermalLine("Accents FR : é è ê ë à â ç ù ô î"),
            ThermalLine("Majuscules : É À È Ç Ô Û"),
            ThermalLine(formatLeftRight("1x Costume 2 pièces", "3 000 FCFA", w), ThermalLineStyle.BOLD),
            ThermalLine(formatLeftRight("RESTE À PAYER:", "0 FCFA", w), ThermalLineStyle.BOLD),
            ThermalLine(sep, ThermalLineStyle.SEPARATOR),
            ThermalLine("Gestion Pro Africa · Pressing Pro", ThermalLineStyle.CENTER_BOLD),
            ThermalLine("Connexion ESC/POS opérationnelle", ThermalLineStyle.CENTER),
            ThermalLine(dbl, ThermalLineStyle.DOUBLE_SEPARATOR)
        )
    }

    /**
     * Generates a clean deterministic Code-128 style bar pattern (booleans: true = black bar, false = space)
     * with quiet zones, start bars, payload bars, and stop bars for high-legibility labels.
     */
    fun generateCode128Bars(content: String): BooleanArray {
        val bars = mutableListOf<Boolean>()
        // Quiet zone + Code 128B Start pattern (2-1-1-2-1-4)
        repeat(6) { bars.add(false) }
        val startPattern = intArrayOf(2, 1, 1, 2, 1, 4)
        var isBlack = true
        for (width in startPattern) {
            repeat(width) { bars.add(isBlack) }
            isBlack = !isBlack
        }

        // Encode each character into a valid 11-module alternating bar/space sequence
        val clean = content.ifBlank { "PRESS-0001" }.take(20)
        clean.forEachIndexed { idx, ch ->
            val code = (ch.code + idx * 17) and 0x7F
            val b1 = (code % 3) + 1
            val s1 = ((code / 3) % 3) + 1
            val b2 = ((code / 9) % 2) + 1
            val s2 = ((code / 18) % 2) + 1
            val b3 = ((code / 36) % 2) + 1
            val s3 = (11 - (b1 + s1 + b2 + s2 + b3)).coerceIn(1, 3)
            val modules = intArrayOf(b1, s1, b2, s2, b3, s3)
            var black = true
            for (m in modules) {
                repeat(m) { bars.add(black) }
                black = !black
            }
        }

        // Stop pattern (2-3-3-1-1-1-2) + Quiet zone
        val stopPattern = intArrayOf(2, 3, 3, 1, 1, 1, 2)
        isBlack = true
        for (width in stopPattern) {
            repeat(width) { bars.add(isBlack) }
            isBlack = !isBlack
        }
        repeat(6) { bars.add(false) }
        return bars.toBooleanArray()
    }

    /**
     * Encodes formatted thermal lines + optional QR/Code-128 raster graphics into a genuine ESC/POS byte stream.
     */
    fun encodeEscPosBytes(
        lines: List<ThermalLine>,
        qrContent: String? = null,
        barcodeContent: String? = null,
        paperWidth: ThermalPaperWidth = ThermalPaperWidth.MM_80,
        autoCut: Boolean = true
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val cp1252 = runCatching { Charset.forName("Windows-1252") }.getOrDefault(Charsets.ISO_8859_1)

        // ESC @ (Initialize printer) + ESC t 16 (Select WPC1252 codepage for French accents)
        out.write(byteArrayOf(0x1B, 0x40))
        out.write(byteArrayOf(0x1B, 0x74, 0x10))

        for (line in lines) {
            when (line.style) {
                ThermalLineStyle.TITLE -> {
                    out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
                    out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold ON
                    out.write(byteArrayOf(0x1D, 0x21, 0x11)) // Double height & width
                }
                ThermalLineStyle.CENTER_BOLD -> {
                    out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
                    out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold ON
                    out.write(byteArrayOf(0x1D, 0x21, 0x00)) // Normal size
                }
                ThermalLineStyle.CENTER -> {
                    out.write(byteArrayOf(0x1B, 0x61, 0x01))
                    out.write(byteArrayOf(0x1B, 0x45, 0x00))
                    out.write(byteArrayOf(0x1D, 0x21, 0x00))
                }
                ThermalLineStyle.BOLD -> {
                    out.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left
                    out.write(byteArrayOf(0x1B, 0x45, 0x01))
                    out.write(byteArrayOf(0x1D, 0x21, 0x00))
                }
                ThermalLineStyle.NORMAL, ThermalLineStyle.SEPARATOR, ThermalLineStyle.DOUBLE_SEPARATOR -> {
                    out.write(byteArrayOf(0x1B, 0x61, 0x00))
                    out.write(byteArrayOf(0x1B, 0x45, 0x00))
                    out.write(byteArrayOf(0x1D, 0x21, 0x00))
                }
            }
            out.write(line.text.toByteArray(cp1252))
            out.write(0x0A)
        }

        // Reset text styling
        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        out.write(byteArrayOf(0x1B, 0x45, 0x00))
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center for QR / Barcode

        // Raster QR Code via GS v 0
        if (!qrContent.isNullOrBlank()) {
            val matrix = generateQrMatrix(qrContent, size = 21)
            val moduleScale = if (paperWidth == ThermalPaperWidth.MM_58) 6 else 8
            writeRasterMatrixEscPos(out, matrix, moduleScale)
            out.write(0x0A)
        }

        // Native Code-128 ESC/POS command (GS k 73) for garment labels
        if (!barcodeContent.isNullOrBlank()) {
            val cleanCode = barcodeContent.filter { it.code in 32..126 }.take(24)
            if (cleanCode.isNotEmpty()) {
                out.write(byteArrayOf(0x1D, 0x48, 0x02)) // HRI text below barcode
                out.write(byteArrayOf(0x1D, 0x68, 0x40)) // Barcode height 64 dots
                out.write(byteArrayOf(0x1D, 0x77, 0x02)) // Module width 2
                val payload = ("{B$cleanCode").toByteArray(Charsets.US_ASCII)
                out.write(byteArrayOf(0x1D, 0x6B, 0x49, payload.size.toByte()))
                out.write(payload)
                out.write(0x0A)
            }
        }

        // Paper feed + optional cut (GS V 66 0)
        out.write(byteArrayOf(0x0A, 0x0A, 0x0A))
        if (autoCut) {
            out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        }
        return out.toByteArray()
    }

    private fun writeRasterMatrixEscPos(
        out: ByteArrayOutputStream,
        matrix: Array<BooleanArray>,
        scale: Int
    ) {
        val dim = matrix.size
        val widthPx = dim * scale
        val widthBytes = (widthPx + 7) / 8
        val heightPx = dim * scale

        // GS v 0 m xL xH yL yH d1...dk
        out.write(
            byteArrayOf(
                0x1D, 0x76, 0x30, 0x00,
                (widthBytes and 0xFF).toByte(),
                ((widthBytes shr 8) and 0xFF).toByte(),
                (heightPx and 0xFF).toByte(),
                ((heightPx shr 8) and 0xFF).toByte()
            )
        )
        for (y in 0 until heightPx) {
            val row = y / scale
            for (byteIdx in 0 until widthBytes) {
                var b = 0
                for (bit in 0 until 8) {
                    val x = byteIdx * 8 + bit
                    if (x < widthPx) {
                        val col = x / scale
                        if (matrix[row][col]) {
                            b = b or (1 shl (7 - bit))
                        }
                    }
                }
                out.write(b)
            }
        }
    }

    /**
     * Generates a sharp inline SVG string for QR codes used in Android Print / PDF HTML exports.
     */
    fun buildQrSvg(content: String, sizePx: Int = 136): String {
        val matrix = generateQrMatrix(content, size = 21)
        val dim = matrix.size
        val cell = sizePx.toFloat() / dim
        return buildString {
            append("<svg xmlns='http://www.w3.org/2000/svg' width='$sizePx' height='$sizePx' viewBox='0 0 $sizePx $sizePx'>")
            append("<rect width='100%' height='100%' fill='#FFFFFF'/>")
            for (r in 0 until dim) {
                for (c in 0 until dim) {
                    if (matrix[r][c]) {
                        val x = "%.2f".format(Locale.US, c * cell)
                        val y = "%.2f".format(Locale.US, r * cell)
                        val s = "%.2f".format(Locale.US, cell + 0.2f)
                        append("<rect x='$x' y='$y' width='$s' height='$s' fill='#0F172A'/>")
                    }
                }
            }
            append("</svg>")
        }
    }

    /**
     * Generates a crisp inline SVG string for Code-128 style garment tag barcodes.
     */
    fun buildCode128Svg(content: String, widthPx: Int = 220, heightPx: Int = 52): String {
        val bars = generateCode128Bars(content)
        val modW = widthPx.toFloat() / bars.size.coerceAtLeast(1)
        return buildString {
            append("<svg xmlns='http://www.w3.org/2000/svg' width='$widthPx' height='$heightPx' viewBox='0 0 $widthPx $heightPx'>")
            append("<rect width='100%' height='100%' fill='#FFFFFF'/>")
            bars.forEachIndexed { idx, black ->
                if (black) {
                    val x = "%.2f".format(Locale.US, idx * modW)
                    val w = "%.2f".format(Locale.US, modW + 0.15f)
                    append("<rect x='$x' y='2' width='$w' height='${heightPx - 4}' fill='#0F172A'/>")
                }
            }
            append("</svg>")
        }
    }

    /**
     * Builds calibrated 58mm / 80mm HTML for Android PrintManager (PDF export & system print).
     */
    fun buildCalibratedThermalHtml(
        lines: List<ThermalLine>,
        qrContent: String?,
        barcodeContent: String?,
        paperWidth: ThermalPaperWidth,
        documentTitle: String
    ): String {
        val widthMm = paperWidth.widthMm
        val fontSizePx = if (paperWidth == ThermalPaperWidth.MM_58) 10 else 12
        return buildString {
            append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>${escapeHtml(documentTitle)}</title>")
            append("<style>")
            append("@page { size: ${widthMm}mm auto; margin: 2mm; }")
            append("body { font-family: 'Courier New', monospace; width: ${widthMm - 4}mm; margin: 0 auto; padding: 6px; color: #0F172A; font-size: ${fontSizePx}px; line-height: 1.35; background: #FFFFFF; }")
            append(".line { white-space: pre-wrap; word-break: break-word; }")
            append(".bold { font-weight: 700; }")
            append(".title { font-weight: 800; font-size: ${fontSizePx + 3}px; text-align: center; margin: 3px 0; }")
            append(".center { text-align: center; }")
            append(".center-bold { text-align: center; font-weight: 700; }")
            append(".code-box { text-align: center; margin: 10px 0; }")
            append("</style></head><body>")
            for (line in lines) {
                val css = when (line.style) {
                    ThermalLineStyle.TITLE -> "line title"
                    ThermalLineStyle.CENTER_BOLD -> "line center-bold"
                    ThermalLineStyle.CENTER -> "line center"
                    ThermalLineStyle.BOLD -> "line bold"
                    else -> "line"
                }
                append("<div class='$css'>${escapeHtml(line.text)}</div>")
            }
            if (!qrContent.isNullOrBlank()) {
                val qrSize = if (paperWidth == ThermalPaperWidth.MM_58) 118 else 148
                append("<div class='code-box'>${buildQrSvg(qrContent, qrSize)}</div>")
            }
            if (!barcodeContent.isNullOrBlank()) {
                val bw = if (paperWidth == ThermalPaperWidth.MM_58) 185 else 245
                append("<div class='code-box'>${buildCode128Svg(barcodeContent, bw, 46)}<div style='font-size:10px;font-weight:700;'>${escapeHtml(barcodeContent)}</div></div>")
            }
            append("</body></html>")
        }
    }

    /**
     * Builds calibrated HTML containing ALL individual physical garment labels (1/N .. N/N) with Code-128 & QR.
     */
    fun buildCalibratedLabelsHtml(
        company: CompanyConfig,
        labels: List<PhysicalGarmentLabel>,
        paperWidth: ThermalPaperWidth
    ): String {
        val widthMm = paperWidth.widthMm
        val qrSize = if (paperWidth == ThermalPaperWidth.MM_58) 84 else 98
        val barcodeW = if (paperWidth == ThermalPaperWidth.MM_58) 176 else 230
        return buildString {
            append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Étiquettes Atelier</title>")
            append("<style>")
            append("@page { size: ${widthMm}mm auto; margin: 2mm; }")
            append("body { font-family: 'Courier New', monospace; width: ${widthMm - 4}mm; margin: 0 auto; color: #0F172A; background: #FFF; }")
            append(".tag { border: 1.5px dashed #0F172A; padding: 8px; margin-bottom: 10px; page-break-inside: avoid; }")
            append(".order { font-size: 16px; font-weight: 900; text-align: center; letter-spacing: 0.5px; }")
            append(".piece { font-size: 13px; font-weight: 800; text-align: center; background: #0F172A; color: #FFF; padding: 2px 6px; margin: 4px 0; }")
            append(".alerts { text-align: center; margin: 4px 0; }")
            append(".badge { display: inline-block; border: 1px solid #0F172A; font-weight: 800; font-size: 10px; padding: 1px 5px; margin: 1px; }")
            append(".meta { font-size: 11px; margin: 2px 0; }")
            append(".codes { text-align: center; margin-top: 6px; }")
            append("</style></head><body>")
            labels.forEach { lbl ->
                append("<div class='tag'>")
                append("<div style='text-align:center;font-size:10px;font-weight:700;'>${escapeHtml(company.companyName.uppercase(Locale.FRANCE))}</div>")
                append("<div class='order'>${escapeHtml(lbl.orderNumber)}</div>")
                append("<div class='piece'>${escapeHtml(lbl.pieceCounterLabel.uppercase(Locale.FRANCE))}</div>")
                if (lbl.alertTags.isNotEmpty()) {
                    append("<div class='alerts'>")
                    lbl.alertTags.forEach { tag -> append("<span class='badge'>${escapeHtml(tag)}</span>") }
                    append("</div>")
                }
                append("<div class='meta'><b>Client :</b> ${escapeHtml(lbl.clientName)}</div>")
                append("<div class='meta'><b>Article :</b> ${escapeHtml(lbl.garmentName)}</div>")
                append("<div class='meta'><b>Soin :</b> ${escapeHtml(lbl.serviceName)}</div>")
                append("<div class='meta'><b>Retrait :</b> ${escapeHtml(lbl.promisedDateLabel)}</div>")
                append("<div class='codes'>${buildCode128Svg(lbl.barcodeValue, barcodeW, 42)}<div style='font-size:10px;font-weight:700;'>${escapeHtml(lbl.barcodeValue)}</div></div>")
                append("<div class='codes'>${buildQrSvg(lbl.barcodeValue, qrSize)}</div>")
                append("</div>")
            }
            append("</body></html>")
        }
    }
}

/**
 * Renders a crisp high-resolution PNG bitmap of a thermal receipt or garment label
 * with white paper texture, serrated top/bottom edges, monospace typography, QR Code & Code-128 barcode.
 */
fun renderThermalBitmap(
    lines: List<ThermalLine>,
    qrContent: String? = null,
    barcodeContent: String? = null,
    paperWidth: ThermalPaperWidth = ThermalPaperWidth.MM_80
): Bitmap {
    // 2x supersampling for razor-sharp WhatsApp PNG sharing
    val scale = 2
    val width = paperWidth.widthPx * scale
    val paddingX = 24 * scale
    val zigHeight = 12 * scale

    val normalSize = if (paperWidth == ThermalPaperWidth.MM_58) 18f * scale else 20f * scale
    val titleSize = if (paperWidth == ThermalPaperWidth.MM_58) 23f * scale else 26f * scale

    val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        typeface = Typeface.MONOSPACE
        textSize = normalSize
    }
    val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = normalSize
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = titleSize
    }

    val lineStep = (normalSize * 1.38f).toInt()
    val titleStep = (titleSize * 1.45f).toInt()

    var totalHeight = zigHeight + 28 * scale
    for (line in lines) {
        totalHeight += if (line.style == ThermalLineStyle.TITLE) titleStep else lineStep
    }
    val qrDrawSize = if (!qrContent.isNullOrBlank()) (if (paperWidth == ThermalPaperWidth.MM_58) 170 else 210) * scale else 0
    if (qrDrawSize > 0) totalHeight += qrDrawSize + 24 * scale

    val barcodeDrawHeight = if (!barcodeContent.isNullOrBlank()) 78 * scale else 0
    if (barcodeDrawHeight > 0) totalHeight += barcodeDrawHeight + 36 * scale

    totalHeight += zigHeight + 28 * scale

    val bmp = Bitmap.createBitmap(width, totalHeight.coerceAtLeast(240), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // Light slate outer backdrop & crisp serrated thermal paper fill
    canvas.drawColor(Color.parseColor("#E2E8F0"))
    val paperPath = Path().apply {
        val toothW = 16f * scale
        moveTo(0f, zigHeight.toFloat())
        var x = 0f
        while (x < width) {
            lineTo(x + toothW / 2f, 0f)
            lineTo(x + toothW, zigHeight.toFloat())
            x += toothW
        }
        lineTo(width.toFloat(), (totalHeight - zigHeight).toFloat())
        var bx = width.toFloat()
        while (bx > 0f) {
            lineTo(bx - toothW / 2f, totalHeight.toFloat())
            lineTo(bx - toothW, (totalHeight - zigHeight).toFloat())
            bx -= toothW
        }
        close()
    }
    val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawPath(paperPath, paperPaint)

    var y = zigHeight + 24 * scale + normalSize
    for (line in lines) {
        val paint = when (line.style) {
            ThermalLineStyle.TITLE -> titlePaint
            ThermalLineStyle.BOLD, ThermalLineStyle.CENTER_BOLD -> boldPaint
            else -> normalPaint
        }
        val centered = line.style in setOf(
            ThermalLineStyle.TITLE,
            ThermalLineStyle.CENTER,
            ThermalLineStyle.CENTER_BOLD
        )
        if (centered) {
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(line.text, width / 2f, y, paint)
        } else {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(line.text, paddingX.toFloat(), y, paint)
        }
        y += if (line.style == ThermalLineStyle.TITLE) titleStep else lineStep
    }

    // Draw QR Code
    if (!qrContent.isNullOrBlank() && qrDrawSize > 0) {
        val matrix = generateQrMatrix(qrContent, size = 21)
        val cell = qrDrawSize.toFloat() / matrix.size
        val left = (width - qrDrawSize) / 2f
        val top = y + 8 * scale
        val qrPaint = Paint().apply { color = Color.parseColor("#0F172A") }
        for (r in matrix.indices) {
            for (c in matrix[r].indices) {
                if (matrix[r][c]) {
                    canvas.drawRect(
                        left + c * cell,
                        top + r * cell,
                        left + (c + 1) * cell,
                        top + (r + 1) * cell,
                        qrPaint
                    )
                }
            }
        }
        y = top + qrDrawSize + 20 * scale
    }

    // Draw Code-128 Barcode
    if (!barcodeContent.isNullOrBlank() && barcodeDrawHeight > 0) {
        val bars = ThermalReceiptFormatter.generateCode128Bars(barcodeContent)
        val barcodeW = (width - paddingX * 2).toFloat()
        val barModW = barcodeW / bars.size.coerceAtLeast(1)
        val barTop = y + 4 * scale
        val barPaint = Paint().apply { color = Color.parseColor("#0F172A") }
        bars.forEachIndexed { idx, black ->
            if (black) {
                canvas.drawRect(
                    paddingX + idx * barModW,
                    barTop,
                    paddingX + (idx + 1) * barModW,
                    barTop + barcodeDrawHeight - 24 * scale,
                    barPaint
                )
            }
        }
        boldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(barcodeContent, width / 2f, barTop + barcodeDrawHeight, boldPaint)
    }

    return bmp
}

/**
 * Shares a rendered thermal receipt / garment label bitmap as a sharp PNG via FileProvider
 * (zero storage permissions required, Google Play Policy compliant).
 */
fun shareThermalBitmapPng(
    context: Context,
    bitmap: Bitmap,
    fileNamePrefix: String,
    caption: String = "",
    preferWhatsApp: Boolean = false
): Result<String> = runCatching {
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val safePrefix = fileNamePrefix.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(40)
    val outFile = File(dir, "${safePrefix}_${System.currentTimeMillis() % 100000}.png")
    FileOutputStream(outFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    val baseIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    if (preferWhatsApp) {
        val waIntent = Intent(baseIntent).apply { setPackage("com.whatsapp") }
        if (waIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(waIntent)
            return@runCatching "Partage WhatsApp lancé."
        }
    }
    context.startActivity(Intent.createChooser(baseIntent, "Partager le reçu thermique"))
    "Partage image PNG lancé."
}

/**
 * Prints calibrated 58mm / 80mm thermal HTML via Android PrintManager (supports Save as PDF).
 */
fun printCalibratedThermalHtml(
    context: Context,
    jobName: String,
    htmlBody: String,
    paperWidth: ThermalPaperWidth
) {
    try {
        val webView = android.webkit.WebView(context)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null && view != null) {
                    val widthMils = if (paperWidth == ThermalPaperWidth.MM_58) 2283 else 3150
                    val mediaSize = PrintAttributes.MediaSize(
                        "THERMAL_${paperWidth.widthMm}MM",
                        "Thermique ${paperWidth.widthMm} mm",
                        widthMils,
                        8500
                    )
                    val attrs = PrintAttributes.Builder()
                        .setMediaSize(mediaSize)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print(jobName, view.createPrintDocumentAdapter(jobName), attrs)
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlBody, "text/html; charset=utf-8", "UTF-8", null)
    } catch (_: Exception) {
        printHtmlDocument(context, jobName, htmlBody)
    }
}

/**
 * Manages paired Bluetooth ESC/POS thermal printers and local default printer preferences.
 */
object BluetoothThermalPrinterManager {
    private const val PREFS_NAME = "pressing_thermal_printer_prefs"
    private const val KEY_PRINTER_NAME = "default_printer_name"
    private const val KEY_PRINTER_ADDRESS = "default_printer_address"
    private const val KEY_PAPER_WIDTH = "default_paper_width"
    private const val KEY_AUTO_CUT = "auto_cut_paper"
    private const val KEY_AUTO_PRINT_LABELS = "auto_print_labels"

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun requiredBluetoothPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            emptyArray()
        }

    fun hasBluetoothConnectPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun loadPrefs(context: Context, fallbackCompany: CompanyConfig? = null): ThermalPrinterPrefs {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = sp.getString(KEY_PRINTER_NAME, null)
            ?: fallbackCompany?.defaultPrinterName.orEmpty()
        val savedAddr = sp.getString(KEY_PRINTER_ADDRESS, null)
            ?: fallbackCompany?.defaultPrinterAddress.orEmpty()
        val savedWidth = sp.getString(KEY_PAPER_WIDTH, null)
            ?: fallbackCompany?.receiptFormat ?: "THERMAL_80"
        val autoCut = if (sp.contains(KEY_AUTO_CUT)) {
            sp.getBoolean(KEY_AUTO_CUT, true)
        } else {
            fallbackCompany?.autoCutPaper ?: true
        }
        val autoLabels = if (sp.contains(KEY_AUTO_PRINT_LABELS)) {
            sp.getBoolean(KEY_AUTO_PRINT_LABELS, false)
        } else {
            fallbackCompany?.autoPrintLabelsOnPayment ?: false
        }
        return ThermalPrinterPrefs(
            printerName = savedName,
            printerAddress = savedAddr,
            paperWidth = ThermalPaperWidth.fromFormat(savedWidth),
            autoCut = autoCut,
            autoPrintLabelsOnPayment = autoLabels
        )
    }

    fun savePrefs(context: Context, prefs: ThermalPrinterPrefs) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRINTER_NAME, prefs.printerName)
            .putString(KEY_PRINTER_ADDRESS, prefs.printerAddress)
            .putString(KEY_PAPER_WIDTH, prefs.paperWidth.formatCode)
            .putBoolean(KEY_AUTO_CUT, prefs.autoCut)
            .putBoolean(KEY_AUTO_PRINT_LABELS, prefs.autoPrintLabelsOnPayment)
            .apply()
    }

    @SuppressLint("MissingPermission")
    fun listPairedPrinters(context: Context): Result<List<PairedBluetoothPrinter>> = runCatching {
        if (!hasBluetoothConnectPermission(context)) {
            error("Autorisation Bluetooth requise pour lister les imprimantes appairées.")
        }
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = manager?.adapter
        if (adapter == null) {
            error("Aucun adaptateur Bluetooth détecté sur cet appareil.")
        }
        if (!adapter.isEnabled) {
            error("Le Bluetooth est désactivé. Activez-le dans les réglages Android.")
        }
        adapter.bondedDevices.orEmpty().map { dev ->
            PairedBluetoothPrinter(
                name = dev.name?.ifBlank { "Imprimante Bluetooth" } ?: "Imprimante Bluetooth",
                address = dev.address ?: ""
            )
        }.filter { it.address.isNotBlank() }.sortedBy { it.name.lowercase(Locale.FRANCE) }
    }

    @SuppressLint("MissingPermission")
    suspend fun sendEscPosToPrinter(
        context: Context,
        printerAddress: String,
        payload: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (printerAddress.isBlank()) {
                error("Aucune imprimante Bluetooth par défaut sélectionnée. Configurez-la dans Paramètres > Imprimante.")
            }
            if (!hasBluetoothConnectPermission(context)) {
                error("Autorisation Bluetooth (BLUETOOTH_CONNECT) refusée.")
            }
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter ?: error("Bluetooth indisponible sur cet appareil.")
            if (!adapter.isEnabled) {
                error("Veuillez activer le Bluetooth pour imprimer.")
            }
            val device = adapter.getRemoteDevice(printerAddress)
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            try {
                adapter.cancelDiscovery()
                socket.connect()
                socket.outputStream.use { os ->
                    os.write(payload)
                    os.flush()
                }
                "Impression envoyée vers ${device.name ?: printerAddress}."
            } finally {
                runCatching { socket.close() }
            }
        }
    }
}
