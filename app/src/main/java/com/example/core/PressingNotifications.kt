package com.example.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.WorkspaceRole
import com.google.firebase.messaging.FirebaseMessaging

private const val NOTIF_TAG = "PressingNotifications"

/**
 * Manages Firebase Cloud Messaging (FCM) topic subscriptions, Android notification channels,
 * and local/remote notification rendering for:
 *  - Cashiers: New order registration & offline sync alerts
 *  - Clients: Garment availability ("Prêt au retrait") alerts
 */
object PressingNotificationManager {

    const val CHANNEL_CASHIER_ORDERS = "pressing_cashier_orders"
    const val CHANNEL_CLIENT_READY = "pressing_client_ready"

    const val TYPE_NEW_ORDER_CASHIER = "NEW_ORDER_CASHIER"
    const val TYPE_ORDER_READY_CLIENT = "ORDER_READY_CLIENT"
    const val TYPE_OFFLINE_SYNCED = "OFFLINE_SYNCED"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val cashierChannel = NotificationChannel(
            CHANNEL_CASHIER_ORDERS,
            "Alertes Caissiers & Nouvelles Commandes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifie les caissiers et gérants à chaque nouvelle commande ou synchronisation hors-ligne."
        }

        val clientChannel = NotificationChannel(
            CHANNEL_CLIENT_READY,
            "Disponibilité Vêtements (Clients)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerte lorsque les vêtements d'une commande sont contrôlés et prêts pour le retrait."
        }

        manager.createNotificationChannel(cashierChannel)
        manager.createNotificationChannel(clientChannel)
    }

    fun sanitizeTopicSlug(raw: String): String {
        val cleaned = raw.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(40)
        return cleaned.ifBlank { "default" }
    }

    fun buildRoleTopics(role: WorkspaceRole, agencyName: String?): List<String> {
        val topics = mutableListOf("pressing_all")
        when (role) {
            WorkspaceRole.CAISSIER -> topics.add("pressing_cashiers")
            WorkspaceRole.GERANT_AGENCE -> {
                topics.add("pressing_cashiers")
                topics.add("pressing_managers")
            }
            WorkspaceRole.DIRECTEUR -> {
                topics.add("pressing_cashiers")
                topics.add("pressing_directors")
            }
            WorkspaceRole.OPERATEUR -> topics.add("pressing_workshop")
            WorkspaceRole.CONTROLE_QUALITE -> topics.add("pressing_quality")
            WorkspaceRole.LIVREUR -> topics.add("pressing_delivery")
        }
        if (!agencyName.isNullOrBlank()) {
            topics.add("agency_${sanitizeTopicSlug(agencyName)}")
        }
        return topics.distinct()
    }

    /**
     * Fetches the device's FCM registration token and subscribes to the appropriate role/agency topics.
     */
    fun registerFcmTokenAndTopics(
        context: Context,
        role: WorkspaceRole,
        agencyName: String?,
        onTokenReady: (token: String, topics: List<String>) -> Unit
    ) {
        ensureChannels(context)
        val topics = buildRoleTopics(role, agencyName)
        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            topics.forEach { topic ->
                messaging.subscribeToTopic(topic)
                    .addOnFailureListener { e -> Log.w(NOTIF_TAG, "Topic subscribe skipped ($topic): ${e.message}") }
            }
            messaging.token
                .addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        onTokenReady(token, topics)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(NOTIF_TAG, "FCM token retrieval unavailable: ${e.message}")
                }
        }.onFailure { e ->
            Log.w(NOTIF_TAG, "FCM initialization skipped: ${e.message}")
        }
    }

    fun showCashierNewOrderNotification(
        context: Context,
        orderNumber: String,
        clientName: String,
        agencyName: String,
        itemCount: Long,
        totalLabel: String
    ) {
        showNotification(
            context = context,
            channelId = CHANNEL_CASHIER_ORDERS,
            title = "Nouvelle commande · $orderNumber",
            body = "$clientName ($itemCount pièce(s)) · $totalLabel · $agencyName",
            notificationId = ("cashier_$orderNumber").hashCode()
        )
    }

    fun showClientGarmentReadyNotification(
        context: Context,
        orderNumber: String,
        clientName: String,
        agencyName: String
    ) {
        showNotification(
            context = context,
            channelId = CHANNEL_CLIENT_READY,
            title = "Vêtements prêts · $orderNumber",
            body = "Bonjour $clientName, vos vêtements sont prêts au retrait à $agencyName.",
            notificationId = ("ready_$orderNumber").hashCode()
        )
    }

    fun showOfflineSyncCompletedNotification(
        context: Context,
        syncedCount: Int
    ) {
        if (syncedCount <= 0) return
        showNotification(
            context = context,
            channelId = CHANNEL_CASHIER_ORDERS,
            title = "Synchronisation hors-ligne réussie",
            body = "$syncedCount commande(s) enregistrée(s) hors-ligne ont été synchronisée(s) vers le cloud.",
            notificationId = 9042
        )
    }

    fun showNotificationFromFcmPayload(
        context: Context,
        title: String,
        body: String,
        type: String,
        orderNumber: String
    ) {
        val channelId = if (type == TYPE_ORDER_READY_CLIENT) {
            CHANNEL_CLIENT_READY
        } else {
            CHANNEL_CASHIER_ORDERS
        }
        showNotification(
            context = context,
            channelId = channelId,
            title = title.ifBlank { "Pressing Pro" },
            body = body.ifBlank { "Mise à jour de commande $orderNumber" },
            notificationId = ("fcm_${type}_${orderNumber}_${System.currentTimeMillis() % 1000}").hashCode()
        )
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        body: String,
        notificationId: Int
    ) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }.onFailure { e ->
            Log.w(NOTIF_TAG, "Unable to post notification: ${e.message}")
        }
    }
}
