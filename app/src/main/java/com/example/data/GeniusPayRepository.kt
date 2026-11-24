package com.example.data

import android.util.Log
import androidx.annotation.Keep
import com.example.BuildConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val GENIUSPAY_TAG = "GeniusPayRepository"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

enum class SubscriptionPaymentStatus(val labelFr: String) {
    PENDING("En attente"),
    COMPLETED("Payé & Activé"),
    FAILED("Échoué"),
    CANCELLED("Annulé");

    companion object {
        fun fromGeniusPayRaw(raw: String?): SubscriptionPaymentStatus {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return when {
                normalized in listOf("completed", "paid", "succeeded", "success", "approved") -> COMPLETED
                normalized in listOf("failed", "declined", "error", "expired") -> FAILED
                normalized in listOf("cancelled", "canceled", "aborted") -> CANCELLED
                else -> PENDING
            }
        }
    }
}

data class SubscriptionRenewalQuote(
    val plan: SubscriptionPlan,
    val billingCycle: String,
    val months: Int,
    val baseFcfa: Long,
    val discountPercent: Int,
    val finalAmountFcfa: Long,
    val periodLabelFr: String
)

/**
 * Authoritative SaaS renewal pricing calculator in XOF (FCFA).
 * Mirrors the server-side pricing table in backend/functions/index.js.
 */
object SubscriptionRenewalPricing {
    fun computeQuote(plan: SubscriptionPlan, billingCycle: String): SubscriptionRenewalQuote {
        val normalizedCycle = billingCycle.trim().uppercase()
        val (months, discountPercent, label) = when (normalizedCycle) {
            "QUARTERLY" -> Triple(3, 10, "Trimestriel (3 mois · -10%)")
            "ANNUAL" -> Triple(12, 20, "Annuel (12 mois · -20%)")
            else -> Triple(1, 0, "Mensuel (1 mois)")
        }
        val base = plan.monthlyFcfa * months.toLong()
        val discount = (base * discountPercent.toLong()) / 100L
        val finalAmount = (base - discount).coerceAtLeast(200L)
        return SubscriptionRenewalQuote(
            plan = plan,
            billingCycle = if (normalizedCycle in listOf("MONTHLY", "QUARTERLY", "ANNUAL")) normalizedCycle else "MONTHLY",
            months = months,
            baseFcfa = base,
            discountPercent = discountPercent,
            finalAmountFcfa = finalAmount,
            periodLabelFr = label
        )
    }
}

/**
 * Strict URL allowlist and HTTPS validation to prevent SSRF or open-redirect attacks
 * on GeniusPay API endpoints and hosted checkout links.
 */
object GeniusPayUrlValidator {
    private val allowedHosts = setOf("geniuspay.ci", "pay.genius.ci", "api.geniuspay.ci")

    fun isAllowedBaseUrl(rawUrl: String): Boolean {
        val cleaned = rawUrl.trim()
        if (!cleaned.startsWith("https://", ignoreCase = true)) return false
        return try {
            val uri = URI(cleaned)
            val host = uri.host?.lowercase() ?: return false
            host in allowedHosts
        } catch (_: Exception) {
            false
        }
    }

    fun isSafeCheckoutUrl(rawUrl: String): Boolean {
        val cleaned = rawUrl.trim()
        if (!cleaned.startsWith("https://", ignoreCase = true)) return false
        if (cleaned.contains("javascript:", ignoreCase = true) || cleaned.contains("data:", ignoreCase = true)) {
            return false
        }
        return try {
            val uri = URI(cleaned)
            val host = uri.host?.lowercase() ?: return false
            host in allowedHosts || host.endsWith(".geniuspay.ci") || host.endsWith(".genius.ci")
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Constant-time HMAC-SHA256 webhook signature validator (used in tests and mirrored in Cloud Functions).
 */
object GeniusPayWebhookSignature {
    fun computeHmacSha256Hex(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun verifyHmacSha256(payload: String, signatureHeader: String, secret: String): Boolean {
        if (payload.isEmpty() || signatureHeader.isBlank() || secret.isBlank()) return false
        val cleanedSig = signatureHeader.trim()
            .removePrefix("sha256=")
            .removePrefix("v1=")
            .lowercase()
        val expected = computeHmacSha256Hex(payload, secret).lowercase()
        return MessageDigest.isEqual(
            cleanedSig.toByteArray(Charsets.UTF_8),
            expected.toByteArray(Charsets.UTF_8)
        )
    }
}

data class GeniusPayRuntimeConfig(
    val apiKey: String = BuildConfig.GENIUSPAY_API_KEY,
    val apiSecret: String = BuildConfig.GENIUSPAY_API_SECRET,
    val mode: String = BuildConfig.GENIUSPAY_MODE,
    val baseUrl: String = BuildConfig.GENIUSPAY_BASE_URL
) {
    val sanitizedBaseUrl: String
        get() = baseUrl.trim().trimEnd('/').ifBlank { "https://geniuspay.ci/api/v1/merchant" }

    val normalizedMode: String
        get() = if (mode.trim().equals("test", ignoreCase = true)) "test" else "live"

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() &&
            apiSecret.isNotBlank() &&
            !apiKey.contains("PLACEHOLDER", ignoreCase = true) &&
            !apiSecret.contains("PLACEHOLDER", ignoreCase = true) &&
            GeniusPayUrlValidator.isAllowedBaseUrl(sanitizedBaseUrl)
}

data class GeniusPayCheckoutSession(
    val reference: String,
    val checkoutUrl: String,
    val amountFcfa: Long,
    val currency: String = "XOF",
    val plan: SubscriptionPlan,
    val billingCycle: String,
    val mode: String,
    val status: SubscriptionPaymentStatus = SubscriptionPaymentStatus.PENDING
)

@Keep
@IgnoreExtraProperties
data class SubscriptionPaymentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reference: String = "",
    val provider: String = "GENIUSPAY",
    val mode: String = "live",
    val plan: String = "BUSINESS",
    val billingCycle: String = "MONTHLY",
    val amountFcfa: Long = 0L,
    val currency: String = "XOF",
    val status: String = SubscriptionPaymentStatus.PENDING.name,
    val checkoutUrl: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun statusEnum(): SubscriptionPaymentStatus =
        SubscriptionPaymentStatus.entries.firstOrNull { it.name == status }
            ?: SubscriptionPaymentStatus.PENDING

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "reference" to reference.trim().take(80),
        "provider" to "GENIUSPAY",
        "mode" to if (mode.equals("test", ignoreCase = true)) "test" else "live",
        "plan" to if (plan in listOf("STARTER", "BUSINESS", "ENTERPRISE")) plan else "BUSINESS",
        "billingCycle" to if (billingCycle in listOf("MONTHLY", "QUARTERLY", "ANNUAL")) billingCycle else "MONTHLY",
        "amountFcfa" to amountFcfa.coerceIn(200L, 100_000_000L),
        "currency" to currency.trim().ifBlank { "XOF" }.take(10),
        "status" to if (status in listOf("PENDING", "COMPLETED", "FAILED", "CANCELLED")) status else "PENDING",
        "checkoutUrl" to checkoutUrl.trim().take(400),
        "customerEmail" to customerEmail.trim().take(120),
        "customerPhone" to customerPhone.trim().take(30),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

/**
 * Real HTTP integration with GeniusPay Côte d'Ivoire Merchant API v1 (https://geniuspay.ci/api/v1/merchant).
 * Never logs API keys or secrets. Reads credentials from BuildConfig populated via the AI Studio Secrets panel.
 */
class GeniusPayRepository(
    private val configProvider: () -> GeniusPayRuntimeConfig = { GeniusPayRuntimeConfig() },
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    fun currentConfig(): GeniusPayRuntimeConfig = configProvider()

    suspend fun initiateSubscriptionRenewal(
        companyName: String,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        plan: SubscriptionPlan,
        billingCycle: String,
        tenantUid: String
    ): Result<GeniusPayCheckoutSession> = withContext(Dispatchers.IO) {
        val cfg = configProvider()
        if (!cfg.isConfigured) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Clés GeniusPay non configurées dans l'environnement. Ajoutez GENIUSPAY_API_KEY et GENIUSPAY_API_SECRET dans le panneau Secrets d'AI Studio."
                )
            )
        }
        val quote = SubscriptionRenewalPricing.computeQuote(plan, billingCycle)
        val endpoint = "${cfg.sanitizedBaseUrl}/payments"

        val customerObj = JSONObject().apply {
            put("name", customerName.trim().ifBlank { companyName.trim().ifBlank { "Client Pressing Pro" } }.take(100))
            if (customerEmail.isNotBlank()) put("email", customerEmail.trim().take(120))
            if (customerPhone.isNotBlank()) put("phone", customerPhone.trim().take(30))
        }
        val metadataObj = JSONObject().apply {
            put("product", "pressing_pro_saas_renewal")
            put("plan", quote.plan.name)
            put("billing_cycle", quote.billingCycle)
            put("months", quote.months)
            put("organization_uid", tenantUid.take(64))
            put("mode", cfg.normalizedMode)
        }
        val payload = JSONObject().apply {
            put("amount", quote.finalAmountFcfa)
            put("currency", "XOF")
            put("description", "Réabonnement Pressing Pro ${quote.plan.labelFr} — ${quote.periodLabelFr} (${companyName.take(60)})")
            put("customer", customerObj)
            put("metadata", metadataObj)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("X-API-Key", cfg.apiKey)
            .addHeader("X-API-Secret", cfg.apiSecret)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val serverMsg = runCatching {
                        JSONObject(rawBody).optString("message").takeIf { it.isNotBlank() }
                    }.getOrNull()
                    Log.w(GENIUSPAY_TAG, "GeniusPay initiate payment HTTP ${response.code}")
                    return@withContext Result.failure(
                        IllegalStateException(
                            serverMsg ?: "GeniusPay a refusé l'initialisation du paiement (HTTP ${response.code})."
                        )
                    )
                }

                val root = JSONObject(rawBody)
                val data = root.optJSONObject("data") ?: root
                val reference = data.optString("reference")
                    .ifBlank { data.optString("id") }
                    .ifBlank { data.optString("transaction_id") }
                    .trim()
                val checkoutUrl = data.optString("checkout_url")
                    .ifBlank { data.optString("payment_url") }
                    .ifBlank { data.optString("url") }
                    .trim()
                val rawStatus = data.optString("status", "pending")

                if (reference.isBlank() || !GeniusPayUrlValidator.isSafeCheckoutUrl(checkoutUrl)) {
                    return@withContext Result.failure(
                        IllegalStateException("Réponse GeniusPay incomplète ou lien de paiement sécurisé introuvable.")
                    )
                }

                Result.success(
                    GeniusPayCheckoutSession(
                        reference = reference,
                        checkoutUrl = checkoutUrl,
                        amountFcfa = quote.finalAmountFcfa,
                        currency = "XOF",
                        plan = quote.plan,
                        billingCycle = quote.billingCycle,
                        mode = cfg.normalizedMode,
                        status = SubscriptionPaymentStatus.fromGeniusPayRaw(rawStatus)
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(GENIUSPAY_TAG, "GeniusPay checkout call failed: ${e.javaClass.simpleName}")
            Result.failure(
                IllegalStateException("Impossible de joindre GeniusPay. Vérifiez votre connexion Internet et vos clés API.")
            )
        }
    }

    suspend fun verifyPaymentStatus(reference: String): Result<SubscriptionPaymentStatus> = withContext(Dispatchers.IO) {
        val cleanRef = reference.trim()
        if (!Regex("^[A-Za-z0-9_\\-]{3,80}$").matches(cleanRef)) {
            return@withContext Result.failure(IllegalArgumentException("Référence de transaction GeniusPay invalide."))
        }
        val cfg = configProvider()
        if (!cfg.isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Clés GeniusPay non configurées dans le panneau Secrets d'AI Studio.")
            )
        }

        val endpoint = "${cfg.sanitizedBaseUrl}/payments/$cleanRef"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("X-API-Key", cfg.apiKey)
            .addHeader("X-API-Secret", cfg.apiSecret)
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(GENIUSPAY_TAG, "GeniusPay verify payment HTTP ${response.code}")
                    return@withContext Result.failure(
                        IllegalStateException("Impossible de vérifier la transaction GeniusPay (HTTP ${response.code}).")
                    )
                }
                val root = JSONObject(rawBody)
                val data = root.optJSONObject("data") ?: root
                val rawStatus = data.optString("status", "pending")
                Result.success(SubscriptionPaymentStatus.fromGeniusPayRaw(rawStatus))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(GENIUSPAY_TAG, "GeniusPay verify call failed: ${e.javaClass.simpleName}")
            Result.failure(
                IllegalStateException("Échec réseau lors de la vérification du paiement GeniusPay.")
            )
        }
    }
}
