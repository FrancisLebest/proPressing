package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.configureAppCheck
import com.example.data.AiRepository
import com.example.data.AuthSessionState
import com.example.data.AuthUserProfile
import com.example.data.PressingRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainWorkspace
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.theme.PressingProTheme
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.PressingViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            PressingProTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        isDarkTheme = darkTheme,
                        onToggleDarkTheme = { darkTheme = it }
                    )
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    when (val session = sessionState) {
        AuthSessionState.CheckingSession,
        AuthSessionState.Unauthenticated -> {
            // Unauthenticated session: only AuthScreen is composed; zero Firestore queries run
            AuthScreen(authViewModel = authViewModel)
        }

        is AuthSessionState.Authenticated -> {
            val userProfile = session.user
            // Key composition & ViewModel by UID so account switches tear down listeners cleanly
            key(userProfile.uid) {
                AuthenticatedPressingApp(
                    userProfile = userProfile,
                    authUiState = authUiState,
                    authViewModel = authViewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedPressingApp(
    userProfile: AuthUserProfile,
    authUiState: AuthUiState,
    authViewModel: AuthViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: PressingViewModel = viewModel(
        key = userProfile.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PressingViewModel(
                    repository = PressingRepository(db),
                    aiRepository = AiRepository(),
                    currentUserId = userProfile.uid,
                    currentUserEmail = userProfile.email,
                    currentUserPhone = userProfile.phoneNumber,
                    currentUserName = userProfile.displayLabel
                )
            }
        }
    )

    val companyState by viewModel.companyState.collectAsStateWithLifecycle()
    val servicesState by viewModel.servicesState.collectAsStateWithLifecycle()
    val employeesState by viewModel.employeesState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    var forceSetupWizard by remember { mutableStateOf(false) }

    when (val state = companyState) {
        UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Chargement de votre espace Pressing Pro...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Erreur de synchronisation cloud",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { authViewModel.signOut(credentialManager) }) {
                        Text("Se reconnecter")
                    }
                }
            }
        }

        is UiState.Success -> {
            val config = state.data
            if (config == null || !config.setupCompleted || forceSetupWizard) {
                SetupWizardScreen(
                    existingConfig = config,
                    services = (servicesState as? UiState.Success)?.data.orEmpty(),
                    employees = (employeesState as? UiState.Success)?.data.orEmpty(),
                    isBusy = isBusy,
                    onSaveStep = { updated, silent -> viewModel.saveCompanyConfig(updated, silent) },
                    onSeedStarterServices = { viewModel.seedStarterServices() },
                    onAddService = { viewModel.saveService(it) },
                    onAddEmployee = { viewModel.saveEmployee(it) },
                    onCompleteSetup = { completedConfig ->
                        viewModel.saveCompanyConfig(completedConfig, silent = false) {
                            forceSetupWizard = false
                        }
                    }
                )
            } else {
                MainWorkspace(
                    company = config,
                    userProfile = userProfile,
                    authUiState = authUiState,
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onReopenSetupWizard = { forceSetupWizard = true },
                    onUpdateDisplayName = { authViewModel.updateDisplayName(it) },
                    onRefreshSession = { authViewModel.refreshSession() },
                    onClearAuthMessages = { authViewModel.clearMessages() },
                    onSignOut = { authViewModel.signOut(credentialManager) }
                )
            }
        }
    }
}
