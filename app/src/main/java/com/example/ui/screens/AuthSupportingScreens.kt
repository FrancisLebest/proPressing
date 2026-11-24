package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PressingProTheme
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer
import kotlinx.coroutines.delay

/**
 * 6-digit OTP input composable with visual digit boxes and clear keyboard handling (Section 14).
 */
@Composable
fun OtpInputField(
    code: String,
    onCodeChange: (String) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            repeat(6) { index ->
                val char = code.getOrNull(index)?.toString().orEmpty()
                val active = index == code.length.coerceAtMost(5)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (char.isNotBlank()) SoftBlueContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (active) 2.dp else 1.dp,
                        color = when {
                            isError -> CrimsonDanger
                            active -> RoyalBlue
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier.size(width = 46.dp, height = 54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = char.ifBlank { "•" },
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (char.isNotBlank()) MidnightNavy else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        OutlinedTextField(
            value = code,
            onValueChange = { raw -> onCodeChange(raw.filter { it.isDigit() }.take(6)) },
            label = { Text("Code OTP à 6 chiffres") },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_otp_code_input")
        )
    }
}

/**
 * Email Verification Screen with 60-second resend countdown and Firebase reload check (Section 7 #9).
 */
@Composable
fun EmailVerificationScreen(
    email: String,
    isLoading: Boolean,
    statusMessage: String? = null,
    onCheckVerified: () -> Unit,
    onResendLink: () -> Unit,
    onSignOut: () -> Unit
) {
    var cooldownSeconds by remember { mutableIntStateOf(60) }
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000L)
            cooldownSeconds--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MidnightNavy, DeepNavySurface)))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SoftBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MarkEmailRead, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(34.dp))
                }
                Text(
                    text = "Vérifiez votre adresse e-mail",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MidnightNavy,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Un lien de confirmation sécurisé a été envoyé à $email. Ouvrez-le puis appuyez sur le bouton ci-dessous.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center
                )
                if (!statusMessage.isNullOrBlank()) {
                    Surface(shape = RoundedCornerShape(12.dp), color = EmeraldContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                Button(
                    onClick = onCheckVerified,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("verify_email_check_btn")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("J'ai vérifié mon adresse", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        cooldownSeconds = 60
                        onResendLink()
                    },
                    enabled = !isLoading && cooldownSeconds == 0,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("verify_email_resend_btn")
                ) {
                    Text(
                        if (cooldownSeconds > 0) "Renvoyer le lien (${cooldownSeconds}s)" else "Renvoyer le lien de vérification",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = onSignOut, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Utiliser un autre compte", color = CrimsonDanger)
                }
            }
        }
    }
}

/**
 * Account Suspended / Session Expired security guard screens (Section 7 #16 & #17).
 */
@Composable
fun AccountStatusGuardScreen(
    isSuspended: Boolean,
    reason: String,
    onPrimaryAction: () -> Unit,
    onContactSupport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MidnightNavy, DeepNavySurface)))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CrimsonContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSuspended) Icons.Rounded.Block else Icons.Rounded.LockClock,
                        contentDescription = null,
                        tint = CrimsonDanger,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    text = if (isSuspended) "Compte suspendu" else "Session expirée",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MidnightNavy,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onPrimaryAction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                ) {
                    Text(if (isSuspended) "Retour à la connexion" else "Se reconnecter", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onContactSupport,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Contacter l'assistance")
                }
            }
        }
    }
}

@Composable
fun LegalConsentDialog(
    isPrivacyPolicy: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.GppGood, contentDescription = null, tint = RoyalBlue) },
        title = {
            Text(
                text = if (isPrivacyPolicy) "Politique de confidentialité" else "Conditions d'utilisation",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPrivacyPolicy) {
                    Text("Version : PRIVACY-2026.1 — GESTION PRO AFRICA (Pressing Pro)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                    Text("• Isolation stricte multi-entreprises : vos clients, commandes, paiements et photos atelier sont cloisonnés par organisation et agence dans Cloud Firestore.", style = MaterialTheme.typography.bodySmall)
                    Text("• Zéro mot de passe en clair : l'authentification est gérée exclusivement par Firebase Auth avec protection App Check.", style = MaterialTheme.typography.bodySmall)
                    Text("• Traçabilité : toute action sensible et session d'assistance est journalisée dans l'audit sécurité.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Version : CGU-2026.1 — GESTION PRO AFRICA (Pressing Pro)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                    Text("• Le compte créé via l'inscription publique reçoit le rôle Administrateur d'entreprise (PRESSING_ADMIN) pour gérer son pressing, ses agences et ses gestionnaires.", style = MaterialTheme.typography.bodySmall)
                    Text("• L'administrateur est responsable de la confidentialité de ses identifiants et de la gestion des habilitations de ses employés.", style = MaterialTheme.typography.bodySmall)
                    Text("• Les montants en FCFA (XOF) et écritures comptables suivent les règles de traçabilité SYSCOHADA.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Text("J'ai compris")
            }
        }
    )
}

@Composable
fun AuthSupportDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = RoyalBlue) },
        title = { Text("Assistance GESTION PRO AFRICA", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Besoin d'aide pour accéder à votre espace Pressing Pro ou activer un compte gestionnaire ?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Email, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                    Text("support@gestionpro.africa", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Phone, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                    Text("+225 07 00 00 00 00 (Abidjan · Lun–Sam 8h–19h)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.HelpOutline, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(18.dp))
                    Text("Un employé/gestionnaire doit demander son activation à l'Admin Pressing de son agence.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Text("Fermer")
            }
        }
    )
}

@Preview(name = "Email Verification — Light", showBackground = true, widthDp = 390)
@Preview(name = "Email Verification — Tablet", showBackground = true, widthDp = 840)
@Composable
private fun EmailVerificationPreview() {
    PressingProTheme(themeMode = AppThemeMode.LIGHT) {
        EmailVerificationScreen(
            email = "direction@pressing-elite.ci",
            isLoading = false,
            onCheckVerified = {},
            onResendLink = {},
            onSignOut = {}
        )
    }
}
