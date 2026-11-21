package com.example.core

import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private const val FCM_SERVICE_TAG = "PressingFcmService"

/**
 * Firebase Cloud Messaging service that receives push alerts for:
 * 1. Cashiers / Agency Managers when a new order is placed or synced (`NEW_ORDER_CASHIER`)
 * 2. Clients when garments pass quality control and are ready for pickup (`ORDER_READY_CLIENT`)
 */
class PressingFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(FCM_SERVICE_TAG, "Refreshed FCM token received")
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: return
        if (token.isBlank()) return

        runCatching {
            val dbId = applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            val tokenDocId = "device_${token.hashCode().toUInt()}"
            val payload = mapOf(
                "userId" to uid,
                "token" to token.take(300),
                "role" to "STAFF",
                "agencyName" to "",
                "topics" to listOf("pressing_all"),
                "platform" to "ANDROID",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("fcm_tokens").document(tokenDocId)
                .set(payload)
        }.onFailure { e ->
            Log.w(FCM_SERVICE_TAG, "Deferred FCM token persistence: ${e.message}")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val type = data["type"] ?: PressingNotificationManager.TYPE_NEW_ORDER_CASHIER
        val orderNumber = data["orderNumber"].orEmpty()
        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: when (type) {
                PressingNotificationManager.TYPE_ORDER_READY_CLIENT -> "Vos vêtements sont prêts !"
                else -> "Nouvelle commande Pressing Pro"
            }
        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: if (orderNumber.isNotBlank()) "Commande $orderNumber mise à jour." else "Nouvelle notification comptoir."

        PressingNotificationManager.showNotificationFromFcmPayload(
            context = applicationContext,
            title = title,
            body = body,
            type = type,
            orderNumber = orderNumber
        )
    }
}
