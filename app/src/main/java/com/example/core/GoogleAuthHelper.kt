package com.example.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.CancellationException

private const val AUTH_TAG = "Auth"

sealed interface GoogleCredentialOutcome {
    data class Token(val idToken: String) : GoogleCredentialOutcome
    data object Cancelled : GoogleCredentialOutcome
    data object NoAuthorizedAccount : GoogleCredentialOutcome
    data class Error(val message: String) : GoogleCredentialOutcome
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun resolveWebClientId(context: Context): String? {
    return try {
        context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

/**
 * Silent auto-sign-in credential lookup using a single GetGoogleIdOption per request.
 */
suspend fun fetchSilentGoogleIdToken(
    context: Context,
    credentialManager: CredentialManager
): GoogleCredentialOutcome {
    val clientId = resolveWebClientId(context) ?: return GoogleCredentialOutcome.NoAuthorizedAccount
    val activity = context.findActivity() ?: return GoogleCredentialOutcome.NoAuthorizedAccount

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val credential = credentialManager.getCredential(activity, request).credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            GoogleCredentialOutcome.Token(token)
        } else {
            GoogleCredentialOutcome.NoAuthorizedAccount
        }
    } catch (e: NoCredentialException) {
        Log.d(AUTH_TAG, "No previously authorized Google account for silent sign-in")
        GoogleCredentialOutcome.NoAuthorizedAccount
    } catch (e: GetCredentialCancellationException) {
        Log.w(AUTH_TAG, "Silent Google Sign-In cancelled or dismissed: ${e.message}", e)
        GoogleCredentialOutcome.Cancelled
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.d(AUTH_TAG, "Silent Google Sign-In skipped: ${e.message}")
        GoogleCredentialOutcome.NoAuthorizedAccount
    }
}

/**
 * Interactive Google Sign-In flow using a single GetSignInWithGoogleOption per request
 * so the Credential Manager bottom sheet stays responsive on Android API 36.
 */
suspend fun fetchInteractiveGoogleIdToken(
    context: Context,
    credentialManager: CredentialManager,
    clearExistingStateFirst: Boolean = false
): GoogleCredentialOutcome {
    val clientId = resolveWebClientId(context)
        ?: return GoogleCredentialOutcome.Error(
            "Configuration Google Sign-In introuvable (default_web_client_id)."
        )
    val activity = context.findActivity()
        ?: return GoogleCredentialOutcome.Error(
            "Impossible d'ouvrir la fenêtre de connexion Google."
        )

    if (clearExistingStateFirst) {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(AUTH_TAG, "Could not clear prior credential state before switch: ${e.message}")
        }
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    return try {
        val credential = credentialManager.getCredential(activity, request).credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            GoogleCredentialOutcome.Token(token)
        } else {
            GoogleCredentialOutcome.Error("Type d'identifiant Google inattendu.")
        }
    } catch (e: GetCredentialCancellationException) {
        Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
        GoogleCredentialOutcome.Cancelled
    } catch (e: NoCredentialException) {
        Log.w(AUTH_TAG, "No Google credential available on device: ${e.message}", e)
        GoogleCredentialOutcome.Error(
            "Aucun compte Google disponible sur cet appareil. Ajoutez un compte Google dans les paramètres Android."
        )
    } catch (e: GetCredentialException) {
        Log.e(AUTH_TAG, "Credential Manager error during Google Sign-In", e)
        GoogleCredentialOutcome.Error(
            e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                ?: "Échec de la sélection du compte Google. Vérifiez votre connexion."
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "Google Sign-In failed", e)
        GoogleCredentialOutcome.Error(
            e.localizedMessage ?: "Échec de la connexion Google."
        )
    }
}
