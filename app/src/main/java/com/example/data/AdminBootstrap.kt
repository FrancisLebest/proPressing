package com.example.data

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.example.BuildConfig
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

private const val ADMIN_BOOTSTRAP_TAG = "AdminBootstrap"

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "SUPER_ADMIN",
    val active: Boolean = true,
    val provider: String = "CLIENT_DEBUG_BOOTSTRAP",
    val bootstrapSource: String = "android_debug_bootstrap",
    val createdByUid: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

sealed interface AdminRecordBootstrapResult {
    data class Created(val documentId: String, val email: String) : AdminRecordBootstrapResult
    data class AlreadyExists(val documentId: String, val email: String) : AdminRecordBootstrapResult
    data object SkippedInProductionBuild : AdminRecordBootstrapResult
    data object SkippedUnauthorizedSession : AdminRecordBootstrapResult
    data class Error(val message: String) : AdminRecordBootstrapResult
}

/**
 * Debug-only Firestore admin registry bootstrapper.
 *
 * Strictly gated by [BuildConfig.ALLOW_CLIENT_ADMIN_BOOTSTRAP] (compiled as `false` in release
 * builds) so production binaries never attempt client-side admin document creation.
 * Checks whether `best2london@gmail.com` already exists inside the Firestore `admins` collection
 * on the project's named database (`R.string.firestore_database_id`) and creates it with server
 * timestamps and audit metadata if missing.
 */
object AdminBootstrap {
    const val ADMINS_COLLECTION = "admins"
    const val TARGET_ADMIN_EMAIL = "best2london@gmail.com"
    const val TARGET_ADMIN_DOC_ID = "best2london_gmail_com"

    private val mutex = Mutex()

    /**
     * Pure pre-flight policy check for unit testing and debug-gate enforcement.
     */
    fun evaluatePreFlightGuard(
        allowClientBootstrap: Boolean = BuildConfig.ALLOW_CLIENT_ADMIN_BOOTSTRAP,
        currentUserUid: String?,
        currentUserEmail: String?
    ): AdminRecordBootstrapResult? {
        if (!allowClientBootstrap) {
            return AdminRecordBootstrapResult.SkippedInProductionBuild
        }
        if (currentUserUid.isNullOrBlank() ||
            !PlatformSuperAdminConfig.isPlatformSuperAdminEmail(currentUserEmail)
        ) {
            return AdminRecordBootstrapResult.SkippedUnauthorizedSession
        }
        return null
    }

    /**
     * Builds the canonical Firestore payload aligned 1:1 with `firestore.rules` `/admins/{adminId}`.
     */
    fun buildAdminDocumentPayload(
        createdByUid: String,
        email: String = TARGET_ADMIN_EMAIL
    ): Map<String, Any> = mapOf(
        "email" to email.trim().lowercase(),
        "displayName" to "Super-Admin Gestion Pro Africa",
        "role" to "SUPER_ADMIN",
        "active" to true,
        "provider" to "CLIENT_DEBUG_BOOTSTRAP",
        "bootstrapSource" to "android_debug_bootstrap",
        "createdByUid" to createdByUid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    /**
     * Ensures the `best2london@gmail.com` Super-Admin document exists in the Firestore `admins`
     * collection when running in debug builds ([BuildConfig.ALLOW_CLIENT_ADMIN_BOOTSTRAP] == true).
     */
    suspend fun ensureAdminExists(
        context: Context,
        allowClientBootstrap: Boolean = BuildConfig.ALLOW_CLIENT_ADMIN_BOOTSTRAP
    ): AdminRecordBootstrapResult {
        if (!allowClientBootstrap) {
            return AdminRecordBootstrapResult.SkippedInProductionBuild
        }
        val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(databaseId)
        return ensureAdminExists(
            firestore = firestore,
            auth = Firebase.auth,
            allowClientBootstrap = allowClientBootstrap
        )
    }

    /**
     * Overload accepting explicit [FirebaseFirestore] and [FirebaseAuth] instances.
     */
    suspend fun ensureAdminExists(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth = Firebase.auth,
        allowClientBootstrap: Boolean = BuildConfig.ALLOW_CLIENT_ADMIN_BOOTSTRAP
    ): AdminRecordBootstrapResult {
        if (!allowClientBootstrap) {
            return AdminRecordBootstrapResult.SkippedInProductionBuild
        }

        val currentUser = auth.currentUser
        val preFlightBlock = evaluatePreFlightGuard(
            allowClientBootstrap = allowClientBootstrap,
            currentUserUid = currentUser?.uid,
            currentUserEmail = currentUser?.email
        )
        if (preFlightBlock != null) {
            return preFlightBlock
        }

        val actorUid = currentUser!!.uid
        val path = "$ADMINS_COLLECTION/$TARGET_ADMIN_DOC_ID"

        return mutex.withLock {
            try {
                val adminsRef = firestore.collection(ADMINS_COLLECTION)

                // 1. Check deterministic document ID first
                val directDocRef = adminsRef.document(TARGET_ADMIN_DOC_ID)
                val directSnap = directDocRef.get().await()
                if (directSnap.exists()) {
                    Log.d(ADMIN_BOOTSTRAP_TAG, "Super-Admin record already present at $path")
                    return@withLock AdminRecordBootstrapResult.AlreadyExists(
                        documentId = directSnap.id,
                        email = TARGET_ADMIN_EMAIL
                    )
                }

                // 2. Also query by email in case an admin record was created under a custom UID
                val querySnap = adminsRef
                    .whereEqualTo("email", TARGET_ADMIN_EMAIL)
                    .limit(1)
                    .get()
                    .await()
                if (!querySnap.isEmpty) {
                    val existingDoc = querySnap.documents.first()
                    Log.d(ADMIN_BOOTSTRAP_TAG, "Super-Admin record found via query: ${existingDoc.id}")
                    return@withLock AdminRecordBootstrapResult.AlreadyExists(
                        documentId = existingDoc.id,
                        email = TARGET_ADMIN_EMAIL
                    )
                }

                // 3. Create the Firestore document with full metadata & server timestamps
                val payload = buildAdminDocumentPayload(
                    createdByUid = actorUid,
                    email = TARGET_ADMIN_EMAIL
                )
                directDocRef.set(payload).await()
                Log.i(ADMIN_BOOTSTRAP_TAG, "Created Super-Admin Firestore document at $path")
                AdminRecordBootstrapResult.Created(
                    documentId = TARGET_ADMIN_DOC_ID,
                    email = TARGET_ADMIN_EMAIL
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, path)
                AdminRecordBootstrapResult.Error(
                    e.localizedMessage ?: "Failed to verify or create Super-Admin document in admins."
                )
            }
        }
    }
}

/**
 * Top-level convenience entry point delegating to [AdminBootstrap.ensureAdminExists].
 */
suspend fun ensureAdminExists(
    context: Context,
    allowClientBootstrap: Boolean = BuildConfig.ALLOW_CLIENT_ADMIN_BOOTSTRAP
): AdminRecordBootstrapResult = AdminBootstrap.ensureAdminExists(
    context = context,
    allowClientBootstrap = allowClientBootstrap
)
