package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.formatDateMillis
import com.example.core.formatMoney
import com.example.core.openMapIntent
import com.example.core.shareTextIntent
import com.example.data.AccessPermission
import com.example.data.AccountingEntry
import com.example.data.ActiveRoleSession
import com.example.data.AuthUserProfile
import com.example.data.ClaimRecord
import com.example.data.ClientRecord
import com.example.data.CompanyConfig
import com.example.data.OrderStatus
import com.example.data.PressingOrder
import com.example.data.ServiceItem
import com.example.data.SubscriptionPlan
import com.example.ui.components.GestionProBrandHeader
import com.example.ui.components.HorizontalOrderTimeline
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.PaymentStatusChip
import com.example.ui.components.RoleBadgeChip
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.RoyalBlue
import com.example.ui.viewmodel.AiStreamState
import com.example.ui.viewmodel.AuthUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    aiState: AiStreamState,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onAskQuestion: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var prompt by remember { mutableStateOf("") }
    val canSeeFinances = activeSession.has(AccessPermission.VIEW_FINANCIAL_KPIS)

    val quickPrompts = if (canSeeFinances) {
        listOf(
            "Analyse ma rentabilité et mes créances clients",
            "Quelles commandes risquent d'être en retard ?",
            "Prévision de mes besoins en lessive et stocks",
            "Génère un rapport financier exécutif en français",
            "Propose 3 idées de promotions pour fidéliser mes clients"
        )
    } else {
        listOf(
            "Quelles commandes risquent d'être en retard ?",
            "Priorités contrôle qualité et retouches SAV aujourd'hui",
            "Conseils détachage pour tissus délicats (bazin, soie)",
            "Comment réduire les réclamations qualité en atelier ?"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AfricaGold)
                        Text("Assistant IA Pressing Pro", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy, titleContentColor = Color.White)
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PastelBlueTint,
                border = BorderStroke(1.dp, PastelBlueBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Contexte IA strictement isolé", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightNavy)
                        Text(
                            if (canSeeFinances) activeSession.scopeLabel()
                            else "${activeSession.scopeLabel()} · Données financières masquées",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlue
                        )
                    }
                    RoleBadgeChip(role = activeSession.role)
                }
            }

            Surface(shape = RoundedCornerShape(14.dp), color = AmberContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Assistant opérationnel connecté en temps réel à vos données isolées Firestore. Toute décision financière ou traitement textile critique nécessite validation humaine.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Text("Analyses rapides :", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                quickPrompts.forEach { q ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            prompt = q
                            onAskQuestion(q)
                        },
                        label = { Text(q, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Posez une question sur votre pressing...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input"),
                    maxLines = 3
                )
                Button(
                    onClick = { if (prompt.isNotBlank()) onAskQuestion(prompt.trim()) },
                    enabled = prompt.isNotBlank() && aiState !is AiStreamState.Loading,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("ai_send_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Envoyer")
                }
            }

            when (aiState) {
                AiStreamState.Idle -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "Sélectionnez une analyse rapide ci-dessus ou interrogez directement vos données de commandes, caisse, stocks et rentabilité.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                AiStreamState.Loading -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            Text("Analyse Gemini en cours sur vos données...")
                        }
                    }
                }
                is AiStreamState.Streaming -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, RoyalBlue)
                    ) {
                        Text(aiState.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                    }
                }
                is AiStreamState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, EmeraldSuccess)
                    ) {
                        Text(aiState.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                    }
                }
                is AiStreamState.Error -> {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(aiState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(14.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientPortalScreen(
    company: CompanyConfig,
    orders: List<PressingOrder>,
    services: List<ServiceItem>,
    onCreateClaim: (ClaimRecord) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var trackingQuery by remember { mutableStateOf("") }
    var showSupportTicketDialog by remember { mutableStateOf(false) }

    val matchedOrder = remember(trackingQuery, orders) {
        if (trackingQuery.isBlank()) orders.firstOrNull()
        else orders.firstOrNull {
            it.orderNumber.contains(trackingQuery, ignoreCase = true) ||
                it.clientPhone.contains(trackingQuery, ignoreCase = true) ||
                it.clientName.contains(trackingQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portail Client · ${company.companyName}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy, titleContentColor = Color.White)
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mockup-faithful Public Hero Banner: "Vos vêtements entre de bonnes mains !"
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightNavy)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GestionProBrandHeader(compact = true)
                        Text(
                            text = "Vos vêtements entre de bonnes mains !",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Un service rapide, propre et professionnel à ${company.commune.ifBlank { company.city }}. Suivez vos commandes en temps réel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD5E1FF)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showSupportTicketDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AfricaGold, contentColor = MidnightNavy),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Demander une collecte / Support →", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = trackingQuery,
                    onValueChange = { trackingQuery = it },
                    label = { Text("Suivre ma commande (N° ticket ou téléphone)") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (matchedOrder != null) {
                item {
                    val stepIdx = matchedOrder.orderStatusEnum().stepIndex
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, RoyalBlue),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(matchedOrder.orderNumber, fontWeight = FontWeight.ExtraBold, color = RoyalBlue)
                                OrderStatusChip(matchedOrder.orderStatusEnum(), matchedOrder.isLate())
                            }
                            Text("Client : ${matchedOrder.clientName}", fontWeight = FontWeight.SemiBold)
                            Text("Disponibilité estimée : ${formatDateMillis(matchedOrder.dueDateMillis)}", style = MaterialTheme.typography.bodySmall)
                            HorizontalOrderTimeline(currentStepIndex = stepIdx)
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                PaymentStatusChip(matchedOrder.paymentStatusEnum())
                                Text("Reste : ${formatMoney(matchedOrder.remainingAmount, company.currency)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Localisation & Horaires du Pressing", fontWeight = FontWeight.Bold)
                        Text("${company.address.ifBlank { company.commune }}, ${company.city}", style = MaterialTheme.typography.bodySmall)
                        Text("Horaires : ${company.openingHours}", style = MaterialTheme.typography.bodySmall)
                        Text("Contact : ${company.phone}", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                        OutlinedButton(
                            onClick = { openMapIntent(context, company.latitude, company.longitude, company.companyName) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Itinéraire vers le pressing")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tarifs publics (${company.currency})", fontWeight = FontWeight.Bold)
                        services.filter { it.available }.forEach { srv ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${srv.name} (${srv.treatmentType})", style = MaterialTheme.typography.bodySmall)
                                Text(formatMoney(srv.unitPrice, company.currency), fontWeight = FontWeight.Bold, color = RoyalBlue)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSupportTicketDialog) {
        var clientName by remember { mutableStateOf(matchedOrder?.clientName ?: "") }
        var orderNum by remember { mutableStateOf(matchedOrder?.orderNumber ?: "") }
        var subject by remember { mutableStateOf("Demande de collecte / Assistance") }
        var message by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSupportTicketDialog = false },
            title = { Text("Nouveau Ticket Support / Collecte", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Votre nom *") }, singleLine = true)
                    OutlinedTextField(value = orderNum, onValueChange = { orderNum = it }, label = { Text("N° Commande (optionnel)") }, singleLine = true)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Sujet *") }, singleLine = true)
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Votre message / Adresse de collecte *") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.isNotBlank() && subject.isNotBlank() && message.isNotBlank()) {
                            onCreateClaim(
                                ClaimRecord(
                                    orderNumber = orderNum.trim(),
                                    clientName = clientName.trim(),
                                    subject = subject.trim(),
                                    description = message.trim(),
                                    severity = "MEDIUM"
                                )
                            )
                            showSupportTicketDialog = false
                        }
                    },
                    enabled = clientName.isNotBlank() && subject.isNotBlank() && message.isNotBlank()
                ) { Text("Envoyer le message") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSupportTicketDialog = false }) { Text("Annuler") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SaasAdminScreen(
    company: CompanyConfig,
    orders: List<PressingOrder>,
    clients: List<ClientRecord>,
    transactions: List<AccountingEntry>,
    onUpdateCompany: (CompanyConfig) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.fromRaw(company.subscriptionPlan)) }
    var selectedBilling by remember { mutableStateOf(company.subscriptionBilling) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Super-Admin SaaS & Abonnements", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy, titleContentColor = Color.White)
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            var newAgencyName by remember { mutableStateOf("") }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GestionProBrandHeader(compact = true)
                    Text("Espace Tenant : ${company.companyName}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Statut SaaS : ${company.subscriptionStatus} · Formule ${selectedPlan.labelFr}", color = AfricaGold, style = MaterialTheme.typography.bodySmall)
                    Text("${company.agencies.size} entité(s)/agence(s) · ${clients.size} clients · ${orders.size} commandes · ${transactions.size} écritures", color = Color(0xFFCCD9FF), style = MaterialTheme.typography.labelSmall)
                }
            }

            // Multi-Entity / Isolated Agencies Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, PastelBlueBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Business, contentDescription = null, tint = RoyalBlue)
                        Text("Entités & Agences Privées (${company.agencies.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Chaque agence dispose d'un périmètre isolé (commandes, caisse, tableau de bord et personnel dédiés).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    company.agencies.forEach { ag ->
                        val agOrders = orders.count { it.agencyName.equals(ag, ignoreCase = true) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PastelBlueTint,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ag, fontWeight = FontWeight.SemiBold, color = MidnightNavy)
                                Text("$agOrders cmd", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RoyalBlue)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newAgencyName,
                            onValueChange = { newAgencyName = it },
                            placeholder = { Text("Ex : Agence Marcory Zone 4") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("saas_new_agency_input")
                        )
                        Button(
                            onClick = {
                                val trimmed = newAgencyName.trim()
                                if (trimmed.isNotBlank() && company.agencies.none { it.equals(trimmed, ignoreCase = true) }) {
                                    onUpdateCompany(company.copy(agencies = company.agencies + trimmed))
                                    newAgencyName = ""
                                }
                            },
                            enabled = newAgencyName.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("saas_add_agency_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Ajouter une agence")
                        }
                    }
                }
            }

            Text("Formules & Tarifs SaaS Pressing Pro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SubscriptionPlan.entries.forEach { plan ->
                val isSelected = selectedPlan == plan
                Card(
                    onClick = { selectedPlan = plan },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) RoyalBlue else MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plan.labelFr, fontWeight = FontWeight.Bold)
                            Text(plan.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${formatMoney(plan.monthlyFcfa, "FCFA")}/m", fontWeight = FontWeight.ExtraBold, color = RoyalBlue)
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("MONTHLY" to "Mensuel", "QUARTERLY" to "Trimestriel", "ANNUAL" to "Annuel").forEach { (c, l) ->
                    FilterChip(selected = selectedBilling == c, onClick = { selectedBilling = c }, label = { Text(l) })
                }
            }

            Button(
                onClick = {
                    onUpdateCompany(company.copy(subscriptionPlan = selectedPlan.name, subscriptionBilling = selectedBilling))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Text("Mettre à jour la formule d'abonnement")
            }

            OutlinedButton(
                onClick = {
                    val backupSummary = buildString {
                        appendLine("{")
                        appendLine("  \"company\": \"${company.companyName}\",")
                        appendLine("  \"plan\": \"${company.subscriptionPlan}\",")
                        appendLine("  \"ordersCount\": ${orders.size},")
                        appendLine("  \"clientsCount\": ${clients.size},")
                        appendLine("  \"transactionsCount\": ${transactions.size}")
                        appendLine("}")
                    }
                    shareTextIntent(context, "Sauvegarde SaaS ${company.companyName}", backupSummary)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exporter une sauvegarde des données (JSON)")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    company: CompanyConfig,
    userProfile: AuthUserProfile,
    authUiState: AuthUiState,
    isDarkTheme: Boolean,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onToggleDarkTheme: (Boolean) -> Unit,
    onReopenSetupWizard: () -> Unit,
    onOpenRbacMatrix: () -> Unit = {},
    onUpdateDisplayName: (String) -> Unit,
    onRefreshSession: () -> Unit,
    onClearAuthMessages: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onClearAuthMessages()
        onBack()
    }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember(userProfile.displayName) { mutableStateOf(userProfile.displayLabel) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres Entreprise", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        onClearAuthMessages()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightNavy, titleContentColor = Color.White)
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active Role & Isolation Security Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PastelBlueTint,
                border = BorderStroke(1.dp, PastelBlueBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RoleBadgeChip(role = activeSession.role)
                        Text(activeSession.scopeLabel(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MidnightNavy)
                    }
                    if (activeSession.has(AccessPermission.MANAGE_STAFF_AND_ROLES)) {
                        OutlinedButton(onClick = onOpenRbacMatrix) {
                            Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Droits")
                        }
                    }
                }
            }

            // Connected Firebase Auth Account Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(RoyalBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.initials,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile.displayLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userProfile.effectiveContactLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                editedName = userProfile.displayLabel
                                showEditNameDialog = true
                            },
                            modifier = Modifier.testTag("settings_edit_profile_name_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Modifier le nom du profil", tint = RoyalBlue)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = when {
                                        userProfile.hasGoogleProvider -> "Google Sign-In · Actif"
                                        userProfile.isSyntheticPhoneAccount -> "Téléphone · Actif"
                                        else -> "E-mail & Mot de passe · Actif"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = "Compte sécurisé vérifié",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (authUiState.errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CrimsonContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authUiState.errorMessage,
                                color = CrimsonDanger,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    if (authUiState.infoMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authUiState.infoMessage,
                                color = EmeraldSuccess,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onRefreshSession,
                        enabled = !authUiState.isProfileUpdating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_refresh_session_button")
                    ) {
                        if (authUiState.isProfileUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Synchronisation...")
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Actualiser la session Firebase Auth")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(company.companyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${company.legalName} · RCCM ${company.rccm.ifBlank { "Non renseigné" }}", style = MaterialTheme.typography.bodySmall)
                    Text("${company.address.ifBlank { company.commune }}, ${company.city} (${company.country})", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = RoyalBlue)
                        Column {
                            Text("Mode Sombre", fontWeight = FontWeight.Bold)
                            Text("Basculer entre thème clair et nuit", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Switch(checked = isDarkTheme, onCheckedChange = onToggleDarkTheme)
                }
            }

            Button(
                onClick = onReopenSetupWizard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_reopen_setup_button"),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Modifier la configuration complète (10 étapes)")
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_sign_out_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDanger),
                border = BorderStroke(1.dp, CrimsonDanger)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Se déconnecter", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Modifier le nom du profil", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Nom complet") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_edit_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDisplayName(editedName)
                        showEditNameDialog = false
                    },
                    modifier = Modifier.testTag("settings_save_name_button")
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
