package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.BluetoothThermalPrinterManager
import com.example.core.CashCloseSummary
import com.example.core.PairedBluetoothPrinter
import com.example.core.ThermalPaperWidth
import com.example.core.ThermalPrinterPrefs
import com.example.core.ThermalReceiptFormatter
import com.example.core.formatMoney
import com.example.core.printCalibratedThermalHtml
import com.example.data.AccessPermission
import com.example.data.AccountingEntry
import com.example.data.ActiveRoleSession
import com.example.data.AuthUserProfile
import com.example.data.BillingUnit
import com.example.data.CompanyConfig
import com.example.data.DefaultCatalogTemplates
import com.example.data.ServiceItem
import com.example.ui.components.RoleBadgeChip
import com.example.ui.components.SerratedThermalPaperCard
import com.example.ui.components.ThermalLinesPreviewBody
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
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.viewmodel.AuthUiState
import kotlinx.coroutines.launch

private enum class AgencySettingsTab(val label: String, val icon: ImageVector) {
    IDENTITY("Identité & Délais", Icons.Rounded.Business),
    CATALOG("Catalogue (43) & Remises", Icons.Rounded.Checkroom),
    PRINTER("Imprimante Bluetooth", Icons.Rounded.Bluetooth),
    CASH_CLOSE("Clôture Z Caisse", Icons.Rounded.PointOfSale),
    ACCOUNT("Compte", Icons.Rounded.VerifiedUser)
}

/**
 * Full Responsive Agency Administration & Cashier Settings Screen.
 * Strict RBAC:
 * - Directeur Général & Gérant d'Agence: all 5 sections (Identity, 43-item Catalog & Discount caps, Printer, Z Cash Close, Account).
 * - Caissier: ONLY Local Bluetooth Printer, Daily Z Cash Close, and Account.
 * - Opérateur / Contrôle Qualité / Livreur: blocked upstream by route RBAC.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgencySettingsScreen(
    company: CompanyConfig,
    services: List<ServiceItem>,
    transactions: List<AccountingEntry>,
    userProfile: AuthUserProfile,
    authUiState: AuthUiState,
    isDarkTheme: Boolean,
    isBusy: Boolean,
    activeSession: ActiveRoleSession,
    onSaveCompanyConfig: (CompanyConfig) -> Unit,
    onSaveService: (ServiceItem) -> Unit,
    onSeedDefaultCatalog: () -> Unit,
    onRecordDailyCashClose: (CashCloseSummary, String, () -> Unit) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onReopenSetupWizard: () -> Unit,
    onOpenRbacMatrix: () -> Unit,
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

    val availableTabs = remember(activeSession) {
        buildList {
            if (activeSession.canEditAgencySettingsAndTariffs) {
                add(AgencySettingsTab.IDENTITY)
                add(AgencySettingsTab.CATALOG)
            }
            if (activeSession.canManageLocalPrinter) {
                add(AgencySettingsTab.PRINTER)
            }
            if (activeSession.canRunCashClose) {
                add(AgencySettingsTab.CASH_CLOSE)
            }
            add(AgencySettingsTab.ACCOUNT)
        }
    }
    var selectedTab by remember(availableTabs) { mutableStateOf(availableTabs.first()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (activeSession.canEditAgencySettingsAndTariffs) "Administration Agence" else "Caisse & Imprimante",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${company.companyName} · ${activeSession.role.shortTitleFr}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD6E4FF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onClearAuthMessages()
                            onBack()
                        },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightNavy,
                    titleContentColor = Color.White
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
        ) {
            // RBAC Scope Pill + Adaptive Horizontal Section Selector
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableTabs) { tab ->
                            FilterChip(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                leadingIcon = {
                                    Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_tab_${tab.name}")
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                AgencySettingsTab.IDENTITY -> IdentityAndDelaysSection(
                    company = company,
                    activeSession = activeSession,
                    isBusy = isBusy,
                    onSave = onSaveCompanyConfig
                )
                AgencySettingsTab.CATALOG -> CatalogAndDiscountsSection(
                    company = company,
                    services = services,
                    isBusy = isBusy,
                    onSaveCompanyConfig = onSaveCompanyConfig,
                    onSaveService = onSaveService,
                    onSeedDefaultCatalog = onSeedDefaultCatalog
                )
                AgencySettingsTab.PRINTER -> BluetoothPrinterSection(
                    company = company,
                    onSavePrinterToCompany = { prefs ->
                        onSaveCompanyConfig(
                            company.copy(
                                receiptFormat = prefs.paperWidth.formatCode,
                                autoCutPaper = prefs.autoCut,
                                autoPrintLabelsOnPayment = prefs.autoPrintLabelsOnPayment,
                                defaultPrinterName = prefs.printerName,
                                defaultPrinterAddress = prefs.printerAddress
                            )
                        )
                    }
                )
                AgencySettingsTab.CASH_CLOSE -> DailyCashCloseZSection(
                    company = company,
                    transactions = transactions,
                    activeSession = activeSession,
                    isBusy = isBusy,
                    onRecordDailyCashClose = onRecordDailyCashClose
                )
                AgencySettingsTab.ACCOUNT -> AccountAndPreferencesSection(
                    company = company,
                    userProfile = userProfile,
                    authUiState = authUiState,
                    isDarkTheme = isDarkTheme,
                    activeSession = activeSession,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onReopenSetupWizard = onReopenSetupWizard,
                    onOpenRbacMatrix = onOpenRbacMatrix,
                    onUpdateDisplayName = onUpdateDisplayName,
                    onRefreshSession = onRefreshSession,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SECTION 1: IDENTITÉ DE L'AGENCE, MENTIONS LÉGALES & DÉLAIS / MAJORATIONS
// -----------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityAndDelaysSection(
    company: CompanyConfig,
    activeSession: ActiveRoleSession,
    isBusy: Boolean,
    onSave: (CompanyConfig) -> Unit
) {
    var companyName by remember(company) { mutableStateOf(company.companyName) }
    var activeAgency by remember(company) { mutableStateOf(activeSession.isolatedAgency ?: company.agencies.firstOrNull() ?: "Agence Cocody") }
    var newAgencyInput by remember { mutableStateOf("") }
    var agencies by remember(company) { mutableStateOf(company.agencies.ifEmpty { listOf("Agence Principale") }) }
    var address by remember(company) { mutableStateOf(company.address) }
    var commune by remember(company) { mutableStateOf(company.commune) }
    var city by remember(company) { mutableStateOf(company.city) }
    var phone by remember(company) { mutableStateOf(company.phone) }
    var whatsapp by remember(company) { mutableStateOf(company.whatsapp) }
    var ncc by remember(company) { mutableStateOf(company.nif) }
    var rccm by remember(company) { mutableStateOf(company.rccm) }
    var receiptFooter by remember(company) { mutableStateOf(company.receiptFooter) }
    var normalDelayStr by remember(company) { mutableStateOf(company.standardDelayHours.toString()) }
    var expressDelayStr by remember(company) { mutableStateOf(company.expressDelayHours.toString()) }
    var expressSurchargeStr by remember(company) { mutableStateOf(company.expressSurchargePercent.toInt().toString()) }
    var ultraDelayStr by remember(company) { mutableStateOf(company.ultraExpressDelayHours.toString()) }
    var ultraSurchargeStr by remember(company) { mutableStateOf(company.ultraExpressSurchargePercent.toInt().toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Identité commerciale & Agence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MidnightNavy)
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Nom commercial du pressing *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("settings_company_name_input")
                    )
                    OutlinedTextField(
                        value = activeAgency,
                        onValueChange = { activeAgency = it },
                        label = { Text("Nom de l'agence principale (ex: Agence Cocody, Agence Anyama)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    )
                    if (agencies.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            agencies.forEach { ag ->
                                FilterChip(
                                    selected = ag.equals(activeAgency, ignoreCase = true),
                                    onClick = { activeAgency = ag },
                                    label = { Text(ag) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newAgencyInput,
                            onValueChange = { newAgencyInput = it },
                            label = { Text("Ajouter une agence") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                        )
                        Button(
                            onClick = {
                                val trimmed = newAgencyInput.trim()
                                if (trimmed.isNotBlank() && agencies.none { it.equals(trimmed, ignoreCase = true) }) {
                                    agencies = agencies + trimmed
                                    activeAgency = trimmed
                                    newAgencyInput = ""
                                }
                            },
                            modifier = Modifier.heightIn(min = 52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Ajouter agence")
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Adresse précise / Repère") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    )
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth >= 460.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(value = commune, onValueChange = { commune = it }, label = { Text("Commune / Quartier") }, singleLine = true, modifier = Modifier.weight(1f).heightIn(min = 56.dp))
                                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Ville") }, singleLine = true, modifier = Modifier.weight(1f).heightIn(min = 56.dp))
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(value = commune, onValueChange = { commune = it }, label = { Text("Commune / Quartier") }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp))
                                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Ville") }, singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp))
                            }
                        }
                    }
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone agence") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    )
                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp client") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    )
                    OutlinedTextField(
                        value = ncc,
                        onValueChange = { ncc = it },
                        label = { Text("Numéro de Compte Contribuable (NCC / IFU)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    )
                    OutlinedTextField(
                        value = rccm,
                        onValueChange = { rccm = it },
                        label = { Text("RCCM (facultatif)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    )
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("Message de pied de ticket & conditions de garde") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Délais de livraison par défaut & Majorations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MidnightNavy)
                    OutlinedTextField(
                        value = normalDelayStr,
                        onValueChange = { normalDelayStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Délai Normal (heures — défaut 48h)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = expressDelayStr,
                            onValueChange = { expressDelayStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Express (h)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                        )
                        OutlinedTextField(
                            value = expressSurchargeStr,
                            onValueChange = { expressSurchargeStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Majoration (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = ultraDelayStr,
                            onValueChange = { ultraDelayStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Ultra-Express (h)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                        )
                        OutlinedTextField(
                            value = ultraSurchargeStr,
                            onValueChange = { ultraSurchargeStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Majoration (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val updatedAgencies = buildList {
                                if (activeAgency.isNotBlank()) add(activeAgency.trim())
                                agencies.forEach { a ->
                                    if (a.isNotBlank() && none { it.equals(a.trim(), ignoreCase = true) }) add(a.trim())
                                }
                            }.ifEmpty { listOf("Agence Principale") }

                            onSave(
                                company.copy(
                                    companyName = companyName.trim().ifBlank { company.companyName },
                                    agencies = updatedAgencies,
                                    address = address.trim(),
                                    commune = commune.trim(),
                                    city = city.trim(),
                                    phone = phone.trim(),
                                    whatsapp = whatsapp.trim(),
                                    nif = ncc.trim(),
                                    rccm = rccm.trim(),
                                    receiptFooter = receiptFooter.trim(),
                                    standardDelayHours = (normalDelayStr.toLongOrNull() ?: 48L).coerceIn(1L, 720L),
                                    expressDelayHours = (expressDelayStr.toLongOrNull() ?: 24L).coerceIn(1L, 168L),
                                    expressSurchargePercent = (expressSurchargeStr.toDoubleOrNull() ?: 50.0).coerceIn(0.0, 200.0),
                                    ultraExpressDelayHours = (ultraDelayStr.toLongOrNull() ?: 6L).coerceIn(1L, 168L),
                                    ultraExpressSurchargePercent = (ultraSurchargeStr.toDoubleOrNull() ?: 100.0).coerceIn(0.0, 300.0)
                                )
                            )
                        },
                        enabled = !isBusy && companyName.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("settings_save_identity_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enregistrer identité & délais", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// -----------------------------------------------------------------------------
// SECTION 2: CATALOGUE DES 43 VÊTEMENTS, TARIFS & RÈGLES DE REMISES
// -----------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogAndDiscountsSection(
    company: CompanyConfig,
    services: List<ServiceItem>,
    isBusy: Boolean,
    onSaveCompanyConfig: (CompanyConfig) -> Unit,
    onSaveService: (ServiceItem) -> Unit,
    onSeedDefaultCatalog: () -> Unit
) {
    val currency = company.currency.ifBlank { "FCFA" }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Toutes") }
    var maxCashierDiscStr by remember(company) { mutableStateOf(company.maxCashierDiscountPercent.toInt().toString()) }
    var corporateDiscStr by remember(company) { mutableStateOf(company.corporateDiscountPercent.toInt().toString()) }
    var editingService by remember { mutableStateOf<ServiceItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val catalogItems = remember(services) {
        if (services.isNotEmpty()) services else DefaultCatalogTemplates.ivorianStarterServices()
    }
    val categories = remember(catalogItems) {
        listOf("Toutes") + catalogItems.map { it.category }.distinct()
    }
    val filtered = remember(catalogItems, search, selectedCategory) {
        catalogItems.filter { srv ->
            val okCat = selectedCategory == "Toutes" || srv.category.equals(selectedCategory, ignoreCase = true)
            val q = search.trim()
            val okQ = q.isBlank() || srv.name.contains(q, true) || srv.category.contains(q, true) || srv.treatmentType.contains(q, true)
            okCat && okQ
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Discount Policy Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PastelAmberTint),
                border = BorderStroke(1.dp, PastelAmberBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Règles de remises & plafonds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MidnightNavy)
                    OutlinedTextField(
                        value = maxCashierDiscStr,
                        onValueChange = { maxCashierDiscStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Plafond remise Caissier sans validation Gérant (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("settings_cashier_discount_cap_input")
                    )
                    OutlinedTextField(
                        value = corporateDiscStr,
                        onValueChange = { corporateDiscStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Remise tarifs abonnés / entreprises (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    )
                    Button(
                        onClick = {
                            onSaveCompanyConfig(
                                company.copy(
                                    maxCashierDiscountPercent = (maxCashierDiscStr.toDoubleOrNull() ?: 10.0).coerceIn(0.0, 100.0),
                                    corporateDiscountPercent = (corporateDiscStr.toDoubleOrNull() ?: 15.0).coerceIn(0.0, 100.0)
                                )
                            )
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightNavy),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enregistrer les règles de remises", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Catalog Search, Categories & Add Action
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Catalogue des vêtements (${filtered.size}/${catalogItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MidnightNavy
                    )
                    Button(
                        onClick = {
                            editingService = null
                            showEditDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("settings_add_custom_garment_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ajouter vêtement", fontWeight = FontWeight.Bold)
                    }
                }

                if (services.size < 43) {
                    OutlinedButton(
                        onClick = onSeedDefaultCatalog,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text("Synchroniser la liste complète des 43 vêtements standards", softWrap = true)
                    }
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Rechercher (Bazin, Costume, Couette, Chaussures...)") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotBlank()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Effacer")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }
        }

        items(filtered, key = { it.id.ifBlank { "${it.category}_${it.name}" } }) { srv ->
            Card(
                onClick = {
                    editingService = srv
                    showEditDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (srv.available) MaterialTheme.colorScheme.outlineVariant else CrimsonDanger.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(srv.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightNavy, softWrap = true)
                            Text(
                                "${srv.category} · ${srv.treatmentType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true
                            )
                        }
                        Switch(
                            checked = srv.available,
                            onCheckedChange = { active -> onSaveService(srv.copy(available = active)) }
                        )
                    }
                    HorizontalDivider()
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column {
                            Text(
                                text = "Normal : ${formatMoney(srv.unitPrice, currency)} / ${BillingUnit.fromRaw(srv.billingUnit).labelFr}",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalBlue
                            )
                            Text(
                                text = "Express : ${formatMoney(srv.expressPrice, currency)} · Délai ${srv.delayHours}h",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                editingService = srv
                                showEditDialog = true
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Modifier tarif")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showEditDialog) {
        var name by remember(editingService) { mutableStateOf(editingService?.name ?: "") }
        var category by remember(editingService) { mutableStateOf(editingService?.category ?: "Chemises & Hauts") }
        var treatment by remember(editingService) { mutableStateOf(editingService?.treatmentType ?: "Lavage + Repassage") }
        var unit by remember(editingService) { mutableStateOf(BillingUnit.fromRaw(editingService?.billingUnit ?: "PIECE")) }
        var priceStr by remember(editingService) { mutableStateOf((editingService?.unitPrice ?: 1000L).toString()) }
        var expressStr by remember(editingService) { mutableStateOf((editingService?.expressPrice ?: 1500L).toString()) }
        var delayStr by remember(editingService) { mutableStateOf((editingService?.delayHours ?: 48L).toString()) }
        var available by remember(editingService) { mutableStateOf(editingService?.available ?: true) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (editingService == null) "Nouveau vêtement personnalisé" else "Modifier tarif · ${editingService?.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom du vêtement *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Type de prestation :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Lavage + Repassage", "Repassage seul", "Nettoyage à sec", "Blanchisserie au kg").forEach { preset ->
                            FilterChip(selected = treatment == preset, onClick = { treatment = preset }, label = { Text(preset) })
                        }
                    }
                    OutlinedTextField(value = treatment, onValueChange = { treatment = it }, label = { Text("Prestation détaillée") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BillingUnit.entries.forEach { u ->
                            FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u.labelFr) })
                        }
                    }
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Prix unitaire Normal ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expressStr,
                        onValueChange = { expressStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Prix Express ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = delayStr,
                        onValueChange = { delayStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Délai standard (heures)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Actif dans le catalogue")
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
                                    id = editingService?.id ?: "",
                                    name = name.trim(),
                                    category = category.trim().ifBlank { "Vêtements" },
                                    treatmentType = treatment.trim().ifBlank { "Lavage + Repassage" },
                                    billingUnit = unit.name,
                                    unitPrice = priceStr.toLongOrNull() ?: 1000L,
                                    expressPrice = expressStr.toLongOrNull() ?: 1500L,
                                    delayHours = (delayStr.toLongOrNull() ?: 48L).coerceAtLeast(1L),
                                    available = available
                                )
                            )
                            showEditDialog = false
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Annuler") }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// SECTION 3: IMPRIMANTE THERMIQUE BLUETOOTH (58mm / 80mm) & TICKET DE TEST
// -----------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BluetoothPrinterSection(
    company: CompanyConfig,
    onSavePrinterToCompany: (ThermalPrinterPrefs) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prefs by remember(company) { mutableStateOf(BluetoothThermalPrinterManager.loadPrefs(context, company)) }
    var pairedDevices by remember { mutableStateOf<List<PairedBluetoothPrinter>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var isPrintingTest by remember { mutableStateOf(false) }
    var showTestPreview by remember { mutableStateOf(false) }

    fun scanPairedPrinters() {
        val result = BluetoothThermalPrinterManager.listPairedPrinters(context)
        result.fold(
            onSuccess = { devices ->
                pairedDevices = devices
                statusIsError = false
                statusMessage = if (devices.isEmpty()) {
                    "Aucun périphérique Bluetooth appairé trouvé. Appairez votre imprimante thermique dans les paramètres Android."
                } else {
                    "${devices.size} périphérique(s) Bluetooth appairé(s) détecté(s)."
                }
            },
            onFailure = { err ->
                statusIsError = true
                statusMessage = err.localizedMessage ?: "Bluetooth indisponible."
            }
        )
    }

    val btPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            scanPairedPrinters()
        } else {
            statusIsError = true
            statusMessage = "Permission Bluetooth refusée. Autorisez « Appareils à proximité » pour détecter l'imprimante."
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PastelBlueTint),
                border = BorderStroke(1.dp, PastelBlueBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(26.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Imprimante thermique par défaut", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MidnightNavy)
                            Text(
                                text = if (prefs.printerName.isNotBlank()) "${prefs.printerName} (${prefs.printerAddress})" else "Aucune imprimante sélectionnée",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (prefs.printerName.isNotBlank()) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (BluetoothThermalPrinterManager.hasBluetoothConnectPermission(context)) {
                                scanPairedPrinters()
                            } else {
                                val perms = BluetoothThermalPrinterManager.requiredBluetoothPermissions()
                                if (perms.isNotEmpty()) btPermLauncher.launch(perms) else scanPairedPrinters()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightNavy),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_scan_bt_printers_button")
                    ) {
                        Icon(Icons.Rounded.BluetoothSearching, contentDescription = null, tint = AfricaGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Rechercher les imprimantes Bluetooth appairées", fontWeight = FontWeight.Bold, softWrap = true)
                    }

                    statusMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (statusIsError) CrimsonContainer else EmeraldContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (statusIsError) CrimsonDanger else Color(0xFF065F46),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (pairedDevices.isNotEmpty()) {
                        HorizontalDivider()
                        pairedDevices.forEach { dev ->
                            val isSelected = dev.address == prefs.printerAddress
                            Surface(
                                onClick = {
                                    val updated = prefs.copy(printerName = dev.name, printerAddress = dev.address)
                                    prefs = updated
                                    BluetoothThermalPrinterManager.savePrefs(context, updated)
                                    onSavePrinterToCompany(updated)
                                    statusIsError = false
                                    statusMessage = "Imprimante par défaut enregistrée : ${dev.name}"
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) EmeraldContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) EmeraldSuccess else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dev.name, fontWeight = FontWeight.Bold, color = MidnightNavy)
                                        Text(dev.address, style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Sélectionnée", tint = EmeraldSuccess)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Paper Width, Auto-Cut & Auto-Print Labels Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Paramètres papier & automatisation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MidnightNavy)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThermalPaperWidth.entries.forEach { widthOpt ->
                            FilterChip(
                                selected = prefs.paperWidth == widthOpt,
                                onClick = {
                                    val updated = prefs.copy(paperWidth = widthOpt)
                                    prefs = updated
                                    BluetoothThermalPrinterManager.savePrefs(context, updated)
                                    onSavePrinterToCompany(updated)
                                },
                                label = { Text(widthOpt.labelFr, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Découpe automatique du papier (ESC/POS)", fontWeight = FontWeight.SemiBold, softWrap = true)
                            Text("Envoie la commande de coupe en fin de ticket", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = prefs.autoCut,
                            onCheckedChange = { cut ->
                                val updated = prefs.copy(autoCut = cut)
                                prefs = updated
                                BluetoothThermalPrinterManager.savePrefs(context, updated)
                                onSavePrinterToCompany(updated)
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Impression auto des étiquettes à l'encaissement", fontWeight = FontWeight.SemiBold, softWrap = true)
                            Text("Ouvre directement les étiquettes vêtements 1/N après règlement", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = prefs.autoPrintLabelsOnPayment,
                            onCheckedChange = { autoLbl ->
                                val updated = prefs.copy(autoPrintLabelsOnPayment = autoLbl)
                                prefs = updated
                                BluetoothThermalPrinterManager.savePrefs(context, updated)
                                onSavePrinterToCompany(updated)
                            }
                        )
                    }

                    Button(
                        onClick = {
                            val testLines = ThermalReceiptFormatter.buildTestTicketLines(company, prefs.paperWidth)
                            if (prefs.printerAddress.isNotBlank() && BluetoothThermalPrinterManager.hasBluetoothConnectPermission(context)) {
                                scope.launch {
                                    isPrintingTest = true
                                    val bytes = ThermalReceiptFormatter.encodeEscPosBytes(
                                        lines = testLines,
                                        qrContent = "TEST|${company.companyName}|${prefs.paperWidth.widthMm}MM",
                                        paperWidth = prefs.paperWidth,
                                        autoCut = prefs.autoCut
                                    )
                                    val res = BluetoothThermalPrinterManager.sendEscPosToPrinter(context, prefs.printerAddress, bytes)
                                    isPrintingTest = false
                                    res.fold(
                                        onSuccess = {
                                            statusIsError = false
                                            statusMessage = it
                                        },
                                        onFailure = { err ->
                                            statusIsError = true
                                            statusMessage = err.localizedMessage ?: "Échec impression test."
                                            showTestPreview = true
                                        }
                                    )
                                }
                            } else {
                                showTestPreview = true
                            }
                        },
                        enabled = !isPrintingTest,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("settings_print_test_ticket_button")
                    ) {
                        if (isPrintingTest) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Impression du ticket test...")
                        } else {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Imprimer un ticket de test", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showTestPreview) {
        val testLines = remember(company, prefs.paperWidth) {
            ThermalReceiptFormatter.buildTestTicketLines(company, prefs.paperWidth)
        }
        AlertDialog(
            onDismissRequest = { showTestPreview = false },
            title = { Text("Ticket de test (${prefs.paperWidth.labelFr})", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SerratedThermalPaperCard(modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp)) {
                        ThermalLinesPreviewBody(lines = testLines, paperWidth = prefs.paperWidth)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val html = ThermalReceiptFormatter.buildCalibratedThermalHtml(
                            lines = testLines,
                            qrContent = "TEST|${company.companyName}",
                            barcodeContent = null,
                            paperWidth = prefs.paperWidth,
                            documentTitle = "Ticket_Test"
                        )
                        printCalibratedThermalHtml(context, "Ticket_Test", html, prefs.paperWidth)
                        showTestPreview = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Imprimer / PDF") }
            },
            dismissButton = {
                TextButton(onClick = { showTestPreview = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Fermer") }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// SECTION 4: CLÔTURE DE CAISSE JOURNALIÈRE (Z DE CAISSE) & CONTRÔLE DES ÉCARTS
// -----------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyCashCloseZSection(
    company: CompanyConfig,
    transactions: List<AccountingEntry>,
    activeSession: ActiveRoleSession,
    isBusy: Boolean,
    onRecordDailyCashClose: (CashCloseSummary, String, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currency = company.currency.ifBlank { "FCFA" }
    val todayKey = remember { ThermalReceiptFormatter.todayDateKey() }
    val todayLabel = remember { ThermalReceiptFormatter.todayDisplayDate() }
    val agencyName = activeSession.isolatedAgency ?: company.agencies.firstOrNull() ?: "Agence Principale"
    val isDayLocked = company.lastCashCloseDate == todayKey

    var countedCashStr by remember { mutableStateOf("") }
    var justification by remember { mutableStateOf("") }
    var printFeedback by remember { mutableStateOf<String?>(null) }

    val countedCash = countedCashStr.toLongOrNull() ?: 0L
    val summary = remember(transactions, countedCash, agencyName, todayKey) {
        ThermalReceiptFormatter.calculateDailyCashClose(
            transactions = transactions,
            countedCash = countedCash,
            agencyName = agencyName,
            targetDateKey = todayKey
        )
    }
    val canSubmitClose = !isDayLocked && !isBusy && countedCashStr.isNotBlank() &&
        (!summary.requiresJustification || justification.trim().length >= 5)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDayLocked) EmeraldContainer else PastelBlueTint,
                border = BorderStroke(1.dp, if (isDayLocked) EmeraldSuccess else PastelBlueBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isDayLocked) Icons.Rounded.Lock else Icons.Rounded.PointOfSale,
                        contentDescription = null,
                        tint = if (isDayLocked) EmeraldSuccess else RoyalBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDayLocked) "Journée du $todayLabel VERROUILLÉE (Z enregistré)" else "Clôture Journalière (Z) · $todayLabel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MidnightNavy,
                            softWrap = true
                        )
                        Text(
                            text = "$agencyName · ${summary.transactionCount} opération(s) comptabilisée(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDayLocked) Color(0xFF065F46) else RoyalBlue
                        )
                    }
                }
            }
        }

        // Daily Payment Method Breakdown Card (Stacked mobile-friendly rows)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Récapitulatif automatique par mode de paiement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MidnightNavy)
                    CashBreakdownRow("Espèces (FCFA)", summary.cashSalesTotal, currency, EmeraldSuccess)
                    CashBreakdownRow("Wave", summary.waveTotal, currency, RoyalBlue)
                    CashBreakdownRow("Orange Money", summary.orangeMoneyTotal, currency, AmberWarning)
                    CashBreakdownRow("MTN Mobile Money", summary.mtnMomoTotal, currency, Color(0xFFB45309))
                    CashBreakdownRow("Moov Money", summary.moovMoneyTotal, currency, RoyalBlue)
                    if (summary.otherElectronicTotal > 0L) {
                        CashBreakdownRow("Carte / Virement", summary.otherElectronicTotal, currency, MidnightNavy)
                    }
                    HorizontalDivider()
                    CashBreakdownRow("Total Recettes du jour", summary.totalDayRevenue, currency, RoyalBlue, bold = true)
                    CashBreakdownRow("Dépenses / Décaissements", -summary.totalExpenses, currency, CrimsonDanger)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PastelGreenTint,
                        border = BorderStroke(1.dp, PastelGreenBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Espèces théoriques attendues en tiroir", style = MaterialTheme.typography.labelSmall, color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                            Text(
                                text = formatMoney(summary.expectedCashInDrawer, currency),
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = MidnightNavy
                            )
                        }
                    }
                }
            }
        }

        // Physical Cash Count & Variance Control Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Contrôle physique du tiroir-caisse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MidnightNavy)
                    OutlinedTextField(
                        value = countedCashStr,
                        onValueChange = { countedCashStr = it.filter { c -> c.isDigit() } },
                        enabled = !isDayLocked,
                        label = { Text("Montant physique compté en espèces ($currency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("settings_cash_counted_input")
                    )

                    if (countedCashStr.isNotBlank()) {
                        val varianceBg = when {
                            summary.isBalanced -> EmeraldContainer
                            summary.isSurplus -> AmberContainer
                            else -> CrimsonContainer
                        }
                        val varianceFg = when {
                            summary.isBalanced -> Color(0xFF065F46)
                            summary.isSurplus -> Color(0xFF92400E)
                            else -> CrimsonDanger
                        }
                        val varianceTitle = when {
                            summary.isBalanced -> "Caisse équilibrée (écart nul)"
                            summary.isSurplus -> "Excédent de caisse : +${formatMoney(summary.variance, currency)}"
                            else -> "Manquant de caisse : ${formatMoney(summary.variance, currency)}"
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = varianceBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (summary.isBalanced) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = varianceFg,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(varianceTitle, fontWeight = FontWeight.Bold, color = varianceFg, softWrap = true)
                            }
                        }
                    }

                    if (summary.requiresJustification && countedCashStr.isNotBlank()) {
                        OutlinedTextField(
                            value = justification,
                            onValueChange = { justification = it },
                            enabled = !isDayLocked,
                            label = { Text("Justification obligatoire de l'écart *") },
                            placeholder = { Text("Ex: rendu monnaie, avance fournisseur, erreur saisie...") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_cash_variance_justification_input")
                        )
                    }

                    Button(
                        onClick = {
                            onRecordDailyCashClose(summary, justification.trim()) {
                                printFeedback = "Clôture Z validée, horodatée dans Firestore et journée verrouillée."
                            }
                        },
                        enabled = canSubmitClose,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("settings_validate_cash_close_button")
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isDayLocked) "Journée déjà clôturée" else "Valider & Verrouiller la journée (Z)",
                            fontWeight = FontWeight.Bold,
                            softWrap = true
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val prefs = BluetoothThermalPrinterManager.loadPrefs(context, company)
                                val zLines = ThermalReceiptFormatter.buildZCashCloseLines(
                                    company = company,
                                    summary = summary,
                                    operatorName = activeSession.operatorName,
                                    justification = justification.trim(),
                                    paperWidth = prefs.paperWidth
                                )
                                if (prefs.printerAddress.isNotBlank() && BluetoothThermalPrinterManager.hasBluetoothConnectPermission(context)) {
                                    scope.launch {
                                        val bytes = ThermalReceiptFormatter.encodeEscPosBytes(
                                            lines = zLines,
                                            qrContent = "ZCLOSE|${summary.dateKey}|${summary.totalDayRevenue}",
                                            paperWidth = prefs.paperWidth,
                                            autoCut = prefs.autoCut
                                        )
                                        val res = BluetoothThermalPrinterManager.sendEscPosToPrinter(context, prefs.printerAddress, bytes)
                                        printFeedback = res.fold({ it }, { it.localizedMessage ?: "Échec Bluetooth." })
                                    }
                                } else {
                                    printFeedback = "Configurez une imprimante Bluetooth dans l'onglet Imprimante ou utilisez l'export PDF."
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Imprimer Z (Bluetooth)")
                        }
                        OutlinedButton(
                            onClick = {
                                val prefs = BluetoothThermalPrinterManager.loadPrefs(context, company)
                                val zLines = ThermalReceiptFormatter.buildZCashCloseLines(
                                    company = company,
                                    summary = summary,
                                    operatorName = activeSession.operatorName,
                                    justification = justification.trim(),
                                    paperWidth = prefs.paperWidth
                                )
                                val html = ThermalReceiptFormatter.buildCalibratedThermalHtml(
                                    lines = zLines,
                                    qrContent = "ZCLOSE|${summary.dateKey}|${summary.totalDayRevenue}",
                                    barcodeContent = null,
                                    paperWidth = prefs.paperWidth,
                                    documentTitle = "Z_Caisse_${summary.dateKey}"
                                )
                                printCalibratedThermalHtml(context, "Z_Caisse_${summary.dateKey}", html, prefs.paperWidth)
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Exporter Z (PDF)")
                        }
                    }

                    printFeedback?.let { msg ->
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CashBreakdownRow(
    label: String,
    amount: Long,
    currency: String,
    valueColor: Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            softWrap = true
        )
        Text(
            text = formatMoney(amount, currency),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

// -----------------------------------------------------------------------------
// SECTION 5: COMPTE CONNECTÉ, APPARENCE & SÉCURITÉ
// -----------------------------------------------------------------------------
@Composable
private fun AccountAndPreferencesSection(
    company: CompanyConfig,
    userProfile: AuthUserProfile,
    authUiState: AuthUiState,
    isDarkTheme: Boolean,
    activeSession: ActiveRoleSession,
    onToggleDarkTheme: (Boolean) -> Unit,
    onReopenSetupWizard: () -> Unit,
    onOpenRbacMatrix: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onRefreshSession: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember(userProfile.displayName) { mutableStateOf(userProfile.displayLabel) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            RoleBadgeChip(role = activeSession.role)
                            Text(activeSession.scopeLabel(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MidnightNavy)
                        }
                        if (activeSession.has(AccessPermission.EDIT_PRIVILEGE_MATRIX) || activeSession.has(AccessPermission.VIEW_SECURITY_AUDIT_LOG)) {
                            OutlinedButton(onClick = onOpenRbacMatrix, modifier = Modifier.heightIn(min = 48.dp)) {
                                Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Droits")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(RoyalBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(userProfile.initials, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userProfile.displayLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(userProfile.effectiveContactLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = {
                                editedName = userProfile.displayLabel
                                showEditNameDialog = true
                            },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag("settings_edit_profile_name_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Modifier le nom du profil", tint = RoyalBlue)
                        }
                    }
                    OutlinedButton(
                        onClick = onRefreshSession,
                        enabled = !authUiState.isProfileUpdating,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_refresh_session_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Actualiser la session sécurisée")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = RoyalBlue)
                        Column {
                            Text("Mode Sombre", fontWeight = FontWeight.Bold)
                            Text("Basculer entre thème clair et nuit", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Switch(checked = isDarkTheme, onCheckedChange = onToggleDarkTheme)
                }
            }
        }

        if (activeSession.canEditAgencySettingsAndTariffs) {
            item {
                Button(
                    onClick = onReopenSetupWizard,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("settings_reopen_setup_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Assistant de configuration (10 étapes)")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("settings_sign_out_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDanger),
                border = BorderStroke(1.dp, CrimsonDanger)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Se déconnecter", fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
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
                    modifier = Modifier.fillMaxWidth().testTag("settings_edit_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDisplayName(editedName)
                        showEditNameDialog = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("settings_save_name_button")
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Annuler") }
            }
        )
    }
}
