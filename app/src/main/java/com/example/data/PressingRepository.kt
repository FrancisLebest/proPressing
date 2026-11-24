package com.example.data

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.core.nowFormatted
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PressingRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Authentification requise pour accéder à votre espace.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Session utilisateur modifiée ; opération annulée." }
        }
        return uid
    }

    /**
     * Immediate one-shot lookup of the authenticated user's isolated entity (company config)
     * and agency staff roster so role and agency permissions resolve right after sign-in.
     */
    suspend fun fetchUserRoleContext(userId: String): Result<Pair<CompanyConfig?, List<EmployeeRecord>>> {
        val path = "users/$userId"
        return try {
            requireUserId(userId)
            val companySnap = userDoc(userId).get().await()
            val company = if (companySnap.exists()) {
                companySnap.toObject(CompanyConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            val staffSnap = userDoc(userId).collection("employees").get().await()
            val employees = staffSnap
                .toObjects(EmployeeRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.fullName.lowercase() }
            Result.success(company to employees)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(IllegalStateException("Impossible de récupérer votre profil professionnel pour le moment."))
        }
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun <T> Flow<T>.logAndRethrow(uid: String, op: OperationType, path: String): Flow<T> =
        catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val toLog = fsError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- REAL-TIME STREAMS ---

    fun observeCompanyConfig(userId: String): Flow<CompanyConfig?> = flow {
        requireUserId(userId)
        val path = "users/$userId"
        emitAll(
            userDoc(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(CompanyConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logAndRethrow(userId, OperationType.GET, path)
        )
    }

    fun observeServices(userId: String): Flow<List<ServiceItem>> = flow {
        requireUserId(userId)
        val path = "users/$userId/services"
        emitAll(
            userDoc(userId).collection("services")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ServiceItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeClients(userId: String): Flow<List<ClientRecord>> = flow {
        requireUserId(userId)
        val path = "users/$userId/clients"
        emitAll(
            userDoc(userId).collection("clients")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ClientRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.fullName.lowercase() }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeOrders(userId: String): Flow<List<PressingOrder>> = flow {
        requireUserId(userId)
        val path = "users/$userId/orders"
        emitAll(
            userDoc(userId).collection("orders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PressingOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeTransactions(userId: String): Flow<List<AccountingEntry>> = flow {
        requireUserId(userId)
        val path = "users/$userId/transactions"
        emitAll(
            userDoc(userId).collection("transactions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AccountingEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeStocks(userId: String): Flow<List<StockItem>> = flow {
        requireUserId(userId)
        val path = "users/$userId/stocks"
        emitAll(
            userDoc(userId).collection("stocks")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StockItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeEmployees(userId: String): Flow<List<EmployeeRecord>> = flow {
        requireUserId(userId)
        val path = "users/$userId/employees"
        emitAll(
            userDoc(userId).collection("employees")
                .snapshots()
                .map { snap ->
                    snap.toObjects(EmployeeRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.fullName.lowercase() }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeClaims(userId: String): Flow<List<ClaimRecord>> = flow {
        requireUserId(userId)
        val path = "users/$userId/claims"
        emitAll(
            userDoc(userId).collection("claims")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ClaimRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    fun observeAuditLogs(userId: String): Flow<List<AuditLogRecord>> = flow {
        requireUserId(userId)
        val path = "users/$userId/audit_logs"
        emitAll(
            userDoc(userId).collection("audit_logs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AuditLogRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                        .take(50)
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    // --- MUTATIONS ---

    suspend fun saveCompanyConfig(userId: String, config: CompanyConfig): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId(userId)
            val docRef = userDoc(userId)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(config.toUpdateMap()).await()
            } else {
                docRef.set(config.toCreateMap(userId)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Idempotently provisions the initial Super-Admin Firestore company document (`users/{uid}`)
     * if missing, without exposing or handling any plaintext password.
     * Returns `true` if newly created, `false` if already present.
     */
    suspend fun ensureSuperAdminCompanyProfile(userId: String, email: String): Result<Boolean> {
        val path = "users/$userId"
        return try {
            requireUserId(userId)
            if (!PlatformSuperAdminConfig.isPlatformSuperAdminEmail(email)) {
                return Result.failure(SecurityException("Adresse e-mail non autorisée pour le profil Super-Admin."))
            }
            val docRef = userDoc(userId)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                return Result.success(false)
            }
            val initialConfig = CompanyConfig(
                userId = userId,
                companyName = "Gestion Pro Africa — Siège",
                legalName = "SAS",
                description = "Compte Super-Admin Plateforme Pressing Pro",
                email = email.trim().lowercase(),
                subscriptionPlan = SubscriptionPlan.ENTERPRISE.name,
                subscriptionBilling = "ANNUAL",
                subscriptionStatus = "ACTIVE",
                setupStep = 10L,
                setupCompleted = true
            )
            docRef.set(initialConfig.toCreateMap(userId)).await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun seedStarterServicesIfEmpty(userId: String, starterList: List<ServiceItem>): Result<Unit> {
        val path = "users/$userId/services"
        return try {
            requireUserId(userId)
            val col = userDoc(userId).collection("services")
            val existing = col.limit(1).get().await()
            if (existing.isEmpty) {
                val batch = db.batch()
                starterList.forEach { item ->
                    val doc = col.document()
                    batch.set(doc, item.toCreateMap(userId))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveService(userId: String, item: ServiceItem): Result<Unit> {
        val col = userDoc(userId).collection("services")
        val isEdit = item.id.isNotBlank()
        val path = if (isEdit) "users/$userId/services/${item.id}" else "users/$userId/services"
        return try {
            requireUserId(userId)
            require(item.name.isNotBlank()) { "Le nom du service est obligatoire." }
            if (isEdit) {
                col.document(item.id).update(item.toUpdateMap()).await()
            } else {
                col.document().set(item.toCreateMap(userId)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isEdit) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteService(userId: String, serviceId: String): Result<Unit> {
        val path = "users/$userId/services/$serviceId"
        return try {
            requireUserId(userId)
            require(serviceId.isNotBlank())
            userDoc(userId).collection("services").document(serviceId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveClient(userId: String, client: ClientRecord): Result<String> {
        val col = userDoc(userId).collection("clients")
        val isEdit = client.id.isNotBlank()
        val path = if (isEdit) "users/$userId/clients/${client.id}" else "users/$userId/clients"
        return try {
            requireUserId(userId)
            require(client.fullName.isNotBlank()) { "Le nom du client est obligatoire." }
            require(client.phone.isNotBlank()) { "Le téléphone du client est obligatoire." }
            if (isEdit) {
                col.document(client.id).update(client.toUpdateMap()).await()
                Result.success(client.id)
            } else {
                val doc = col.document()
                doc.set(client.toCreateMap(userId)).await()
                Result.success(doc.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isEdit) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteClient(userId: String, clientId: String): Result<Unit> {
        val path = "users/$userId/clients/$clientId"
        return try {
            requireUserId(userId)
            require(clientId.isNotBlank())
            userDoc(userId).collection("clients").document(clientId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createOrder(
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
    ): Result<String> {
        val col = userDoc(userId).collection("orders")
        val docRef = col.document()
        val path = docRef.path
        return try {
            requireUserId(userId)
            require(clientName.isNotBlank()) { "Veuillez sélectionner ou saisir un client." }
            require(items.isNotEmpty()) { "Ajoutez au moins un vêtement à la commande." }

            val safeToken = trackingToken.trim().uppercase().takeIf { TrackingTokenGenerator.isValidFormat(it) }
                ?: TrackingTokenGenerator.generate()
            val itemMaps = items.take(50).map { it.toMap() }
            val itemCount = items.sumOf { it.quantity.coerceAtLeast(1L) }.coerceAtMost(1000L)
            val subtotal = items.sumOf { it.quantity.coerceAtLeast(1L) * it.unitPrice.coerceAtLeast(0L) }
            val safeDiscount = discountAmount.coerceIn(0L, subtotal)
            val totalAmount = (subtotal - safeDiscount + expressFee.coerceAtLeast(0L) + deliveryFee.coerceAtLeast(0L) + taxAmount.coerceAtLeast(0L))
                .coerceIn(0L, 100_000_000L)
            val safePaid = paidAmount.coerceIn(0L, totalAmount)
            val remaining = (totalAmount - safePaid).coerceAtLeast(0L)
            val payStatus = when {
                safePaid <= 0L -> PaymentStatus.UNPAID
                remaining == 0L -> PaymentStatus.PAID
                else -> PaymentStatus.PARTIAL
            }

            val payload = mapOf(
                "userId" to userId,
                "orderNumber" to orderNumber.trim().take(40),
                "trackingToken" to safeToken,
                "agencyName" to agencyName.trim().take(100),
                "clientId" to clientId.trim().take(128),
                "clientName" to clientName.trim().take(120),
                "clientPhone" to clientPhone.trim().take(30),
                "clientAddress" to clientAddress.trim().take(250),
                "status" to OrderStatus.PENDING.name,
                "priority" to priority.name,
                "items" to itemMaps,
                "itemCount" to itemCount,
                "subtotal" to subtotal.coerceIn(0L, 100_000_000L),
                "discountAmount" to safeDiscount,
                "expressFee" to expressFee.coerceIn(0L, 100_000_000L),
                "deliveryFee" to deliveryFee.coerceIn(0L, 100_000_000L),
                "taxAmount" to taxAmount.coerceIn(0L, 100_000_000L),
                "totalAmount" to totalAmount,
                "paidAmount" to safePaid,
                "remainingAmount" to remaining,
                "paymentStatus" to payStatus.name,
                "paymentMethod" to paymentMethod.name,
                "deliveryRequested" to deliveryRequested,
                "deliveryDriver" to "",
                "deliveryZone" to deliveryZone.trim().take(100),
                "assignedEmployee" to assignedEmployee.trim().take(100),
                "qualityChecked" to false,
                "clientSignatureName" to clientSignatureName.trim().take(120),
                "observations" to observations.trim().take(500),
                "dueDateMillis" to dueDateMillis.coerceAtLeast(0L),
                "statusHistory" to listOf("${nowFormatted()} · Commande enregistrée (${payStatus.labelFr})"),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            // Automatic SYSCOHADA accounting entry when cash/Mobile Money is received
            if (safePaid > 0L) {
                val txEntry = AccountingEntry(
                    reference = "ENC-${orderNumber.takeLast(8)}",
                    entryType = "REVENUE",
                    category = "Prestation Pressing",
                    syscohadaAccount = "706",
                    label = "Encaissement $orderNumber (${payStatus.labelFr}) - $clientName",
                    amount = safePaid,
                    paymentMethod = paymentMethod.name,
                    orderId = docRef.id,
                    counterparty = clientName,
                    agencyName = agencyName
                )
                userDoc(userId).collection("transactions").document().set(txEntry.toCreateMap(userId)).await()
            }

            // Update client loyalty points & balance due if client exists
            if (clientId.isNotBlank()) {
                val clientRef = userDoc(userId).collection("clients").document(clientId)
                val clientSnap = clientRef.get().await()
                if (clientSnap.exists()) {
                    val currentPoints = clientSnap.getLong("loyaltyPoints") ?: 0L
                    val currentBalance = clientSnap.getLong("balanceDue") ?: 0L
                    val addedPoints = (totalAmount / 500L).coerceAtLeast(1L)
                    clientRef.update(
                        mapOf(
                            "loyaltyPoints" to (currentPoints + addedPoints).coerceAtMost(10_000_000L),
                            "balanceDue" to (currentBalance + remaining).coerceIn(-100_000_000L, 100_000_000L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(
        userId: String,
        order: PressingOrder,
        newStatus: OrderStatus,
        assignedEmployee: String = order.assignedEmployee,
        qualityChecked: Boolean = order.qualityChecked,
        note: String = ""
    ): Result<Unit> {
        val path = "users/$userId/orders/${order.id}"
        return try {
            requireUserId(userId)
            require(order.id.isNotBlank())
            val entryText = buildString {
                append("${nowFormatted()} · ${newStatus.labelFr}")
                if (assignedEmployee.isNotBlank()) append(" ($assignedEmployee)")
                if (note.isNotBlank()) append(" — $note")
            }.take(160)
            val newHistory = (order.statusHistory + entryText).takeLast(30)
            val newObs = if (note.isNotBlank()) note.take(500) else order.observations
            userDoc(userId).collection("orders").document(order.id).update(
                mapOf(
                    "status" to newStatus.name,
                    "assignedEmployee" to assignedEmployee.trim().take(100),
                    "qualityChecked" to (qualityChecked || newStatus == OrderStatus.READY),
                    "statusHistory" to newHistory,
                    "observations" to newObs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateGarmentWorkshopStatus(
        userId: String,
        order: PressingOrder,
        garmentIndex: Int,
        newStatus: GarmentWorkshopStatus,
        operatorName: String,
        allowReadyOrderSync: Boolean
    ): Result<Unit> {
        val path = "users/$userId/orders/${order.id}"
        return try {
            requireUserId(userId)
            require(order.id.isNotBlank())
            val parsed = order.parsedItems().toMutableList()
            require(garmentIndex in parsed.indices) { "Article introuvable dans cette commande." }

            val target = parsed[garmentIndex]
            parsed[garmentIndex] = target.copy(workshopStatus = newStatus.name)
            val updatedItemsMaps = parsed.map { it.toMap() }

            // Compute synchronized order status from aggregate garment stages
            val minStep = parsed.minOf { it.workshopStatusEnum().stepIndex }
            val maxStep = parsed.maxOf { it.workshopStatusEnum().stepIndex }
            val currentOrderStatus = order.orderStatusEnum()
            val derivedOrderStatus = when {
                minStep >= GarmentWorkshopStatus.READY.stepIndex && allowReadyOrderSync -> OrderStatus.READY
                minStep >= GarmentWorkshopStatus.QUALITY_CHECK.stepIndex -> OrderStatus.QUALITY_CHECK
                minStep >= GarmentWorkshopStatus.IRONING.stepIndex -> OrderStatus.IRONING
                minStep >= GarmentWorkshopStatus.DRYING.stepIndex -> OrderStatus.DRYING
                maxStep >= GarmentWorkshopStatus.WASHING.stepIndex && currentOrderStatus.stepIndex < OrderStatus.WASHING.stepIndex -> OrderStatus.WASHING
                else -> currentOrderStatus
            }

            val logEntry = "${nowFormatted()} · ${target.garmentName} → ${newStatus.labelFr}" +
                if (operatorName.isNotBlank()) " ($operatorName)" else ""
            val newHistory = (order.statusHistory + logEntry.take(160)).takeLast(30)

            userDoc(userId).collection("orders").document(order.id).update(
                mapOf(
                    "items" to updatedItemsMaps,
                    "status" to derivedOrderStatus.name,
                    "assignedEmployee" to operatorName.ifBlank { order.assignedEmployee }.trim().take(100),
                    "qualityChecked" to (order.qualityChecked || derivedOrderStatus == OrderStatus.READY),
                    "statusHistory" to newHistory,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordOrderPayment(
        userId: String,
        order: PressingOrder,
        amountReceived: Long,
        paymentMethod: PaymentMethod
    ): Result<Unit> {
        val path = "users/$userId/orders/${order.id}"
        return try {
            requireUserId(userId)
            require(order.id.isNotBlank())
            require(amountReceived > 0L) { "Le montant encaissé doit être supérieur à 0." }
            val newPaid = (order.paidAmount + amountReceived).coerceAtMost(order.totalAmount)
            val actualAdded = (newPaid - order.paidAmount).coerceAtLeast(0L)
            val newRemaining = (order.totalAmount - newPaid).coerceAtLeast(0L)
            val newPayStatus = if (newRemaining == 0L) PaymentStatus.PAID else PaymentStatus.PARTIAL
            val newHistory = (order.statusHistory + "${nowFormatted()} · Paiement +$actualAdded (${paymentMethod.labelFr})").takeLast(30)

            userDoc(userId).collection("orders").document(order.id).update(
                mapOf(
                    "paidAmount" to newPaid,
                    "remainingAmount" to newRemaining,
                    "paymentStatus" to newPayStatus.name,
                    "paymentMethod" to paymentMethod.name,
                    "statusHistory" to newHistory,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (actualAdded > 0L) {
                val txEntry = AccountingEntry(
                    reference = "SOL-${order.orderNumber.takeLast(8)}",
                    entryType = "REVENUE",
                    category = "Règlement créance client",
                    syscohadaAccount = "411",
                    label = "Règlement ${order.orderNumber} (${paymentMethod.labelFr}) - ${order.clientName}",
                    amount = actualAdded,
                    paymentMethod = paymentMethod.name,
                    orderId = order.id,
                    counterparty = order.clientName,
                    agencyName = order.agencyName
                )
                userDoc(userId).collection("transactions").document().set(txEntry.toCreateMap(userId)).await()

                if (order.clientId.isNotBlank()) {
                    val clientRef = userDoc(userId).collection("clients").document(order.clientId)
                    val clientSnap = clientRef.get().await()
                    if (clientSnap.exists()) {
                        val currentBalance = clientSnap.getLong("balanceDue") ?: 0L
                        clientRef.update(
                            mapOf(
                                "balanceDue" to (currentBalance - actualAdded).coerceAtLeast(0L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun assignDelivery(
        userId: String,
        order: PressingOrder,
        driverName: String,
        zone: String,
        markDelivered: Boolean = false
    ): Result<Unit> {
        val path = "users/$userId/orders/${order.id}"
        return try {
            requireUserId(userId)
            require(order.id.isNotBlank())
            val nextStatus = if (markDelivered) OrderStatus.DELIVERED else order.orderStatusEnum()
            val logEntry = if (markDelivered) {
                "${nowFormatted()} · Livrée par $driverName ($zone)"
            } else {
                "${nowFormatted()} · Livreur affecté : $driverName ($zone)"
            }
            val newHistory = (order.statusHistory + logEntry).takeLast(30)
            userDoc(userId).collection("orders").document(order.id).update(
                mapOf(
                    "deliveryRequested" to true,
                    "deliveryDriver" to driverName.trim().take(100),
                    "deliveryZone" to zone.trim().take(100),
                    "status" to nextStatus.name,
                    "statusHistory" to newHistory,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addAccountingEntry(userId: String, entry: AccountingEntry): Result<Unit> {
        val path = "users/$userId/transactions"
        return try {
            requireUserId(userId)
            require(entry.label.isNotBlank()) { "Le libellé de l'écriture est obligatoire." }
            require(entry.amount >= 0L)
            userDoc(userId).collection("transactions").document().set(entry.toCreateMap(userId)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveStockItem(userId: String, item: StockItem): Result<Unit> {
        val col = userDoc(userId).collection("stocks")
        val isEdit = item.id.isNotBlank()
        val path = if (isEdit) "users/$userId/stocks/${item.id}" else "users/$userId/stocks"
        return try {
            requireUserId(userId)
            require(item.name.isNotBlank()) { "Le nom de l'article/machine est obligatoire." }
            if (isEdit) {
                col.document(item.id).update(item.toUpdateMap()).await()
            } else {
                col.document().set(item.toCreateMap(userId)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isEdit) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStockItem(userId: String, stockId: String): Result<Unit> {
        val path = "users/$userId/stocks/$stockId"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("stocks").document(stockId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveEmployee(userId: String, emp: EmployeeRecord): Result<Unit> {
        val col = userDoc(userId).collection("employees")
        val isEdit = emp.id.isNotBlank()
        val path = if (isEdit) "users/$userId/employees/${emp.id}" else "users/$userId/employees"
        return try {
            requireUserId(userId)
            require(emp.fullName.isNotBlank()) { "Le nom de l'employé est obligatoire." }
            if (isEdit) {
                col.document(emp.id).update(emp.toUpdateMap()).await()
            } else {
                col.document().set(emp.toCreateMap(userId)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isEdit) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteEmployee(userId: String, empId: String): Result<Unit> {
        val path = "users/$userId/employees/$empId"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("employees").document(empId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createClaim(userId: String, claim: ClaimRecord): Result<Unit> {
        val path = "users/$userId/claims"
        return try {
            requireUserId(userId)
            require(claim.clientName.isNotBlank() && claim.subject.isNotBlank() && claim.description.isNotBlank()) {
                "Veuillez renseigner le client, le sujet et la description."
            }
            userDoc(userId).collection("claims").document().set(claim.toCreateMap(userId)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateClaimStatus(
        userId: String,
        claimId: String,
        status: String,
        severity: String,
        resolutionNote: String
    ): Result<Unit> {
        val path = "users/$userId/claims/$claimId"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("claims").document(claimId).update(
                mapOf(
                    "status" to status,
                    "severity" to severity,
                    "resolutionNote" to resolutionNote.trim().take(400),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun returnOrderToRework(
        userId: String,
        order: PressingOrder,
        targetStage: GarmentWorkshopStatus,
        reason: String,
        operatorName: String
    ): Result<Unit> {
        val path = "users/$userId/orders/${order.id}"
        return try {
            requireUserId(userId)
            require(order.id.isNotBlank())
            val parsed = order.parsedItems().map { item ->
                item.copy(workshopStatus = targetStage.name).toMap()
            }
            val safeReason = reason.trim().ifBlank { "Reprise qualité demandée" }.take(200)
            val entryText = "${nowFormatted()} · Reprise → ${targetStage.labelFr} ($operatorName) : $safeReason".take(160)
            val newHistory = (order.statusHistory + entryText).takeLast(30)
            val targetOrderStatus = if (targetStage == GarmentWorkshopStatus.IRONING) OrderStatus.IRONING else OrderStatus.WASHING
            userDoc(userId).collection("orders").document(order.id).update(
                mapOf(
                    "items" to parsed,
                    "status" to targetOrderStatus.name,
                    "assignedEmployee" to operatorName.ifBlank { order.assignedEmployee }.trim().take(100),
                    "qualityChecked" to false,
                    "observations" to safeReason.take(500),
                    "statusHistory" to newHistory,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun confirmDeliveryWithProof(
        userId: String,
        order: PressingOrder,
        driverName: String,
        zone: String,
        recipientName: String,
        proofNote: String
    ): Result<Unit> {
        val path = "users/$userId/orders/${order.id}"
        return try {
            requireUserId(userId)
            require(order.id.isNotBlank())
            val safeRecipient = recipientName.trim().ifBlank { order.clientName }.take(80)
            val safeNote = proofNote.trim().take(120)
            val logEntry = buildString {
                append("${nowFormatted()} · Livrée par $driverName ($zone) · Reçu par $safeRecipient")
                if (safeNote.isNotBlank()) append(" · $safeNote")
            }.take(160)
            val newHistory = (order.statusHistory + logEntry).takeLast(30)
            userDoc(userId).collection("orders").document(order.id).update(
                mapOf(
                    "deliveryRequested" to true,
                    "deliveryDriver" to driverName.trim().take(100),
                    "deliveryZone" to zone.trim().take(100),
                    "status" to OrderStatus.DELIVERED.name,
                    "statusHistory" to newHistory,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun logAuditEvent(userId: String, record: AuditLogRecord): Result<Unit> {
        val path = "users/$userId/audit_logs"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("audit_logs").document().set(record.toCreateMap(userId)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Best-effort audit logging: log without crashing UI flow
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observePushNotifications(userId: String): Flow<List<PushNotificationRecord>> = flow {
        requireUserId(userId)
        val path = "users/$userId/notifications"
        emitAll(
            userDoc(userId).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PushNotificationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                        .take(30)
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    suspend fun saveFcmToken(userId: String, record: FcmTokenRecord): Result<Unit> {
        val docId = "device_${record.token.hashCode().toUInt()}"
        val path = "users/$userId/fcm_tokens/$docId"
        return try {
            requireUserId(userId)
            if (record.token.isBlank()) return Result.success(Unit)
            userDoc(userId).collection("fcm_tokens").document(docId)
                .set(record.toCreateMap(userId)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun dispatchPushNotificationRecord(userId: String, record: PushNotificationRecord): Result<Unit> {
        val path = "users/$userId/notifications"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("notifications").document()
                .set(record.toCreateMap(userId)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- RBAC+ABAC MANAGER POLICIES & ADMINISTRATIVE APPROVAL WORKFLOW ---

    fun observeManagerPolicies(userId: String): Flow<List<ManagerAccessPolicy>> = flow {
        requireUserId(userId)
        val path = "users/$userId/manager_policies"
        emitAll(
            userDoc(userId).collection("manager_policies")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ManagerAccessPolicy::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.employeeName.lowercase() }
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    suspend fun saveManagerPolicy(userId: String, policy: ManagerAccessPolicy): Result<Unit> {
        val docId = policy.id.ifBlank {
            policy.employeeId.ifBlank { "policy_${policy.employeeName.hashCode().toUInt()}" }
        }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(100)
        val path = "users/$userId/manager_policies/$docId"
        return try {
            requireUserId(userId)
            val docRef = userDoc(userId).collection("manager_policies").document(docId)
            val exists = docRef.get().await().exists()
            docRef.set(policy.toFirestoreMap(userId, isCreate = !exists)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeApprovalRequests(userId: String): Flow<List<ApprovalRequestRecord>> = flow {
        requireUserId(userId)
        val path = "users/$userId/approval_requests"
        emitAll(
            userDoc(userId).collection("approval_requests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ApprovalRequestRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                        .take(50)
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    suspend fun submitApprovalRequest(userId: String, request: ApprovalRequestRecord): Result<String> {
        val path = "users/$userId/approval_requests"
        return try {
            requireUserId(userId)
            val docRef = userDoc(userId).collection("approval_requests").document()
            docRef.set(request.toCreateMap(userId)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun reviewApprovalRequest(
        userId: String,
        requestId: String,
        newStatus: ApprovalStatus,
        reviewedBy: String,
        reviewNote: String,
        executedAtMillis: Long = 0L
    ): Result<Unit> {
        val path = "users/$userId/approval_requests/$requestId"
        return try {
            requireUserId(userId)
            require(requestId.isNotBlank())
            userDoc(userId).collection("approval_requests").document(requestId).update(
                mapOf(
                    "status" to newStatus.name,
                    "reviewedBy" to reviewedBy.trim().take(100),
                    "reviewNote" to reviewNote.trim().take(240),
                    "executedAtMillis" to executedAtMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- GENIUSPAY SUBSCRIPTION RENEWAL PAYMENTS ---

    fun observeSubscriptionPayments(userId: String): Flow<List<SubscriptionPaymentRecord>> = flow {
        requireUserId(userId)
        val path = "users/$userId/subscription_payments"
        emitAll(
            userDoc(userId).collection("subscription_payments")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SubscriptionPaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                        .take(30)
                }
                .logAndRethrow(userId, OperationType.LIST, path)
        )
    }

    suspend fun saveSubscriptionPayment(
        userId: String,
        record: SubscriptionPaymentRecord
    ): Result<String> {
        val safeDocId = record.reference
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(80)
            .ifBlank { "gp_${System.currentTimeMillis()}" }
        val path = "users/$userId/subscription_payments/$safeDocId"
        return try {
            requireUserId(userId)
            userDoc(userId).collection("subscription_payments").document(safeDocId)
                .set(record.toCreateMap(userId)).await()
            Result.success(safeDocId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSubscriptionPaymentStatus(
        userId: String,
        reference: String,
        status: SubscriptionPaymentStatus
    ): Result<Unit> {
        val safeDocId = reference.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(80)
        val path = "users/$userId/subscription_payments/$safeDocId"
        return try {
            requireUserId(userId)
            require(safeDocId.isNotBlank())
            userDoc(userId).collection("subscription_payments").document(safeDocId).update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
