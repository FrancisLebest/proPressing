package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.generateQrMatrix
import com.example.data.AccessPermission
import com.example.data.ActiveRoleSession
import com.example.data.OrderStatus
import com.example.data.PaymentStatus
import com.example.data.WorkspaceRole
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AfricaGoldBright
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BrightAzure
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.VioletAccent

/**
 * Custom vector emblem faithful to the Gestion Pro Africa logo:
 * golden African continent silhouette + rising Royal Blue bar columns + curved orbit arc.
 */
@Composable
fun GestionProAfricaEmblem(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Stylized Africa silhouette
        val africa = Path().apply {
            moveTo(w * 0.20f, h * 0.14f)
            lineTo(w * 0.56f, h * 0.10f)
            lineTo(w * 0.78f, h * 0.24f)
            lineTo(w * 0.88f, h * 0.42f)
            lineTo(w * 0.74f, h * 0.62f)
            lineTo(w * 0.63f, h * 0.88f)
            lineTo(w * 0.46f, h * 0.90f)
            lineTo(w * 0.40f, h * 0.60f)
            lineTo(w * 0.18f, h * 0.48f)
            lineTo(w * 0.12f, h * 0.28f)
            close()
        }
        drawPath(
            path = africa,
            brush = Brush.linearGradient(
                listOf(AfricaGoldBright, AfricaGold),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Three rising growth bars inside the emblem
        val barWidth = w * 0.11f
        val baseY = h * 0.80f
        val bars = listOf(
            Triple(w * 0.34f, h * 0.26f, BrightAzure),
            Triple(w * 0.50f, h * 0.38f, RoyalBlue),
            Triple(w * 0.66f, h * 0.50f, MidnightNavy)
        )
        bars.forEach { (x, bh, col) ->
            drawRoundRect(
                color = col,
                topLeft = Offset(x, baseY - bh),
                size = Size(barWidth, bh),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }

        // Golden sweeping orbit arc
        drawArc(
            color = AfricaGoldBright,
            startAngle = 140f,
            sweepAngle = 135f,
            useCenter = false,
            topLeft = Offset(w * 0.05f, h * 0.08f),
            size = Size(w * 0.90f, h * 0.82f),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun GestionProBrandHeader(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    darkText: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GestionProAfricaEmblem(size = if (compact) 36.dp else 46.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = if (darkText) MidnightNavy else Color.White)) {
                        append("GESTION PRO ")
                    }
                    withStyle(SpanStyle(color = AfricaGold)) {
                        append("AFRICA")
                    }
                },
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                softWrap = true,
                maxLines = 2
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = RoyalBlue
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Checkroom,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Pressing Pro",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Pastel KPI card with Bezier area sparkline matching the Gestion Pro Africa executive dashboard mockup.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SparklineKpiCard(
    title: String,
    value: String,
    trendLabel: String,
    icon: ImageVector,
    accentColor: Color,
    pastelTint: Color,
    pastelBorder: Color,
    sparklinePoints: List<Float>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.35f
    val bgBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                accentColor.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.linearGradient(listOf(Color.White, pastelTint))
    }
    val borderColor = if (isDark) accentColor.copy(alpha = 0.28f) else pastelBorder
    val safePoints = if (sparklinePoints.size >= 2) sparklinePoints else listOf(0.2f, 0.45f, 0.35f, 0.7f, 0.6f, 0.9f)

    // Auto-scale monetary / numeric typography so large FCFA amounts never clip
    val valueFontSize = when {
        value.length >= 16 -> 13.5.sp
        value.length >= 12 -> 15.sp
        value.length >= 9 -> 16.5.sp
        else -> 18.sp
    }

    val cardContent: @Composable () -> Unit = {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgBrush)
                .padding(14.dp)
        ) {
            val compactCard = maxWidth < 185.dp
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (compactCard) 36.dp else 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(if (compactCard) 19.dp else 21.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            softWrap = true
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = valueFontSize,
                                lineHeight = valueFontSize * 1.25f
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            softWrap = true
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = trendLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            softWrap = true,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .width(if (compactCard) 46.dp else 56.dp)
                            .height(24.dp)
                    ) {
                        val maxP = (safePoints.maxOrNull() ?: 1f).coerceAtLeast(0.01f)
                        val minP = safePoints.minOrNull() ?: 0f
                        val range = (maxP - minP).coerceAtLeast(0.01f)
                        val stepX = size.width / (safePoints.size - 1)
                        val pts = safePoints.mapIndexed { i, v ->
                            val norm = ((v - minP) / range).coerceIn(0.08f, 0.95f)
                            Offset(i * stepX, size.height * (1f - norm))
                        }

                        val linePath = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 1 until pts.size) {
                                val prev = pts[i - 1]
                                val curr = pts[i]
                                val cx = (prev.x + curr.x) / 2f
                                cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(pts.last().x, size.height)
                            lineTo(pts.first().x, size.height)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(accentColor.copy(alpha = 0.30f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = accentColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) { cardContent() }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) { cardContent() }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    SparklineKpiCard(
        title = title,
        value = value,
        trendLabel = subtitle,
        icon = icon,
        accentColor = accentColor,
        pastelTint = accentColor.copy(alpha = 0.08f),
        pastelBorder = accentColor.copy(alpha = 0.22f),
        sparklinePoints = listOf(0.3f, 0.5f, 0.42f, 0.72f, 0.65f, 0.92f),
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
fun RoleBadgeChip(
    role: WorkspaceRole,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (role) {
        WorkspaceRole.DIRECTEUR -> SoftBlueContainer to RoyalBlue
        WorkspaceRole.GERANT_AGENCE -> Color(0xFFEDE9FE) to VioletAccent
        WorkspaceRole.CAISSIER -> EmeraldContainer to Color(0xFF047857)
        WorkspaceRole.OPERATEUR -> AmberContainer to Color(0xFFB45309)
        WorkspaceRole.CONTROLE_QUALITE -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
        WorkspaceRole.LIVREUR -> CrimsonContainer to CrimsonDanger
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = role.labelFr,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 2,
                softWrap = true
            )
        }
    }
}

@Composable
fun RestrictedAccessCard(
    session: ActiveRoleSession,
    requiredPermission: AccessPermission?,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CrimsonDanger.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CrimsonContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Accès restreint",
                        tint = CrimsonDanger,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Accès restreint par votre rôle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                RoleBadgeChip(role = session.role)
                Text(
                    text = if (requiredPermission != null) {
                        "Ce module nécessite le privilège « ${requiredPermission.labelFr} ». Vos données restent strictement isolées au périmètre : ${session.scopeLabel()}."
                    } else {
                        "Votre profil actuel ne dispose pas des droits requis pour consulter cet écran."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onBackToDashboard,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Retour à mon tableau de bord isolé", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Adaptive horizontal numbered stepper: scrollable LazyRow on narrow phones (< 420.dp)
 * so step titles are never truncated, full-width connected bar on wider screens.
 */
@Composable
fun MockupStepIndicator(
    steps: List<String>,
    currentStepIndex: Int,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    LaunchedEffect(currentStepIndex) {
        if (currentStepIndex in steps.indices) {
            lazyListState.animateScrollToItem(currentStepIndex)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 420.dp) {
            LazyRow(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                itemsIndexed(steps) { idx, label ->
                    val isCompleted = idx < currentStepIndex
                    val isActive = idx == currentStepIndex
                    val containerColor = when {
                        isActive -> RoyalBlue
                        isCompleted -> EmeraldContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    }
                    val contentColor = when {
                        isActive -> Color.White
                        isCompleted -> Color(0xFF047857)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = containerColor,
                        border = BorderStroke(
                            1.dp,
                            when {
                                isActive -> RoyalBlue
                                isCompleted -> EmeraldSuccess.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier.heightIn(min = 42.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActive -> Color.White.copy(alpha = 0.22f)
                                            isCompleted -> EmeraldSuccess
                                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isCompleted) "✓" else "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive || isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                color = contentColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                steps.forEachIndexed { idx, label ->
                    val isCompleted = idx < currentStepIndex
                    val isActive = idx == currentStepIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(
                                        if (idx == 0) Color.Transparent
                                        else if (idx <= currentStepIndex) RoyalBlue
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isActive -> RoyalBlue
                                            isCompleted -> EmeraldSuccess
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isCompleted) "✓" else "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive || isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(
                                        if (idx == steps.lastIndex) Color.Transparent
                                        else if (idx < currentStepIndex) EmeraldSuccess
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isActive -> RoyalBlue
                                isCompleted -> EmeraldSuccess
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            softWrap = true,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus, isLate: Boolean = false) {
    val (bg, fg, text) = when {
        isLate -> Triple(CrimsonContainer, CrimsonDanger, "En retard")
        status == OrderStatus.READY || status == OrderStatus.PICKED_UP || status == OrderStatus.DELIVERED ->
            Triple(EmeraldContainer, Color(0xFF047857), status.labelFr)
        status == OrderStatus.CANCELLED ->
            Triple(CrimsonContainer, CrimsonDanger, status.labelFr)
        status == OrderStatus.PENDING ->
            Triple(SoftBlueContainer, RoyalBlue, status.labelFr)
        else ->
            Triple(AmberContainer, Color(0xFFB45309), status.labelFr)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PaymentStatusChip(status: PaymentStatus) {
    val (bg, fg) = when (status) {
        PaymentStatus.PAID -> EmeraldContainer to Color(0xFF047857)
        PaymentStatus.PARTIAL -> AmberContainer to Color(0xFFB45309)
        PaymentStatus.UNPAID -> CrimsonContainer to CrimsonDanger
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text = status.labelFr,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RevenueBarChart(
    bars: List<Pair<String, Long>>,
    modifier: Modifier = Modifier
) {
    val maxVal = (bars.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier.fillMaxWidth()) {
        // Equal-width aligned value labels above bars (never overlap on 320dp phones)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bars.forEach { (_, value) ->
                val shortVal = when {
                    value >= 1_000_000L -> "${value / 1_000_000L}M"
                    value >= 1_000L -> "${value / 1_000L}k"
                    else -> "$value"
                }
                Text(
                    text = shortVal,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (value > 0L) RoyalBlue else labelColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
        ) {
            val count = bars.size.coerceAtLeast(1)
            val gap = size.width / (count * 3.2f)
            val barWidth = (size.width - gap * (count + 1)) / count
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = size.height * i / gridLines
                drawLine(
                    color = Color(0x2264748B),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            bars.forEachIndexed { idx, (_, value) ->
                val ratio = (value.toFloat() / maxVal.toFloat()).coerceIn(0.06f, 1f)
                val barHeight = size.height * ratio
                val left = gap + idx * (barWidth + gap)
                val top = size.height - barHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(BrightAzure, RoyalBlue)),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bars.forEach { (day, _) ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ServiceDonutChart(
    slices: List<Pair<String, Float>>,
    centerLabel: String,
    modifier: Modifier = Modifier
) {
    val visibleSlices = slices.filter { it.second.isFinite() && it.second > 0f }
    if (visibleSlices.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Aucune donnée disponible",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "La répartition apparaîtra après l'enregistrement des premiers paiements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val palette = listOf(RoyalBlue, AfricaGold, EmeraldSuccess, VioletAccent, AmberWarning)
    val total = visibleSlices.sumOf { it.second.toDouble() }.toFloat()

    val donutGraphic: @Composable () -> Unit = {
        Box(
            modifier = Modifier.size(116.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(102.dp)) {
                var startAngle = -90f
                val strokeWidth = 19.dp.toPx()
                visibleSlices.forEachIndexed { idx, (_, value) ->
                    val sweep = (value / total) * 360f
                    drawArc(
                        color = palette[idx % palette.size],
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2.5f).coerceAtLeast(2f),
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true
            )
        }
    }

    val legendList: @Composable (Modifier) -> Unit = { legendMod ->
        Column(
            modifier = legendMod,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleSlices.forEachIndexed { idx, (name, value) ->
                val pct = ((value / total) * 100f).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette[idx % palette.size])
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        softWrap = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 310.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                donutGraphic()
                legendList(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                donutGraphic()
                legendList(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun QrCodeCanvas(
    content: String,
    size: Dp = 132.dp,
    modifier: Modifier = Modifier
) {
    val matrix = remember(content) { generateQrMatrix(content, 21) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDCE4F2)),
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .padding(8.dp)
                .size(size)
        ) {
            val cellSize = this.size.width / matrix.size
            for (r in matrix.indices) {
                for (c in matrix[r].indices) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = MidnightNavy,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HorizontalOrderTimeline(
    currentStepIndex: Int,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        0 to "Reçue",
        2 to "Lavage",
        4 to "Repassage",
        6 to "Qualité",
        7 to "Prête"
    )
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 360.dp) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(steps) { idx, (stepThreshold, label) ->
                    val isCompleted = currentStepIndex >= stepThreshold
                    val isCurrent = currentStepIndex >= stepThreshold &&
                        (idx == steps.lastIndex || currentStepIndex < steps[idx + 1].first)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 28.dp else 22.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCurrent -> RoyalBlue
                                        isCompleted -> EmeraldSuccess
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCompleted) "✓" else "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCompleted || isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isCurrent -> RoyalBlue
                                isCompleted -> EmeraldSuccess
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEachIndexed { idx, (stepThreshold, label) ->
                    val isCompleted = currentStepIndex >= stepThreshold
                    val isCurrent = currentStepIndex >= stepThreshold &&
                        (idx == steps.lastIndex || currentStepIndex < steps[idx + 1].first)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 28.dp else 22.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCurrent -> RoyalBlue
                                        isCompleted -> EmeraldSuccess
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCompleted) "✓" else "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCompleted || isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isCurrent -> RoyalBlue
                                isCompleted -> EmeraldSuccess
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            softWrap = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapPreviewCanvas(
    centerTitle: String,
    pinLabels: List<String>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFEAF3FA),
        border = BorderStroke(1.dp, Color(0xFFC5D8EC)),
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(color = Color(0xFFEBF4E6))
                drawCircle(
                    color = Color(0xFFC4E3F8),
                    radius = size.height * 0.65f,
                    center = Offset(size.width * 0.78f, size.height * 0.85f)
                )
                val gridCount = 5
                for (i in 1..gridCount) {
                    val y = size.height * i / (gridCount + 1)
                    drawLine(Color(0xFFFFFFFF), Offset(0f, y), Offset(size.width, y), strokeWidth = 3.dp.toPx())
                    val x = size.width * i / (gridCount + 1)
                    drawLine(Color(0xFFFFFFFF), Offset(x, 0f), Offset(x, size.height), strokeWidth = 3.dp.toPx())
                }
                val hq = Offset(size.width * 0.22f, size.height * 0.34f)
                val dest = Offset(size.width * 0.76f, size.height * 0.70f)
                val mid = Offset(size.width * 0.50f, size.height * 0.45f)
                drawLine(RoyalBlue, hq, mid, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                drawLine(RoyalBlue, mid, dest, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)

                drawCircle(Color.White, radius = 11.dp.toPx(), center = hq)
                drawCircle(MidnightNavy, radius = 8.dp.toPx(), center = hq)

                drawCircle(Color.White, radius = 11.dp.toPx(), center = mid)
                drawCircle(EmeraldSuccess, radius = 7.dp.toPx(), center = mid)

                drawCircle(Color.White, radius = 11.dp.toPx(), center = dest)
                drawCircle(RoyalBlue, radius = 8.dp.toPx(), center = dest)
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MidnightNavy.copy(alpha = 0.88f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .widthIn(max = 260.dp)
            ) {
                Text(
                    text = "📍 $centerTitle",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            if (pinLabels.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RoyalBlue,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .widthIn(max = 260.dp)
                ) {
                    Text(
                        text = "🚚 ${pinLabels.take(2).joinToString(" · ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
