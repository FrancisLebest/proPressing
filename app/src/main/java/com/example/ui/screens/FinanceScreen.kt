package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.escapeHtml
import com.example.core.formatDateTime
import com.example.core.formatMoney
import com.example.core.nowFormatted
import com.example.core.openWhatsAppIntent
import com.example.core.printHtmlDocument
import com.example.core.shareTextIntent
import com.example.data.AccessPermission
import com.example.data.AccountingEntry
import com.example.data.ActiveRoleSession
import com.example.data.CompanyConfig
import com.example.data.PaymentMethod
import com.example.data.PressingOrder
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.RoleBadgeChip
import com.example.ui.components.SparklineKpiCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinanceScreen(
    company: CompanyConfig,
    orders: List<PressingOrder>,
    transactions: List<AccountingEntry>,
    activeSession: ActiveRoleSession = ActiveRoleSession(),
    onAddEntry: (AccountingEntry) -> Unit,
    onOpenOrder: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currency = company.currency
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var defaultType by remember { mutableStateOf("EXPENSE") }
    val canAddExpense = activeSession.has(AccessPermission.ADD_EXPENSE)
    val canManageCash = canAddExpense || activeSession.has(AccessPermission.COLLECT_PAYMENT)

    val totalRevenue = transactions.filter { it.entryType == "REVENUE" }.sumOf { it.amount }
    val totalExpenses = transactions.filter { it.entryType == "EXPENSE" || it.entryType == "REFUND" }.sumOf { it.amount }
    val netProfit = totalRevenue - totalExpenses
    val receivables = orders.filter { it.status != "CANCELLED" }.sumOf { it.remainingAmount }

    val byMethod = PaymentMethod.entries.map { method ->
        val incoming = transactions.filter { it.paymentMethod == method.name && it.entryType == "REVENUE" }.sumOf { it.amount }
        val outgoing = transactions.filter { it.paymentMethod == method.name && (it.entryType == "EXPENSE" || it.entryType == "REFUND") }.sumOf { it.amount }
        Triple(method, incoming, outgoing)
    }

    fun exportCsv(): String = buildString {
        appendLine("Date;Reference;Type;Compte_SYSCOHADA;Categorie;Libelle;Mode;Montant_$currency")
        transactions.forEach { t ->
            appendLine("${formatDateTime(t.createdAt)};${t.reference};${t.entryType};${t.syscohadaAccount};${t.category};${t.label};${t.paymentMethod};${t.amount}")
        }
    }

    fun buildFinancialReportHtml(): String = buildString {
        append("<html><body style='font-family:sans-serif;padding:24px;color:#081B4B;'>")
        append("<h2>RAPPORT FINANCIER SYSCOHADA · ${escapeHtml(company.companyName)}</h2>")
        append("<p>Édité le ${nowFormatted()} · RCCM ${escapeHtml(company.rccm.ifBlank { "-" })} · IFU ${escapeHtml(company.nif.ifBlank { "-" })}</p><hr/>")
        append("<p><b>Total Produits (Classe 7) :</b> ${formatMoney(totalRevenue, currency)}<br/>")
        append("<b>Total Charges (Classe 6) :</b> ${formatMoney(totalExpenses, currency)}<br/>")
        append("<b>RÉSULTAT NET :</b> ${formatMoney(netProfit, currency)}<br/>")
        append("<b>Créances Clients (411) :</b> ${formatMoney(receivables, currency)}</p>")
        append("<h3>Ventilation Trésorerie</h3><ul>")
        byMethod.forEach { (m, inc, out) ->
            append("<li>${escapeHtml(m.labelFr)} (Cpt ${m.syscohadaTreasuryAccount}) : Net ${formatMoney(inc - out, currency)} (Entrées ${formatMoney(inc, currency)})</li>")
        }
        append("</ul></body></html>")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Caisse") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Journal") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Bilan") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Clôture") })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Isolated Entity / Agency Scope Banner
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Comptabilité SYSCOHADA & Trésorerie",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MidnightNavy,
                                softWrap = true
                            )
                            Text(
                                text = activeSession.scopeLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalBlue,
                                softWrap = true
                            )
                        }
                        RoleBadgeChip(role = activeSession.role)
                    }
                }
            }

            // Adaptive Pastel Sparkline KPI Grid (1-col < 340dp, 2x2 phone, 4-col tablet/desktop)
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWideKpi = maxWidth >= 720.dp
                    val isUltraNarrow = maxWidth < 340.dp
                    val kpiRevenue: @Composable (Modifier) -> Unit = { m ->
                        SparklineKpiCard(
                            title = "Recettes encaissées",
                            value = formatMoney(totalRevenue, currency),
                            trendLabel = "Crédit 706",
                            icon = Icons.Rounded.TrendingUp,
                            accentColor = EmeraldSuccess,
                            pastelTint = PastelGreenTint,
                            pastelBorder = PastelGreenBorder,
                            sparklinePoints = listOf(0.32f, 0.48f, 0.44f, 0.68f, 0.76f, 0.94f),
                            modifier = m
                        )
                    }
                    val kpiExpenses: @Composable (Modifier) -> Unit = { m ->
                        SparklineKpiCard(
                            title = "Charges & Dépenses",
                            value = formatMoney(totalExpenses, currency),
                            trendLabel = "Débit 6xx",
                            icon = Icons.Rounded.TrendingDown,
                            accentColor = CrimsonDanger,
                            pastelTint = PastelRoseTint,
                            pastelBorder = PastelRoseBorder,
                            sparklinePoints = listOf(0.55f, 0.48f, 0.62f, 0.40f, 0.45f, 0.36f),
                            modifier = m
                        )
                    }
                    val kpiNet: @Composable (Modifier) -> Unit = { m ->
                        SparklineKpiCard(
                            title = "Bénéfice Net",
                            value = formatMoney(netProfit, currency),
                            trendLabel = "Résultat",
                            icon = Icons.Rounded.AccountBalance,
                            accentColor = RoyalBlue,
                            pastelTint = PastelBlueTint,
                            pastelBorder = PastelBlueBorder,
                            sparklinePoints = listOf(0.28f, 0.42f, 0.56f, 0.64f, 0.80f, 0.92f),
                            modifier = m
                        )
                    }
                    val kpiReceivables: @Composable (Modifier) -> Unit = { m ->
                        SparklineKpiCard(
                            title = "Créances clients",
                            value = formatMoney(receivables, currency),
                            trendLabel = "Compte 411",
                            icon = Icons.Rounded.Payments,
                            accentColor = AmberWarning,
                            pastelTint = PastelAmberTint,
                            pastelBorder = PastelAmberBorder,
                            sparklinePoints = listOf(0.60f, 0.52f, 0.58f, 0.46f, 0.42f, 0.38f),
                            modifier = m
                        )
                    }
                    when {
                        isWideKpi -> Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            kpiRevenue(Modifier.weight(1f))
                            kpiExpenses(Modifier.weight(1f))
                            kpiNet(Modifier.weight(1f))
                            kpiReceivables(Modifier.weight(1f))
                        }
                        isUltraNarrow -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            kpiRevenue(Modifier.fillMaxWidth())
                            kpiExpenses(Modifier.fillMaxWidth())
                            kpiNet(Modifier.fillMaxWidth())
                            kpiReceivables(Modifier.fillMaxWidth())
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                kpiRevenue(Modifier.weight(1f))
                                kpiExpenses(Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                kpiNet(Modifier.weight(1f))
                                kpiReceivables(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Action Buttons (RBAC-gated, FlowRow prevents overflow on small screens)
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (canAddExpense) {
                        Button(
                            onClick = {
                                defaultType = "EXPENSE"
                                showAddDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("finance_add_expense_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dépense", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (canManageCash) {
                        Button(
                            onClick = {
                                defaultType = "REVENUE"
                                showAddDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("finance_add_revenue_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Recette / Caisse", fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = { shareTextIntent(context, "Export Comptable SYSCOHADA CSV", exportCsv()) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("finance_export_csv_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = "Exporter CSV", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("CSV", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // Caisse & Mobile Money Breakdown
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Ventilation Trésorerie & Mobile Money", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, softWrap = true)
                                byMethod.forEach { (method, inc, out) ->
                                    val net = inc - out
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Column {
                                            Text(method.labelFr, fontWeight = FontWeight.SemiBold, softWrap = true)
                                            Text(
                                                "Compte ${method.syscohadaTreasuryAccount} · Entrées ${formatMoney(inc, currency)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                softWrap = true
                                            )
                                        }
                                        Text(
                                            formatMoney(net, currency),
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            fontWeight = FontWeight.Bold,
                                            color = if (net >= 0) EmeraldSuccess else CrimsonDanger
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Journal des opérations
                    if (transactions.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.AccountBalance,
                                title = "Journal de caisse vide",
                                subtitle = "Les paiements de commandes et dépenses s'enregistrent automatiquement ici sans suppression possible."
                            )
                        }
                    } else {
                        items(transactions, key = { it.id }) { tx ->
                            val isCredit = tx.entryType == "REVENUE" || tx.entryType == "CASH_OPEN"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(tx.label, fontWeight = FontWeight.SemiBold, softWrap = true)
                                        Text(
                                            "${tx.reference} · Cpt ${tx.syscohadaAccount} · ${PaymentMethod.fromRaw(tx.paymentMethod).labelFr}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            softWrap = true
                                        )
                                        Text(formatDateTime(tx.createdAt), style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(
                                        text = "${if (isCredit) "+" else "-"} ${formatMoney(tx.amount, currency)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCredit) EmeraldSuccess else CrimsonDanger
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Balance & Compte de Résultat SYSCOHADA
                    item {
                        val groupedAccounts = transactions.groupBy { it.syscohadaAccount }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Balance des Comptes SYSCOHADA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, softWrap = true)
                                Text(
                                    "Piste d'audit immuable (aucune suppression silencieuse autorisée). Consultez un expert-comptable pour clôture fiscale.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    softWrap = true
                                )
                                HorizontalDivider()
                                if (groupedAccounts.isEmpty()) {
                                    Text("Aucune écriture comptable pour cette période.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    groupedAccounts.forEach { (acct, entries) ->
                                        val total = entries.sumOf { it.amount }
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Compte $acct (${entries.first().category})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, softWrap = true)
                                            Text(formatMoney(total, currency), style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily), fontWeight = FontWeight.Bold, color = RoyalBlue)
                                        }
                                    }
                                }
                                HorizontalDivider()
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Total Produits (Classe 7)", fontWeight = FontWeight.SemiBold)
                                    Text(formatMoney(totalRevenue, currency), color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                }
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Total Charges (Classe 6)", fontWeight = FontWeight.SemiBold)
                                    Text(formatMoney(totalExpenses, currency), color = CrimsonDanger, fontWeight = FontWeight.Bold)
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FlowRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("RÉSULTAT NET", fontWeight = FontWeight.ExtraBold)
                                        Text(
                                            formatMoney(netProfit, currency),
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RoyalBlue
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { printHtmlDocument(context, "Rapport_SYSCOHADA_${company.companyName}", buildFinancialReportHtml()) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Imprimer / Exporter Rapport PDF", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Ouverture / Fermeture de Caisse & Créances Clients (411)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Ouverture & Fermeture de Caisse (Compte 571)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, softWrap = true)
                                Text("Enregistrez votre fonds de caisse initial et la clôture journalière dans le journal immuable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = true)
                                if (canManageCash) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                defaultType = "CASH_OPEN"
                                                showAddDialog = true
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                                        ) { Text("Ouvrir Caisse", fontWeight = FontWeight.Bold) }
                                        Button(
                                            onClick = {
                                                defaultType = "CASH_CLOSE"
                                                showAddDialog = true
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                                        ) { Text("Clôturer Caisse", fontWeight = FontWeight.Bold) }
                                    }
                                } else {
                                    Text(
                                        "Mode consultation seule : ouverture et clôture de caisse réservées aux rôles habilités.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = true
                                    )
                                }
                            }
                        }
                    }

                    val unpaidOrders = orders.filter { it.remainingAmount > 0L && it.status != "CANCELLED" }
                    item {
                        Text("Créances clients en cours (${unpaidOrders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (unpaidOrders.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.Payments,
                                title = "Aucune créance impayée",
                                subtitle = "Toutes les commandes actives sont intégralement réglées."
                            )
                        }
                    } else {
                        items(unpaidOrders, key = { "rec_${it.id}" }) { ord ->
                            Card(
                                onClick = { onOpenOrder(ord.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, AmberWarning)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("${ord.clientName} · ${ord.orderNumber}", fontWeight = FontWeight.Bold, softWrap = true)
                                        Text(
                                            formatMoney(ord.remainingAmount, currency),
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = CrimsonDanger,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "Total ${formatMoney(ord.totalAmount, currency)} · Payé ${formatMoney(ord.paidAmount, currency)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            softWrap = true
                                        )
                                        if (ord.clientPhone.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    openWhatsAppIntent(
                                                        context,
                                                        ord.clientPhone,
                                                        "Bonjour ${ord.clientName}, solde de ${formatMoney(ord.remainingAmount, currency)} en attente sur votre commande ${ord.orderNumber} chez ${company.companyName}."
                                                    )
                                                },
                                                modifier = Modifier.heightIn(min = 48.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) { Text("Relancer WhatsApp", style = MaterialTheme.typography.labelMedium) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showAddDialog) {
        var entryType by remember { mutableStateOf(defaultType) }
        var label by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(if (defaultType == "EXPENSE") "Lessive & Produits" else "Fonds de caisse / Recette") }
        var account by remember { mutableStateOf(if (defaultType == "EXPENSE") "604" else "706") }
        var amountStr by remember { mutableStateOf("") }
        var counterparty by remember { mutableStateOf("") }
        var method by remember { mutableStateOf(PaymentMethod.ESPECES) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nouvelle écriture comptable", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "EXPENSE" to "Dépense (6xx)",
                            "REVENUE" to "Recette (706)",
                            "CASH_OPEN" to "Ouverture (571)",
                            "CASH_CLOSE" to "Clôture (571)",
                            "REFUND" to "Remboursement"
                        ).forEach { (code, lbl) ->
                            FilterChip(
                                selected = entryType == code,
                                onClick = {
                                    entryType = code
                                    account = when (code) {
                                        "EXPENSE" -> "604"
                                        "CASH_OPEN", "CASH_CLOSE" -> "571"
                                        "REFUND" -> "709"
                                        else -> "706"
                                    }
                                },
                                label = { Text(lbl) }
                            )
                        }
                    }
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Libellé *") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Catégorie") }, modifier = Modifier.weight(1.2f), singleLine = true)
                        OutlinedTextField(value = account, onValueChange = { account = it }, label = { Text("Cpt SYSCOHADA") }, modifier = Modifier.weight(0.8f), singleLine = true)
                    }
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Montant ($currency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(value = counterparty, onValueChange = { counterparty = it }, label = { Text("Fournisseur / Tiers") }, singleLine = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentMethod.entries.forEach { m ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m.labelFr) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toLongOrNull() ?: 0L
                        if (label.isNotBlank() && amt > 0L) {
                            onAddEntry(
                                AccountingEntry(
                                    reference = "ECR-${System.currentTimeMillis() % 100000}",
                                    entryType = entryType,
                                    category = category.ifBlank { "Opération" },
                                    syscohadaAccount = account.ifBlank { "604" },
                                    label = label.trim(),
                                    amount = amt,
                                    paymentMethod = method.name,
                                    counterparty = counterparty.trim(),
                                    agencyName = activeSession.isolatedAgency ?: company.agencies.firstOrNull() ?: "Agence Principale"
                                )
                            )
                            showAddDialog = false
                        }
                    },
                    enabled = label.isNotBlank() && (amountStr.toLongOrNull() ?: 0L) > 0L
                ) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Annuler") } }
        )
    }
}
