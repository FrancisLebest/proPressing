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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.dialPhoneIntent
import com.example.core.formatMoney
import com.example.core.openMapIntent
import com.example.core.openWhatsAppIntent
import com.example.data.AccessPermission
import com.example.data.ActiveRoleSession
import com.example.data.BillingUnit
import com.example.data.ClaimRecord
import com.example.data.CompanyConfig
import com.example.data.EmployeeRecord
import com.example.data.EmployeeRole
import com.example.data.PressingOrder
import com.example.data.ServiceItem
import com.example.data.StockItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MapPreviewCanvas
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.RoleBadgeChip
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelAmberBorder
import com.example.ui.theme.PastelAmberTint
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelBlueTint
import com.example.ui.theme.PastelGreenBorder
import com.example.ui.theme.PastelGreenTint
import com.example.ui.theme.PastelRoseBorder
import com.example.ui.theme.PastelRoseTint
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.VioletAccent

@Composable
fun MoreHubScreen(
    activeSession: ActiveRoleSession,
    onOpenModule: (String) -> Unit
) {
    data class HubItem(val route: String, val title: String, val subtitle: String, val icon: ImageVector, val color: Color)

    val allModules = listOf(
        HubItem("workshop_tracker", "Atelier & Suivi Vêtements", "Suivi pièce par pièce (Lavage, Séchage, Repassage, Prêt) + Scanner QR", Icons.Rounded.LocalLaundryService, EmeraldSuccess),
        HubItem("roles_rbac", "Rôles, Droits & Isolation", "Matrice RBAC granulaire (27 droits), entités privées & agences", Icons.Rounded.Security, RoyalBlue),
        HubItem("pos_cash", "Caisse Enregistreuse (POS)", "Touches tactiles articles & encaissement comptoir 1 clic", Icons.Rounded.PointOfSale, RoyalBlue),
        HubItem("invoices", "Facturation & Reçus", "Factures FAC-xxxx, filtres payées/impayées & export PDF", Icons.Rounded.ReceiptLong, EmeraldSuccess),
        HubItem("reports", "Rapports & Analyses", "Ventes, stocks, taux de fidélité & finances", Icons.Rounded.BarChart, VioletAccent),
        HubItem("ai_assistant", "Assistant IA Gemini", "Analyse rentabilité, prévisions & conseils", Icons.Rounded.AutoAwesome, RoyalBlue),
        HubItem("quotes", "Devis & Simulateur", "Devis pro instantané, PDF & envoi WhatsApp", Icons.Rounded.RequestQuote, RoyalBlue),
        HubItem("marketing", "Fidélité & Marketing", "Relances retrait, impayés, points VIP & promos", Icons.Rounded.Campaign, EmeraldSuccess),
        HubItem("catalog", "Services & Tarifs", "Vêtements, prix pièce/kg/m², délais, express", Icons.Rounded.Checkroom, EmeraldSuccess),
        HubItem("stocks", "Stocks & Machines", "Consommables, alertes seuils, maintenance", Icons.Rounded.Inventory2, AmberWarning),
        HubItem("deliveries", "Livraisons & GPS", "Tournées livreurs, zones & carte", Icons.Rounded.LocalShipping, VioletAccent),
        HubItem("staff", "Personnel & Équipe", "Rôles, présences, salaires & commissions", Icons.Rounded.Badge, RoyalBlue),
        HubItem("claims", "Réclamations & Qualité", "Tickets qualité, retouches & suivi client", Icons.Rounded.ReportProblem, CrimsonDanger),
        HubItem("client_portal", "Portail Client", "Suivi commande en direct & tarifs publics", Icons.Rounded.Public, EmeraldSuccess),
        HubItem("saas_admin", "Super-Admin SaaS", "Formules Starter/Business/Enterprise & Sauvegarde", Icons.Rounded.AdminPanelSettings, MidnightNavy),
        HubItem("help", "Centre d'aide & FAQ", "Guides opérationnels, SYSCOHADA & support", Icons.Rounded.HelpOutline, VioletAccent),
        HubItem("settings", "Paramètres & Setup", "Identité entreprise, reçus, thème & compte", Icons.Rounded.Settings, RoyalBlue)
    )

    val authorizedModules = remember(activeSession) {
        allModules.filter { activeSession.canAccessRoute(it.route) }
    }
    val restrictedCount = allModules.size - authorizedModules.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hub_role_isolation_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PastelBlueTint),
                border = BorderStroke(1.dp, PastelBlueBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RoleBadgeChip(role = activeSession.role)
                        Text(
                            text = activeSession.scopeLabel(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MidnightNavy
                        )
                        Text(
                            text = "${authorizedModules.size} modules autorisés" +
                                if (restrictedCount > 0) " · $restrictedCount masqués par vos privilèges" else " · Accès intégral",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = "Rôle verrouillé", tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                            Text("Verrouillé", style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(2.dp))
            Text("Modules autorisés (${authorizedModules.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Votre suite Gestion Pro Africa s'adapte dynamiquement à vos droits et à votre agence isolée.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(authorizedModules) { m ->
            Card(
                onClick = { onOpenModule(m.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hub_module_${m.route}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(m.color.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(m.icon, contentDescription = m.title, tint = m.color, modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(m.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(m.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatalogScreen(
    company: CompanyConfig,
    services: List<ServiceItem>,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onSaveService: (ServiceItem) -> Unit,
    onDeleteService: (String) -> Unit,
    onSeedDefaults: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ServiceItem?>(null) }
    val canManageCatalog = activeSession.has(AccessPermission.MANAGE_CATALOG)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Services & Tarifs (${services.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    if (canManageCatalog) {
                        IconButton(onClick = { editing = null; showDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Ajouter service", tint = AfricaGold)
                        }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
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
                            Text("Grille tarifaire · ${company.companyName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightNavy)
                            Text(activeSession.scopeLabel(), style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                        }
                        RoleBadgeChip(role = activeSession.role)
                    }
                }
            }
            if (services.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Checkroom,
                        title = "Catalogue vide",
                        subtitle = "Chargez la liste exhaustive des 43 vêtements & tarifs pressing standards ou ajoutez vos propres prestations."
                    )
                    if (canManageCatalog) {
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onSeedDefaults, modifier = Modifier.fillMaxWidth()) {
                            Text("Charger le catalogue exhaustif (43 vêtements)")
                        }
                    }
                }
            } else {
                if (canManageCatalog && services.size < 30) {
                    item {
                        OutlinedButton(
                            onClick = onSeedDefaults,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Compléter avec la liste exhaustive (43 vêtements)")
                        }
                    }
                }
                items(services, key = { it.id }) { srv ->
                    Card(
                        onClick = { if (canManageCatalog) { editing = srv; showDialog = true } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(srv.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${srv.category} · ${srv.treatmentType} (${BillingUnit.fromRaw(srv.billingUnit).labelFr})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Délai ${srv.delayHours}h · Express ${formatMoney(srv.expressPrice, company.currency)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (srv.available) EmeraldSuccess else CrimsonDanger
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    formatMoney(srv.unitPrice, company.currency),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoyalBlue
                                )
                                if (canManageCatalog) {
                                    IconButton(onClick = { onDeleteService(srv.id) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Supprimer", tint = CrimsonDanger, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var category by remember { mutableStateOf(editing?.category ?: "Vêtements") }
        var treatment by remember { mutableStateOf(editing?.treatmentType ?: "Lavage + Repassage") }
        var unit by remember { mutableStateOf(BillingUnit.fromRaw(editing?.billingUnit ?: "PIECE")) }
        var priceStr by remember { mutableStateOf((editing?.unitPrice ?: 1000L).toString()) }
        var expressStr by remember { mutableStateOf((editing?.expressPrice ?: 1500L).toString()) }
        var delayStr by remember { mutableStateOf((editing?.delayHours ?: 48L).toString()) }
        var available by remember { mutableStateOf(editing?.available ?: true) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "Nouveau service" else "Modifier service", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom article / prestation *") }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie") }, singleLine = true)
                    OutlinedTextField(value = treatment, onValueChange = { treatment = it }, label = { Text("Type de traitement") }, singleLine = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BillingUnit.entries.forEach { u ->
                            FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u.labelFr) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Prix (${company.currency})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = expressStr,
                            onValueChange = { expressStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Express") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = delayStr,
                        onValueChange = { delayStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Délai standard (heures)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Disponible à la commande")
                        Switch(checked = available, onCheckedChange = { available = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveService(
                                ServiceItem(
                                    id = editing?.id ?: "",
                                    name = name.trim(),
                                    category = category.trim(),
                                    treatmentType = treatment.trim(),
                                    billingUnit = unit.name,
                                    unitPrice = priceStr.toLongOrNull() ?: 1000L,
                                    expressPrice = expressStr.toLongOrNull() ?: 1500L,
                                    delayHours = (delayStr.toLongOrNull() ?: 48L).coerceAtLeast(1L),
                                    available = available
                                )
                            )
                            showDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Annuler") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StocksScreen(
    company: CompanyConfig,
    stocks: List<StockItem>,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onSaveStock: (StockItem) -> Unit,
    onDeleteStock: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StockItem?>(null) }
    val canEditStocks = activeSession.has(AccessPermission.ADVANCE_PRODUCTION) || activeSession.has(AccessPermission.CONFIGURE_PRINTER_AND_AGENCY)
    val showFinancials = activeSession.canViewFinancialAmounts
    val lowCount = stocks.count { it.isLowStock() || it.machineStatus != "OPERATIONAL" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stocks & Machines (${stocks.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    if (canEditStocks) {
                        IconButton(onClick = { editing = null; showDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Ajouter stock", tint = AfricaGold)
                        }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
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
                            Text("Consommables & Parc Machines", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightNavy)
                            Text(activeSession.scopeLabel(), style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                        }
                        RoleBadgeChip(role = activeSession.role)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PastelGreenTint,
                        border = BorderStroke(1.dp, PastelGreenBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Articles suivis", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess, fontWeight = FontWeight.SemiBold)
                            Text("${stocks.size}", style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily), fontWeight = FontWeight.ExtraBold, color = MidnightNavy)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (lowCount > 0) PastelRoseTint else PastelAmberTint,
                        border = BorderStroke(1.dp, if (lowCount > 0) PastelRoseBorder else PastelAmberBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Alertes / Pannes", style = MaterialTheme.typography.labelSmall, color = if (lowCount > 0) CrimsonDanger else AmberWarning, fontWeight = FontWeight.SemiBold)
                            Text("$lowCount", style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily), fontWeight = FontWeight.ExtraBold, color = MidnightNavy)
                        }
                    }
                }
            }
            if (stocks.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Inventory2,
                        title = "Aucun consommable ou équipement",
                        subtitle = "Suivez vos stocks (lessive, détergents, cintres, housses) et la maintenance de vos laveuses/sécheuses."
                    )
                }
            } else {
                items(stocks, key = { it.id }) { st ->
                    val alert = st.isLowStock() || st.machineStatus != "OPERATIONAL"
                    Card(
                        onClick = { if (canEditStocks) { editing = st; showDialog = true } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (alert) CrimsonDanger else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = if (st.itemKind == "MACHINE") Icons.Rounded.Build else Icons.Rounded.Inventory2,
                                        contentDescription = null,
                                        tint = if (alert) CrimsonDanger else RoyalBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(st.name, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    if (st.itemKind == "MACHINE") "Machine · Statut : ${st.machineStatus}"
                                    else "${st.category} · Seuil alerte : ${st.minThreshold} ${st.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (alert) CrimsonDanger else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (st.supplierName.isNotBlank()) {
                                    Text("Fournisseur : ${st.supplierName}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${st.quantity} ${st.unit}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (alert) CrimsonDanger else EmeraldSuccess
                                )
                                if (canEditStocks) {
                                    IconButton(onClick = { onDeleteStock(st.id) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Supprimer", tint = CrimsonDanger, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog && canEditStocks) {
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var kind by remember { mutableStateOf(editing?.itemKind ?: "CONSUMABLE") }
        var category by remember { mutableStateOf(editing?.category ?: "Produits d'entretien") }
        var qtyStr by remember { mutableStateOf((editing?.quantity ?: 10L).toString()) }
        var minStr by remember { mutableStateOf((editing?.minThreshold ?: 5L).toString()) }
        var unit by remember { mutableStateOf(editing?.unit ?: "kg") }
        var costStr by remember { mutableStateOf((editing?.unitCost ?: 0L).toString()) }
        var supplier by remember { mutableStateOf(editing?.supplierName ?: "") }
        var machineStatus by remember { mutableStateOf(editing?.machineStatus ?: "OPERATIONAL") }
        var maintNotes by remember { mutableStateOf(editing?.maintenanceNotes ?: "") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "Ajouter Stock / Machine" else "Modifier", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = kind == "CONSUMABLE", onClick = { kind = "CONSUMABLE" }, label = { Text("Consommable") })
                        FilterChip(selected = kind == "MACHINE", onClick = { kind = "MACHINE" }, label = { Text("Machine") })
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Désignation *") }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = qtyStr, onValueChange = { qtyStr = it.filter { c -> c.isDigit() } }, label = { Text("Qté") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = minStr, onValueChange = { minStr = it.filter { c -> c.isDigit() } }, label = { Text("Seuil") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unité") }, modifier = Modifier.weight(0.8f), singleLine = true)
                    }
                    if (showFinancials) {
                        OutlinedTextField(value = costStr, onValueChange = { costStr = it.filter { c -> c.isDigit() } }, label = { Text("Coût unitaire (${company.currency})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                    OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Fournisseur") }, singleLine = true)
                    if (kind == "MACHINE") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("OPERATIONAL" to "Opérationnelle", "MAINTENANCE" to "Maintenance", "BREAKDOWN" to "En panne").forEach { (c, l) ->
                                FilterChip(selected = machineStatus == c, onClick = { machineStatus = c }, label = { Text(l) })
                            }
                        }
                        OutlinedTextField(value = maintNotes, onValueChange = { maintNotes = it }, label = { Text("Notes de maintenance / panne") }, minLines = 2)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveStock(
                                StockItem(
                                    id = editing?.id ?: "",
                                    name = name.trim(),
                                    itemKind = kind,
                                    category = category.trim(),
                                    quantity = qtyStr.toLongOrNull() ?: 0L,
                                    minThreshold = minStr.toLongOrNull() ?: 0L,
                                    unit = unit.trim().ifBlank { "pcs" },
                                    unitCost = costStr.toLongOrNull() ?: 0L,
                                    supplierName = supplier.trim(),
                                    machineStatus = machineStatus,
                                    maintenanceNotes = maintNotes.trim()
                                )
                            )
                            showDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Annuler") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveriesScreen(
    company: CompanyConfig,
    orders: List<PressingOrder>,
    employees: List<EmployeeRecord>,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onAssignDelivery: (PressingOrder, String, String, Boolean) -> Unit,
    onOpenOrder: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val deliveryOrders = orders.filter { it.deliveryRequested || it.deliveryDriver.isNotBlank() }
    val canDispatch = activeSession.has(AccessPermission.MANAGE_DELIVERY_RUNS)
    val toDeliverCount = deliveryOrders.count { it.status != "DELIVERED" && it.status != "CANCELLED" }
    val deliveredCount = deliveryOrders.count { it.status == "DELIVERED" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Livraisons & Géolocalisation", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
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
                            Text("Tournées GPS ($toDeliverCount en cours · $deliveredCount livrées)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightNavy)
                            Text(activeSession.scopeLabel(), style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                        }
                        RoleBadgeChip(role = activeSession.role)
                    }
                }
            }
            item {
                MapPreviewCanvas(
                    centerTitle = "${company.city} · ${company.commune}",
                    pinLabels = deliveryOrders.map { it.deliveryZone.ifBlank { it.clientName } }
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Siège ${company.companyName}", fontWeight = FontWeight.Bold)
                            Text("${company.commune}, ${company.city} (${"%.4f".format(company.latitude)}, ${"%.4f".format(company.longitude)})", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { openMapIntent(context, company.latitude, company.longitude, company.companyName) }) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Carte")
                        }
                    }
                }
            }

            if (deliveryOrders.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.LocalShipping,
                        title = "Aucune livraison planifiée",
                        subtitle = "Activez « Livraison à domicile » lors de la création ou sur la fiche d'une commande."
                    )
                }
            } else {
                items(deliveryOrders, key = { it.id }) { order ->
                    Card(
                        onClick = { onOpenOrder(order.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${order.orderNumber} · ${order.clientName}", fontWeight = FontWeight.Bold)
                                OrderStatusChip(order.orderStatusEnum(), order.isLate())
                            }
                            Text("Zone : ${order.deliveryZone.ifBlank { order.clientAddress }} · Livreur : ${order.deliveryDriver.ifBlank { "Non affecté" }}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                if (order.clientPhone.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { dialPhoneIntent(context, order.clientPhone) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) { Text("Appeler", style = MaterialTheme.typography.labelMedium) }
                                    OutlinedButton(
                                        onClick = {
                                            openWhatsAppIntent(
                                                context,
                                                order.clientPhone,
                                                "Bonjour ${order.clientName}, votre livreur ${company.companyName} est en route pour votre commande ${order.orderNumber}."
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) { Text("WhatsApp", style = MaterialTheme.typography.labelMedium) }
                                }
                                if (canDispatch && order.status != "DELIVERED" && order.status != "CANCELLED") {
                                    Button(
                                        onClick = {
                                            val defaultDriver = activeSession.operatorName.ifBlank {
                                                employees.firstOrNull { it.role == "LIVREUR" }?.fullName ?: "Livreur Pressing"
                                            }
                                            onAssignDelivery(order, order.deliveryDriver.ifBlank { defaultDriver }, order.deliveryZone.ifBlank { company.commune }, true)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) { Text("Livrer", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StaffScreen(
    company: CompanyConfig,
    employees: List<EmployeeRecord>,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onSaveEmployee: (EmployeeRecord) -> Unit,
    onDeleteEmployee: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EmployeeRecord?>(null) }
    val canManageStaff = activeSession.has(AccessPermission.MANAGE_USERS_AND_ROLES)
    val canViewSalaries = activeSession.has(AccessPermission.VIEW_HR_SALARIES)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personnel & Équipe (${employees.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    if (canManageStaff) {
                        IconButton(
                            onClick = { editing = null; showDialog = true },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Ajouter employé", tint = AfricaGold)
                        }
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
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PastelBlueTint,
                    border = BorderStroke(1.dp, PastelBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column {
                            Text("Équipe & Affectations Agences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightNavy, softWrap = true)
                            Text(activeSession.scopeLabel(), style = MaterialTheme.typography.labelSmall, color = RoyalBlue, softWrap = true)
                        }
                        RoleBadgeChip(role = activeSession.role)
                    }
                }
            }
            if (employees.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Badge,
                        title = "Aucun employé enregistré",
                        subtitle = "Ajoutez vos caissiers, opérateurs de lavage, contrôleurs qualité et livreurs."
                    )
                }
            } else {
                items(employees, key = { it.id }) { emp ->
                    val initials = emp.fullName.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "EM" }
                    Card(
                        onClick = { if (canManageStaff) { editing = emp; showDialog = true } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(SoftBlueContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(initials, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = RoyalBlue)
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(emp.fullName, fontWeight = FontWeight.Bold, softWrap = true)
                                    Text("${EmployeeRole.fromRaw(emp.role).labelFr} · ${emp.agencyName}", style = MaterialTheme.typography.bodySmall, color = RoyalBlue, softWrap = true)
                                    Text(
                                        text = if (!canViewSalaries || (emp.monthlySalary == 0L && emp.commissionRate == 0.0)) {
                                            "Rémunération : Confidentielle (accès restreint)"
                                        } else {
                                            "Salaire : ${formatMoney(emp.monthlySalary, company.currency)} · Comm. ${emp.commissionRate}%"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = true
                                    )
                                }
                            }
                            if (canManageStaff) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (emp.presentToday) "Présent aujourd'hui" else "Absent",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Switch(
                                            checked = emp.presentToday,
                                            onCheckedChange = { present -> onSaveEmployee(emp.copy(presentToday = present)) }
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteEmployee(emp.id) },
                                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Supprimer employé", tint = CrimsonDanger, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog && canManageStaff) {
        var name by remember { mutableStateOf(editing?.fullName ?: "") }
        var phone by remember { mutableStateOf(editing?.phone ?: "+225 ") }
        var role by remember { mutableStateOf(EmployeeRole.fromRaw(editing?.role ?: "OPERATEUR")) }
        var agency by remember { mutableStateOf(editing?.agencyName?.ifBlank { null } ?: activeSession.isolatedAgency ?: company.agencies.firstOrNull() ?: "Agence Principale") }
        var salaryStr by remember { mutableStateOf((editing?.monthlySalary ?: 100000L).toString()) }
        var commStr by remember { mutableStateOf((editing?.commissionRate ?: 0.0).toString()) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "Nouvel employé" else "Modifier employé", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom complet *") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Téléphone") }, singleLine = true)
                    Text("Rôle opérationnel :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EmployeeRole.entries.forEach { r ->
                            FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.labelFr) })
                        }
                    }
                    if (company.agencies.isNotEmpty()) {
                        Text("Entité / Agence d'affectation :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            company.agencies.forEach { ag ->
                                FilterChip(selected = agency == ag, onClick = { agency = ag }, label = { Text(ag) })
                            }
                        }
                    }
                    if (canViewSalaries) {
                        OutlinedTextField(value = salaryStr, onValueChange = { salaryStr = it.filter { c -> c.isDigit() } }, label = { Text("Salaire mensuel (${company.currency})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        OutlinedTextField(value = commStr, onValueChange = { commStr = it }, label = { Text("Commission (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSaveEmployee(
                                EmployeeRecord(
                                    id = editing?.id ?: "",
                                    fullName = name.trim(),
                                    phone = phone.trim(),
                                    role = role.name,
                                    agencyName = agency.ifBlank { company.agencies.firstOrNull() ?: "Agence Principale" },
                                    monthlySalary = if (canViewSalaries) salaryStr.toLongOrNull() ?: 0L else editing?.monthlySalary ?: 0L,
                                    commissionRate = if (canViewSalaries) commStr.toDoubleOrNull() ?: 0.0 else editing?.commissionRate ?: 0.0,
                                    presentToday = editing?.presentToday ?: true
                                )
                            )
                            showDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Annuler") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClaimsScreen(
    claims: List<ClaimRecord>,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onCreateClaim: (ClaimRecord) -> Unit,
    onUpdateClaim: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var showNewDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réclamations & Qualité (${claims.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showNewDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Nouvelle réclamation", tint = AfricaGold)
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
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
                            Text("Suivi Qualité & Retouches SAV", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightNavy)
                            Text(activeSession.scopeLabel(), style = MaterialTheme.typography.labelSmall, color = RoyalBlue)
                        }
                        RoleBadgeChip(role = activeSession.role)
                    }
                }
            }
            if (claims.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.ReportProblem,
                        title = "Aucune réclamation",
                        subtitle = "Suivez ici les signalements de défauts, reprises ou tickets d'assistance client."
                    )
                }
            } else {
                items(claims, key = { it.id }) { claim ->
                    val resolved = claim.status == "RESOLVED"
                    val statusFr = when (claim.status) {
                        "RESOLVED" -> "Résolue"
                        "IN_PROGRESS" -> "En cours"
                        else -> "Ouverte"
                    }
                    val severityFr = when (claim.severity) {
                        "HIGH" -> "Urgente"
                        "LOW" -> "Faible"
                        else -> "Moyenne"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(claim.subject, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(shape = RoundedCornerShape(50), color = AmberContainer) {
                                        Text(
                                            severityFr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFB45309),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(50), color = if (resolved) EmeraldContainer else CrimsonContainer) {
                                        Text(
                                            statusFr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (resolved) Color(0xFF047857) else CrimsonDanger,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text("Client : ${claim.clientName} · Cmd : ${claim.orderNumber.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                            Text(claim.description, style = MaterialTheme.typography.bodySmall)
                            if (!resolved) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onUpdateClaim(claim.id, "IN_PROGRESS", claim.severity, "Prise en charge en atelier") }) {
                                        Text("En cours")
                                    }
                                    Button(
                                        onClick = { onUpdateClaim(claim.id, "RESOLVED", claim.severity, "Résolu et validé avec le client") },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                                    ) { Text("Marquer résolue") }
                                }
                            } else if (claim.resolutionNote.isNotBlank()) {
                                Text("Résolution : ${claim.resolutionNote}", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        var clientName by remember { mutableStateOf("") }
        var orderNumber by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var severity by remember { mutableStateOf("MEDIUM") }

        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text("Nouvelle réclamation", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client *") }, singleLine = true)
                    OutlinedTextField(value = orderNumber, onValueChange = { orderNumber = it }, label = { Text("N° de commande") }, singleLine = true)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Objet *") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description détaillée *") }, minLines = 3)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("LOW" to "Faible", "MEDIUM" to "Moyenne", "HIGH" to "Urgente").forEach { (c, l) ->
                            FilterChip(selected = severity == c, onClick = { severity = c }, label = { Text(l) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.isNotBlank() && subject.isNotBlank() && desc.isNotBlank()) {
                            onCreateClaim(
                                ClaimRecord(
                                    clientName = clientName.trim(),
                                    orderNumber = orderNumber.trim(),
                                    subject = subject.trim(),
                                    description = desc.trim(),
                                    severity = severity
                                )
                            )
                            showNewDialog = false
                        }
                    },
                    enabled = clientName.isNotBlank() && subject.isNotBlank() && desc.isNotBlank()
                ) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showNewDialog = false }) { Text("Annuler") } }
        )
    }
}
