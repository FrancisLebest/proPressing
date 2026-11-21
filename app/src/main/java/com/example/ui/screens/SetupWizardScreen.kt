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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.formatMoney
import com.example.data.CompanyConfig
import com.example.data.EmployeeRecord
import com.example.data.EmployeeRole
import com.example.data.PaymentMethod
import com.example.data.ServiceItem
import com.example.data.SubscriptionPlan
import com.example.ui.components.GestionProBrandHeader
import com.example.ui.components.MockupStepIndicator
import com.example.ui.theme.AfricaGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PastelBlueBorder
import com.example.ui.theme.PastelVioletBorder
import com.example.ui.theme.PastelVioletTint
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.VioletAccent

private val STEP_TITLES = listOf(
    "1. Identité & Géolocalisation",
    "2. Exploitation & Agences",
    "3. Services & Catalogue",
    "4. Tarification & Taxes",
    "5. Délais de traitement",
    "6. Moyens de paiement",
    "7. Documents & Reçus",
    "8. Personnel initial",
    "9. Notifications clients",
    "10. Abonnement & Activation"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupWizardScreen(
    existingConfig: CompanyConfig?,
    services: List<ServiceItem>,
    employees: List<EmployeeRecord>,
    isBusy: Boolean,
    onSaveStep: (CompanyConfig, Boolean) -> Unit,
    onSeedStarterServices: () -> Unit,
    onAddService: (ServiceItem) -> Unit,
    onAddEmployee: (EmployeeRecord) -> Unit,
    onCompleteSetup: (CompanyConfig) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf((existingConfig?.setupStep?.toInt() ?: 1).coerceIn(1, 10)) }

    // Step 1: Identity & GPS
    var companyName by remember { mutableStateOf(existingConfig?.companyName ?: "") }
    var legalName by remember { mutableStateOf(existingConfig?.legalName ?: "SARL") }
    var rccm by remember { mutableStateOf(existingConfig?.rccm ?: "") }
    var nif by remember { mutableStateOf(existingConfig?.nif ?: "") }
    var description by remember { mutableStateOf(existingConfig?.description ?: "Pressing, laverie et blanchisserie moderne") }
    var phone by remember { mutableStateOf(existingConfig?.phone ?: "+225 07 ") }
    var whatsapp by remember { mutableStateOf(existingConfig?.whatsapp ?: "+225 07 ") }
    var email by remember { mutableStateOf(existingConfig?.email ?: "") }
    var country by remember { mutableStateOf(existingConfig?.country ?: "Côte d'Ivoire") }
    var city by remember { mutableStateOf(existingConfig?.city ?: "Abidjan") }
    var commune by remember { mutableStateOf(existingConfig?.commune ?: "Cocody") }
    var address by remember { mutableStateOf(existingConfig?.address ?: "") }
    var latitude by remember { mutableDoubleStateOf(existingConfig?.latitude ?: 5.359952) }
    var longitude by remember { mutableDoubleStateOf(existingConfig?.longitude ?: -4.008256) }
    var gpsStatus by remember { mutableStateOf("") }

    // Step 2: Operations & Agencies
    var openingHours by remember { mutableStateOf(existingConfig?.openingHours ?: "Lun-Sam : 07h30 - 19h30") }
    var agencies by remember { mutableStateOf(existingConfig?.agencies?.ifEmpty { listOf("Agence Principale - Cocody") } ?: listOf("Agence Principale - Cocody")) }
    var newAgencyInput by remember { mutableStateOf("") }

    // Step 3: Quick add custom service
    var quickSrvName by remember { mutableStateOf("") }
    var quickSrvPrice by remember { mutableStateOf("1500") }

    // Step 4: Pricing & Tax
    var currency by remember { mutableStateOf(existingConfig?.currency ?: "FCFA") }
    var taxRateStr by remember { mutableStateOf((existingConfig?.taxRatePercent ?: 0.0).toString()) }
    var expressPctStr by remember { mutableStateOf((existingConfig?.expressSurchargePercent ?: 50.0).toString()) }

    // Step 5: Delays
    var stdDelayStr by remember { mutableStateOf((existingConfig?.standardDelayHours ?: 48L).toString()) }
    var expDelayStr by remember { mutableStateOf((existingConfig?.expressDelayHours ?: 24L).toString()) }

    // Step 6: Payments
    var acceptedPayments by remember {
        mutableStateOf(
            existingConfig?.acceptedPayments?.ifEmpty {
                listOf("ESPECES", "ORANGE_MONEY", "MTN_MOMO", "WAVE", "MOOV_MONEY")
            } ?: listOf("ESPECES", "ORANGE_MONEY", "MTN_MOMO", "WAVE", "MOOV_MONEY")
        )
    }

    // Step 7: Documents & Receipts
    var orderPrefix by remember { mutableStateOf(existingConfig?.orderPrefix ?: "PRESS-2026") }
    var receiptFormat by remember { mutableStateOf(existingConfig?.receiptFormat ?: "THERMAL_80") }
    var receiptFooter by remember {
        mutableStateOf(
            existingConfig?.receiptFooter ?: "Merci pour votre confiance. Vêtements conservés 30 jours maximum."
        )
    }

    // Step 8: Initial Staff
    var empName by remember { mutableStateOf("") }
    var empPhone by remember { mutableStateOf("") }
    var empRole by remember { mutableStateOf(EmployeeRole.CAISSIER) }
    var empAgency by remember(agencies) { mutableStateOf(agencies.firstOrNull() ?: "Agence Principale") }

    // Step 9: Notifications
    var smsEnabled by remember { mutableStateOf(existingConfig?.smsNotificationsEnabled ?: true) }
    var whatsappEnabled by remember { mutableStateOf(existingConfig?.whatsappNotificationsEnabled ?: true) }

    // Step 10: Subscription Plan
    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.fromRaw(existingConfig?.subscriptionPlan ?: "BUSINESS")) }
    var selectedBilling by remember { mutableStateOf(existingConfig?.subscriptionBilling ?: "MONTHLY") }

    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentStep) {
        if (currentStep == 3 && services.isEmpty()) {
            onSeedStarterServices()
        }
    }

    @SuppressLint("MissingPermission")
    fun captureGpsLocation() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (loc != null) {
            latitude = loc.latitude
            longitude = loc.longitude
            gpsStatus = "Position capturée : ${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}"
        } else {
            gpsStatus = "Signal GPS indisponible : coordonnées Abidjan conservées (${"%.4f".format(latitude)}, ${"%.4f".format(longitude)})"
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) captureGpsLocation()
        else gpsStatus = "Autorisation GPS refusée : vous pouvez continuer avec votre adresse."
    }

    fun buildConfig(stepNum: Int, completed: Boolean): CompanyConfig = CompanyConfig(
        companyName = companyName.trim().ifBlank { "Pressing Nouvelle Étoile" },
        legalName = legalName,
        rccm = rccm,
        nif = nif,
        description = description,
        phone = phone,
        whatsapp = whatsapp,
        email = email,
        country = country,
        city = city,
        commune = commune,
        address = address,
        latitude = latitude,
        longitude = longitude,
        currency = currency,
        taxRatePercent = taxRateStr.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 0.0,
        openingHours = openingHours,
        standardDelayHours = stdDelayStr.toLongOrNull()?.coerceIn(1L, 720L) ?: 48L,
        expressDelayHours = expDelayStr.toLongOrNull()?.coerceIn(1L, 168L) ?: 24L,
        expressSurchargePercent = expressPctStr.toDoubleOrNull()?.coerceIn(0.0, 200.0) ?: 50.0,
        orderPrefix = orderPrefix.trim().ifBlank { "PRESS-2026" },
        receiptFooter = receiptFooter,
        receiptFormat = receiptFormat,
        acceptedPayments = acceptedPayments.ifEmpty { listOf("ESPECES") },
        agencies = agencies.ifEmpty { listOf("Agence Principale") },
        smsNotificationsEnabled = smsEnabled,
        whatsappNotificationsEnabled = whatsappEnabled,
        subscriptionPlan = selectedPlan.name,
        subscriptionBilling = selectedBilling,
        subscriptionStatus = "ACTIVE",
        setupStep = stepNum.toLong(),
        setupCompleted = completed
    )

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(MidnightNavy, RoyalBlue)))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GestionProBrandHeader(compact = true)
                        TextButton(
                            onClick = {
                                if (services.isEmpty()) onSeedStarterServices()
                                onCompleteSetup(buildConfig(10, true))
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("setup_quick_complete_button")
                        ) {
                            Text("Config Express", color = AfricaGold, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Configuration Obligatoire · Étape $currentStep / 10",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFD6E4FF),
                        softWrap = true
                    )
                    Text(
                        text = STEP_TITLES[currentStep - 1],
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        softWrap = true
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { currentStep / 10f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = AfricaGold,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
                // Crisp white horizontal 4-phase stepper matching the Gestion Pro Africa creation mockup
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val mockupPhase = when (currentStep) {
                        in 1..2 -> 0
                        in 3..5 -> 1
                        in 6..9 -> 2
                        else -> 3
                    }
                    MockupStepIndicator(
                        steps = listOf("Entreprise", "Exploitation", "Paiements & RH", "Abonnement"),
                        currentStepIndex = mockupPhase,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val narrowBottom = maxWidth < 360.dp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = {
                                    validationError = null
                                    currentStep -= 1
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("setup_prev_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (narrowBottom) "Retour" else "Précédent",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Button(
                            onClick = {
                                if (currentStep == 1 && companyName.trim().length < 2) {
                                    validationError = "Veuillez saisir le nom commercial de votre pressing."
                                    return@Button
                                }
                                validationError = null
                                if (currentStep < 10) {
                                    val nextStep = currentStep + 1
                                    onSaveStep(buildConfig(nextStep, false), true)
                                    currentStep = nextStep
                                } else {
                                    if (services.isEmpty()) onSeedStarterServices()
                                    onCompleteSetup(buildConfig(10, true))
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(if (currentStep > 1) 1.35f else 1f)
                                .heightIn(min = 48.dp)
                                .testTag("setup_next_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentStep == 10) EmeraldSuccess else RoyalBlue
                            )
                        ) {
                            Text(
                                text = when {
                                    currentStep == 10 && narrowBottom -> "Activer"
                                    currentStep == 10 -> "Activer mon Pressing"
                                    narrowBottom -> "Suivant"
                                    else -> "Enregistrer & Suivant"
                                },
                                fontWeight = FontWeight.Bold,
                                softWrap = true
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = if (currentStep == 10) Icons.Rounded.RocketLaunch else Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWide = maxWidth >= 780.dp
            val isCompactForm = maxWidth < 520.dp
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isWide) {
                    // Left SaaS value-proposition column matching Création_de_compte_entreprise mockup
                    Surface(
                        color = PastelVioletTint,
                        border = BorderStroke(1.dp, PastelVioletBorder),
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Le SaaS tout-en-un pour les pressings africains",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MidnightNavy
                            )
                            listOf(
                                "Multi-agences isolées & données privées",
                                "Facturation FCFA, Mobile Money & reçus QR",
                                "Stocks, consommables & suivi des machines",
                                "Clients VIP, fidélisation & portail client",
                                "Comptabilité SYSCOHADA & rapports",
                                "Rôles granulaires & accès 24h/24"
                            ).forEach { perk ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(18.dp))
                                    Text(perk, style = MaterialTheme.typography.bodyMedium, color = MidnightNavy)
                                }
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
            if (validationError != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, PastelBlueBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = STEP_TITLES[currentStep - 1],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MidnightNavy
                    )
                    when (currentStep) {
                1 -> {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Nom commercial du pressing *") },
                        placeholder = { Text("Ex. Pressing Nouvelle Étoile") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_company_name_input"),
                        singleLine = true
                    )
                    AdaptiveSetupFieldPair(
                        isCompact = isCompactForm,
                        first = { mod ->
                            OutlinedTextField(
                                value = legalName,
                                onValueChange = { legalName = it },
                                label = { Text("Forme juridique") },
                                modifier = mod,
                                singleLine = true
                            )
                        },
                        second = { mod ->
                            OutlinedTextField(
                                value = rccm,
                                onValueChange = { rccm = it },
                                label = { Text("N° RCCM") },
                                modifier = mod,
                                singleLine = true
                            )
                        }
                    )
                    AdaptiveSetupFieldPair(
                        isCompact = isCompactForm,
                        first = { mod ->
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Téléphone *") },
                                modifier = mod,
                                singleLine = true
                            )
                        },
                        second = { mod ->
                            OutlinedTextField(
                                value = whatsapp,
                                onValueChange = { whatsapp = it },
                                label = { Text("WhatsApp") },
                                modifier = mod,
                                singleLine = true
                            )
                        }
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email professionnel") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    AdaptiveSetupFieldPair(
                        isCompact = isCompactForm,
                        first = { mod ->
                            OutlinedTextField(
                                value = country,
                                onValueChange = { country = it },
                                label = { Text("Pays") },
                                modifier = mod,
                                singleLine = true
                            )
                        },
                        second = { mod ->
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("Ville") },
                                modifier = mod,
                                singleLine = true
                            )
                        }
                    )
                    AdaptiveSetupFieldPair(
                        isCompact = isCompactForm,
                        first = { mod ->
                            OutlinedTextField(
                                value = commune,
                                onValueChange = { commune = it },
                                label = { Text("Commune / Quartier") },
                                modifier = mod,
                                singleLine = true
                            )
                        },
                        second = { mod ->
                            OutlinedTextField(
                                value = nif,
                                onValueChange = { nif = it },
                                label = { Text("N° Fiscal (NIF)") },
                                modifier = mod,
                                singleLine = true
                            )
                        }
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Adresse complète") },
                        placeholder = { Text("Ex. Cocody Angré 8e tranche") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) captureGpsLocation()
                            else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("setup_gps_button")
                    ) {
                        Icon(Icons.Rounded.GpsFixed, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Capturer la position GPS du pressing", softWrap = true)
                    }
                    if (gpsStatus.isNotBlank()) {
                        Text(gpsStatus, style = MaterialTheme.typography.bodySmall, color = RoyalBlue, softWrap = true)
                    }
                }

                2 -> {
                    OutlinedTextField(
                        value = openingHours,
                        onValueChange = { openingHours = it },
                        label = { Text("Horaires d'ouverture & fermeture") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description de l'activité") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Text("Agences & Succursales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newAgencyInput,
                            onValueChange = { newAgencyInput = it },
                            label = { Text("Ajouter une agence") },
                            placeholder = { Text("Ex. Agence Plateau") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (newAgencyInput.isNotBlank()) {
                                    agencies = agencies + newAgencyInput.trim()
                                    newAgencyInput = ""
                                }
                            },
                            modifier = Modifier.sizeIn(minWidth = 52.dp, minHeight = 52.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Ajouter agence")
                        }
                    }
                    agencies.forEachIndexed { idx, agency ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = agency,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    softWrap = true,
                                    modifier = Modifier.weight(1f)
                                )
                                if (agencies.size > 1) {
                                    IconButton(
                                        onClick = { agencies = agencies.filterIndexed { i, _ -> i != idx } },
                                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Supprimer agence", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    Text(
                        "Catalogue initial modifiable (${services.size} prestations chargées)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        softWrap = true
                    )
                    Text(
                        "Les tarifs indicatifs ivoiriens sont préchargés. Ajoutez ou modifiez vos prestations dès maintenant ou dans le module Services.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true
                    )
                    val addQuickServiceAction = {
                        val p = quickSrvPrice.toLongOrNull() ?: 1000L
                        if (quickSrvName.isNotBlank()) {
                            onAddService(
                                ServiceItem(
                                    name = quickSrvName.trim(),
                                    category = "Personnalisé",
                                    treatmentType = "Lavage + Repassage",
                                    unitPrice = p,
                                    expressPrice = (p * 1.5).toLong()
                                )
                            )
                            quickSrvName = ""
                        }
                    }
                    if (isCompactForm) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = quickSrvName,
                                onValueChange = { quickSrvName = it },
                                label = { Text("Nouveau vêtement / service") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = quickSrvPrice,
                                    onValueChange = { quickSrvPrice = it.filter { c -> c.isDigit() } },
                                    label = { Text("Prix ($currency)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = addQuickServiceAction,
                                    modifier = Modifier.sizeIn(minWidth = 52.dp, minHeight = 52.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Ajouter service")
                                }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = quickSrvName,
                                onValueChange = { quickSrvName = it },
                                label = { Text("Nouveau vêtement / service") },
                                modifier = Modifier.weight(1.4f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = quickSrvPrice,
                                onValueChange = { quickSrvPrice = it.filter { c -> c.isDigit() } },
                                label = { Text("Prix ($currency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.9f),
                                singleLine = true
                            )
                            Button(
                                onClick = addQuickServiceAction,
                                modifier = Modifier.sizeIn(minWidth = 52.dp, minHeight = 52.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Ajouter service")
                            }
                        }
                    }
                    services.take(12).forEach { srv ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(srv.name, fontWeight = FontWeight.SemiBold, softWrap = true)
                                    Text(
                                        "${srv.treatmentType} · Délai ${srv.delayHours}h",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = true
                                    )
                                }
                                Text(
                                    text = formatMoney(srv.unitPrice, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue,
                                    softWrap = true
                                )
                            }
                        }
                    }
                }

                4 -> {
                    Text("Devise principale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("FCFA", "XAF", "EUR", "USD", "MAD").forEach { cur ->
                            FilterChip(
                                selected = currency == cur,
                                onClick = { currency = cur },
                                label = { Text(cur) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = taxRateStr,
                        onValueChange = { taxRateStr = it },
                        label = { Text("Taux de TVA (%) — 0 si non assujetti") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = expressPctStr,
                        onValueChange = { expressPctStr = it },
                        label = { Text("Supplément Traitement Express (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                5 -> {
                    OutlinedTextField(
                        value = stdDelayStr,
                        onValueChange = { stdDelayStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Délai standard par défaut (heures)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = expDelayStr,
                        onValueChange = { expDelayStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Délai Express (heures)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Note qualité : une commande n'est jamais annoncée comme prête uniquement par dépassement de date — la validation humaine du contrôle qualité reste obligatoire.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                6 -> {
                    Text("Modes d'encaissement acceptés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Sélectionnez tous les moyens de paiement disponibles dans votre pressing (Espèces, Mobile Money Côte d'Ivoire, Banque).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            val selected = method.name in acceptedPayments
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    acceptedPayments = if (selected) {
                                        (acceptedPayments - method.name).ifEmpty { listOf("ESPECES") }
                                    } else {
                                        acceptedPayments + method.name
                                    }
                                },
                                label = { Text(method.labelFr) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }

                7 -> {
                    OutlinedTextField(
                        value = orderPrefix,
                        onValueChange = { orderPrefix = it.uppercase().take(20) },
                        label = { Text("Préfixe automatique des commandes") },
                        placeholder = { Text("PRESS-2026") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Format d'impression des reçus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "THERMAL_58" to "Ticket 58 mm",
                            "THERMAL_80" to "Ticket 80 mm",
                            "A4_PDF" to "Facture A4 / PDF"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = receiptFormat == code,
                                onClick = { receiptFormat = code },
                                label = { Text(label) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("Mentions légales & conditions de conservation") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                8 -> {
                    Text("Équipe initiale (${employees.size} membre(s))", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = empName,
                        onValueChange = { empName = it },
                        label = { Text("Nom complet de l'employé") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = empPhone,
                        onValueChange = { empPhone = it },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EmployeeRole.entries.forEach { r ->
                            FilterChip(
                                selected = empRole == r,
                                onClick = { empRole = r },
                                label = { Text(r.labelFr) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                    if (agencies.isNotEmpty()) {
                        Text("Agence d'affectation :", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            agencies.forEach { ag ->
                                FilterChip(
                                    selected = empAgency == ag,
                                    onClick = { empAgency = ag },
                                    label = { Text(ag) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (empName.isNotBlank()) {
                                onAddEmployee(
                                    EmployeeRecord(
                                        fullName = empName.trim(),
                                        phone = empPhone.trim(),
                                        role = empRole.name,
                                        agencyName = empAgency.ifBlank { agencies.firstOrNull() ?: "Agence Principale" }
                                    )
                                )
                                empName = ""
                                empPhone = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Ajouter cet employé")
                    }
                    employees.forEach { e ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(e.fullName, fontWeight = FontWeight.SemiBold, softWrap = true)
                                    Text(e.agencyName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = true)
                                }
                                Text(
                                    text = EmployeeRole.fromRaw(e.role).labelFr,
                                    color = RoyalBlue,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = true
                                )
                            }
                        }
                    }
                }

                9 -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Notifications WhatsApp clients", fontWeight = FontWeight.Bold)
                                Text(
                                    "Envoi rapide du reçu et de l'alerte de linge prêt via WhatsApp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = whatsappEnabled, onCheckedChange = { whatsappEnabled = it })
                        }
                    }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Notifications SMS / Partage reçu", fontWeight = FontWeight.Bold)
                                Text(
                                    "Notification automatique lors du retrait ou de la livraison",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = smsEnabled, onCheckedChange = { smsEnabled = it })
                        }
                    }
                }

                10 -> {
                    Text("Choisissez votre formule SaaS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SubscriptionPlan.entries.forEach { plan ->
                        val isSelected = selectedPlan == plan
                        Card(
                            onClick = { selectedPlan = plan },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) RoyalBlue else MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = plan.labelFr,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            softWrap = true
                                        )
                                        if (plan == SubscriptionPlan.BUSINESS) {
                                            Surface(shape = RoundedCornerShape(50), color = AfricaGold) {
                                                Text(
                                                    "Recommandée",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MidnightNavy,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${formatMoney(plan.monthlyFcfa, "FCFA")} / mois",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RoyalBlue,
                                        softWrap = true
                                    )
                                }
                                Text(
                                    text = plan.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    softWrap = true
                                )
                            }
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("MONTHLY" to "Mensuel", "QUARTERLY" to "Trimestriel (-10%)", "ANNUAL" to "Annuel (-20%)").forEach { (code, label) ->
                            FilterChip(
                                selected = selectedBilling == code,
                                onClick = { selectedBilling = code },
                                label = { Text(label) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = EmeraldSuccess.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                            Text(
                                text = "Tout est prêt pour ${companyName.ifBlank { "votre pressing" }} (${agencies.size} agence(s), ${services.size} services, ${acceptedPayments.size} modes de paiement).",
                                style = MaterialTheme.typography.bodySmall,
                                softWrap = true
                            )
                        }
                    }
                }
            }
                }
            }
            Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun AdaptiveSetupFieldPair(
    isCompact: Boolean,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    if (isCompact) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}
