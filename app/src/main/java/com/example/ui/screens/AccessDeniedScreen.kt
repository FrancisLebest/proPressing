package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AccessPermission
import com.example.data.ActiveRoleSession
import com.example.ui.components.RoleBadgeChip
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer

/**
 * Dedicated RBAC security guard screen displayed on any unauthorized route attempt.
 * Automatically logs the denied access attempt to Firestore audit_logs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccessDeniedScreen(
    session: ActiveRoleSession,
    attemptedRoute: String,
    requiredPermission: AccessPermission?,
    onLogAccessDenied: (route: String, requiredPermission: AccessPermission?) -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(attemptedRoute, session.role, requiredPermission) {
        onLogAccessDenied(attemptedRoute, requiredPermission)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        CrimsonContainer.copy(alpha = 0.25f)
                    )
                )
            )
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("access_denied_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, CrimsonDanger.copy(alpha = 0.30f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CrimsonContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GppBad,
                        contentDescription = "Accès refusé",
                        tint = CrimsonDanger,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = "Accès non autorisé",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MidnightNavy,
                    textAlign = TextAlign.Center
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RoleBadgeChip(role = session.role)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SoftBlueContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = session.isolatedAgency ?: "Multi-agences",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            )
                        }
                    }
                }

                Text(
                    text = if (requiredPermission != null) {
                        "Votre profil « ${session.role.labelFr} » ne dispose pas du privilège requis (« ${requiredPermission.labelFr} ») pour accéder à ce module."
                    } else {
                        "Ce module ne fait pas partie du périmètre autorisé pour votre profil « ${session.role.labelFr} »."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider()

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AfricaGold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AfricaGold.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = "Journalisation sécurité",
                            tint = AfricaGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Conformité RBAC : cette tentative d'accès a été enregistrée automatiquement dans le journal d'audit sécurité.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = onBackToDashboard,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("access_denied_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Retour à mon tableau de bord",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
