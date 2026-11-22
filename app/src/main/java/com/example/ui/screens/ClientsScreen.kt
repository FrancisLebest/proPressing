package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.dialPhoneIntent
import com.example.core.formatMoney
import com.example.core.formatShortDate
import com.example.core.openMapIntent
import com.example.core.openWhatsAppIntent
import com.example.core.shareTextIntent
import com.example.data.AccessPermission
import com.example.data.ActiveRoleSession
import com.example.data.ClientCategory
import com.example.data.ClientRecord
import com.example.data.CompanyConfig
import com.example.data.PressingOrder
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.RoleBadgeChip
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClientsScreen(
    company: CompanyConfig,
    clients: List<ClientRecord>,
    orders: List<PressingOrder> = emptyList(),
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onOpenOrder: (String) -> Unit = {},
    onSaveClient: (ClientRecord) -> Unit,
    onDeleteClient: (String) -> Unit
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var editingClient by remember { mutableStateOf<ClientRecord?>(null) }
    var viewingClient by remember { mutableStateOf<ClientRecord?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val canEditClients = activeSession.has(AccessPermission.MANAGE_CLIENTS)
    val canDeleteClients = activeSession.has(AccessPermission.CANCEL_DELETE_ORDER)
    val canExportClients = activeSession.has(AccessPermission.EXPORT_FINANCIAL_REPORTS) || canEditClients
    val showFinancials = activeSession.canViewFinancialAmounts

    fun exportClientsCsv(): String = buildString {
        if (showFinancials) {
            appendLine("Nom;Telephone;WhatsApp;Email;Categorie;Adresse;Points_Fidelite;Remise_Pct;Solde_Du")
            clients.forEach { c ->
                appendLine("${c.fullName};${c.phone};${c.whatsapp};${c.email};${c.category};${c.address};${c.loyaltyPoints};${c.discountPercent};${c.balanceDue}")
            }
        } else {
            appendLine("Nom;Telephone;WhatsApp;Email;Categorie;Adresse;Points_Fidelite")
            clients.forEach { c ->
                appendLine("${c.fullName};${c.phone};${c.whatsapp};${c.email};${c.category};${c.address};${c.loyaltyPoints}")
            }
        }
    }

    val filtered = remember(clients, search, selectedCategory) {
        clients.filter { c ->
            val okSearch = search.isBlank() ||
                c.fullName.contains(search, ignoreCase = true) ||
                c.phone.contains(search, ignoreCase = true) ||
                c.address.contains(search, ignoreCase = true)
            val okCat = selectedCategory == "ALL" || c.category == selectedCategory
            okSearch && okCat
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Isolated Scope & Role Banner
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
                        Text(
                            text = "Clients & Fidélité (${clients.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MidnightNavy
                        )
                        Text(
                            text = activeSession.scopeLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlue
                        )
                    }
                    RoleBadgeChip(role = activeSession.role)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Rechercher un client, tél, quartier...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clients_search_input")
                )
                if (canExportClients) {
                    OutlinedButton(
                        onClick = { shareTextIntent(context, "Export Clients ${company.companyName} (CSV)", exportClientsCsv()) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .testTag("clients_export_csv_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = "Exporter CSV")
                    }
                }
                if (canEditClients) {
                    Button(
                        onClick = {
                            editingClient = null
                            showEditor = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .testTag("clients_add_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Nouveau client")
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == "ALL",
                    onClick = { selectedCategory = "ALL" },
                    label = { Text("Tous (${clients.size})") }
                )
                ClientCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.name,
                        onClick = { selectedCategory = cat.name },
                        label = { Text(cat.labelFr) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Rounded.People,
                    title = "Aucun client enregistré",
                    subtitle = "Ajoutez vos clients particuliers, VIP, hôtels et entreprises pour suivre leur fidélité et leurs commandes."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { client ->
                    val initials = client.fullName
                        .trim()
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }
                        .ifBlank { "CL" }

                    Card(
                        onClick = { viewingClient = client },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (client.category == "VIP") AfricaGold else SoftBlueContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (client.category == "VIP") MidnightNavy else RoyalBlue
                                        )
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(client.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            if (client.category == "VIP") {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(shape = RoundedCornerShape(50), color = AfricaGold) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Rounded.Star, contentDescription = null, tint = MidnightNavy, modifier = Modifier.size(12.dp))
                                                        Spacer(Modifier.width(2.dp))
                                                        Text("VIP", style = MaterialTheme.typography.labelSmall, color = MidnightNavy, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        Text(
                                            "${client.phone} · ${client.address.ifBlank { "Adresse non renseignée" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(shape = RoundedCornerShape(50), color = PastelBlueTint) {
                                    Text(
                                        ClientCategory.fromRaw(client.category).labelFr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RoyalBlue,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Fidélité : ${client.loyaltyPoints} pts · Remise ${client.discountPercent.toInt()}%", style = MaterialTheme.typography.labelMedium, color = EmeraldSuccess)
                                if (showFinancials && client.balanceDue > 0L) {
                                    Text(
                                        "Solde dû : ${formatMoney(client.balanceDue, company.currency)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                        color = CrimsonDanger,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = {
                                        openWhatsAppIntent(
                                            context,
                                            client.whatsapp.ifBlank { client.phone },
                                            "Bonjour ${client.fullName}, ici ${company.companyName}."
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("WhatsApp", style = MaterialTheme.typography.labelMedium) }
                                OutlinedButton(
                                    onClick = { openMapIntent(context, client.latitude, client.longitude, client.fullName) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Carte", style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.weight(1f))
                                if (canDeleteClients) {
                                    IconButton(onClick = { onDeleteClient(client.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Supprimer client", tint = CrimsonDanger)
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    viewingClient?.let { client ->
        val clientOrders = remember(orders, client) {
            orders.filter { it.clientId == client.id || (it.clientName.equals(client.fullName, ignoreCase = true) && it.clientPhone == client.phone) }
        }
        val totalSpent = clientOrders.filter { it.status != "CANCELLED" }.sumOf { it.totalAmount }
        val unpaidTotal = clientOrders.filter { it.status != "CANCELLED" }.sumOf { it.remainingAmount }

        AlertDialog(
            onDismissRequest = { viewingClient = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(client.fullName, fontWeight = FontWeight.Bold)
                        Text(ClientCategory.fromRaw(client.category).labelFr, style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                    }
                    if (canEditClients) {
                        IconButton(onClick = {
                            viewingClient = null
                            editingClient = client
                            showEditor = true
                        }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Modifier client", tint = RoyalBlue)
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Tél : ${client.phone} · WhatsApp : ${client.whatsapp.ifBlank { client.phone }}", style = MaterialTheme.typography.bodySmall)
                    if (client.email.isNotBlank()) Text("Email : ${client.email}", style = MaterialTheme.typography.bodySmall)
                    Text("Adresse : ${client.address.ifBlank { "Non renseignée" }}", style = MaterialTheme.typography.bodySmall)
                    if (client.preferences.isNotBlank()) Text("Préférences : ${client.preferences}", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                    if (client.notes.isNotBlank()) Text("Notes : ${client.notes}", style = MaterialTheme.typography.bodySmall)

                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (showFinancials) "Commandes : ${clientOrders.size} · Total : ${formatMoney(totalSpent, company.currency)}"
                                else "Commandes enregistrées : ${clientOrders.size}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("Points fidélité : ${client.loyaltyPoints} pts · Remise : ${client.discountPercent.toInt()}%", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                            if (showFinancials && unpaidTotal > 0L) {
                                Text("Reste dû : ${formatMoney(unpaidTotal, company.currency)}", style = MaterialTheme.typography.labelSmall, color = CrimsonDanger, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { dialPhoneIntent(context, client.phone) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                            Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Appeler", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { openWhatsAppIntent(context, client.whatsapp.ifBlank { client.phone }, "Bonjour ${client.fullName}, ici ${company.companyName}.") },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) { Text("WhatsApp", style = MaterialTheme.typography.labelSmall) }
                        OutlinedButton(onClick = { openMapIntent(context, client.latitude, client.longitude, client.fullName) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                            Text("GPS", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Text("Historique des commandes (${clientOrders.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (clientOrders.isEmpty()) {
                        Text("Aucune commande enregistrée pour ce client.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        clientOrders.take(8).forEach { ord ->
                            Card(
                                onClick = {
                                    viewingClient = null
                                    onOpenOrder(ord.id)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ord.orderNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                        Text("${ord.itemCount} pcs · ${formatShortDate(ord.dueDateMillis)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (showFinancials) {
                                            Text(formatMoney(ord.totalAmount, company.currency), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = RoyalBlue)
                                        }
                                        OrderStatusChip(ord.orderStatusEnum(), ord.isLate())
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingClient = null }) { Text("Fermer") }
            }
        )
    }

    if (showEditor) {
        ClientEditorDialog(
            initial = editingClient,
            onDismiss = { showEditor = false },
            onSave = { updated ->
                onSaveClient(updated)
                showEditor = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientEditorDialog(
    initial: ClientRecord?,
    onDismiss: () -> Unit,
    onSave: (ClientRecord) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(initial?.fullName ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "+225 ") }
    var whatsapp by remember { mutableStateOf(initial?.whatsapp ?: "+225 ") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var category by remember { mutableStateOf(ClientCategory.fromRaw(initial?.category ?: "PARTICULIER")) }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var discountStr by remember { mutableStateOf((initial?.discountPercent ?: 0.0).toInt().toString()) }
    var preferences by remember { mutableStateOf(initial?.preferences ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var latitude by remember { mutableDoubleStateOf(initial?.latitude ?: 5.359952) }
    var longitude by remember { mutableDoubleStateOf(initial?.longitude ?: -4.008256) }
    var gpsMsg by remember { mutableStateOf("") }

    @SuppressLint("MissingPermission")
    fun captureGps() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (loc != null) {
            latitude = loc.latitude
            longitude = loc.longitude
            gpsMsg = "GPS : ${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}"
        } else {
            gpsMsg = "GPS par défaut conservé."
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) captureGps()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau client" else "Modifier client", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Nom complet *") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Téléphone principal *") }, singleLine = true)
                OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ClientCategory.entries.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.labelFr) })
                    }
                }
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Adresse / Quartier") }, singleLine = true)
                OutlinedButton(
                    onClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) captureGps() else permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Position GPS client")
                }
                if (gpsMsg.isNotBlank()) Text(gpsMsg, style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                OutlinedTextField(
                    value = discountStr,
                    onValueChange = { discountStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Remise fidélité (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(value = preferences, onValueChange = { preferences = it }, label = { Text("Préférences lavage / repassage") }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes internes") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && phone.isNotBlank()) {
                        onSave(
                            ClientRecord(
                                id = initial?.id ?: "",
                                fullName = fullName.trim(),
                                phone = phone.trim(),
                                whatsapp = whatsapp.trim().ifBlank { phone.trim() },
                                email = email.trim(),
                                category = category.name,
                                address = address.trim(),
                                latitude = latitude,
                                longitude = longitude,
                                loyaltyPoints = initial?.loyaltyPoints ?: 0L,
                                discountPercent = (discountStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
                                creditLimit = initial?.creditLimit ?: 0L,
                                balanceDue = initial?.balanceDue ?: 0L,
                                preferences = preferences.trim(),
                                notes = notes.trim()
                            )
                        )
                    }
                },
                enabled = fullName.isNotBlank() && phone.isNotBlank()
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
