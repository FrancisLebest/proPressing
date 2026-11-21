package com.example.data

import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_REPO_TAG = "AuthRepository"
private const val PHONE_AUTH_DOMAIN = "pressingpro.africa"
private const val PHONE_AUTH_PREFIX = "tel."

enum class AuthMethod(
    val labelFr: String,
    val isCloudProvisioned: Boolean
) {
    GOOGLE("Google", true),
    EMAIL_PASSWORD("E-mail", true),
    PHONE_PASSWORD("Téléphone", true)
}

data class CountryDialCode(
    val countryName: String,
    val isoCode: String,
    val flagEmoji: String,
    val dialCode: String,
    val placeholder: String
)

object AfricanDialCodes {
    val defaultCountry = CountryDialCode("Côte d'Ivoire", "CI", "🇨🇮", "+225", "07 58 42 19 63")

    val supported: List<CountryDialCode> = listOf(
        defaultCountry,
        CountryDialCode("Sénégal", "SN", "🇸🇳", "+221", "77 123 45 67"),
        CountryDialCode("Mali", "ML", "🇲🇱", "+223", "70 12 34 56"),
        CountryDialCode("Burkina Faso", "BF", "🇧🇫", "+226", "70 12 34 56"),
        CountryDialCode("Bénin", "BJ", "🇧🇯", "+229", "97 12 34 56"),
        CountryDialCode("Togo", "TG", "🇹🇬", "+228", "90 12 34 56"),
        CountryDialCode("Cameroun", "CM", "🇨🇲", "+237", "6 70 12 34 56"),
        CountryDialCode("Gabon", "GA", "🇬🇦", "+241", "06 12 34 56"),
        CountryDialCode("RD Congo", "CD", "🇨🇩", "+243", "81 123 45 67"),
        CountryDialCode("Niger", "NE", "🇳🇪", "+227", "90 12 34 56"),
        CountryDialCode("Guinée", "GN", "🇬🇳", "+224", "620 12 34 56"),
        CountryDialCode("France", "FR", "🇫🇷", "+33", "6 12 34 56 78")
    )
}

data class AuthUserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val providerIds: List<String>,
    val isEmailVerified: Boolean,
    val creationTimestamp: Long?,
    val lastSignInTimestamp: Long?,
    val phoneNumber: String = ""
) {
    val hasGoogleProvider: Boolean
        get() = providerIds.contains(GoogleAuthProvider.PROVIDER_ID)

    val hasPasswordProvider: Boolean
        get() = providerIds.contains(EmailAuthProvider.PROVIDER_ID)

    val isSyntheticPhoneAccount: Boolean
        get() = AuthInputValidator.extractPhoneFromAuthIdentifier(email).isNotBlank()

    val effectiveContactLabel: String
        get() = when {
            phoneNumber.isNotBlank() -> phoneNumber
            isSyntheticPhoneAccount -> AuthInputValidator.extractPhoneFromAuthIdentifier(email)
            email.isNotBlank() -> email
            else -> "Compte vérifié"
        }

    val displayLabel: String
        get() = displayName.ifBlank {
            if (isSyntheticPhoneAccount) {
                AuthInputValidator.extractPhoneFromAuthIdentifier(email)
            } else {
                email.substringBefore("@").ifBlank { phoneNumber.ifBlank { "Utilisateur" } }
            }
        }

    val initials: String
        get() {
            val parts = displayLabel.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
                parts.size == 1 -> parts[0].filter { it.isLetterOrDigit() }.take(2).uppercase().ifBlank { "PP" }
                else -> "PP"
            }
        }
}

sealed interface AuthSessionState {
    data object CheckingSession : AuthSessionState
    data object Unauthenticated : AuthSessionState
    data class Authenticated(val user: AuthUserProfile) : AuthSessionState
}

enum class PasswordStrengthLevel(val labelFr: String) {
    WEAK("Faible"),
    MEDIUM("Moyen"),
    STRONG("Fort"),
    VERY_STRONG("Très fort")
}

data class PasswordStrengthResult(
    val score: Int,
    val level: PasswordStrengthLevel,
    val missingRequirements: List<String>
) {
    val isAcceptable: Boolean get() = score >= 3 && missingRequirements.isEmpty()
}

object AuthInputValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val SYNTHETIC_PHONE_REGEX = Regex("^$PHONE_AUTH_PREFIX(\\d{8,15})@$PHONE_AUTH_DOMAIN$", RegexOption.IGNORE_CASE)

    fun validateEmail(rawEmail: String): String? {
        val email = rawEmail.trim()
        if (email.isEmpty()) return "Veuillez saisir une adresse e-mail professionnelle."
        if (email.contains(" ")) return "L'adresse e-mail ne doit pas contenir d'espaces."
        if (!EMAIL_REGEX.matches(email)) return "Format d'adresse e-mail invalide (ex. : contact@monpressing.ci)."
        return null
    }

    fun validateSignInPassword(password: String): String? {
        if (password.isBlank()) return "Veuillez saisir votre mot de passe."
        if (password.length < 6) return "Le mot de passe doit contenir au moins 6 caractères."
        return null
    }

    fun validatePhone(dialCode: String, rawLocalNumber: String): String? {
        val cleanDialDigits = dialCode.filter { it.isDigit() }
        if (cleanDialDigits.isEmpty()) return "Sélectionnez un indicatif pays valide (ex. : +225)."
        val localDigits = extractLocalDigits(dialCode, rawLocalNumber)
        if (localDigits.isEmpty()) return "Veuillez saisir votre numéro de téléphone."
        if (localDigits.length < 8) return "Le numéro de téléphone est trop court (8 chiffres minimum)."
        if (localDigits.length > 12) return "Le numéro de téléphone est trop long (12 chiffres maximum)."
        return null
    }

    fun normalizePhoneToE164(dialCode: String, rawLocalNumber: String): String {
        val cleanDialDigits = dialCode.filter { it.isDigit() }
        val localDigits = extractLocalDigits(dialCode, rawLocalNumber)
        return "+$cleanDialDigits$localDigits"
    }

    private fun extractLocalDigits(dialCode: String, rawLocalNumber: String): String {
        val cleanDialDigits = dialCode.filter { it.isDigit() }
        val trimmed = rawLocalNumber.trim()
        val allDigits = trimmed.filter { it.isDigit() }
        return when {
            trimmed.startsWith("+") && allDigits.startsWith(cleanDialDigits) ->
                allDigits.removePrefix(cleanDialDigits)
            trimmed.startsWith("00$cleanDialDigits") ->
                allDigits.removePrefix("00$cleanDialDigits")
            allDigits.startsWith(cleanDialDigits) && allDigits.length >= cleanDialDigits.length + 8 ->
                allDigits.removePrefix(cleanDialDigits)
            else -> allDigits
        }
    }

    fun phoneToAuthIdentifier(e164Phone: String): String {
        val digits = e164Phone.filter { it.isDigit() }
        return "$PHONE_AUTH_PREFIX$digits@$PHONE_AUTH_DOMAIN"
    }

    fun extractPhoneFromAuthIdentifier(email: String): String {
        val match = SYNTHETIC_PHONE_REGEX.matchEntire(email.trim()) ?: return ""
        return "+${match.groupValues[1]}"
    }

    fun evaluatePassword(password: String): PasswordStrengthResult {
        val missing = mutableListOf<String>()
        var score = 0
        if (password.length >= 8) score++ else missing += "Au moins 8 caractères"
        if (password.any { it.isUpperCase() }) score++ else missing += "Une lettre majuscule"
        if (password.any { it.isLowerCase() }) score++ else missing += "Une lettre minuscule"
        if (password.any { it.isDigit() }) score++ else missing += "Un chiffre"
        if (password.any { !it.isLetterOrDigit() }) score++

        val level = when {
            score >= 5 -> PasswordStrengthLevel.VERY_STRONG
            score == 4 -> PasswordStrengthLevel.STRONG
            score >= 2 -> PasswordStrengthLevel.MEDIUM
            else -> PasswordStrengthLevel.WEAK
        }
        return PasswordStrengthResult(
            score = score.coerceAtMost(4),
            level = level,
            missingRequirements = missing
        )
    }

    fun validateRegistration(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean
    ): List<String> {
        val errors = mutableListOf<String>()
        if (fullName.trim().length < 2) {
            errors += "Veuillez renseigner le nom complet de l'administrateur."
        }
        validateEmail(email)?.let { errors += it }
        val pwd = evaluatePassword(password)
        if (!pwd.isAcceptable) {
            errors += "Mot de passe insuffisant : ${pwd.missingRequirements.joinToString(", ")}."
        }
        if (password != confirmPassword) {
            errors += "Les mots de passe ne correspondent pas."
        }
        if (!acceptedTerms) {
            errors += "Veuillez accepter les Conditions d'utilisation."
        }
        return errors
    }

    fun validatePhoneRegistration(
        fullName: String,
        dialCode: String,
        rawLocalNumber: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean
    ): List<String> {
        val errors = mutableListOf<String>()
        if (fullName.trim().length < 2) {
            errors += "Veuillez renseigner votre nom complet."
        }
        validatePhone(dialCode, rawLocalNumber)?.let { errors += it }
        val pwd = evaluatePassword(password)
        if (!pwd.isAcceptable) {
            errors += "Mot de passe insuffisant : ${pwd.missingRequirements.joinToString(", ")}."
        }
        if (password != confirmPassword) {
            errors += "Les mots de passe ne correspondent pas."
        }
        if (!acceptedTerms) {
            errors += "Veuillez accepter les Conditions d'utilisation."
        }
        return errors
    }

    fun validateDisplayName(rawName: String): String? {
        val trimmed = rawName.trim()
        if (trimmed.length < 2) return "Le nom doit contenir au moins 2 caractères."
        if (trimmed.length > 60) return "Le nom ne peut pas dépasser 60 caractères."
        return null
    }
}

object AuthErrorMapper {
    fun mapFirebaseErrorCode(errorCode: String?): String {
        return when (errorCode) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_WRONG_PASSWORD" ->
                "Identifiants invalides ou mot de passe incorrect. Vérifiez vos informations et réessayez."

            "ERROR_INVALID_EMAIL" ->
                "L'identifiant ou l'adresse e-mail fourni n'est pas valide."

            "ERROR_USER_DISABLED" ->
                "Ce compte utilisateur a été désactivé par l'administrateur."

            "ERROR_USER_NOT_FOUND" ->
                "Aucun compte associé à ces identifiants n'a été trouvé."

            "ERROR_EMAIL_ALREADY_IN_USE",
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "Un compte existe déjà avec cet identifiant. Connectez-vous ou utilisez Google Sign-In."

            "ERROR_WEAK_PASSWORD" ->
                "Mot de passe trop faible. Utilisez au moins 8 caractères avec majuscule et chiffre."

            "ERROR_OPERATION_NOT_ALLOWED" ->
                "La méthode E-mail / Mot de passe n'est pas encore activée dans la Console Firebase du projet (Authentication → Sign-in method). Vous pouvez vous connecter immédiatement avec Google."

            "ERROR_TOO_MANY_REQUESTS" ->
                "Trop de tentatives de connexion. Patientez quelques instants avant de réessayer."

            "ERROR_USER_TOKEN_EXPIRED",
            "ERROR_INVALID_USER_TOKEN" ->
                "Votre session a expiré. Veuillez vous reconnecter."

            "ERROR_NETWORK_REQUEST_FAILED" ->
                "Connexion réseau indisponible. Vérifiez votre accès Internet."

            else ->
                "Erreur d'authentification Firebase (${errorCode ?: "inconnue"}). Réessayez."
        }
    }

    fun mapThrowable(throwable: Throwable): String {
        return when (throwable) {
            is FirebaseAuthException -> mapFirebaseErrorCode(throwable.errorCode)
            is FirebaseNetworkException -> "Connexion réseau indisponible. Vérifiez votre accès Internet."
            else -> throwable.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Échec de l'authentification. Veuillez réessayer."
        }
    }
}

fun FirebaseUser.toAuthUserProfile(): AuthUserProfile {
    val rawEmail = email.orEmpty()
    val resolvedPhone = phoneNumber?.takeIf { it.isNotBlank() }
        ?: AuthInputValidator.extractPhoneFromAuthIdentifier(rawEmail)
    return AuthUserProfile(
        uid = uid,
        displayName = displayName.orEmpty(),
        email = rawEmail,
        photoUrl = photoUrl?.toString(),
        providerIds = providerData
            .map { it.providerId }
            .filter { it.isNotBlank() && it != "firebase" }
            .distinct(),
        isEmailVerified = isEmailVerified,
        creationTimestamp = metadata?.creationTimestamp,
        lastSignInTimestamp = metadata?.lastSignInTimestamp,
        phoneNumber = resolvedPhone
    )
}

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = Firebase.auth
) {
    fun currentUserProfile(): AuthUserProfile? = firebaseAuth.currentUser?.toAuthUserProfile()

    fun observeAuthState(): Flow<AuthUserProfile?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUserProfile())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUserProfile> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Jeton Google manquant."))
        }
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Session utilisateur introuvable après connexion."))
            Result.success(user.toAuthUserProfile())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_REPO_TAG, "Firebase Google credential sign-in failed", e)
            Result.failure(Exception(AuthErrorMapper.mapThrowable(e), e))
        }
    }

    suspend fun signInWithEmailCredential(
        rawEmail: String,
        password: String
    ): Result<AuthUserProfile> {
        AuthInputValidator.validateEmail(rawEmail)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        AuthInputValidator.validateSignInPassword(password)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        return try {
            val credential = EmailAuthProvider.getCredential(rawEmail.trim(), password)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Session utilisateur introuvable après connexion."))
            Result.success(user.toAuthUserProfile())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_REPO_TAG, "Firebase email credential sign-in failed", e)
            Result.failure(Exception(AuthErrorMapper.mapThrowable(e), e))
        }
    }

    suspend fun registerWithEmailPassword(
        fullName: String,
        rawEmail: String,
        password: String
    ): Result<AuthUserProfile> {
        AuthInputValidator.validateDisplayName(fullName)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        AuthInputValidator.validateEmail(rawEmail)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        val pwdEval = AuthInputValidator.evaluatePassword(password)
        if (!pwdEval.isAcceptable) {
            return Result.failure(
                IllegalArgumentException("Mot de passe insuffisant : ${pwdEval.missingRequirements.joinToString(", ")}.")
            )
        }
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(rawEmail.trim(), password).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Impossible de créer le compte utilisateur."))
            val profileReq = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName.trim())
                .build()
            user.updateProfile(profileReq).await()
            user.reload().await()
            Result.success(firebaseAuth.currentUser?.toAuthUserProfile() ?: user.toAuthUserProfile())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_REPO_TAG, "Firebase email registration failed", e)
            Result.failure(Exception(AuthErrorMapper.mapThrowable(e), e))
        }
    }

    suspend fun sendPasswordResetEmail(rawEmail: String): Result<Unit> {
        AuthInputValidator.validateEmail(rawEmail)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        return try {
            firebaseAuth.sendPasswordResetEmail(rawEmail.trim()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_REPO_TAG, "Firebase password reset failed", e)
            Result.failure(Exception(AuthErrorMapper.mapThrowable(e), e))
        }
    }

    suspend fun signInWithPhonePassword(
        dialCode: String,
        rawLocalNumber: String,
        password: String
    ): Result<AuthUserProfile> {
        AuthInputValidator.validatePhone(dialCode, rawLocalNumber)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        AuthInputValidator.validateSignInPassword(password)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        val e164 = AuthInputValidator.normalizePhoneToE164(dialCode, rawLocalNumber)
        val authId = AuthInputValidator.phoneToAuthIdentifier(e164)
        return try {
            val credential = EmailAuthProvider.getCredential(authId, password)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Session utilisateur introuvable après connexion."))
            Result.success(user.toAuthUserProfile())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_REPO_TAG, "Firebase phone+password credential sign-in failed", e)
            Result.failure(Exception(AuthErrorMapper.mapThrowable(e), e))
        }
    }

    suspend fun registerWithPhonePassword(
        fullName: String,
        dialCode: String,
        rawLocalNumber: String,
        password: String
    ): Result<AuthUserProfile> {
        AuthInputValidator.validateDisplayName(fullName)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        AuthInputValidator.validatePhone(dialCode, rawLocalNumber)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        val pwdEval = AuthInputValidator.evaluatePassword(password)
        if (!pwdEval.isAcceptable) {
            return Result.failure(
                IllegalArgumentException("Mot de passe insuffisant : ${pwdEval.missingRequirements.joinToString(", ")}.")
            )
        }
        val e164 = AuthInputValidator.normalizePhoneToE164(dialCode, rawLocalNumber)
        val authId = AuthInputValidator.phoneToAuthIdentifier(e164)
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(authId, password).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Impossible de créer le compte utilisateur."))
            val profileReq = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName.trim())
                .build()
            user.updateProfile(profileReq).await()
            user.reload().await()
            Result.success(firebaseAuth.currentUser?.toAuthUserProfile() ?: user.toAuthUserProfile())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_REPO_TAG, "Firebase phone+password registration failed", e)
            Result.failure(Exception(AuthErrorMapper.mapThrowable(e), e))
        }
    }

    suspend fun reloadCurrentUser(): Result<AuthUserProfile?> {
        val user = firebaseAuth.currentUser ?: return Result.success(null)
        return try {
            user.reload().await()
            Result.success(firebaseAuth.currentUser?.toAuthUserProfile())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_REPO_TAG, "Failed to reload Firebase user session", e)
            Result.failure(Exception(AuthErrorMapper.mapThrowable(e), e))
        }
    }

    suspend fun updateDisplayName(newDisplayName: String): Result<AuthUserProfile> {
        val validationError = AuthInputValidator.validateDisplayName(newDisplayName)
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }
        val user = firebaseAuth.currentUser
            ?: return Result.failure(IllegalStateException("Aucun utilisateur connecté."))
        return try {
            val request = UserProfileChangeRequest.Builder()
                .setDisplayName(newDisplayName.trim())
                .build()
            user.updateProfile(request).await()
            user.reload().await()
            val updated = firebaseAuth.currentUser?.toAuthUserProfile() ?: user.toAuthUserProfile()
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_REPO_TAG, "Failed to update display name", e)
            Result.failure(Exception(AuthErrorMapper.mapThrowable(e), e))
        }
    }

    suspend fun signOut(credentialManager: CredentialManager): Result<Unit> {
        return withContext(NonCancellable) {
            try {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(AUTH_REPO_TAG, "Failed to clear Credential Manager state: ${e.message}")
                }
                firebaseAuth.signOut()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(AUTH_REPO_TAG, "Sign-out failed", e)
                Result.failure(Exception(AuthErrorMapper.mapThrowable(e), e))
            }
        }
    }
}
