package com.example.core

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val AUTH_INIT_TAG = "FirebaseAuthInit"

enum class EmailPasswordProviderState {
    CONFIGURED_CLIENT_READY,
    VERIFIED_ENABLED,
    DISABLED_IN_FIREBASE_CONSOLE
}

data class FirebaseAuthBootstrapSnapshot(
    val initialized: Boolean = false,
    val activeClientProviders: Set<String> = emptySet(),
    val emailPasswordState: EmailPasswordProviderState = EmailPasswordProviderState.CONFIGURED_CLIENT_READY,
    val languageCode: String = "fr"
) {
    val isEmailPasswordClientEnabled: Boolean
        get() = EmailAuthProvider.PROVIDER_ID in activeClientProviders
}

/**
 * Explicit FirebaseAuth initializer that configures the Auth instance at startup with
 * Email/Password (`EmailAuthProvider.PROVIDER_ID`) and Google Sign-In (`GoogleAuthProvider.PROVIDER_ID`)
 * client readiness and French localization.
 *
 * Note: The Firebase Android client SDK configures client-side provider support and tracks
 * server-side provider status; toggling the project-level Identity Platform Email/Password flag
 * itself requires Firebase Console or Firebase Admin SDK (`backend/scripts/bootstrap-super-admin.mjs`).
 */
object FirebaseAuthInitializer {
    private val _state = MutableStateFlow(FirebaseAuthBootstrapSnapshot())
    val state: StateFlow<FirebaseAuthBootstrapSnapshot> = _state.asStateFlow()

    fun initializeAuth(
        context: Context,
        firebaseAuth: FirebaseAuth? = null
    ): FirebaseAuthBootstrapSnapshot {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val authInstance = firebaseAuth ?: Firebase.auth
            authInstance.setLanguageCode("fr")
            // Explicitly verify FirebaseAuthSettings accessibility and keep verification protections intact
            authInstance.firebaseAuthSettings.setAppVerificationDisabledForTesting(false)

            val snapshot = FirebaseAuthBootstrapSnapshot(
                initialized = true,
                activeClientProviders = setOf(
                    EmailAuthProvider.PROVIDER_ID,
                    GoogleAuthProvider.PROVIDER_ID
                ),
                emailPasswordState = _state.value.emailPasswordState,
                languageCode = "fr"
            )
            _state.value = snapshot
            Log.i(
                AUTH_INIT_TAG,
                "FirebaseAuth initialized with client providers=${snapshot.activeClientProviders} (lang=${snapshot.languageCode})"
            )
            snapshot
        } catch (e: Exception) {
            Log.e(AUTH_INIT_TAG, "Failed to configure FirebaseAuth instance", e)
            _state.value
        }
    }

    fun markEmailPasswordVerifiedEnabled() {
        _state.value = _state.value.copy(
            emailPasswordState = EmailPasswordProviderState.VERIFIED_ENABLED
        )
    }

    fun markEmailPasswordDisabledInConsole() {
        _state.value = _state.value.copy(
            emailPasswordState = EmailPasswordProviderState.DISABLED_IN_FIREBASE_CONSOLE
        )
    }
}
