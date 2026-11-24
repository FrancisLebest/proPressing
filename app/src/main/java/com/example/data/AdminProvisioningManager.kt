package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.core.FirebaseAuthInitializer
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

private const val ADMIN_PROVISION_TAG = "AdminProvisioning"

sealed interface AdminBootstrapResult {
    data class Provisioned(val uid: String, val email: String) : AdminBootstrapResult
    data class AlreadyExists(val email: String) : AdminBootstrapResult
    data class FirestoreProfileEnsured(val uid: String, val email: String) : AdminBootstrapResult
    data object SkippedInProductionBuild : AdminBootstrapResult
    data object SkippedMissingDebugCredentials : AdminBootstrapResult
    data class ProviderDisabledInConsole(val detail: String) : AdminBootstrapResult
    data class UnauthorizedEmailRejected(val email: String) : AdminBootstrapResult
    data class Error(val message: String) : AdminBootstrapResult
}

/**
 * Abstraction for reading optional debug-only Super-Admin bootstrap credentials.
 * In release builds, `BuildConfig.ALLOW_CLIENT_ADMIN_BOOTSTRAP` is compiled as `false`
 * and both credential fields are empty strings `""` so zero secrets exist in production binaries.
 */
interface AdminBootstrapCredentialSource {
    fun isClientBootstrapAllowed(): Boolean
    fun superAdminEmail(): String
    fun superAdminPassword(): String
}

object BuildConfigAdminCredentialSource : AdminBootstrapCredentialSource {
    override fun isClientBootstrapAllowed(): Boolean = BuildConfig.ALLOW_CLIENT_ADMIN_BOOTSTRAP
    override fun superAdminEmail(): String = BuildConfig.SUPER_ADMIN_BOOTSTRAP_EMAIL
    override fun superAdminPassword(): String = BuildConfig.SUPER_ADMIN_BOOTSTRAP_PASSWORD
}

/**
 * Robust, idempotent `ensureAdminExists()` manager:
 * 1. Never contains or logs hardcoded plaintext passwords.
 * 2. In production release builds (`ALLOW_CLIENT_ADMIN_BOOTSTRAP == false`), skips client-side
 *    password creation entirely and provisions the Super-Admin Firestore profile automatically
 *    once an authorized Super-Admin authenticates via Firebase Auth (Email/Password or Google).
 * 3. In debug builds with optional environment-injected credentials (`SUPER_ADMIN_BOOTSTRAP_EMAIL`
 *    and `SUPER_ADMIN_BOOTSTRAP_PASSWORD`), idempotently provisions the Super-Admin Auth account
 *    and seeds the initial Firestore Enterprise profile, then signs out the temporary bootstrap
 *    session so the login screen remains gated.
 */
class AdminProvisioningManager(
    private val credentialSource: AdminBootstrapCredentialSource = BuildConfigAdminCredentialSource,
    private val firebaseAuthProvider: () -> FirebaseAuth = { Firebase.auth },
    private val firestoreProfileSeeder: (suspend (uid: String, email: String) -> Result<Boolean>)? = null
) {
    private val bootstrapMutex = Mutex()
    @Volatile
    private var completedOnce = false

    suspend fun ensureAdminExists(forceRecheck: Boolean = false): AdminBootstrapResult {
        return bootstrapMutex.withLock {
            if (completedOnce && !forceRecheck) {
                val activeUser = runCatching { firebaseAuthProvider().currentUser }.getOrNull()
                if (activeUser != null && PlatformSuperAdminConfig.isPlatformSuperAdminEmail(activeUser.email)) {
                    return@withLock AdminBootstrapResult.FirestoreProfileEnsured(
                        uid = activeUser.uid,
                        email = activeUser.email.orEmpty()
                    )
                }
            }

            // Path A: If an authorized Super-Admin is already authenticated (Debug OR Release),
            // idempotently ensure their initial Firestore Super-Admin profile exists without needing any password.
            val currentUser = runCatching { firebaseAuthProvider().currentUser }.getOrNull()
            if (currentUser != null && PlatformSuperAdminConfig.isPlatformSuperAdminEmail(currentUser.email)) {
                val email = currentUser.email.orEmpty().trim().lowercase()
                val seeded = firestoreProfileSeeder?.invoke(currentUser.uid, email)
                completedOnce = true
                return@withLock if (seeded == null || seeded.isSuccess) {
                    AdminBootstrapResult.FirestoreProfileEnsured(currentUser.uid, email)
                } else {
                    AdminBootstrapResult.Error(
                        seeded.exceptionOrNull()?.message ?: "Impossible de vérifier le profil Super-Admin Firestore."
                    )
                }
            }

            // Path B: Unauthenticated startup — strictly blocked in release binaries so zero credentials exist in APK
            if (!credentialSource.isClientBootstrapAllowed()) {
                completedOnce = true
                return@withLock AdminBootstrapResult.SkippedInProductionBuild
            }

            val targetEmail = credentialSource.superAdminEmail().trim().lowercase()
            val targetPassword = credentialSource.superAdminPassword()
            if (targetEmail.isBlank() || targetPassword.isBlank()) {
                completedOnce = true
                return@withLock AdminBootstrapResult.SkippedMissingDebugCredentials
            }

            if (!PlatformSuperAdminConfig.isPlatformSuperAdminEmail(targetEmail)) {
                Log.w(ADMIN_PROVISION_TAG, "Rejected unauthorized bootstrap email configuration.")
                return@withLock AdminBootstrapResult.UnauthorizedEmailRejected(targetEmail)
            }

            val pwdEval = AuthInputValidator.evaluatePassword(targetPassword)
            if (!pwdEval.isAcceptable) {
                return@withLock AdminBootstrapResult.Error(
                    "Mot de passe de provisioning Super-Admin trop faible."
                )
            }

            val auth = try {
                firebaseAuthProvider()
            } catch (e: Exception) {
                return@withLock AdminBootstrapResult.Error(
                    "Instance FirebaseAuth indisponible : ${e.message.orEmpty()}"
                )
            }

            // Never interrupt an active non-admin user session
            if (auth.currentUser != null) {
                return@withLock AdminBootstrapResult.AlreadyExists(targetEmail)
            }

            try {
                val result = auth.createUserWithEmailAndPassword(targetEmail, targetPassword).await()
                val createdUser = result.user
                    ?: return@withLock AdminBootstrapResult.Error("Compte Super-Admin créé sans identifiant UID.")

                runCatching {
                    val profileReq = UserProfileChangeRequest.Builder()
                        .setDisplayName("Super-Admin Gestion Pro Africa")
                        .build()
                    createdUser.updateProfile(profileReq).await()
                }

                firestoreProfileSeeder?.invoke(createdUser.uid, targetEmail)
                FirebaseAuthInitializer.markEmailPasswordVerifiedEnabled()

                // Sign out temporary bootstrap session so normal authentication gate remains enforced
                auth.signOut()
                completedOnce = true
                Log.i(ADMIN_PROVISION_TAG, "Super-Admin account provisioned cleanly for UID=${createdUser.uid}")
                AdminBootstrapResult.Provisioned(createdUser.uid, targetEmail)
            } catch (e: CancellationException) {
                throw e
            } catch (_: FirebaseAuthUserCollisionException) {
                FirebaseAuthInitializer.markEmailPasswordVerifiedEnabled()
                completedOnce = true
                AdminBootstrapResult.AlreadyExists(targetEmail)
            } catch (e: FirebaseAuthException) {
                if (e.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                    FirebaseAuthInitializer.markEmailPasswordVerifiedEnabled()
                    completedOnce = true
                    AdminBootstrapResult.AlreadyExists(targetEmail)
                } else if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED") {
                    FirebaseAuthInitializer.markEmailPasswordDisabledInConsole()
                    completedOnce = true
                    AdminBootstrapResult.ProviderDisabledInConsole(
                        AuthErrorMapper.mapFirebaseErrorCode("ERROR_OPERATION_NOT_ALLOWED")
                    )
                } else {
                    Log.w(ADMIN_PROVISION_TAG, "Super-Admin bootstrap failed with code=${e.errorCode}")
                    AdminBootstrapResult.Error(AuthErrorMapper.mapFirebaseErrorCode(e.errorCode))
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                    msg.contains("provider is disabled", ignoreCase = true)
                ) {
                    FirebaseAuthInitializer.markEmailPasswordDisabledInConsole()
                    completedOnce = true
                    AdminBootstrapResult.ProviderDisabledInConsole(
                        AuthErrorMapper.mapFirebaseErrorCode("ERROR_OPERATION_NOT_ALLOWED")
                    )
                } else {
                    Log.w(ADMIN_PROVISION_TAG, "Super-Admin bootstrap failed: ${e.javaClass.simpleName}")
                    AdminBootstrapResult.Error("Échec du provisioning Super-Admin.")
                }
            }
        }
    }

    /**
     * Pure validation helper used for unit testing and pre-flight verification without Firebase runtime.
     */
    fun evaluatePreFlightPolicy(): AdminBootstrapResult? {
        if (!credentialSource.isClientBootstrapAllowed()) {
            return AdminBootstrapResult.SkippedInProductionBuild
        }
        val targetEmail = credentialSource.superAdminEmail().trim().lowercase()
        val targetPassword = credentialSource.superAdminPassword()
        if (targetEmail.isBlank() || targetPassword.isBlank()) {
            return AdminBootstrapResult.SkippedMissingDebugCredentials
        }
        if (!PlatformSuperAdminConfig.isPlatformSuperAdminEmail(targetEmail)) {
            return AdminBootstrapResult.UnauthorizedEmailRejected(targetEmail)
        }
        if (!AuthInputValidator.evaluatePassword(targetPassword).isAcceptable) {
            return AdminBootstrapResult.Error("Mot de passe de provisioning Super-Admin trop faible.")
        }
        return null
    }
}
