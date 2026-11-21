package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.Timestamp
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun formatMoney(amount: Long, currency: String = "FCFA"): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.FRANCE)
    return "${formatter.format(amount)} $currency"
}

fun formatDateTime(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: Date()
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(date)
}

fun formatDateMillis(millis: Long): String {
    if (millis <= 0L) return "—"
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(millis))
}

fun formatShortDate(millis: Long): String {
    if (millis <= 0L) return "—"
    return SimpleDateFormat("dd MMM · HH:mm", Locale.FRANCE).format(Date(millis))
}

fun nowFormatted(): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date())

const val GARMENT_PHOTO_MAX_DIM = 1024
const val GARMENT_PHOTO_JPEG_QUALITY = 80
const val GARMENT_PHOTO_MAX_BASE64_CHARS = 180_000

/**
 * Computes proportional target dimensions bounded by [maxDim] without upscaling.
 */
fun computeTargetPhotoDimensions(
    width: Int,
    height: Int,
    maxDim: Int = GARMENT_PHOTO_MAX_DIM
): Pair<Int, Int> {
    val safeW = width.coerceAtLeast(1)
    val safeH = height.coerceAtLeast(1)
    val safeMax = maxDim.coerceAtLeast(1)
    val ratio = minOf(safeMax.toFloat() / safeW, safeMax.toFloat() / safeH, 1f)
    return if (ratio < 1f) {
        (safeW * ratio).toInt().coerceAtLeast(1) to (safeH * ratio).toInt().coerceAtLeast(1)
    } else {
        safeW to safeH
    }
}

/**
 * Estimates decoded byte payload size in kilobytes from a Base64 string.
 */
fun estimateBase64SizeKb(base64: String): Int {
    if (base64.isBlank()) return 0
    val estimatedBytes = (base64.length * 3) / 4
    return ((estimatedBytes + 1023) / 1024).coerceAtLeast(1)
}

/**
 * Compresses a garment photo to max 1024px and 80% JPEG quality for fast 3G/4G upload.
 * If an unusually dense image exceeds [maxBase64Chars], gracefully steps down quality/scale
 * so the output always remains a valid, non-corrupted JPEG fitting inside Firestore documents.
 */
fun compressBitmapToBase64(
    bitmap: Bitmap,
    maxDim: Int = GARMENT_PHOTO_MAX_DIM,
    quality: Int = GARMENT_PHOTO_JPEG_QUALITY,
    maxBase64Chars: Int = GARMENT_PHOTO_MAX_BASE64_CHARS
): String {
    var currentDim = maxDim.coerceAtLeast(256)
    var currentQuality = quality.coerceIn(40, 95)

    repeat(4) {
        val (targetW, targetH) = computeTargetPhotoDimensions(bitmap.width, bitmap.height, currentDim)
        val scaled = if (targetW != bitmap.width || targetH != bitmap.height) {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else {
            bitmap
        }
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, currentQuality, baos)
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        if (encoded.length <= maxBase64Chars) {
            return encoded
        }
        currentQuality = (currentQuality - 10).coerceAtLeast(50)
        currentDim = (currentDim * 0.78f).toInt().coerceAtLeast(384)
    }

    val (fallbackW, fallbackH) = computeTargetPhotoDimensions(bitmap.width, bitmap.height, 480)
    val fallbackBitmap = if (fallbackW != bitmap.width || fallbackH != bitmap.height) {
        Bitmap.createScaledBitmap(bitmap, fallbackW, fallbackH, true)
    } else {
        bitmap
    }
    val baos = ByteArrayOutputStream()
    fallbackBitmap.compress(Bitmap.CompressFormat.JPEG, 55, baos)
    if (fallbackBitmap !== bitmap) {
        fallbackBitmap.recycle()
    }
    return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
}

fun decodeBase64ToBitmap(base64: String): Bitmap? {
    if (base64.isBlank()) return null
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

/**
 * Memory-safe URI decoder using two-pass subsampling before scaling to [maxDim] (default 1024px).
 */
fun decodeUriToBitmap(context: Context, uri: Uri, maxDim: Int = GARMENT_PHOTO_MAX_DIM): Bitmap? {
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val rawW = boundsOpts.outWidth
        val rawH = boundsOpts.outHeight
        var sampleSize = 1
        if (rawW > 0 && rawH > 0) {
            while ((rawW / sampleSize) > maxDim * 2 || (rawH / sampleSize) > maxDim * 2) {
                sampleSize *= 2
            }
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val decoded = BitmapFactory.decodeStream(stream, null, decodeOpts) ?: return null
            val (targetW, targetH) = computeTargetPhotoDimensions(decoded.width, decoded.height, maxDim)
            if (targetW != decoded.width || targetH != decoded.height) {
                val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                if (scaled !== decoded) decoded.recycle()
                scaled
            } else {
                decoded
            }
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Generates a deterministic 21x21 QR-style matrix with genuine finder patterns for order verification codes.
 */
fun generateQrMatrix(content: String, size: Int = 21): Array<BooleanArray> {
    val grid = Array(size) { BooleanArray(size) }

    fun drawFinder(top: Int, left: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val border = r == 0 || r == 6 || c == 0 || c == 6
                val center = r in 2..4 && c in 2..4
                grid[top + r][left + c] = border || center
            }
        }
    }
    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    var seed = content.hashCode().toLong() * 2654435761L
    for (r in 0 until size) {
        for (c in 0 until size) {
            val inTopLeft = r < 8 && c < 8
            val inTopRight = r < 8 && c >= size - 8
            val inBottomLeft = r >= size - 8 && c < 8
            if (!inTopLeft && !inTopRight && !inBottomLeft) {
                seed = seed * 6364136223846793005L + 1442695040888963407L
                grid[r][c] = (abs(seed shr 32) % 2L) == 0L
            }
        }
    }
    return grid
}

fun shareTextIntent(context: Context, title: String, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

fun openWhatsAppIntent(context: Context, phone: String, message: String) {
    val cleanPhone = phone.filter { it.isDigit() }
    val uri = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(message)}")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
        shareTextIntent(context, "Message client", message)
    }
}

fun openMapIntent(context: Context, lat: Double, lng: Double, label: String) {
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.openstreetmap.org/?mlat=$lat&mlon=$lng#map=16/$lat/$lng")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

fun dialPhoneIntent(context: Context, phone: String) {
    val clean = phone.filter { it.isDigit() || it == '+' }
    if (clean.isBlank()) return
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        shareTextIntent(context, "Téléphone", clean)
    }
}

fun escapeHtml(raw: String): String =
    raw.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

/**
 * Triggers Android's native Print / Save-as-PDF dialog using a lightweight off-screen WebView.
 */
fun printHtmlDocument(context: Context, jobName: String, htmlBody: String) {
    try {
        val webView = android.webkit.WebView(context)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
                if (printManager != null && view != null) {
                    val adapter = view.createPrintDocumentAdapter(jobName)
                    printManager.print(
                        jobName,
                        adapter,
                        android.print.PrintAttributes.Builder().build()
                    )
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlBody, "text/html; charset=utf-8", "UTF-8", null)
    } catch (_: Exception) {
        shareTextIntent(context, jobName, htmlBody)
    }
}
