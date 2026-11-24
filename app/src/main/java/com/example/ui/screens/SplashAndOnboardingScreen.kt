package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.PressingNotificationManager
import com.example.ui.components.GestionProAfricaEmblem
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AfricaGoldBright
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.VividRoyalBlue
import kotlinx.coroutines.delay

object OnboardingPrefs {
    private const val PREF_NAME = "gpa_pressing_pro_prefs"
    private const val KEY_ONBOARDING_DONE = "onboarding_completed_v1"
    private const val KEY_THEME_MODE = "app_theme_mode"

    fun isOnboardingCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_DONE, false)

    fun markOnboardingCompleted(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .apply()
    }

    fun getSavedThemeMode(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, null)

    fun saveThemeMode(context: Context, modeName: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, modeName)
            .apply()
    }
}

/**
 * Mockup-faithful animated Splash Screen (Screen 1):
 * Deep Midnight Navy canvas, golden GESTION PRO AFRICA emblem, Royal Blue "Pressing Pro" badge,
 * golden diagonal swooping banner revealing commercial laundry photography, and signature slogan.
 */
@Composable
fun BrandSplashScreen(
    onFinished: () -> Unit
) {
    val scaleAnim = remember { Animatable(0.86f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1f, animationSpec = tween(450, easing = FastOutSlowInEasing))
        delay(950L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MidnightNavy, DeepNavySurface, MidnightNavy)))
            .clickable { onFinished() }
            .testTag("brand_splash_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Brand Identity Block
            Column(
                modifier = Modifier
                    .padding(top = 36.dp, start = 24.dp, end = 24.dp)
                    .scale(scaleAnim.value)
                    .alpha(alphaAnim.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GestionProAfricaEmblem(size = 82.dp)
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                            append("GESTION\nPRO ")
                        }
                        withStyle(SpanStyle(color = AfricaGold, fontWeight = FontWeight.ExtraBold)) {
                            append("AFRICA")
                        }
                    },
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        letterSpacing = 0.8.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = RoyalBlue,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Checkroom,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Pressing Pro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = "Pressing  •  Laverie  •  Blanchisserie",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD5E1FF),
                    fontWeight = FontWeight.Medium
                )
            }

            // Signature Diagonal Golden Ribbon + Commercial Laundry Photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .alpha(alphaAnim.value)
            ) {
                val diagonalClip = remember {
                    GenericShape { size, _ ->
                        moveTo(0f, size.height * 0.24f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height * 0.78f)
                        lineTo(0f, size.height)
                        close()
                    }
                }
                Image(
                    painter = painterResource(id = R.drawable.img_splash_laundry),
                    contentDescription = "Atelier Pressing Pro — machines professionnelles et linge propre",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(diagonalClip)
                )
                // Signature golden sweep lines framing top and bottom edges of the photo
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val topRibbon = Path().apply {
                        moveTo(0f, size.height * 0.24f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height * 0.055f)
                        lineTo(0f, size.height * 0.295f)
                        close()
                    }
                    val bottomRibbon = Path().apply {
                        moveTo(0f, size.height * 0.945f)
                        lineTo(size.width, size.height * 0.725f)
                        lineTo(size.width, size.height * 0.78f)
                        lineTo(0f, size.height)
                        close()
                    }
                    val goldBrush = Brush.horizontalGradient(listOf(AfricaGold, AfricaGoldBright, AfricaGold))
                    drawPath(topRibbon, brush = goldBrush)
                    drawPath(bottomRibbon, brush = goldBrush)
                }
            }

            // Bottom Slogan
            Column(
                modifier = Modifier
                    .padding(bottom = 28.dp, start = 24.dp, end = 24.dp)
                    .alpha(alphaAnim.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "La gestion professionnelle,\nsimplement.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Côte d'Ivoire • Afrique francophone • FCFA",
                    style = MaterialTheme.typography.labelSmall,
                    color = AfricaGold
                )
            }
        }
    }
}

private data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val imageRes: Int,
    val imageDescription: String,
    val badgeText: String
)

/**
 * Mockup-faithful Onboarding Carousel (Screen 2):
 * Deep navy top arch framing an authentic African Pressing Pro specialist portrait,
 * crisp white bottom sheet with pager dots, Royal Blue 'Suivant'/'Commencer' CTA, and 'Passer'.
 */
@Composable
fun OnboardingCarouselScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentSlide by remember { mutableIntStateOf(0) }
    var notifGranted by remember {
        mutableStateOf(PressingNotificationManager.hasNotificationPermission(context))
    }
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }

    val slides = remember {
        listOf(
            OnboardingSlide(
                title = "Gérez votre pressing\nen toute simplicité",
                subtitle = "Commandes, clients, traitements, caisse, stocks et rapports, partout et à tout moment.",
                imageRes = R.drawable.img_onboarding_employee,
                imageDescription = "Gestionnaire Pressing Pro en uniforme dans un pressing moderne",
                badgeText = "Pressing • Laverie • Blanchisserie"
            ),
            OnboardingSlide(
                title = "Suivi atelier &\nétiquettes QR",
                subtitle = "Photographiez chaque vêtement avant lavage, suivez les 6 étapes et imprimez en thermique 58/80 mm.",
                imageRes = R.drawable.img_onboarding_atelier,
                imageDescription = "Atelier Pressing Pro avec chemises repassées sur portant",
                badgeText = "Scan QR • Contrôle Qualité • IA"
            ),
            OnboardingSlide(
                title = "Caisse Mobile Money\n& SYSCOHADA",
                subtitle = "Encaissez en Espèces, Wave, Orange Money et MTN MoMo avec mode hors connexion et multi-agences.",
                imageRes = R.drawable.img_splash_laundry,
                imageDescription = "Machines professionnelles et linge blanc plié",
                badgeText = "RBAC Multi-Agences • Cloud Sécurisé"
            )
        )
    }

    val slide = slides[currentSlide]
    val isLast = currentSlide == slides.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .testTag("onboarding_carousel_screen")
    ) {
        // Top Navy Curved Header with Full-Bleed Portrait / Visual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.15f)
                .background(MidnightNavy)
        ) {
            val archShape = remember {
                GenericShape { size, _ ->
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.86f)
                    quadraticTo(
                        size.width * 0.5f,
                        size.height * 1.04f,
                        0f,
                        size.height * 0.86f
                    )
                    close()
                }
            }

            AnimatedContent(
                targetState = slide,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(200)) },
                label = "onboarding_photo"
            ) { current ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = current.imageRes),
                        contentDescription = current.imageDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 0.dp)
                            .clip(archShape)
                    )
                    // Subtle top scrim for status bar legibility + badge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(MidnightNavy.copy(alpha = 0.72f), Color.Transparent)
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MidnightNavy.copy(alpha = 0.84f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                GestionProAfricaEmblem(size = 20.dp)
                                Text(
                                    text = current.badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AfricaGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Content Sheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.95f)
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = slide.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // On final slide, offer quick Locale badge & optional Notifications opt-in
                if (isLast) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = SoftBlueContainer) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(15.dp))
                                Text(
                                    text = "Français · Côte d'Ivoire (FCFA)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MidnightNavy
                                )
                            }
                        }
                        if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(
                                onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Activer alertes", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Surface(shape = RoundedCornerShape(50), color = EmeraldContainer) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "Alertes prêtes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF065F46),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pager Dots (matching mockup: gold active pill + muted dots)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.indices.forEach { idx ->
                        val active = idx == currentSlide
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (active) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) AfricaGold else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isLast) {
                            OnboardingPrefs.markOnboardingCompleted(context)
                            onComplete()
                        } else {
                            currentSlide += 1
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VividRoyalBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Text(
                        text = if (isLast) "Commencer avec Pressing Pro" else "Suivant",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = {
                        OnboardingPrefs.markOnboardingCompleted(context)
                        onComplete()
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = "Passer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
