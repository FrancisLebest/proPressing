package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AfricanDialCodes
import com.example.data.AuthInputValidator
import com.example.data.AuthMethod
import com.example.data.CountryDialCode
import com.example.data.PasswordStrengthLevel
import com.example.ui.components.GestionProBrandHeader
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AfricaGoldBright
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        authViewModel.attemptAutoSignInOnce(context, credentialManager)
    }

    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var forgotPasswordEmail by rememberSaveable { mutableStateOf("") }
    var showTermsDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(false) }
    var showSupportDialog by rememberSaveable { mutableStateOf(false) }

    val bgBrush = Brush.verticalGradient(
        listOf(MidnightNavy, DeepNavySurface, Color(0xFF0A2463))
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        val isWideScreen = maxWidth >= 820.dp

        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) {
                AuthBrandShowcasePane(
                    isRegistrationMode = uiState.isRegistrationMode,
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 24.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AuthActionCard(
                        uiState = uiState,
                        onSelectMethod = { authViewModel.selectMethod(it) },
                        onToggleRegistration = { authViewModel.setRegistrationMode(it) },
                        onGoogleSignIn = { switchAccount ->
                            authViewModel.signInWithGoogle(context, credentialManager, switchAccount)
                        },
                        onEmailSignIn = { email, password ->
                            authViewModel.signInWithEmail(email, password)
                        },
                        onEmailRegister = { name, email, password, confirm, accepted ->
                            authViewModel.registerWithEmail(name, email, password, confirm, accepted)
                        },
                        onPhoneSignIn = { dial, phone, password ->
                            authViewModel.signInWithPhone(dial, phone, password)
                        },
                        onPhoneRegister = { name, dial, phone, password, confirm, accepted ->
                            authViewModel.registerWithPhone(name, dial, phone, password, confirm, accepted)
                        },
                        onUsernameSignIn = { username, password ->
                            authViewModel.signInWithUsername(username, password)
                        },
                        onUsernameRegister = { name, username, password, confirm, accepted ->
                            authViewModel.registerWithUsername(name, username, password, confirm, accepted)
                        },
                        onForgotPassword = { prefill ->
                            forgotPasswordEmail = prefill
                            showForgotPasswordDialog = true
                        },
                        onOpenTerms = { showTermsDialog = true },
                        onOpenPrivacy = { showPrivacyDialog = true },
                        onOpenSupport = { showSupportDialog = true },
                        onClearMessages = { authViewModel.clearMessages() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Compact Photographic Brand Header faithful to both Gestion Pro Africa mockups
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, AfricaGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_onboarding_employee),
                            contentDescription = "Professionnelle Pressing Pro Africa",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(74.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            GestionProBrandHeader(compact = true)
                            Text(
                                text = "Pressing · Laverie · Blanchisserie",
                                style = MaterialTheme.typography.labelMedium,
                                color = AfricaGoldBright,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Main Login / Registration Card
                AuthActionCard(
                    uiState = uiState,
                    onSelectMethod = { authViewModel.selectMethod(it) },
                    onToggleRegistration = { authViewModel.setRegistrationMode(it) },
                    onGoogleSignIn = { switchAccount ->
                        authViewModel.signInWithGoogle(context, credentialManager, switchAccount)
                    },
                    onEmailSignIn = { email, password ->
                        authViewModel.signInWithEmail(email, password)
                    },
                    onEmailRegister = { name, email, password, confirm, accepted ->
                        authViewModel.registerWithEmail(name, email, password, confirm, accepted)
                    },
                    onPhoneSignIn = { dial, phone, password ->
                        authViewModel.signInWithPhone(dial, phone, password)
                    },
                    onPhoneRegister = { name, dial, phone, password, confirm, accepted ->
                        authViewModel.registerWithPhone(name, dial, phone, password, confirm, accepted)
                    },
                    onUsernameSignIn = { username, password ->
                        authViewModel.signInWithUsername(username, password)
                    },
                    onUsernameRegister = { name, username, password, confirm, accepted ->
                        authViewModel.registerWithUsername(name, username, password, confirm, accepted)
                    },
                    onForgotPassword = { prefill ->
                        forgotPasswordEmail = prefill
                        showForgotPasswordDialog = true
                    },
                    onOpenTerms = { showTermsDialog = true },
                    onOpenPrivacy = { showPrivacyDialog = true },
                    onOpenSupport = { showSupportDialog = true },
                    onClearMessages = { authViewModel.clearMessages() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                )

                // 3 Mobile Feature Cards (100% visible, scrollable, zero clipping)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tout votre pressing dans une seule plateforme",
                        style = MaterialTheme.typography.titleSmall,
                        color = AfricaGoldBright,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        softWrap = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                    AuthMobileFeatureCard(
                        icon = Icons.Rounded.Checkroom,
                        title = "Gestion des commandes & QR Code",
                        subtitle = "Étiquetage atelier, suivi lavage/séchage/repassage et photos"
                    )
                    AuthMobileFeatureCard(
                        icon = Icons.Rounded.People,
                        title = "Suivi des clients & fidélité VIP",
                        subtitle = "Historique complet, remises automatiques et alertes clients"
                    )
                    AuthMobileFeatureCard(
                        icon = Icons.Rounded.PointOfSale,
                        title = "Caisse Mobile Money, SYSCOHADA & Livraisons",
                        subtitle = "Wave, Orange Money, MTN MoMo, factures, livraisons GPS et rôles isolés"
                    )
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = forgotPasswordEmail,
            isLoading = uiState.isLoading,
            onDismiss = { showForgotPasswordDialog = false },
            onSendReset = { email ->
                authViewModel.sendPasswordReset(email)
                showForgotPasswordDialog = false
            }
        )
    }

    if (showTermsDialog) {
        LegalConsentDialog(isPrivacyPolicy = false, onDismiss = { showTermsDialog = false })
    }
    if (showPrivacyDialog) {
        LegalConsentDialog(isPrivacyPolicy = true, onDismiss = { showPrivacyDialog = false })
    }
    if (showSupportDialog) {
        AuthSupportDialog(onDismiss = { showSupportDialog = false })
    }
}

@Composable
private fun AuthBrandShowcasePane(
    isRegistrationMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GestionProBrandHeader(compact = false)

        Text(
            text = "Gestion de Pressing, Laverie et Blanchisserie",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFD6E4FF),
            fontWeight = FontWeight.Medium,
            softWrap = true
        )

        // Hero Professional Photography faithful to both Gestion Pro Africa mockups
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(2.dp, AfricaGold),
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_onboarding_employee),
                    contentDescription = "Gestionnaire Pressing Pro Africa",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, MidnightNavy.copy(alpha = 0.78f))
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = AfricaGold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "La gestion professionnelle, simplement.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MidnightNavy,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isRegistrationMode) {
                AuthFeatureRow(Icons.Rounded.CheckCircle, "Gérez vos commandes")
                AuthFeatureRow(Icons.Rounded.People, "Suivez vos clients")
                AuthFeatureRow(Icons.Rounded.PointOfSale, "Analysez vos revenus")
                AuthFeatureRow(Icons.Rounded.LocalShipping, "Partout, à tout moment")
            } else {
                AuthFeatureRow(Icons.Rounded.Checkroom, "Gérez vos commandes de A à Z")
                AuthFeatureRow(Icons.Rounded.People, "Suivez vos clients et paiements")
                AuthFeatureRow(Icons.Rounded.PointOfSale, "Boostez la performance de votre pressing")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AuthActionCard(
    uiState: AuthUiState,
    onSelectMethod: (AuthMethod) -> Unit,
    onToggleRegistration: (Boolean) -> Unit,
    onGoogleSignIn: (switchAccount: Boolean) -> Unit,
    onEmailSignIn: (String, String) -> Unit,
    onEmailRegister: (String, String, String, String, Boolean) -> Unit,
    onPhoneSignIn: (String, String, String) -> Unit,
    onPhoneRegister: (String, String, String, String, String, Boolean) -> Unit,
    onUsernameSignIn: (String, String) -> Unit,
    onUsernameRegister: (String, String, String, String, Boolean) -> Unit,
    onForgotPassword: (String) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSupport: () -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    // Form state (preserved across orientation/recomposition)
    var fullName by rememberSaveable { mutableStateOf("") }
    var emailInput by rememberSaveable { mutableStateOf("") }
    var usernameInput by rememberSaveable { mutableStateOf("") }
    var selectedCountryIso by rememberSaveable { mutableStateOf(AfricanDialCodes.defaultCountry.isoCode) }
    val selectedCountry = remember(selectedCountryIso) {
        AfricanDialCodes.supported.firstOrNull { it.isoCode == selectedCountryIso }
            ?: AfricanDialCodes.defaultCountry
    }
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var confirmPasswordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var acceptedTerms by rememberSaveable { mutableStateOf(true) }
    var dialDropdownExpanded by remember { mutableStateOf(false) }

    val passwordStrength = remember(passwordInput) {
        AuthInputValidator.evaluatePassword(passwordInput)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Airy Header — Zero Fixed Heights, Zero Overlapping
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoftBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Checkroom,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = if (uiState.isRegistrationMode && uiState.selectedMethod != AuthMethod.GOOGLE) {
                        "Créer un compte"
                    } else {
                        "Connexion"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MidnightNavy,
                    textAlign = TextAlign.Center,
                    softWrap = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Accédez à votre espace professionnel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    softWrap = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Wrapping Brand Badge (never collides with subtitle on narrow phones)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GESTION PRO ",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MidnightNavy,
                            softWrap = true
                        )
                        Text(
                            text = "AFRICA",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AfricaGold,
                            softWrap = true
                        )
                        Text(
                            text = " –",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = RoyalBlue
                    ) {
                        Text(
                            text = "Pressing Pro",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            softWrap = true,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 2. Responsive 4-Method Auth Selector (Google / E-mail / Téléphone / Login)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compactTabs = maxWidth < 400.dp
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AuthMethodTab(
                                label = "Google",
                                icon = Icons.Rounded.AccountCircle,
                                selected = uiState.selectedMethod == AuthMethod.GOOGLE,
                                compact = compactTabs,
                                onClick = { onSelectMethod(AuthMethod.GOOGLE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_tab_google")
                            )
                            AuthMethodTab(
                                label = "E-mail",
                                icon = Icons.Rounded.Email,
                                selected = uiState.selectedMethod == AuthMethod.EMAIL_PASSWORD,
                                compact = compactTabs,
                                onClick = { onSelectMethod(AuthMethod.EMAIL_PASSWORD) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_tab_email")
                            )
                            AuthMethodTab(
                                label = "Tél.",
                                icon = Icons.Rounded.Phone,
                                selected = uiState.selectedMethod == AuthMethod.PHONE_PASSWORD,
                                compact = compactTabs,
                                onClick = { onSelectMethod(AuthMethod.PHONE_PASSWORD) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_tab_phone")
                            )
                            AuthMethodTab(
                                label = "Login",
                                icon = Icons.Rounded.Badge,
                                selected = uiState.selectedMethod == AuthMethod.USERNAME_LOGIN,
                                compact = compactTabs,
                                onClick = { onSelectMethod(AuthMethod.USERNAME_LOGIN) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_tab_username")
                            )
                        }
                    }
                }

                Text(
                    text = when (uiState.selectedMethod) {
                        AuthMethod.GOOGLE -> "Mode recommandé : Compte Google (activé et prêt)"
                        AuthMethod.EMAIL_PASSWORD -> "Mode actif : E-mail + Mot de passe"
                        AuthMethod.PHONE_PASSWORD -> "Mode actif : Téléphone (${selectedCountry.dialCode}) + Mot de passe"
                        AuthMethod.USERNAME_LOGIN -> "Mode actif : Identifiant unique (Login) + Mot de passe"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    softWrap = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Inline Feedback Banners (Error / Info)
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CrimsonContainer,
                        border = BorderStroke(1.dp, CrimsonDanger.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_error_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = CrimsonDanger,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(top = 1.dp)
                                )
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Medium,
                                    softWrap = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (uiState.isEmailProviderDisabled) {
                                Button(
                                    onClick = {
                                        onSelectMethod(AuthMethod.GOOGLE)
                                        onGoogleSignIn(false)
                                    },
                                    enabled = !uiState.isLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("auth_error_google_fallback_button")
                                ) {
                                    Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Continuer avec Google maintenant", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = uiState.infoMessage != null) {
                uiState.infoMessage?.let { info ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldContainer,
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_info_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 1.dp)
                            )
                            Text(
                                text = info,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.Medium,
                                softWrap = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Method-Specific Form Content
            when (uiState.selectedMethod) {
                AuthMethod.EMAIL_PASSWORD -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.isRegistrationMode) {
                            AuthTextField(
                                value = fullName,
                                onValueChange = { fullName = it; onClearMessages() },
                                label = "Nom complet *",
                                placeholder = "Ex. : Kouassi Yao",
                                leadingIcon = Icons.Rounded.Person,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                testTag = "auth_fullname_input"
                            )
                        }

                        AuthTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it; onClearMessages() },
                            label = "Adresse e-mail *",
                            placeholder = "contact@monpressing.ci",
                            leadingIcon = Icons.Rounded.Email,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            testTag = "auth_email_input"
                        )

                        AuthPasswordField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it; onClearMessages() },
                            label = "Mot de passe *",
                            placeholder = "Entrez votre mot de passe",
                            visible = passwordVisible,
                            onToggleVisibility = { passwordVisible = !passwordVisible },
                            imeAction = if (uiState.isRegistrationMode) ImeAction.Next else ImeAction.Done,
                            onDone = {
                                focusManager.clearFocus()
                                if (!uiState.isLoading && !uiState.isRegistrationMode) {
                                    onEmailSignIn(emailInput, passwordInput)
                                }
                            },
                            testTag = "auth_password_input"
                        )

                        if (uiState.isRegistrationMode) {
                            PasswordStrengthBar(
                                password = passwordInput,
                                score = passwordStrength.score,
                                level = passwordStrength.level
                            )

                            AuthPasswordField(
                                value = confirmPasswordInput,
                                onValueChange = { confirmPasswordInput = it; onClearMessages() },
                                label = "Confirmer le mot de passe *",
                                placeholder = "Répétez le mot de passe",
                                visible = confirmPasswordVisible,
                                onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                                imeAction = ImeAction.Done,
                                onDone = {
                                    focusManager.clearFocus()
                                    if (!uiState.isLoading) {
                                        onEmailRegister(fullName, emailInput, passwordInput, confirmPasswordInput, acceptedTerms)
                                    }
                                },
                                testTag = "auth_confirm_password_input"
                            )

                            AuthTermsCheckbox(
                                checked = acceptedTerms,
                                onCheckedChange = { acceptedTerms = it }
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(
                                    onClick = { onForgotPassword(emailInput) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("auth_forgot_password_button")
                                ) {
                                    Text(
                                        text = "Mot de passe oublié ?",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RoyalBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (uiState.isRegistrationMode) {
                                    onEmailRegister(fullName, emailInput, passwordInput, confirmPasswordInput, acceptedTerms)
                                } else {
                                    onEmailSignIn(emailInput, passwordInput)
                                }
                            },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("auth_email_submit_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBlue,
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.auth_connecting_label), fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = if (uiState.isRegistrationMode) "Créer mon compte" else "Se connecter",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        AuthModeToggleRow(
                            isRegistrationMode = uiState.isRegistrationMode,
                            onToggle = { onToggleRegistration(!uiState.isRegistrationMode) }
                        )

                        AuthOrDivider()

                        GoogleQuickActionButton(
                            isLoading = uiState.isLoading,
                            onClick = { onGoogleSignIn(false) },
                            testTag = "auth_google_shortcut_button"
                        )
                    }
                }

                AuthMethod.PHONE_PASSWORD -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.isRegistrationMode) {
                            AuthTextField(
                                value = fullName,
                                onValueChange = { fullName = it; onClearMessages() },
                                label = "Nom complet *",
                                placeholder = "Ex. : Aminata Koné",
                                leadingIcon = Icons.Rounded.Person,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                testTag = "auth_phone_fullname_input"
                            )
                        }

                        // Country Dial Code Selector + Local Phone Input
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Indicatif pays & Numéro de téléphone *",
                                style = MaterialTheme.typography.labelMedium,
                                color = MidnightNavy,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    Surface(
                                        onClick = { dialDropdownExpanded = true },
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .heightIn(min = 56.dp)
                                            .testTag("auth_dial_code_selector")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${selectedCountry.flagEmoji} ${selectedCountry.dialCode}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MidnightNavy
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDropDown,
                                                contentDescription = "Choisir l'indicatif pays",
                                                tint = RoyalBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = dialDropdownExpanded,
                                        onDismissRequest = { dialDropdownExpanded = false }
                                    ) {
                                        AfricanDialCodes.supported.forEach { country ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${country.flagEmoji} ${country.countryName} (${country.dialCode})",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (country.isoCode == selectedCountry.isoCode) {
                                                            FontWeight.Bold
                                                        } else {
                                                            FontWeight.Normal
                                                        }
                                                    )
                                                },
                                                onClick = {
                                                    selectedCountryIso = country.isoCode
                                                    dialDropdownExpanded = false
                                                    onClearMessages()
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = phoneInput,
                                    onValueChange = { raw ->
                                        phoneInput = raw.filter { it.isDigit() || it == ' ' || it == '-' || it == '+' }
                                        onClearMessages()
                                    },
                                    placeholder = {
                                        Text(
                                            text = selectedCountry.placeholder,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = authTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 56.dp)
                                        .testTag("auth_phone_input")
                                )
                            }
                            Text(
                                text = "${selectedCountry.flagEmoji} ${selectedCountry.countryName} (${selectedCountry.dialCode}) · Liaison automatique avec votre profil et votre agence",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                softWrap = true
                            )
                        }

                        AuthPasswordField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it; onClearMessages() },
                            label = "Mot de passe *",
                            placeholder = "Entrez votre mot de passe",
                            visible = passwordVisible,
                            onToggleVisibility = { passwordVisible = !passwordVisible },
                            imeAction = if (uiState.isRegistrationMode) ImeAction.Next else ImeAction.Done,
                            onDone = {
                                focusManager.clearFocus()
                                if (!uiState.isLoading && !uiState.isRegistrationMode) {
                                    onPhoneSignIn(selectedCountry.dialCode, phoneInput, passwordInput)
                                }
                            },
                            testTag = "auth_phone_password_input"
                        )

                        if (uiState.isRegistrationMode) {
                            PasswordStrengthBar(
                                password = passwordInput,
                                score = passwordStrength.score,
                                level = passwordStrength.level
                            )

                            AuthPasswordField(
                                value = confirmPasswordInput,
                                onValueChange = { confirmPasswordInput = it; onClearMessages() },
                                label = "Confirmer le mot de passe *",
                                placeholder = "Répétez le mot de passe",
                                visible = confirmPasswordVisible,
                                onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                                imeAction = ImeAction.Done,
                                onDone = {
                                    focusManager.clearFocus()
                                    if (!uiState.isLoading) {
                                        onPhoneRegister(
                                            fullName,
                                            selectedCountry.dialCode,
                                            phoneInput,
                                            passwordInput,
                                            confirmPasswordInput,
                                            acceptedTerms
                                        )
                                    }
                                },
                                testTag = "auth_phone_confirm_password_input"
                            )

                            AuthTermsCheckbox(
                                checked = acceptedTerms,
                                onCheckedChange = { acceptedTerms = it }
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TextButton(
                                    onClick = { onForgotPassword(emailInput) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("auth_phone_forgot_password_button")
                                ) {
                                    Text(
                                        text = "Mot de passe oublié ?",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RoyalBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (uiState.isRegistrationMode) {
                                    onPhoneRegister(
                                        fullName,
                                        selectedCountry.dialCode,
                                        phoneInput,
                                        passwordInput,
                                        confirmPasswordInput,
                                        acceptedTerms
                                    )
                                } else {
                                    onPhoneSignIn(selectedCountry.dialCode, phoneInput, passwordInput)
                                }
                            },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("auth_phone_submit_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBlue,
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.auth_connecting_label), fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = if (uiState.isRegistrationMode) {
                                        "Créer avec ce numéro"
                                    } else {
                                        "Se connecter par téléphone"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        AuthModeToggleRow(
                            isRegistrationMode = uiState.isRegistrationMode,
                            onToggle = { onToggleRegistration(!uiState.isRegistrationMode) }
                        )

                        AuthOrDivider()

                        GoogleQuickActionButton(
                            isLoading = uiState.isLoading,
                            onClick = { onGoogleSignIn(false) },
                            testTag = "auth_phone_google_shortcut_button"
                        )
                    }
                }

                AuthMethod.GOOGLE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SoftBlueContainer,
                            border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.22f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(RoyalBlue.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = RoyalBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Connexion Cloud Google sécurisée",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MidnightNavy,
                                        softWrap = true
                                    )
                                    Text(
                                        text = "Rattachement automatique à votre entreprise, votre agence isolée et votre rôle.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475569),
                                        softWrap = true
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { onGoogleSignIn(false) },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBlue,
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.auth_connecting_label), fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.auth_google_button),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        OutlinedButton(
                            onClick = { onGoogleSignIn(true) },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("switch_google_account_button"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue)
                        ) {
                            Icon(Icons.Rounded.SwitchAccount, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.auth_switch_google_account),
                                fontWeight = FontWeight.SemiBold,
                                softWrap = true
                            )
                        }
                    }
                }

                AuthMethod.USERNAME_LOGIN -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.isRegistrationMode) {
                            AuthTextField(
                                value = fullName,
                                onValueChange = { fullName = it; onClearMessages() },
                                label = "Nom complet *",
                                placeholder = "Ex. : Kouassi Yao",
                                leadingIcon = Icons.Rounded.Person,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                testTag = "auth_username_fullname_input"
                            )
                        }

                        AuthTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it.filter { c -> !c.isWhitespace() }.take(32); onClearMessages() },
                            label = "Identifiant de connexion (Login) *",
                            placeholder = "ex. pressing.cocody",
                            leadingIcon = Icons.Rounded.Badge,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Next
                            ),
                            testTag = "auth_username_input"
                        )

                        AuthPasswordField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it; onClearMessages() },
                            label = "Mot de passe *",
                            placeholder = "Entrez votre mot de passe",
                            visible = passwordVisible,
                            onToggleVisibility = { passwordVisible = !passwordVisible },
                            imeAction = if (uiState.isRegistrationMode) ImeAction.Next else ImeAction.Done,
                            onDone = {
                                focusManager.clearFocus()
                                if (!uiState.isLoading && !uiState.isRegistrationMode) {
                                    onUsernameSignIn(usernameInput, passwordInput)
                                }
                            },
                            testTag = "auth_username_password_input"
                        )

                        if (uiState.isRegistrationMode) {
                            PasswordStrengthBar(
                                password = passwordInput,
                                score = passwordStrength.score,
                                level = passwordStrength.level
                            )
                            AuthPasswordField(
                                value = confirmPasswordInput,
                                onValueChange = { confirmPasswordInput = it; onClearMessages() },
                                label = "Confirmer le mot de passe *",
                                placeholder = "Confirmez votre mot de passe",
                                visible = confirmPasswordVisible,
                                onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                                imeAction = ImeAction.Done,
                                onDone = {
                                    focusManager.clearFocus()
                                    if (!uiState.isLoading) {
                                        onUsernameRegister(fullName, usernameInput, passwordInput, confirmPasswordInput, acceptedTerms)
                                    }
                                },
                                testTag = "auth_username_confirm_input"
                            )
                            AuthTermsCheckbox(
                                checked = acceptedTerms,
                                onCheckedChange = { acceptedTerms = it },
                                onOpenTerms = onOpenTerms,
                                onOpenPrivacy = onOpenPrivacy
                            )
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (uiState.isRegistrationMode) {
                                    onUsernameRegister(fullName, usernameInput, passwordInput, confirmPasswordInput, acceptedTerms)
                                } else {
                                    onUsernameSignIn(usernameInput, passwordInput)
                                }
                            },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("auth_username_submit_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBlue,
                                contentColor = Color.White
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.5.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Traitement...", fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = if (uiState.isRegistrationMode) "S'inscrire" else "Se connecter",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        AuthModeToggleRow(
                            isRegistrationMode = uiState.isRegistrationMode,
                            onToggle = { onToggleRegistration(!uiState.isRegistrationMode) }
                        )

                        AuthOrDivider()

                        GoogleQuickActionButton(
                            isLoading = uiState.isLoading,
                            onClick = { onGoogleSignIn(false) },
                            testTag = "auth_username_google_shortcut_button"
                        )
                    }
                }
            }

            // 5. Public Registration Security Notice + Support Action
            if (uiState.isRegistrationMode && uiState.selectedMethod != AuthMethod.GOOGLE) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftBlueContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "L'inscription crée exclusivement un compte Administrateur Pressing (PRESSING_ADMIN). Les rôles Gestionnaires sont créés depuis votre espace sécurisé.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MidnightNavy,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onOpenTerms,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("CGU", style = MaterialTheme.typography.labelMedium, color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                }
                Text("·", color = Color(0xFF94A3B8))
                TextButton(
                    onClick = onOpenPrivacy,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Confidentialité", style = MaterialTheme.typography.labelMedium, color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                }
                Text("·", color = Color(0xFF94A3B8))
                TextButton(
                    onClick = onOpenSupport,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("auth_contact_support_button")
                ) {
                    Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Assistance", style = MaterialTheme.typography.labelMedium, color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                }
            }

            // 6. Security Trust Strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Sécurisé, fiable et accessible 24h/24 et 7j/7",
                        style = MaterialTheme.typography.labelMedium,
                        color = MidnightNavy,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        softWrap = true
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthMethodTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) RoyalBlue else Color.Transparent,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) Color.White else RoyalBlueDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White else MidnightNavy,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else RoyalBlueDark,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) Color.White else MidnightNavy,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RoyalBlue,
    unfocusedBorderColor = Color(0xFFCBD5E1),
    focusedTextColor = MidnightNavy,
    unfocusedTextColor = MidnightNavy,
    focusedLabelColor = RoyalBlue,
    unfocusedLabelColor = Color(0xFF475569),
    focusedContainerColor = Color(0xFFF8FAFC),
    unfocusedContainerColor = Color(0xFFF8FAFC)
)

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardOptions: KeyboardOptions,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Color(0xFF94A3B8)) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = RoyalBlue) },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(14.dp),
        colors = authTextFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag)
    )
}

@Composable
private fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    imeAction: ImeAction,
    onDone: () -> Unit,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Color(0xFF94A3B8)) },
        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = RoyalBlue) },
        trailingIcon = {
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.testTag("${testTag}_toggle_visibility")
            ) {
                Icon(
                    imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Masquer le mot de passe" else "Afficher le mot de passe",
                    tint = Color(0xFF475569)
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = RoundedCornerShape(14.dp),
        colors = authTextFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag)
    )
}

@Composable
private fun PasswordStrengthBar(
    password: String,
    score: Int,
    level: PasswordStrengthLevel
) {
    if (password.isEmpty()) return
    val activeSegments = when (level) {
        PasswordStrengthLevel.WEAK -> 1
        PasswordStrengthLevel.MEDIUM -> 2
        PasswordStrengthLevel.STRONG, PasswordStrengthLevel.VERY_STRONG -> 3
    }
    val color = when (level) {
        PasswordStrengthLevel.WEAK -> CrimsonDanger
        PasswordStrengthLevel.MEDIUM -> AmberWarning
        PasswordStrengthLevel.STRONG, PasswordStrengthLevel.VERY_STRONG -> EmeraldSuccess
    }
    val strengthLabelFr = when (level) {
        PasswordStrengthLevel.WEAK -> "Force : Faible"
        PasswordStrengthLevel.MEDIUM -> "Force : Moyenne"
        PasswordStrengthLevel.STRONG -> "Force : Forte"
        PasswordStrengthLevel.VERY_STRONG -> "Force : Très forte"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { idx ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (idx < activeSegments) color else Color(0xFFE2E8F0))
                )
            }
        }
        Text(
            text = strengthLabelFr,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AuthTermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
            )
            Text(
                text = "J'accepte les Conditions d'utilisation et la Politique de confidentialité de GESTION PRO AFRICA. *",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF334155),
                softWrap = true
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AuthModeToggleRow(
    isRegistrationMode: Boolean,
    onToggle: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRegistrationMode) "Vous avez déjà un compte ?" else "Nouveau sur Pressing Pro ?",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF475569)
        )
        TextButton(
            onClick = onToggle,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("auth_toggle_mode_button")
        ) {
            Text(
                text = if (isRegistrationMode) "Se connecter" else "Créer un compte",
                style = MaterialTheme.typography.labelLarge,
                color = RoyalBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AuthOrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
        Text(
            text = "ou",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
    }
}

@Composable
private fun GoogleQuickActionButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, RoyalBlue.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MidnightNavy)
    ) {
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = null,
            tint = RoyalBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.auth_google_button),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            softWrap = true
        )
    }
}

@Composable
private fun AuthMobileFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RoyalBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AfricaGoldBright,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    softWrap = true
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD6E4FF),
                    softWrap = true
                )
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(RoyalBlue.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AfricaGoldBright, modifier = Modifier.size(20.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            softWrap = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSendReset: (String) -> Unit
) {
    var resetEmail by rememberSaveable(initialEmail) { mutableStateOf(initialEmail) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Réinitialiser le mot de passe",
                fontWeight = FontWeight.Bold,
                color = MidnightNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Saisissez l'adresse e-mail associée à votre compte Pressing Pro pour recevoir un lien de réinitialisation sécurisé. Si votre compte a été créé uniquement par numéro de téléphone, contactez votre Directeur d'agence.",
                    style = MaterialTheme.typography.bodyMedium,
                    softWrap = true
                )
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = {
                        resetEmail = it
                        localError = null
                    },
                    label = { Text("Adresse e-mail") },
                    placeholder = { Text("contact@monpressing.ci") },
                    singleLine = true,
                    isError = localError != null,
                    supportingText = localError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("forgot_password_email_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val err = AuthInputValidator.validateEmail(resetEmail)
                    if (err != null) {
                        localError = err
                    } else {
                        onSendReset(resetEmail)
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("forgot_password_confirm_button")
            ) {
                Text("Envoyer le lien", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Annuler")
            }
        }
    )
}
