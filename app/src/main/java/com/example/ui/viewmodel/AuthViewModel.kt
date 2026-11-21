package com.example.ui.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.GoogleCredentialOutcome
import com.example.core.fetchInteractiveGoogleIdToken
import com.example.core.fetchSilentGoogleIdToken
import com.example.data.AuthInputValidator
import com.example.data.AuthMethod
import com.example.data.AuthRepository
import com.example.data.AuthSessionState
import com.example.data.AuthUserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val selectedMethod: AuthMethod = AuthMethod.EMAIL_PASSWORD,
    val isRegistrationMode: Boolean = false,
    val isLoading: Boolean = false,
    val isAutoSigningIn: Boolean = false,
    val isProfileUpdating: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isCheckingInitialSession = MutableStateFlow(false)
    private var autoSignInAttempted = false

    private val _manualProfileOverride = MutableStateFlow<AuthUserProfile?>(null)

    val sessionState: StateFlow<AuthSessionState> = combine(
        authRepository.observeAuthState(),
        _isCheckingInitialSession,
        _manualProfileOverride
    ) { observedUser, checking, overrideProfile ->
        when {
            observedUser != null -> {
                val effective = if (overrideProfile != null && overrideProfile.uid == observedUser.uid) {
                    overrideProfile
                } else {
                    observedUser
                }
                AuthSessionState.Authenticated(effective)
            }
            checking -> AuthSessionState.CheckingSession
            else -> AuthSessionState.Unauthenticated
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = authRepository.currentUserProfile()
            ?.let { AuthSessionState.Authenticated(it) }
            ?: AuthSessionState.Unauthenticated
    )

    fun selectMethod(method: AuthMethod) {
        _uiState.update {
            it.copy(
                selectedMethod = method,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun setRegistrationMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isRegistrationMode = enabled,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun attemptAutoSignInOnce(context: Context, credentialManager: CredentialManager) {
        if (autoSignInAttempted) return
        autoSignInAttempted = true
        if (authRepository.currentUserProfile() != null) return

        viewModelScope.launch {
            _isCheckingInitialSession.value = true
            _uiState.update { it.copy(isAutoSigningIn = true) }
            try {
                when (val outcome = fetchSilentGoogleIdToken(context, credentialManager)) {
                    is GoogleCredentialOutcome.Token -> {
                        val result = authRepository.signInWithGoogleIdToken(outcome.idToken)
                        result.onFailure { err ->
                            _uiState.update { it.copy(errorMessage = err.message) }
                        }
                    }
                    GoogleCredentialOutcome.Cancelled,
                    GoogleCredentialOutcome.NoAuthorizedAccount,
                    is GoogleCredentialOutcome.Error -> Unit
                }
            } finally {
                _isCheckingInitialSession.value = false
                _uiState.update { it.copy(isAutoSigningIn = false) }
            }
        }
    }

    fun signInWithGoogle(
        context: Context,
        credentialManager: CredentialManager,
        switchAccount: Boolean = false
    ) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }
            when (
                val outcome = fetchInteractiveGoogleIdToken(
                    context = context,
                    credentialManager = credentialManager,
                    clearExistingStateFirst = switchAccount
                )
            ) {
                is GoogleCredentialOutcome.Token -> {
                    val result = authRepository.signInWithGoogleIdToken(outcome.idToken)
                    result.onSuccess { profile ->
                        _manualProfileOverride.value = profile
                        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    }.onFailure { err ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = err.message ?: "Échec de la connexion Firebase."
                            )
                        }
                    }
                }
                GoogleCredentialOutcome.Cancelled -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                GoogleCredentialOutcome.NoAuthorizedAccount -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Aucun compte Google disponible sur cet appareil."
                        )
                    }
                }
                is GoogleCredentialOutcome.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = outcome.message
                        )
                    }
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        if (_uiState.value.isLoading) return
        AuthInputValidator.validateEmail(email)?.let { err ->
            _uiState.update { it.copy(errorMessage = err, infoMessage = null) }
            return
        }
        AuthInputValidator.validateSignInPassword(password)?.let { err ->
            _uiState.update { it.copy(errorMessage = err, infoMessage = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.signInWithEmailCredential(email, password)
            result.onSuccess { profile ->
                _manualProfileOverride.value = profile
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Échec de la connexion par e-mail."
                    )
                }
            }
        }
    }

    fun registerWithEmail(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean
    ) {
        if (_uiState.value.isLoading) return
        val errors = AuthInputValidator.validateRegistration(
            fullName = fullName,
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            acceptedTerms = acceptedTerms
        )
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errorMessage = errors.first(), infoMessage = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.registerWithEmailPassword(fullName, email, password)
            result.onSuccess { profile ->
                _manualProfileOverride.value = profile
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Impossible de créer le compte entreprise."
                    )
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (_uiState.value.isLoading) return
        AuthInputValidator.validateEmail(email)?.let { err ->
            _uiState.update { it.copy(errorMessage = err, infoMessage = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.sendPasswordResetEmail(email)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        infoMessage = "Un lien de réinitialisation a été envoyé à ${email.trim()}."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Impossible d'envoyer l'e-mail de réinitialisation."
                    )
                }
            }
        }
    }

    fun signInWithPhone(dialCode: String, localNumber: String, password: String) {
        if (_uiState.value.isLoading) return
        AuthInputValidator.validatePhone(dialCode, localNumber)?.let { err ->
            _uiState.update { it.copy(errorMessage = err, infoMessage = null) }
            return
        }
        AuthInputValidator.validateSignInPassword(password)?.let { err ->
            _uiState.update { it.copy(errorMessage = err, infoMessage = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.signInWithPhonePassword(dialCode, localNumber, password)
            result.onSuccess { profile ->
                _manualProfileOverride.value = profile
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Échec de la connexion par numéro de téléphone."
                    )
                }
            }
        }
    }

    fun registerWithPhone(
        fullName: String,
        dialCode: String,
        localNumber: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean
    ) {
        if (_uiState.value.isLoading) return
        val errors = AuthInputValidator.validatePhoneRegistration(
            fullName = fullName,
            dialCode = dialCode,
            rawLocalNumber = localNumber,
            password = password,
            confirmPassword = confirmPassword,
            acceptedTerms = acceptedTerms
        )
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errorMessage = errors.first(), infoMessage = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.registerWithPhonePassword(fullName, dialCode, localNumber, password)
            result.onSuccess { profile ->
                _manualProfileOverride.value = profile
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Impossible de créer le compte avec ce numéro."
                    )
                }
            }
        }
    }

    fun updateDisplayName(newName: String) {
        if (_uiState.value.isProfileUpdating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProfileUpdating = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.updateDisplayName(newName)
            result.onSuccess { updated ->
                _manualProfileOverride.value = updated
                _uiState.update {
                    it.copy(
                        isProfileUpdating = false,
                        infoMessage = "Nom du profil mis à jour avec succès."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProfileUpdating = false,
                        errorMessage = err.message ?: "Impossible de mettre à jour le profil."
                    )
                }
            }
        }
    }

    fun refreshSession() {
        if (_uiState.value.isProfileUpdating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProfileUpdating = true, errorMessage = null, infoMessage = null) }
            val result = authRepository.reloadCurrentUser()
            result.onSuccess { refreshed ->
                _manualProfileOverride.value = refreshed
                _uiState.update {
                    it.copy(
                        isProfileUpdating = false,
                        infoMessage = "Session Firebase Auth synchronisée."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProfileUpdating = false,
                        errorMessage = err.message ?: "Impossible d'actualiser la session."
                    )
                }
            }
        }
    }

    fun signOut(credentialManager: CredentialManager) {
        viewModelScope.launch {
            _manualProfileOverride.value = null
            _uiState.update { it.copy(isLoading = false, errorMessage = null, infoMessage = null) }
            authRepository.signOut(credentialManager)
        }
    }
}
