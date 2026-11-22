package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.nowFormatted
import com.example.data.GarmentWorkshopStatus
import com.example.data.OrderGarmentItem
import com.example.data.OrderPriority
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.PaymentStatus
import com.example.data.PressingOrder
import com.example.data.PressingRepository
import com.example.data.TrackingTokenGenerator
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID

/**
 * Local Room entity storing cashier orders recorded during temporary internet outages
 * so reception never stops and orders automatically synchronize to Firestore once back online.
 */
@Entity(tableName = "offline_orders")
data class OfflineOrderEntity(
    @PrimaryKey val localId: String = "offline_${UUID.randomUUID().toString().take(12)}",
    val userId: String,
    val orderNumber: String,
    val trackingToken: String = TrackingTokenGenerator.generate(),
    val agencyName: String,
    val clientId: String,
    val clientName: String,
    val clientPhone: String,
    val clientAddress: String,
    val priority: String,
    val itemsJson: String,
    val discountAmount: Long,
    val expressFee: Long,
    val deliveryFee: Long,
    val taxAmount: Long,
    val paidAmount: Long,
    val paymentMethod: String,
    val deliveryRequested: Boolean,
    val deliveryZone: String,
    val assignedEmployee: String,
    val clientSignatureName: String,
    val observations: String,
    val dueDateMillis: Long,
    val syncStatus: String = SYNC_STATUS_PENDING,
    val retryCount: Int = 0,
    val lastError: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun decodeItems(): List<OrderGarmentItem> = OfflineGarmentJsonCodec.decode(itemsJson)

    /** Converts this locally queued order into a UI-ready [PressingOrder] for seamless cashier display. */
    fun toPressingOrder(): PressingOrder {
        val itemsList = decodeItems()
        val itemCount = itemsList.sumOf { it.quantity.coerceAtLeast(1L) }.coerceAtMost(1000L)
        val subtotal = itemsList.sumOf { it.quantity.coerceAtLeast(1L) * it.unitPrice.coerceAtLeast(0L) }
        val safeDiscount = discountAmount.coerceIn(0L, subtotal)
        val total = (subtotal - safeDiscount + expressFee.coerceAtLeast(0L) + deliveryFee.coerceAtLeast(0L) + taxAmount.coerceAtLeast(0L))
            .coerceIn(0L, 100_000_000L)
        val safePaid = paidAmount.coerceIn(0L, total)
        val remaining = (total - safePaid).coerceAtLeast(0L)
        val payStatus = when {
            safePaid <= 0L -> PaymentStatus.UNPAID
            remaining == 0L -> PaymentStatus.PAID
            else -> PaymentStatus.PARTIAL
        }
        val syncNote = if (syncStatus == SYNC_STATUS_FAILED) {
            "Cache local (Room) · Nouvelle tentative au retour réseau"
        } else {
            "Cache local (Room) · En attente de synchronisation cloud"
        }
        return PressingOrder(
            id = localId,
            userId = userId,
            orderNumber = orderNumber,
            trackingToken = trackingToken,
            agencyName = agencyName,
            clientId = clientId,
            clientName = clientName,
            clientPhone = clientPhone,
            clientAddress = clientAddress,
            status = OrderStatus.PENDING.name,
            priority = priority,
            items = itemsList.map { it.toMap() },
            itemCount = itemCount,
            subtotal = subtotal,
            discountAmount = safeDiscount,
            expressFee = expressFee,
            deliveryFee = deliveryFee,
            taxAmount = taxAmount,
            totalAmount = total,
            paidAmount = safePaid,
            remainingAmount = remaining,
            paymentStatus = payStatus.name,
            paymentMethod = paymentMethod,
            deliveryRequested = deliveryRequested,
            deliveryDriver = "",
            deliveryZone = deliveryZone,
            assignedEmployee = assignedEmployee,
            qualityChecked = false,
            clientSignatureName = clientSignatureName,
            observations = if (observations.isBlank()) syncNote else "$observations · $syncNote",
            dueDateMillis = dueDateMillis,
            statusHistory = listOf("${nowFormatted()} · $syncNote"),
            createdAt = Timestamp(Date(createdAtMillis)),
            updatedAt = Timestamp(Date(createdAtMillis))
        )
    }

    companion object {
        const val SYNC_STATUS_PENDING = "PENDING_SYNC"
        const val SYNC_STATUS_SYNCING = "SYNCING"
        const val SYNC_STATUS_FAILED = "FAILED"
    }
}

/** Pure-Kotlin codec for storing [OrderGarmentItem] lists inside Room without Android JSON stubs. */
object OfflineGarmentJsonCodec {
    private const val RECORD_SEP = "\u001E"
    private const val FIELD_SEP = "\u001F"

    private fun escape(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace(RECORD_SEP, "\\u001E")
            .replace(FIELD_SEP, "\\u001F")

    private fun unescape(raw: String): String =
        raw.replace("\\u001F", FIELD_SEP)
            .replace("\\u001E", RECORD_SEP)
            .replace("\\\\", "\\")

    fun encode(items: List<OrderGarmentItem>): String {
        return items.take(50).joinToString(RECORD_SEP) { item ->
            listOf(
                escape(item.garmentName),
                escape(item.serviceName),
                escape(item.fabric),
                escape(item.color),
                escape(item.soilLevel),
                escape(item.defects),
                escape(item.notes),
                item.quantity.coerceAtLeast(1L).toString(),
                item.unitPrice.coerceAtLeast(0L).toString(),
                item.lineTotal.coerceAtLeast(0L).toString(),
                escape(item.barcode),
                escape(item.photoBase64.take(220_000)),
                escape(item.aiDiagnosis),
                escape(item.workshopStatus)
            ).joinToString(FIELD_SEP)
        }
    }

    fun decode(rawJson: String): List<OrderGarmentItem> {
        if (rawJson.isBlank()) return emptyList()
        return runCatching {
            rawJson.split(RECORD_SEP).mapNotNull { record ->
                if (record.isBlank()) return@mapNotNull null
                val parts = record.split(FIELD_SEP)
                if (parts.size < 14) return@mapNotNull null
                val qty = parts[7].toLongOrNull()?.coerceAtLeast(1L) ?: 1L
                val price = parts[8].toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                val lineTotal = parts[9].toLongOrNull() ?: (qty * price)
                OrderGarmentItem(
                    garmentName = unescape(parts[0]),
                    serviceName = unescape(parts[1]).ifBlank { "Lavage + Repassage" },
                    fabric = unescape(parts[2]).ifBlank { "Coton" },
                    color = unescape(parts[3]),
                    soilLevel = unescape(parts[4]).ifBlank { "Normal" },
                    defects = unescape(parts[5]),
                    notes = unescape(parts[6]),
                    quantity = qty,
                    unitPrice = price,
                    lineTotal = lineTotal,
                    barcode = unescape(parts[10]),
                    photoBase64 = unescape(parts[11]),
                    aiDiagnosis = unescape(parts[12]),
                    workshopStatus = unescape(parts[13]).ifBlank { GarmentWorkshopStatus.RECEIVED.name }
                )
            }
        }.getOrDefault(emptyList())
    }
}

@Dao
interface OfflineOrderDao {
    @Query("SELECT * FROM offline_orders WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observePendingForUser(userId: String): Flow<List<OfflineOrderEntity>>

    @Query("SELECT * FROM offline_orders WHERE userId = :userId ORDER BY createdAtMillis ASC")
    suspend fun getPendingForUser(userId: String): List<OfflineOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OfflineOrderEntity)

    @Query("UPDATE offline_orders SET syncStatus = :status WHERE localId = :localId")
    suspend fun updateSyncStatus(localId: String, status: String)

    @Query("UPDATE offline_orders SET syncStatus = :status, retryCount = retryCount + 1, lastError = :error WHERE localId = :localId")
    suspend fun markFailed(localId: String, status: String, error: String)

    @Query("DELETE FROM offline_orders WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: String)
}

@Database(
    entities = [OfflineOrderEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PressingLocalDatabase : RoomDatabase() {
    abstract fun offlineOrderDao(): OfflineOrderDao

    companion object {
        @Volatile
        private var INSTANCE: PressingLocalDatabase? = null

        fun getInstance(context: Context): PressingLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PressingLocalDatabase::class.java,
                    "pressing_pro_offline_cache.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}

data class OfflineSyncBatchResult(
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val syncedOrderNumbers: List<String> = emptyList()
)

/**
 * Coordinates the Room offline outbox queue with Firestore so cashiers can register orders
 * during internet outages and have them pushed automatically once connectivity returns.
 */
class OfflineSyncRepository(
    private val dao: OfflineOrderDao,
    private val remoteRepository: PressingRepository
) {
    fun observePendingOrders(userId: String): Flow<List<OfflineOrderEntity>> =
        dao.observePendingForUser(userId)

    suspend fun enqueueOrder(
        userId: String,
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
        dueDateMillis: Long,
        trackingToken: String = TrackingTokenGenerator.generate()
    ): OfflineOrderEntity {
        val safeToken = trackingToken.trim().uppercase().takeIf { TrackingTokenGenerator.isValidFormat(it) }
            ?: TrackingTokenGenerator.generate()
        val entity = OfflineOrderEntity(
            userId = userId,
            orderNumber = orderNumber.trim().take(40),
            trackingToken = safeToken,
            agencyName = agencyName.trim().take(100),
            clientId = clientId.trim().take(128),
            clientName = clientName.trim().take(120),
            clientPhone = clientPhone.trim().take(30),
            clientAddress = clientAddress.trim().take(250),
            priority = priority.name,
            itemsJson = OfflineGarmentJsonCodec.encode(items),
            discountAmount = discountAmount,
            expressFee = expressFee,
            deliveryFee = deliveryFee,
            taxAmount = taxAmount,
            paidAmount = paidAmount,
            paymentMethod = paymentMethod.name,
            deliveryRequested = deliveryRequested,
            deliveryZone = deliveryZone.trim().take(100),
            assignedEmployee = assignedEmployee.trim().take(100),
            clientSignatureName = clientSignatureName.trim().take(120),
            observations = observations.trim().take(500),
            dueDateMillis = dueDateMillis
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun syncPendingOrders(userId: String): OfflineSyncBatchResult {
        val pending = dao.getPendingForUser(userId)
        if (pending.isEmpty()) return OfflineSyncBatchResult()

        var synced = 0
        var failed = 0
        val syncedNumbers = mutableListOf<String>()

        for (entry in pending) {
            dao.updateSyncStatus(entry.localId, OfflineOrderEntity.SYNC_STATUS_SYNCING)
            val result = remoteRepository.createOrder(
                userId = entry.userId,
                orderNumber = entry.orderNumber,
                agencyName = entry.agencyName,
                clientId = entry.clientId,
                clientName = entry.clientName,
                clientPhone = entry.clientPhone,
                clientAddress = entry.clientAddress,
                priority = OrderPriority.fromRaw(entry.priority),
                items = entry.decodeItems(),
                discountAmount = entry.discountAmount,
                expressFee = entry.expressFee,
                deliveryFee = entry.deliveryFee,
                taxAmount = entry.taxAmount,
                paidAmount = entry.paidAmount,
                paymentMethod = PaymentMethod.fromRaw(entry.paymentMethod),
                deliveryRequested = entry.deliveryRequested,
                deliveryZone = entry.deliveryZone,
                assignedEmployee = entry.assignedEmployee,
                clientSignatureName = entry.clientSignatureName,
                observations = entry.observations,
                dueDateMillis = entry.dueDateMillis,
                trackingToken = entry.trackingToken
            )
            if (result.isSuccess) {
                dao.deleteByLocalId(entry.localId)
                synced++
                syncedNumbers.add(entry.orderNumber)
            } else {
                val errMsg = result.exceptionOrNull()?.localizedMessage?.take(140) ?: "Réseau indisponible"
                dao.markFailed(entry.localId, OfflineOrderEntity.SYNC_STATUS_FAILED, errMsg)
                failed++
            }
        }
        return OfflineSyncBatchResult(
            syncedCount = synced,
            failedCount = failed,
            syncedOrderNumbers = syncedNumbers
        )
    }
}
