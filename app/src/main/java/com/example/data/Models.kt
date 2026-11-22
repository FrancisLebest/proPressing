package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class OrderStatus(val labelFr: String, val stepIndex: Int) {
    PENDING("En attente", 0),
    ACCEPTED("Acceptée", 1),
    WASHING("En lavage", 2),
    DRYING("En séchage", 3),
    IRONING("En repassage", 4),
    FINISHING("Finition", 5),
    QUALITY_CHECK("Contrôle qualité", 6),
    READY("Prête", 7),
    PICKED_UP("Retirée", 8),
    DELIVERED("Livrée", 8),
    CANCELLED("Annulée", -1);

    companion object {
        fun fromRaw(raw: String): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING

        val productionSteps: List<OrderStatus> = listOf(
            PENDING, ACCEPTED, WASHING, DRYING, IRONING, FINISHING, QUALITY_CHECK, READY, PICKED_UP, DELIVERED
        )
    }
}

enum class GarmentWorkshopStatus(val labelFr: String, val stepIndex: Int) {
    RECEIVED("Réceptionné", 0),
    WASHING("En lavage", 1),
    DRYING("Séchage", 2),
    IRONING("Repassage", 3),
    QUALITY_CHECK("Contrôle qualité", 4),
    READY("Prêt", 5);

    fun nextStage(): GarmentWorkshopStatus? = when (this) {
        RECEIVED -> WASHING
        WASHING -> DRYING
        DRYING -> IRONING
        IRONING -> QUALITY_CHECK
        QUALITY_CHECK -> READY
        READY -> null
    }

    fun toOrderStatus(): OrderStatus = when (this) {
        RECEIVED -> OrderStatus.ACCEPTED
        WASHING -> OrderStatus.WASHING
        DRYING -> OrderStatus.DRYING
        IRONING -> OrderStatus.IRONING
        QUALITY_CHECK -> OrderStatus.QUALITY_CHECK
        READY -> OrderStatus.READY
    }

    companion object {
        fun fromRaw(raw: String): GarmentWorkshopStatus =
            entries.firstOrNull { it.name == raw } ?: RECEIVED
    }
}

enum class OrderPriority(val labelFr: String) {
    ECONOMY("Économique"),
    STANDARD("Normale"),
    EXPRESS("Express");

    companion object {
        fun fromRaw(raw: String): OrderPriority =
            entries.firstOrNull { it.name == raw } ?: STANDARD
    }
}

enum class PaymentStatus(val labelFr: String) {
    UNPAID("À payer"),
    PARTIAL("Acompte"),
    PAID("Payé");

    companion object {
        fun fromRaw(raw: String): PaymentStatus =
            entries.firstOrNull { it.name == raw } ?: UNPAID
    }
}

enum class PaymentMethod(val labelFr: String, val syscohadaTreasuryAccount: String) {
    ESPECES("Espèces", "571"),
    ORANGE_MONEY("Orange Money", "5211"),
    MTN_MOMO("MTN MoMo", "5212"),
    WAVE("Wave", "5213"),
    MOOV_MONEY("Moov Money", "5214"),
    CARTE("Carte Bancaire", "5215"),
    VIREMENT("Virement Bancaire", "521");

    companion object {
        fun fromRaw(raw: String): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: ESPECES
    }
}

enum class ClientCategory(val labelFr: String) {
    PARTICULIER("Particulier"),
    VIP("Client VIP"),
    ENTREPRISE("Entreprise"),
    HOTEL("Hôtel"),
    RESTAURANT("Restaurant");

    companion object {
        fun fromRaw(raw: String): ClientCategory =
            entries.firstOrNull { it.name == raw } ?: PARTICULIER
    }
}

enum class BillingUnit(val labelFr: String) {
    PIECE("Pièce"),
    KG("Kilogramme"),
    SQM("Mètre carré");

    companion object {
        fun fromRaw(raw: String): BillingUnit =
            entries.firstOrNull { it.name == raw } ?: PIECE
    }
}

enum class SubscriptionPlan(val labelFr: String, val monthlyFcfa: Long, val tagline: String) {
    STARTER("Starter", 5000L, "1 établissement · Commandes, Reçus, Caisse"),
    BUSINESS("Business", 10000L, "Comptabilité SYSCOHADA, Photos, Stocks, Livraison, Portail Client"),
    ENTERPRISE("Enterprise", 25000L, "Multi-agences, Analyses IA Gemini, API & Support 24/7");

    companion object {
        fun fromRaw(raw: String): SubscriptionPlan =
            entries.firstOrNull { it.name == raw } ?: BUSINESS
    }
}

enum class EmployeeRole(val labelFr: String) {
    DIRECTEUR("Directeur Général"),
    GERANT_AGENCE("Gérant d'Agence"),
    CAISSIER("Caissier(ère)"),
    OPERATEUR("Opérateur Atelier"),
    CONTROLE_QUALITE("Contrôle Qualité"),
    LIVREUR("Livreur");

    companion object {
        fun fromRaw(raw: String): EmployeeRole =
            entries.firstOrNull { it.name == raw.trim().uppercase() } ?: OPERATEUR
    }
}

/**
 * Generates high-entropy, non-sequential client tracking tokens (TRK-XXXX-XXXX-XXXX)
 * using SecureRandom so order tracking cannot be enumerated by guessing sequential ticket numbers.
 */
object TrackingTokenGenerator {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val secureRandom = java.security.SecureRandom()
    private val tokenRegex = Regex("^TRK-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$")

    fun generate(): String {
        fun chunk(): String = buildString(4) {
            repeat(4) {
                append(ALPHABET[secureRandom.nextInt(ALPHABET.length)])
            }
        }
        return "TRK-${chunk()}-${chunk()}-${chunk()}"
    }

    fun isValidFormat(token: String): Boolean = tokenRegex.matches(token.trim().uppercase())
}

/**
 * Strictly sanitized client-facing order projection for the Client Portal.
 * Excludes internal observations, staff assignments, client phone/address, and garment defect notes.
 */
data class ClientPortalOrderView(
    val orderNumber: String,
    val trackingToken: String,
    val agencyName: String,
    val maskedClientLabel: String,
    val status: OrderStatus,
    val isLate: Boolean,
    val dueDateMillis: Long,
    val itemCount: Long,
    val paymentStatus: PaymentStatus,
    val remainingAmount: Long,
    val deliveryRequested: Boolean
) {
    companion object {
        fun maskClientName(rawName: String): String {
            val parts = rawName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (parts.isEmpty()) return "Client"
            if (parts.size == 1) return parts[0].take(1).uppercase() + "***"
            return "${parts.first()} ${parts.last().take(1).uppercase()}."
        }

        fun fromOrder(order: PressingOrder): ClientPortalOrderView = ClientPortalOrderView(
            orderNumber = order.orderNumber,
            trackingToken = order.trackingToken,
            agencyName = order.agencyName.ifBlank { "Agence Principale" },
            maskedClientLabel = maskClientName(order.clientName),
            status = order.orderStatusEnum(),
            isLate = order.isLate(),
            dueDateMillis = order.dueDateMillis,
            itemCount = order.itemCount.coerceAtLeast(order.parsedItems().sumOf { it.quantity.coerceAtLeast(1L) }),
            paymentStatus = order.paymentStatusEnum(),
            remainingAmount = order.remainingAmount.coerceAtLeast(0L),
            deliveryRequested = order.deliveryRequested
        )

        /**
         * Resolves a single sanitized order ONLY when the caller supplies an exact, non-empty
         * cryptographically random trackingToken (matching case-insensitively).
         */
        fun findByExactToken(orders: List<PressingOrder>, rawInput: String): ClientPortalOrderView? {
            val candidate = rawInput.trim().uppercase()
            if (candidate.isBlank() || !TrackingTokenGenerator.isValidFormat(candidate)) return null
            val matched = orders.firstOrNull { ord ->
                ord.trackingToken.isNotBlank() && ord.trackingToken.equals(candidate, ignoreCase = true)
            } ?: return null
            return fromOrder(matched)
        }
    }
}

@Keep
@IgnoreExtraProperties
data class CompanyConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val companyName: String = "Mon Pressing Pro",
    val legalName: String = "SARL",
    val rccm: String = "",
    val nif: String = "",
    val description: String = "Pressing, laverie et blanchisserie professionnelle",
    val phone: String = "+225 07 00 00 00 00",
    val whatsapp: String = "+225 07 00 00 00 00",
    val email: String = "",
    val country: String = "Côte d'Ivoire",
    val city: String = "Abidjan",
    val commune: String = "Cocody",
    val address: String = "",
    val latitude: Double = 5.359952,
    val longitude: Double = -4.008256,
    val currency: String = "FCFA",
    val taxRatePercent: Double = 0.0,
    val openingHours: String = "Lun-Sam : 07h30 - 19h30",
    val standardDelayHours: Long = 48L,
    val expressDelayHours: Long = 24L,
    val expressSurchargePercent: Double = 50.0,
    val ultraExpressDelayHours: Long = 6L,
    val ultraExpressSurchargePercent: Double = 100.0,
    val maxCashierDiscountPercent: Double = 10.0,
    val corporateDiscountPercent: Double = 15.0,
    val orderPrefix: String = "PRESS-2026",
    val receiptFooter: String = "Merci pour votre confiance. Conservation maximale 30 jours.",
    val receiptFormat: String = "THERMAL_80",
    val autoCutPaper: Boolean = true,
    val autoPrintLabelsOnPayment: Boolean = false,
    val defaultPrinterName: String = "",
    val defaultPrinterAddress: String = "",
    val lastCashCloseDate: String = "",
    val acceptedPayments: List<String> = listOf("ESPECES", "ORANGE_MONEY", "MTN_MOMO", "WAVE", "MOOV_MONEY"),
    val agencies: List<String> = listOf("Agence Principale"),
    val smsNotificationsEnabled: Boolean = true,
    val whatsappNotificationsEnabled: Boolean = true,
    val subscriptionPlan: String = "BUSINESS",
    val subscriptionBilling: String = "MONTHLY",
    val subscriptionStatus: String = "ACTIVE",
    val setupStep: Long = 1L,
    val setupCompleted: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "companyName" to companyName.trim().ifBlank { "Mon Pressing Pro" }.take(120),
        "legalName" to legalName.trim().take(120),
        "rccm" to rccm.trim().take(60),
        "nif" to nif.trim().take(60),
        "description" to description.trim().take(500),
        "phone" to phone.trim().take(30),
        "whatsapp" to whatsapp.trim().take(30),
        "email" to email.trim().take(120),
        "country" to country.trim().take(60),
        "city" to city.trim().take(80),
        "commune" to commune.trim().take(80),
        "address" to address.trim().take(250),
        "latitude" to latitude.coerceIn(-90.0, 90.0),
        "longitude" to longitude.coerceIn(-180.0, 180.0),
        "currency" to currency.trim().ifBlank { "FCFA" }.take(10),
        "taxRatePercent" to taxRatePercent.coerceIn(0.0, 50.0),
        "openingHours" to openingHours.trim().take(120),
        "standardDelayHours" to standardDelayHours.coerceIn(1L, 720L),
        "expressDelayHours" to expressDelayHours.coerceIn(1L, 168L),
        "expressSurchargePercent" to expressSurchargePercent.coerceIn(0.0, 200.0),
        "ultraExpressDelayHours" to ultraExpressDelayHours.coerceIn(1L, 168L),
        "ultraExpressSurchargePercent" to ultraExpressSurchargePercent.coerceIn(0.0, 300.0),
        "maxCashierDiscountPercent" to maxCashierDiscountPercent.coerceIn(0.0, 100.0),
        "corporateDiscountPercent" to corporateDiscountPercent.coerceIn(0.0, 100.0),
        "orderPrefix" to orderPrefix.trim().ifBlank { "PRESS-2026" }.take(20),
        "receiptFooter" to receiptFooter.trim().take(300),
        "receiptFormat" to if (receiptFormat in listOf("THERMAL_58", "THERMAL_80", "A4_PDF")) receiptFormat else "THERMAL_80",
        "autoCutPaper" to autoCutPaper,
        "autoPrintLabelsOnPayment" to autoPrintLabelsOnPayment,
        "defaultPrinterName" to defaultPrinterName.trim().take(100),
        "defaultPrinterAddress" to defaultPrinterAddress.trim().take(40),
        "lastCashCloseDate" to lastCashCloseDate.trim().take(20),
        "acceptedPayments" to acceptedPayments.take(10),
        "agencies" to agencies.filter { it.isNotBlank() }.ifEmpty { listOf("Agence Principale") }.take(20),
        "smsNotificationsEnabled" to smsNotificationsEnabled,
        "whatsappNotificationsEnabled" to whatsappNotificationsEnabled,
        "subscriptionPlan" to if (subscriptionPlan in listOf("STARTER", "BUSINESS", "ENTERPRISE")) subscriptionPlan else "BUSINESS",
        "subscriptionBilling" to if (subscriptionBilling in listOf("MONTHLY", "QUARTERLY", "ANNUAL")) subscriptionBilling else "MONTHLY",
        "subscriptionStatus" to if (subscriptionStatus in listOf("TRIAL", "ACTIVE", "GRACE", "SUSPENDED")) subscriptionStatus else "ACTIVE",
        "setupStep" to setupStep.coerceIn(1L, 10L),
        "setupCompleted" to setupCompleted,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "companyName" to companyName.trim().ifBlank { "Mon Pressing Pro" }.take(120),
        "legalName" to legalName.trim().take(120),
        "rccm" to rccm.trim().take(60),
        "nif" to nif.trim().take(60),
        "description" to description.trim().take(500),
        "phone" to phone.trim().take(30),
        "whatsapp" to whatsapp.trim().take(30),
        "email" to email.trim().take(120),
        "country" to country.trim().take(60),
        "city" to city.trim().take(80),
        "commune" to commune.trim().take(80),
        "address" to address.trim().take(250),
        "latitude" to latitude.coerceIn(-90.0, 90.0),
        "longitude" to longitude.coerceIn(-180.0, 180.0),
        "currency" to currency.trim().ifBlank { "FCFA" }.take(10),
        "taxRatePercent" to taxRatePercent.coerceIn(0.0, 50.0),
        "openingHours" to openingHours.trim().take(120),
        "standardDelayHours" to standardDelayHours.coerceIn(1L, 720L),
        "expressDelayHours" to expressDelayHours.coerceIn(1L, 168L),
        "expressSurchargePercent" to expressSurchargePercent.coerceIn(0.0, 200.0),
        "ultraExpressDelayHours" to ultraExpressDelayHours.coerceIn(1L, 168L),
        "ultraExpressSurchargePercent" to ultraExpressSurchargePercent.coerceIn(0.0, 300.0),
        "maxCashierDiscountPercent" to maxCashierDiscountPercent.coerceIn(0.0, 100.0),
        "corporateDiscountPercent" to corporateDiscountPercent.coerceIn(0.0, 100.0),
        "orderPrefix" to orderPrefix.trim().ifBlank { "PRESS-2026" }.take(20),
        "receiptFooter" to receiptFooter.trim().take(300),
        "receiptFormat" to if (receiptFormat in listOf("THERMAL_58", "THERMAL_80", "A4_PDF")) receiptFormat else "THERMAL_80",
        "autoCutPaper" to autoCutPaper,
        "autoPrintLabelsOnPayment" to autoPrintLabelsOnPayment,
        "defaultPrinterName" to defaultPrinterName.trim().take(100),
        "defaultPrinterAddress" to defaultPrinterAddress.trim().take(40),
        "lastCashCloseDate" to lastCashCloseDate.trim().take(20),
        "acceptedPayments" to acceptedPayments.take(10),
        "agencies" to agencies.filter { it.isNotBlank() }.ifEmpty { listOf("Agence Principale") }.take(20),
        "smsNotificationsEnabled" to smsNotificationsEnabled,
        "whatsappNotificationsEnabled" to whatsappNotificationsEnabled,
        "subscriptionPlan" to if (subscriptionPlan in listOf("STARTER", "BUSINESS", "ENTERPRISE")) subscriptionPlan else "BUSINESS",
        "subscriptionBilling" to if (subscriptionBilling in listOf("MONTHLY", "QUARTERLY", "ANNUAL")) subscriptionBilling else "MONTHLY",
        "subscriptionStatus" to if (subscriptionStatus in listOf("TRIAL", "ACTIVE", "GRACE", "SUSPENDED")) subscriptionStatus else "ACTIVE",
        "setupStep" to setupStep.coerceIn(1L, 10L),
        "setupCompleted" to setupCompleted,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ServiceItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Vêtements",
    val treatmentType: String = "Lavage + Repassage",
    val billingUnit: String = "PIECE",
    val unitPrice: Long = 1000L,
    val expressPrice: Long = 1500L,
    val delayHours: Long = 48L,
    val available: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().take(100),
        "category" to category.trim().ifBlank { "Vêtements" }.take(50),
        "treatmentType" to treatmentType.trim().ifBlank { "Lavage + Repassage" }.take(60),
        "billingUnit" to BillingUnit.fromRaw(billingUnit).name,
        "unitPrice" to unitPrice.coerceIn(0L, 10_000_000L),
        "expressPrice" to expressPrice.coerceIn(0L, 10_000_000L),
        "delayHours" to delayHours.coerceIn(1L, 720L),
        "available" to available,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().take(100),
        "category" to category.trim().ifBlank { "Vêtements" }.take(50),
        "treatmentType" to treatmentType.trim().ifBlank { "Lavage + Repassage" }.take(60),
        "billingUnit" to BillingUnit.fromRaw(billingUnit).name,
        "unitPrice" to unitPrice.coerceIn(0L, 10_000_000L),
        "expressPrice" to expressPrice.coerceIn(0L, 10_000_000L),
        "delayHours" to delayHours.coerceIn(1L, 720L),
        "available" to available,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ClientRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val category: String = "PARTICULIER",
    val address: String = "",
    val latitude: Double = 5.359952,
    val longitude: Double = -4.008256,
    val loyaltyPoints: Long = 0L,
    val discountPercent: Double = 0.0,
    val creditLimit: Long = 0L,
    val balanceDue: Long = 0L,
    val preferences: String = "",
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "fullName" to fullName.trim().take(120),
        "phone" to phone.trim().take(30),
        "whatsapp" to whatsapp.trim().take(30),
        "email" to email.trim().take(120),
        "category" to ClientCategory.fromRaw(category).name,
        "address" to address.trim().take(250),
        "latitude" to latitude.coerceIn(-90.0, 90.0),
        "longitude" to longitude.coerceIn(-180.0, 180.0),
        "loyaltyPoints" to loyaltyPoints.coerceIn(0L, 10_000_000L),
        "discountPercent" to discountPercent.coerceIn(0.0, 100.0),
        "creditLimit" to creditLimit.coerceIn(0L, 100_000_000L),
        "balanceDue" to balanceDue.coerceIn(-100_000_000L, 100_000_000L),
        "preferences" to preferences.trim().take(300),
        "notes" to notes.trim().take(500),
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "fullName" to fullName.trim().take(120),
        "phone" to phone.trim().take(30),
        "whatsapp" to whatsapp.trim().take(30),
        "email" to email.trim().take(120),
        "category" to ClientCategory.fromRaw(category).name,
        "address" to address.trim().take(250),
        "latitude" to latitude.coerceIn(-90.0, 90.0),
        "longitude" to longitude.coerceIn(-180.0, 180.0),
        "loyaltyPoints" to loyaltyPoints.coerceIn(0L, 10_000_000L),
        "discountPercent" to discountPercent.coerceIn(0.0, 100.0),
        "creditLimit" to creditLimit.coerceIn(0L, 100_000_000L),
        "balanceDue" to balanceDue.coerceIn(-100_000_000L, 100_000_000L),
        "preferences" to preferences.trim().take(300),
        "notes" to notes.trim().take(500),
        "active" to active,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class OrderGarmentItem(
    val garmentName: String = "",
    val serviceName: String = "Lavage + Repassage",
    val fabric: String = "Coton",
    val color: String = "",
    val soilLevel: String = "Normal",
    val defects: String = "",
    val notes: String = "",
    val quantity: Long = 1L,
    val unitPrice: Long = 1000L,
    val lineTotal: Long = 1000L,
    val barcode: String = "",
    val photoBase64: String = "",
    val aiDiagnosis: String = "",
    val workshopStatus: String = GarmentWorkshopStatus.RECEIVED.name
) {
    fun workshopStatusEnum(): GarmentWorkshopStatus = GarmentWorkshopStatus.fromRaw(workshopStatus)

    fun toMap(): Map<String, Any> = mapOf(
        "garmentName" to garmentName.trim().take(100),
        "serviceName" to serviceName.trim().take(80),
        "fabric" to fabric.trim().take(50),
        "color" to color.trim().take(40),
        "soilLevel" to soilLevel.trim().take(30),
        "defects" to defects.trim().take(200),
        "notes" to notes.trim().take(200),
        "quantity" to quantity.coerceAtLeast(1L),
        "unitPrice" to unitPrice.coerceAtLeast(0L),
        "lineTotal" to (quantity.coerceAtLeast(1L) * unitPrice.coerceAtLeast(0L)),
        "barcode" to barcode.trim().take(40),
        "photoBase64" to photoBase64.take(220_000),
        "aiDiagnosis" to aiDiagnosis.trim().take(300),
        "workshopStatus" to GarmentWorkshopStatus.fromRaw(workshopStatus).name
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): OrderGarmentItem = OrderGarmentItem(
            garmentName = map["garmentName"] as? String ?: "",
            serviceName = map["serviceName"] as? String ?: "Lavage + Repassage",
            fabric = map["fabric"] as? String ?: "",
            color = map["color"] as? String ?: "",
            soilLevel = map["soilLevel"] as? String ?: "Normal",
            defects = map["defects"] as? String ?: "",
            notes = map["notes"] as? String ?: "",
            quantity = (map["quantity"] as? Number)?.toLong() ?: 1L,
            unitPrice = (map["unitPrice"] as? Number)?.toLong() ?: 0L,
            lineTotal = (map["lineTotal"] as? Number)?.toLong() ?: 0L,
            barcode = map["barcode"] as? String ?: "",
            photoBase64 = map["photoBase64"] as? String ?: "",
            aiDiagnosis = map["aiDiagnosis"] as? String ?: "",
            workshopStatus = map["workshopStatus"] as? String ?: GarmentWorkshopStatus.RECEIVED.name
        )
    }
}

@Keep
@IgnoreExtraProperties
data class PressingOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val trackingToken: String = "",
    val agencyName: String = "Agence Principale",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientAddress: String = "",
    val status: String = "PENDING",
    val priority: String = "STANDARD",
    val items: List<Map<String, Any?>> = emptyList(),
    val itemCount: Long = 0L,
    val subtotal: Long = 0L,
    val discountAmount: Long = 0L,
    val expressFee: Long = 0L,
    val deliveryFee: Long = 0L,
    val taxAmount: Long = 0L,
    val totalAmount: Long = 0L,
    val paidAmount: Long = 0L,
    val remainingAmount: Long = 0L,
    val paymentStatus: String = "UNPAID",
    val paymentMethod: String = "ESPECES",
    val deliveryRequested: Boolean = false,
    val deliveryDriver: String = "",
    val deliveryZone: String = "",
    val assignedEmployee: String = "",
    val qualityChecked: Boolean = false,
    val clientSignatureName: String = "",
    val observations: String = "",
    val dueDateMillis: Long = 0L,
    val statusHistory: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun parsedItems(): List<OrderGarmentItem> = items.map { OrderGarmentItem.fromMap(it) }

    fun orderStatusEnum(): OrderStatus = OrderStatus.fromRaw(status)
    fun paymentStatusEnum(): PaymentStatus = PaymentStatus.fromRaw(paymentStatus)
    fun paymentMethodEnum(): PaymentMethod = PaymentMethod.fromRaw(paymentMethod)
    fun priorityEnum(): OrderPriority = OrderPriority.fromRaw(priority)

    fun isLate(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val st = orderStatusEnum()
        return dueDateMillis > 0L &&
            nowMillis > dueDateMillis &&
            st !in listOf(OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.DELIVERED, OrderStatus.CANCELLED)
    }
}

@Keep
@IgnoreExtraProperties
data class AccountingEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reference: String = "",
    val entryType: String = "REVENUE",
    val category: String = "Prestation Pressing",
    val syscohadaAccount: String = "706",
    val label: String = "",
    val amount: Long = 0L,
    val paymentMethod: String = "ESPECES",
    val orderId: String = "",
    val counterparty: String = "",
    val agencyName: String = "Agence Principale",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "reference" to reference.trim().ifBlank { "ECR-${System.currentTimeMillis() % 100000}" }.take(60),
        "entryType" to if (entryType in listOf("REVENUE", "EXPENSE", "CASH_OPEN", "CASH_CLOSE", "REFUND")) entryType else "REVENUE",
        "category" to category.trim().ifBlank { "Opération" }.take(80),
        "syscohadaAccount" to syscohadaAccount.trim().ifBlank { "706" }.take(20),
        "label" to label.trim().ifBlank { "Écriture comptable" }.take(200),
        "amount" to amount.coerceIn(0L, 100_000_000L),
        "paymentMethod" to PaymentMethod.fromRaw(paymentMethod).name,
        "orderId" to orderId.trim().take(128),
        "counterparty" to counterparty.trim().take(120),
        "agencyName" to agencyName.trim().take(100),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class StockItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val itemKind: String = "CONSUMABLE",
    val category: String = "Produits d'entretien",
    val quantity: Long = 0L,
    val minThreshold: Long = 5L,
    val unit: String = "kg",
    val unitCost: Long = 0L,
    val supplierName: String = "",
    val machineStatus: String = "OPERATIONAL",
    val maintenanceNotes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun isLowStock(): Boolean = itemKind == "CONSUMABLE" && quantity <= minThreshold

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().take(100),
        "itemKind" to if (itemKind in listOf("CONSUMABLE", "MACHINE")) itemKind else "CONSUMABLE",
        "category" to category.trim().ifBlank { "Consommables" }.take(60),
        "quantity" to quantity.coerceIn(0L, 1_000_000L),
        "minThreshold" to minThreshold.coerceIn(0L, 1_000_000L),
        "unit" to unit.trim().ifBlank { "pcs" }.take(30),
        "unitCost" to unitCost.coerceIn(0L, 100_000_000L),
        "supplierName" to supplierName.trim().take(120),
        "machineStatus" to if (machineStatus in listOf("OPERATIONAL", "MAINTENANCE", "BREAKDOWN")) machineStatus else "OPERATIONAL",
        "maintenanceNotes" to maintenanceNotes.trim().take(400),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().take(100),
        "itemKind" to if (itemKind in listOf("CONSUMABLE", "MACHINE")) itemKind else "CONSUMABLE",
        "category" to category.trim().ifBlank { "Consommables" }.take(60),
        "quantity" to quantity.coerceIn(0L, 1_000_000L),
        "minThreshold" to minThreshold.coerceIn(0L, 1_000_000L),
        "unit" to unit.trim().ifBlank { "pcs" }.take(30),
        "unitCost" to unitCost.coerceIn(0L, 100_000_000L),
        "supplierName" to supplierName.trim().take(120),
        "machineStatus" to if (machineStatus in listOf("OPERATIONAL", "MAINTENANCE", "BREAKDOWN")) machineStatus else "OPERATIONAL",
        "maintenanceNotes" to maintenanceNotes.trim().take(400),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class EmployeeRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val role: String = "OPERATEUR",
    val agencyName: String = "Agence Principale",
    val monthlySalary: Long = 100_000L,
    val commissionRate: Double = 0.0,
    val presentToday: Boolean = true,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "fullName" to fullName.trim().take(120),
        "phone" to phone.trim().take(30),
        "role" to EmployeeRole.fromRaw(role).name,
        "agencyName" to agencyName.trim().take(100),
        "monthlySalary" to monthlySalary.coerceIn(0L, 100_000_000L),
        "commissionRate" to commissionRate.coerceIn(0.0, 100.0),
        "presentToday" to presentToday,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "fullName" to fullName.trim().take(120),
        "phone" to phone.trim().take(30),
        "role" to EmployeeRole.fromRaw(role).name,
        "agencyName" to agencyName.trim().take(100),
        "monthlySalary" to monthlySalary.coerceIn(0L, 100_000_000L),
        "commissionRate" to commissionRate.coerceIn(0.0, 100.0),
        "presentToday" to presentToday,
        "active" to active,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ClaimRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val clientName: String = "",
    val subject: String = "",
    val description: String = "",
    val severity: String = "MEDIUM",
    val status: String = "OPEN",
    val resolutionNote: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "orderNumber" to orderNumber.trim().take(40),
        "clientName" to clientName.trim().take(120),
        "subject" to subject.trim().take(120),
        "description" to description.trim().take(600),
        "severity" to if (severity in listOf("LOW", "MEDIUM", "HIGH")) severity else "MEDIUM",
        "status" to if (status in listOf("OPEN", "IN_PROGRESS", "RESOLVED")) status else "OPEN",
        "resolutionNote" to resolutionNote.trim().take(400),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AuditLogRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actor: String = "",
    val roleLabel: String = "",
    val scopeLabel: String = "",
    val action: String = "",
    val route: String = "",
    val allowed: Boolean = true,
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "actor" to actor.trim().ifBlank { "Utilisateur" }.take(100),
        "roleLabel" to roleLabel.trim().ifBlank { "Opérateur" }.take(60),
        "scopeLabel" to scopeLabel.trim().ifBlank { "Agence" }.take(120),
        "action" to action.trim().ifBlank { "Événement sécurité" }.take(240),
        "route" to route.trim().take(60),
        "allowed" to allowed,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class FcmTokenRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val token: String = "",
    val role: String = "STAFF",
    val agencyName: String = "",
    val topics: List<String> = emptyList(),
    val platform: String = "ANDROID",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "token" to token.trim().take(300),
        "role" to role.trim().ifBlank { "STAFF" }.take(40),
        "agencyName" to agencyName.trim().take(100),
        "topics" to topics.filter { it.isNotBlank() }.take(15),
        "platform" to "ANDROID",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class PushNotificationRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val eventType: String = "NEW_ORDER_CASHIER",
    val targetRole: String = "CAISSIER",
    val agencyName: String = "",
    val orderNumber: String = "",
    val clientPhone: String = "",
    val title: String = "",
    val body: String = "",
    val delivered: Boolean = false,
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "eventType" to if (eventType in listOf("NEW_ORDER_CASHIER", "ORDER_READY_CLIENT", "OFFLINE_SYNCED")) eventType else "NEW_ORDER_CASHIER",
        "targetRole" to targetRole.trim().ifBlank { "CAISSIER" }.take(40),
        "agencyName" to agencyName.trim().take(100),
        "orderNumber" to orderNumber.trim().take(40),
        "clientPhone" to clientPhone.trim().take(30),
        "title" to title.trim().ifBlank { "Pressing Pro" }.take(120),
        "body" to body.trim().ifBlank { "Mise à jour commande" }.take(300),
        "delivered" to delivered,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

object DefaultCatalogTemplates {
    fun ivorianStarterServices(): List<ServiceItem> = listOf(
        // 1. Chemises & Hauts
        ServiceItem(name = "Chemise classique", category = "Chemises & Hauts", treatmentType = "Lavage + Repassage", billingUnit = "PIECE", unitPrice = 1000L, expressPrice = 1500L, delayHours = 48L),
        ServiceItem(name = "Chemise en soie / lin", category = "Chemises & Hauts", treatmentType = "Nettoyage à sec", billingUnit = "PIECE", unitPrice = 1500L, expressPrice = 2250L, delayHours = 48L),
        ServiceItem(name = "Chemisier / Blouse", category = "Chemises & Hauts", treatmentType = "Lavage délicat + Repassage", billingUnit = "PIECE", unitPrice = 1000L, expressPrice = 1500L, delayHours = 48L),
        ServiceItem(name = "Polo", category = "Chemises & Hauts", treatmentType = "Lavage + Repassage", billingUnit = "PIECE", unitPrice = 800L, expressPrice = 1200L, delayHours = 48L),
        ServiceItem(name = "T-shirt", category = "Chemises & Hauts", treatmentType = "Lavage + Repassage", billingUnit = "PIECE", unitPrice = 700L, expressPrice = 1000L, delayHours = 48L),
        ServiceItem(name = "Pull / Gilet", category = "Chemises & Hauts", treatmentType = "Lavage délicat", billingUnit = "PIECE", unitPrice = 1500L, expressPrice = 2250L, delayHours = 48L),
        ServiceItem(name = "Sweat-shirt / Hoodie", category = "Chemises & Hauts", treatmentType = "Lavage + Séchage", billingUnit = "PIECE", unitPrice = 1500L, expressPrice = 2250L, delayHours = 48L),

        // 2. Pantalons & Bas
        ServiceItem(name = "Pantalon classique", category = "Pantalons & Bas", treatmentType = "Lavage + Repassage", billingUnit = "PIECE", unitPrice = 1000L, expressPrice = 1500L, delayHours = 48L),
        ServiceItem(name = "Jean / Denim", category = "Pantalons & Bas", treatmentType = "Lavage + Repassage", billingUnit = "PIECE", unitPrice = 1200L, expressPrice = 1800L, delayHours = 48L),
        ServiceItem(name = "Jupe simple", category = "Pantalons & Bas", treatmentType = "Lavage + Repassage", billingUnit = "PIECE", unitPrice = 1000L, expressPrice = 1500L, delayHours = 48L),
        ServiceItem(name = "Jupe plissée", category = "Pantalons & Bas", treatmentType = "Nettoyage à sec", billingUnit = "PIECE", unitPrice = 1500L, expressPrice = 2250L, delayHours = 48L),
        ServiceItem(name = "Short / Bermuda", category = "Pantalons & Bas", treatmentType = "Lavage + Repassage", billingUnit = "PIECE", unitPrice = 800L, expressPrice = 1200L, delayHours = 48L),

        // 3. Costumes, Vestes & Manteaux
        ServiceItem(name = "Costume 2 pièces", category = "Costumes & Vestes", treatmentType = "Nettoyage à sec", billingUnit = "PIECE", unitPrice = 3000L, expressPrice = 4500L, delayHours = 48L),
        ServiceItem(name = "Costume 3 pièces", category = "Costumes & Vestes", treatmentType = "Nettoyage à sec", billingUnit = "PIECE", unitPrice = 4000L, expressPrice = 6000L, delayHours = 48L),
        ServiceItem(name = "Tailleur femme (2 pièces)", category = "Costumes & Vestes", treatmentType = "Nettoyage à sec", billingUnit = "PIECE", unitPrice = 3000L, expressPrice = 4500L, delayHours = 48L),
        ServiceItem(name = "Veste / Blazer", category = "Costumes & Vestes", treatmentType = "Nettoyage à sec", billingUnit = "PIECE", unitPrice = 2000L, expressPrice = 3000L, delayHours = 48L),
        ServiceItem(name = "Manteau / Trench-coat", category = "Costumes & Vestes", treatmentType = "Nettoyage à sec", billingUnit = "PIECE", unitPrice = 3500L, expressPrice = 5000L, delayHours = 72L),
        ServiceItem(name = "Blouson / Doudoune", category = "Costumes & Vestes", treatmentType = "Nettoyage spécial", billingUnit = "PIECE", unitPrice = 3000L, expressPrice = 4500L, delayHours = 72L),

        // 4. Tenues Africaines & Traditionnelles
        ServiceItem(name = "Boubou 3 pièces (Bazin riche)", category = "Tenues Africaines", treatmentType = "Nettoyage délicat + Amidon", billingUnit = "PIECE", unitPrice = 4000L, expressPrice = 6000L, delayHours = 72L),
        ServiceItem(name = "Boubou simple / Caïman", category = "Tenues Africaines", treatmentType = "Lavage + Repassage vapeur", billingUnit = "PIECE", unitPrice = 2500L, expressPrice = 3750L, delayHours = 48L),
        ServiceItem(name = "Ensemble Wax / Pagne (2 pièces)", category = "Tenues Africaines", treatmentType = "Lavage doux + Repassage", billingUnit = "PIECE", unitPrice = 2000L, expressPrice = 3000L, delayHours = 48L),
        ServiceItem(name = "Pagne Kita / Kente traditionnel", category = "Tenues Africaines", treatmentType = "Nettoyage à sec délicat", billingUnit = "PIECE", unitPrice = 3500L, expressPrice = 5250L, delayHours = 72L),
        ServiceItem(name = "Tunique africaine / Dashiki", category = "Tenues Africaines", treatmentType = "Lavage + Repassage", billingUnit = "PIECE", unitPrice = 1500L, expressPrice = 2250L, delayHours = 48L),
        ServiceItem(name = "Robe en pagne / dentelle", category = "Tenues Africaines", treatmentType = "Lavage délicat + Repassage", billingUnit = "PIECE", unitPrice = 2000L, expressPrice = 3000L, delayHours = 48L),

        // 5. Robes & Cérémonies
        ServiceItem(name = "Robe simple", category = "Robes & Cérémonies", treatmentType = "Lavage + Repassage", billingUnit = "PIECE", unitPrice = 1500L, expressPrice = 2250L, delayHours = 48L),
        ServiceItem(name = "Robe de soirée / Cocktail", category = "Robes & Cérémonies", treatmentType = "Nettoyage à sec délicat", billingUnit = "PIECE", unitPrice = 3500L, expressPrice = 5000L, delayHours = 72L),
        ServiceItem(name = "Robe de mariée", category = "Robes & Cérémonies", treatmentType = "Traitement haute couture", billingUnit = "PIECE", unitPrice = 15000L, expressPrice = 22000L, delayHours = 96L),
        ServiceItem(name = "Combinaison / Combishort", category = "Robes & Cérémonies", treatmentType = "Lavage + Repassage", billingUnit = "PIECE", unitPrice = 2000L, expressPrice = 3000L, delayHours = 48L),

        // 6. Linge de Maison & Hôtellerie
        ServiceItem(name = "Drap (1 ou 2 places)", category = "Linge de Maison", treatmentType = "Blanchisserie", billingUnit = "PIECE", unitPrice = 1500L, expressPrice = 2250L, delayHours = 48L),
        ServiceItem(name = "Housse de couette", category = "Linge de Maison", treatmentType = "Blanchisserie", billingUnit = "PIECE", unitPrice = 2000L, expressPrice = 3000L, delayHours = 48L),
        ServiceItem(name = "Couette / Édredon", category = "Linge de Maison", treatmentType = "Nettoyage spécial", billingUnit = "PIECE", unitPrice = 3500L, expressPrice = 5000L, delayHours = 72L),
        ServiceItem(name = "Couverture / Plaid", category = "Linge de Maison", treatmentType = "Nettoyage spécial", billingUnit = "PIECE", unitPrice = 3000L, expressPrice = 4500L, delayHours = 72L),
        ServiceItem(name = "Rideaux (le panneau)", category = "Linge de Maison", treatmentType = "Blanchisserie + Vapeur", billingUnit = "PIECE", unitPrice = 2500L, expressPrice = 3750L, delayHours = 72L),
        ServiceItem(name = "Nappe de table", category = "Linge de Maison", treatmentType = "Détachage + Repassage", billingUnit = "PIECE", unitPrice = 1500L, expressPrice = 2250L, delayHours = 48L),
        ServiceItem(name = "Serviette / Peignoir de bain", category = "Linge de Maison", treatmentType = "Blanchisserie", billingUnit = "PIECE", unitPrice = 1000L, expressPrice = 1500L, delayHours = 48L),
        ServiceItem(name = "Tapis (au m²)", category = "Linge de Maison", treatmentType = "Nettoyage tapis", billingUnit = "SQM", unitPrice = 2500L, expressPrice = 3500L, delayHours = 72L),

        // 7. Chaussures, Accessoires & Services au Kg
        ServiceItem(name = "Baskets / Sneakers", category = "Chaussures & Accessoires", treatmentType = "Nettoyage chaussures", billingUnit = "PIECE", unitPrice = 2500L, expressPrice = 3500L, delayHours = 48L),
        ServiceItem(name = "Chaussures cuir / daim", category = "Chaussures & Accessoires", treatmentType = "Soin & Nettoyage cuir", billingUnit = "PIECE", unitPrice = 3000L, expressPrice = 4500L, delayHours = 48L),
        ServiceItem(name = "Cravate / Nœud papillon", category = "Chaussures & Accessoires", treatmentType = "Nettoyage à sec", billingUnit = "PIECE", unitPrice = 800L, expressPrice = 1200L, delayHours = 48L),
        ServiceItem(name = "Foulard / Écharpe en soie", category = "Chaussures & Accessoires", treatmentType = "Nettoyage délicat", billingUnit = "PIECE", unitPrice = 1000L, expressPrice = 1500L, delayHours = 48L),
        ServiceItem(name = "Sac à main / Sac à dos", category = "Chaussures & Accessoires", treatmentType = "Nettoyage spécial", billingUnit = "PIECE", unitPrice = 2500L, expressPrice = 3500L, delayHours = 72L),
        ServiceItem(name = "Repassage simple (la pièce)", category = "Repassage & Laverie", treatmentType = "Repassage vapeur", billingUnit = "PIECE", unitPrice = 500L, expressPrice = 750L, delayHours = 24L),
        ServiceItem(name = "Lavage au kilogramme", category = "Repassage & Laverie", treatmentType = "Blanchisserie au kg", billingUnit = "KG", unitPrice = 1000L, expressPrice = 1500L, delayHours = 24L)
    )
}
