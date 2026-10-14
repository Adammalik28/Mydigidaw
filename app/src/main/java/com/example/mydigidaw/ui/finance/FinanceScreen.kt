@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mydigidaw.ui.finance

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import com.example.mydigidaw.ui.components.SegmentedTabs
import com.example.mydigidaw.ui.components.SearchPill
import com.example.mydigidaw.ui.components.Spacing
import com.example.mydigidaw.ui.components.SectionTitle
import com.example.mydigidaw.ui.components.InlineEmpty
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mydigidaw.data.db.CategoryEntity
import com.example.mydigidaw.data.model.AccountType
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.data.repo.AccountBalance
import com.example.mydigidaw.ui.LocalAppContainer
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.components.AmountField
import com.example.mydigidaw.ui.components.AnimatedMoney
import com.example.mydigidaw.ui.components.AppCard
import com.example.mydigidaw.ui.components.AutoSizeText
import com.example.mydigidaw.ui.components.DateRangePickerModal
import com.example.mydigidaw.ui.components.DonutChart
import com.example.mydigidaw.ui.components.DonutSlice
import com.example.mydigidaw.ui.components.DropdownField
import com.example.mydigidaw.ui.components.EmojiBadge
import com.example.mydigidaw.ui.components.EmptyState
import com.example.mydigidaw.ui.components.IncomeExpenseBarChart
import com.example.mydigidaw.ui.components.LegendItem
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.Meter
import com.example.mydigidaw.ui.components.MoneyText
import com.example.mydigidaw.ui.components.MonthSwitcher
import com.example.mydigidaw.ui.components.Motion
import com.example.mydigidaw.ui.components.StatTile
import com.example.mydigidaw.ui.components.SwipeAction
import com.example.mydigidaw.ui.components.SwipeRow
import com.example.mydigidaw.ui.components.TrendLineChart
import com.example.mydigidaw.ui.components.digitsToLong
import com.example.mydigidaw.ui.components.enterStagger
import com.example.mydigidaw.ui.components.heroBrush
import com.example.mydigidaw.ui.components.heroDecoration
import com.example.mydigidaw.ui.components.toDigits
import com.example.mydigidaw.ui.home.InsightRow
import com.example.mydigidaw.ui.navigation.Routes
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.Money
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val tabs = listOf("Ringkasan", "Transaksi", "Statistik", "Anggaran")

@Composable
fun FinanceScreen(onNavigate: (String) -> Unit) {
    val vm = appViewModel { FinanceViewModel(it) }
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val hidden by vm.hideAmounts.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val budgetState by vm.budgets.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var editBudget by remember { mutableStateOf<BudgetStatus?>(null) }
    var newBudget by remember { mutableStateOf(false) }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            container.appScope.launch {
                val message = runCatching { container.backup.exportTransactionsCsv(uri) }
                    .fold({ "$it transaksi berhasil diekspor" }, { "Gagal mengekspor: ${it.message}" })
                withContext(Dispatchers.Main) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keuangan") },
                actions = {
                    IconButton(onClick = { vm.toggleHideAmounts() }) {
                        Icon(
                            if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (hidden) "Tampilkan nominal" else "Sembunyikan nominal",
                        )
                    }
                    IconButton(onClick = { onNavigate(Routes.ACCOUNTS) }) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = "Dompet & rekening")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            MenuEntry("Kategori", Icons.AutoMirrored.Outlined.Label) { menu = false; onNavigate(Routes.CATEGORIES) }
                            MenuEntry("Target tabungan", Icons.Outlined.Savings) { menu = false; onNavigate(Routes.GOALS) }
                            MenuEntry("Utang & piutang", Icons.Outlined.Handshake) { menu = false; onNavigate(Routes.DEBTS) }
                            MenuEntry("Transaksi berulang", Icons.Outlined.EventRepeat) { menu = false; onNavigate(Routes.RECURRING) }
                            MenuEntry("Ekspor ke CSV", Icons.Outlined.FileDownload) {
                                menu = false
                                container.appLock.allowExternalFlow()
                                csvLauncher.launch("transaksi-mydigidaw-${LocalDate.now()}.csv")
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            when (tab) {
                0, 1 -> ExtendedFloatingActionButton(
                    onClick = { onNavigate(Routes.txEdit()) },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Transaksi") },
                )
                3 -> if (budgetState?.statuses.isNullOrEmpty().not()) ExtendedFloatingActionButton(
                    onClick = { newBudget = true },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Anggaran") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SegmentedTabs(
                labels = tabs,
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { it / 6 * dir } + fadeIn(tween(Motion.MEDIUM))) togetherWith
                        (slideOutHorizontally(tween(Motion.MEDIUM)) { -it / 6 * dir } + fadeOut(tween(Motion.SHORT)))
                },
                label = "financeTab",
            ) { current ->
                when (current) {
                    0 -> OverviewTab(vm, hidden, onNavigate, onSeeAll = { tab = 1 }, onBudgets = { tab = 3 })
                    1 -> TransactionsTab(vm, hidden, onNavigate)
                    2 -> StatsTab(vm, hidden, onNavigate)
                    else -> BudgetsTab(vm, hidden, onEdit = { editBudget = it }, onAdd = { newBudget = true })
                }
            }
        }
    }

    if (newBudget || editBudget != null) {
        BudgetDialog(
            initial = editBudget,
            categories = categories,
            existingIds = budgetState?.statuses?.map { it.categoryId }?.toSet().orEmpty(),
            onDismiss = {
                newBudget = false
                editBudget = null
            },
            onSave = { id, amount -> vm.setBudget(id, amount) },
            onDelete = { vm.deleteBudget(it) },
        )
    }
}

@Composable
private fun MenuEntry(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, leadingIcon = { Icon(icon, null) }, onClick = onClick)
}

// ---------------------------------------------------------------------------------------------
// Ringkasan
// ---------------------------------------------------------------------------------------------

@Composable
private fun OverviewTab(
    vm: FinanceViewModel,
    hidden: Boolean,
    onNavigate: (String) -> Unit,
    onSeeAll: () -> Unit,
    onBudgets: () -> Unit,
) {
    val state by vm.overview.collectAsStateWithLifecycle()
    val insights by vm.insights.collectAsStateWithLifecycle()
    val s = state ?: return
    val ext = AppColors.current
    var selectedBar by remember { mutableStateOf<Int?>(null) }

    LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = Spacing.fabClearance), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            BalanceHero(
                total = s.totalBalance,
                accountCount = s.balances.count { it.account.includeInTotal },
                hidden = hidden,
                onClick = { onNavigate(Routes.ACCOUNTS) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Pemasukan",
                    value = FinanceCalc.moneyOrMasked(s.monthIncome, hidden),
                    delta = FinanceCalc.deltaLabel(s.monthIncome, s.prevIncome),
                    deltaColor = if (s.monthIncome >= s.prevIncome) ext.goodText else ext.criticalText,
                    icon = Icons.Filled.SouthWest,
                    iconTint = ext.income,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Pengeluaran",
                    value = FinanceCalc.moneyOrMasked(s.monthExpense, hidden),
                    delta = FinanceCalc.deltaLabel(s.monthExpense, s.prevExpense),
                    deltaColor = if (s.monthExpense <= s.prevExpense) ext.goodText else ext.criticalText,
                    icon = Icons.Filled.NorthEast,
                    iconTint = ext.expense,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { AccountsStrip(s.balances, hidden, onManage = { onNavigate(Routes.ACCOUNTS) }) }
        if (!hidden && insights.isNotEmpty()) {
            item { SectionTitle("Wawasan", Modifier.padding(top = 0.dp).enterStagger()) }
            item { InsightRow(insights, Modifier.enterStagger(1)) }
        }
        item {
            ChartCard(title = "6 bulan terakhir", modifier = Modifier.padding(horizontal = 16.dp)) {
                IncomeExpenseLegend()
                val index = selectedBar ?: s.sixMonths.lastIndex
                s.sixMonths.getOrNull(index)?.let { g ->
                    ChartReadout(
                        title = g.label,
                        entries = listOf(
                            Triple(ext.income, FinanceCalc.moneyOrMasked(g.income, hidden), "masuk"),
                            Triple(ext.expense, FinanceCalc.moneyOrMasked(g.expense, hidden), "keluar"),
                        ),
                    )
                }
                IncomeExpenseBarChart(s.sixMonths, selected = selectedBar, onSelect = { selectedBar = it })
            }
        }
        if (s.budgetAlerts.isNotEmpty()) {
            item { BudgetAlertCard(s.budgetAlerts, onBudgets, Modifier.padding(horizontal = 16.dp)) }
        }
        if (s.goals.isNotEmpty()) {
            item {
                ChartCard(
                    title = "Target tabungan",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    actionLabel = "Lihat semua",
                    onAction = { onNavigate(Routes.GOALS) },
                ) {
                    s.goals.take(3).forEach { goal ->
                        val progress = if (goal.targetAmount > 0) goal.savedAmount.toFloat() / goal.targetAmount else 0f
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${goal.emoji} ${goal.name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(FinanceCalc.percent(progress.coerceAtMost(1f)), style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        Meter(progress, ext.seriesColor(goal.color))
                    }
                }
            }
        }
        item {
            Column {
                SectionTitle("Transaksi terakhir", Modifier.padding(top = 0.dp), action = "Lihat semua", onAction = onSeeAll)
                AppCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp).animateContentSize(tween(Motion.MEDIUM))) {
                    if (s.recent.isEmpty()) {
                        InlineEmpty(Icons.AutoMirrored.Outlined.ReceiptLong, "Belum ada transaksi", actionLabel = "Catat", onAction = { onNavigate(Routes.txEdit()) })
                    } else {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            s.recent.forEach { item ->
                                TxRow(item, hidden, onClick = { onNavigate(Routes.txEdit(item.tx.id)) }, showDate = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceHero(total: Long, accountCount: Int, hidden: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(onClick = onClick, brush = heroBrush(), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.heroDecoration().padding(22.dp)) {
            Text("Total saldo", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            AnimatedMoney(total, MaterialTheme.typography.displaySmall, Color.White, hidden = hidden, autoSize = true, minSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Gabungan $accountCount dompet",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun AccountsStrip(balances: List<AccountBalance>, hidden: Boolean, onManage: () -> Unit) {
    val ext = AppColors.current
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(balances, key = { it.account.id }) { b ->
            val type = enumOr(b.account.type, AccountType.OTHER)
            AppCard(onClick = onManage, modifier = Modifier.width(164.dp).height(108.dp).animateItem()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(type.emoji, ext.seriesColor(b.account.color), size = 30.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(b.account.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.weight(1f))
                    MoneyText(b.balance, style = MaterialTheme.typography.titleMedium, hidden = hidden)
                    Text(
                        if (b.account.includeInTotal) type.label else "Tidak dihitung",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        item {
            OutlinedCard(
                onClick = onManage,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                modifier = Modifier.width(112.dp).height(108.dp),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.height(4.dp))
                    Text("Kelola dompet", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AppCard(modifier.fillMaxWidth().enterStagger()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (actionLabel != null && onAction != null) {
                    Text(
                        actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(CircleShape).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun IncomeExpenseLegend() {
    val ext = AppColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(ext.income, "Pemasukan")
        LegendItem(ext.expense, "Pengeluaran")
    }
}

/** Pembacaan nilai untuk titik/batang yang dipilih: nilai di depan, nama seri mengikuti. */
@Composable
private fun ChartReadout(title: String, entries: List<Triple<Color, String, String>>) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            entries.forEach { (color, value, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(12.dp).height(2.dp).background(color))
                    Spacer(Modifier.width(6.dp))
                    Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BudgetAlertCard(alerts: List<BudgetStatus>, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val ext = AppColors.current
    AppCard(onClick = onOpen, containerColor = AppColors.current.warning.copy(alpha = 0.12f), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = ext.warning, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${alerts.size} anggaran hampir habis atau terlampaui",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            alerts.take(3).forEach { st ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${st.emoji} ${st.name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(FinanceCalc.percent(st.ratio), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(4.dp))
                Meter(st.ratio, if (st.ratio > 1f) ext.critical else ext.warning, height = 6.dp)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Transaksi + filter
// ---------------------------------------------------------------------------------------------

@Composable
private fun TransactionsTab(vm: FinanceViewModel, hidden: Boolean, onNavigate: (String) -> Unit) {
    val feedback = LocalFeedback.current
    val state by vm.txList.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    var periodMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var accountMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var categoryDialog by remember { mutableStateOf(false) }
    var rangePicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        SearchPill(
            query = filter.query,
            onQueryChange = { q -> vm.updateFilter { it.copy(query = q) } },
            placeholder = "Cari transaksi",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                FilterPill(
                    label = if (filter.period == Period.CUSTOM) state?.rangeLabel ?: "Kustom" else filter.period.label,
                    active = filter.period != Period.THIS_MONTH,
                    onClick = { periodMenu = true },
                )
                DropdownMenu(expanded = periodMenu, onDismissRequest = { periodMenu = false }) {
                    Period.entries.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.label) },
                            trailingIcon = { if (filter.period == p) Icon(Icons.Filled.Check, null) },
                            onClick = {
                                periodMenu = false
                                if (p == Period.CUSTOM) rangePicker = true else vm.updateFilter { it.copy(period = p) }
                            },
                        )
                    }
                }
            }
            Box {
                FilterPill(filter.type?.label ?: "Semua jenis", filter.type != null) { typeMenu = true }
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    (listOf<TxType?>(null) + TxType.entries).forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t?.label ?: "Semua jenis") },
                            trailingIcon = { if (filter.type == t) Icon(Icons.Filled.Check, null) },
                            onClick = {
                                typeMenu = false
                                vm.updateFilter { it.copy(type = t) }
                            },
                        )
                    }
                }
            }
            FilterPill(
                label = if (filter.categoryIds.isEmpty()) "Kategori" else "${filter.categoryIds.size} kategori",
                active = filter.categoryIds.isNotEmpty(),
            ) { categoryDialog = true }
            Box {
                FilterPill(accounts.firstOrNull { it.id == filter.accountId }?.name ?: "Semua dompet", filter.accountId != null) {
                    accountMenu = true
                }
                DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Semua dompet") },
                        trailingIcon = { if (filter.accountId == null) Icon(Icons.Filled.Check, null) },
                        onClick = {
                            accountMenu = false
                            vm.updateFilter { it.copy(accountId = null) }
                        },
                    )
                    accounts.forEach { a ->
                        DropdownMenuItem(
                            text = { Text("${enumOr(a.type, AccountType.OTHER).emoji} ${a.name}") },
                            trailingIcon = { if (filter.accountId == a.id) Icon(Icons.Filled.Check, null) },
                            onClick = {
                                accountMenu = false
                                vm.updateFilter { it.copy(accountId = a.id) }
                            },
                        )
                    }
                }
            }
            Box {
                FilterPill(filter.sort.label, filter.sort != TxSort.NEWEST) { sortMenu = true }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    TxSort.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.label) },
                            trailingIcon = { if (filter.sort == s) Icon(Icons.Filled.Check, null) },
                            onClick = {
                                sortMenu = false
                                vm.updateFilter { it.copy(sort = s) }
                            },
                        )
                    }
                }
            }
            if (filter.hasExtraFilters || filter.period != Period.THIS_MONTH) {
                AssistChip(
                    onClick = { vm.updateFilter { TxFilter() } },
                    label = { Text("Reset") },
                    leadingIcon = { Icon(Icons.Outlined.FilterAltOff, null, Modifier.size(18.dp)) },
                )
            }
        }
        val s = state ?: return@Column
        if (s.flat.isEmpty()) {
            EmptyState(emoji = "🔍", title = "Tidak ada transaksi", message = "Coba ubah periode atau filter pencarian.")
            return@Column
        }
        SummaryStrip(s, hidden)
        LazyColumn(contentPadding = PaddingValues(bottom = Spacing.fabClearance), modifier = Modifier.fillMaxSize()) {
            if (s.groups.isNotEmpty()) {
                s.groups.forEach { g ->
                    item(key = "h_${g.date}") { DayHeader(g.date, g.net, hidden) }
                    items(g.items, key = { it.tx.id }) { item ->
                        SwipeRow(
                            modifier = Modifier.animateItem(),
                            endAction = SwipeAction(Icons.Outlined.Delete, "Hapus", AppColors.current.critical) {
                                vm.deleteTransaction(item.tx)
                                feedback.undo("Transaksi dihapus") { vm.restoreTransaction(item.tx) }
                            },
                        ) {
                            TxRow(item, hidden, onClick = { onNavigate(Routes.txEdit(item.tx.id)) })
                        }
                    }
                }
            } else {
                items(s.flat, key = { it.tx.id }) { item ->
                    SwipeRow(
                        modifier = Modifier.animateItem(),
                        endAction = SwipeAction(Icons.Outlined.Delete, "Hapus", AppColors.current.critical) {
                            vm.deleteTransaction(item.tx)
                            feedback.undo("Transaksi dihapus") { vm.restoreTransaction(item.tx) }
                        },
                    ) {
                        TxRow(item, hidden, onClick = { onNavigate(Routes.txEdit(item.tx.id)) }, showDate = true)
                    }
                }
            }
        }
    }

    if (categoryDialog) {
        CategoryFilterDialog(
            categories = categories,
            selected = filter.categoryIds,
            onDismiss = { categoryDialog = false },
            onApply = { ids -> vm.updateFilter { it.copy(categoryIds = ids) } },
        )
    }
    if (rangePicker) {
        DateRangePickerModal(
            onDismiss = { rangePicker = false },
            onPick = { start, end -> vm.updateFilter { it.copy(period = Period.CUSTOM, customStart = start, customEnd = end) } },
        )
    }
}

@Composable
private fun SummaryStrip(s: TxListState, hidden: Boolean) {
    val ext = AppColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "${s.rangeLabel} · ${s.flat.size} transaksi",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                SummaryCell("Pemasukan", FinanceCalc.moneyOrMasked(s.income, hidden, true), ext.incomeText, Modifier.weight(1f))
                SummaryCell("Pengeluaran", FinanceCalc.moneyOrMasked(-s.expense, hidden), ext.expenseText, Modifier.weight(1f))
                SummaryCell("Selisih", FinanceCalc.moneyOrMasked(s.income - s.expense, hidden, true), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AutoSizeText(value, MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = color, minSize = 10.sp)
    }
}

@Composable
private fun DayHeader(date: LocalDate, net: Long, hidden: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (date.year == LocalDate.now().year) Dates.relativeDay(date) else Dates.weekdayFull(date),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        MoneyText(
            net,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            withSign = true,
            hidden = hidden,
        )
    }
}

@Composable
private fun CategoryFilterDialog(
    categories: List<CategoryEntity>,
    selected: Set<Long>,
    onDismiss: () -> Unit,
    onApply: (Set<Long>) -> Unit,
) {
    var current by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter kategori") },
        text = {
            LazyColumn(Modifier.height(380.dp)) {
                listOf(TxType.EXPENSE, TxType.INCOME).forEach { type ->
                    item(key = "h_$type") {
                        Text(
                            type.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(categories.filter { it.type == type.name }, key = { it.id }) { cat ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { current = if (cat.id in current) current - cat.id else current + cat.id },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = cat.id in current, onCheckedChange = null)
                            Spacer(Modifier.width(4.dp))
                            Text("${cat.emoji} ${cat.name}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(current)
                onDismiss()
            }) { Text("Terapkan") }
        },
        dismissButton = {
            TextButton(onClick = {
                onApply(emptySet())
                onDismiss()
            }) { Text("Reset") }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Statistik
// ---------------------------------------------------------------------------------------------

/** Irisan donat yang bersebelahan tidak boleh berwarna sama (warna tetap mengikuti kategori). */
private fun distinctAdjacent(indices: List<Int>): List<Int> {
    val out = indices.toMutableList()
    for (i in out.indices) {
        val prev = if (i > 0) out[i - 1] else null
        val next = if (i == out.lastIndex && out.size > 2) out[0] else null
        if (out[i] == prev || out[i] == next) {
            out[i] = (0 until 8).firstOrNull { it != prev && it != next && it !in out }
                ?: (0 until 8).first { it != prev && it != next }
        }
    }
    return out
}

@Composable
private fun StatsTab(vm: FinanceViewModel, hidden: Boolean, onNavigate: (String) -> Unit) {
    val state by vm.stats.collectAsStateWithLifecycle()
    val mode by vm.statsMode.collectAsStateWithLifecycle()
    val type by vm.statsType.collectAsStateWithLifecycle()
    val ext = AppColors.current
    var selectedSlice by remember { mutableStateOf<Int?>(null) }
    var selectedBar by remember { mutableStateOf<Int?>(null) }
    var selectedPoint by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = Spacing.fabClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SegmentedTabs(
                labels = StatsMode.entries.map { it.label },
                selectedIndex = StatsMode.entries.indexOf(mode),
                onSelect = {
                    vm.statsMode.value = StatsMode.entries[it]
                    selectedBar = null
                    selectedPoint = null
                },
            )
        }
        val s = state ?: return@LazyColumn
        item {
            MonthSwitcher(
                label = s.title,
                onPrev = {
                    vm.shiftStats(-1)
                    selectedSlice = null
                    selectedPoint = null
                },
                onNext = {
                    vm.shiftStats(1)
                    selectedSlice = null
                    selectedPoint = null
                },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Pemasukan", FinanceCalc.moneyOrMasked(s.income, hidden), Modifier.weight(1f), icon = Icons.Filled.SouthWest, iconTint = ext.income)
                StatTile("Pengeluaran", FinanceCalc.moneyOrMasked(s.expense, hidden), Modifier.weight(1f), icon = Icons.Filled.NorthEast, iconTint = ext.expense)
            }
        }
        item {
            ChartCard(title = "Komposisi per kategori") {
                SegmentedTabs(
                    labels = listOf(TxType.EXPENSE.label, TxType.INCOME.label),
                    selectedIndex = if (type == TxType.INCOME) 1 else 0,
                    onSelect = {
                        vm.statsType.value = if (it == 1) TxType.INCOME else TxType.EXPENSE
                        selectedSlice = null
                    },
                )
                Spacer(Modifier.height(12.dp))
                if (s.shares.isEmpty()) {
                    Text(
                        "Belum ada ${type.label.lowercase()} pada periode ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    val colors = distinctAdjacent(s.shares.map { it.colorIndex }).map(ext::seriesColor)
                    val slices = s.shares.mapIndexed { i, sh -> DonutSlice(sh.name, sh.amount, colors[i], sh.emoji) }
                    val focus = selectedSlice?.let { s.shares.getOrNull(it) }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(
                            slices = slices,
                            selected = selectedSlice,
                            onSelect = { selectedSlice = it },
                            modifier = Modifier.fillMaxWidth(0.72f),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
                                Text(
                                    focus?.let { "${it.emoji} ${it.name}" } ?: "Total ${type.label.lowercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                AutoSizeText(
                                    FinanceCalc.moneyOrMasked(focus?.amount ?: s.sharesTotal, hidden),
                                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    minSize = 10.sp,
                                )
                                if (focus != null) {
                                    Text(FinanceCalc.percent(focus.fraction), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    s.shares.forEachIndexed { i, sh ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedSlice = if (selectedSlice == i) null else i }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${sh.emoji} ${sh.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                FinanceCalc.percent(sh.fraction),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                FinanceCalc.moneyOrMasked(sh.amount, hidden),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
        item {
            ChartCard(title = "Pemasukan vs pengeluaran") {
                IncomeExpenseLegend()
                val index = selectedBar ?: s.bars.indexOfLast { it.income > 0 || it.expense > 0 }.takeIf { it >= 0 } ?: s.bars.lastIndex
                s.barRows.getOrNull(index)?.let { row ->
                    ChartReadout(
                        title = row.label,
                        entries = listOf(
                            Triple(ext.income, FinanceCalc.moneyOrMasked(row.income, hidden), "masuk"),
                            Triple(ext.expense, FinanceCalc.moneyOrMasked(row.expense, hidden), "keluar"),
                        ),
                    )
                }
                IncomeExpenseBarChart(s.bars, selected = selectedBar, onSelect = { selectedBar = it })
                Spacer(Modifier.height(12.dp))
                MonthTable(s.barRows, hidden, highlight = selectedBar)
            }
        }
        item {
            ChartCard(title = if (mode == StatsMode.MONTH) "Tren saldo harian" else "Saldo akhir bulan") {
                if (s.trend.isEmpty()) {
                    Text(
                        "Belum ada data untuk periode ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    val point = s.trend.getOrNull(selectedPoint ?: s.trend.lastIndex)
                    if (point != null) {
                        ChartReadout(point.label, listOf(Triple(ext.series[0], FinanceCalc.moneyOrMasked(point.value, hidden), "saldo")))
                    }
                    TrendLineChart(
                        points = s.trend,
                        color = ext.series[0],
                        ringColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        selected = selectedPoint,
                        onSelect = { selectedPoint = it },
                    )
                    Text(
                        "Geser jari di grafik untuk melihat saldo per titik.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Rata-rata keluar/hari", FinanceCalc.moneyOrMasked(s.avgDailyExpense, hidden), Modifier.weight(1f))
                StatTile("Jumlah transaksi", "${s.count}", Modifier.weight(1f))
            }
        }
        s.largestExpense?.let { largest ->
            item {
                ChartCard(title = "Pengeluaran terbesar") {
                    TxRow(largest, hidden, onClick = { onNavigate(Routes.txEdit(largest.tx.id)) }, showDate = true)
                }
            }
        }
    }
}

@Composable
private fun MonthTable(rows: List<MonthRow>, hidden: Boolean, highlight: Int?) {
    val ext = AppColors.current
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Bulan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            listOf("Masuk", "Keluar", "Selisih").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
            }
        }
        HorizontalDivider(color = ext.grid)
        rows.forEachIndexed { i, r ->
            val strong = i == highlight
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text(r.label, style = MaterialTheme.typography.bodySmall, fontWeight = if (strong) FontWeight.Bold else null, modifier = Modifier.weight(1f))
                listOf(r.income, r.expense, r.income - r.expense).forEach { v ->
                    Text(
                        if (hidden) "•••" else Money.compact(v),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (strong) FontWeight.Bold else null,
                        modifier = Modifier.weight(1.2f),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Anggaran
// ---------------------------------------------------------------------------------------------

@Composable
private fun BudgetsTab(vm: FinanceViewModel, hidden: Boolean, onEdit: (BudgetStatus) -> Unit, onAdd: () -> Unit) {
    val state by vm.budgets.collectAsStateWithLifecycle()
    val s = state ?: return
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = Spacing.fabClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { MonthSwitcher(Dates.monthYear(s.month), onPrev = { vm.shiftBudgetMonth(-1) }, onNext = { vm.shiftBudgetMonth(1) }) }
        if (s.statuses.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🎯",
                    title = "Belum ada anggaran",
                    message = "Tetapkan batas pengeluaran bulanan per kategori agar keuangan lebih terkendali.",
                    action = { Button(onClick = onAdd) { Text("Atur anggaran") } },
                )
            }
        }
        items(s.statuses, key = { it.categoryId }) { st -> BudgetCard(st, hidden, s.daysLeft, onClick = { onEdit(st) }) }
        if (s.statuses.isNotEmpty()) {
            item {
                Text(
                    "Anggaran berlaku setiap bulan. Ketuk kartu untuk mengubah atau menghapus.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BudgetCard(st: BudgetStatus, hidden: Boolean, daysLeft: Int, onClick: () -> Unit) {
    val ext = AppColors.current
    val money = { v: Long -> FinanceCalc.moneyOrMasked(v, hidden) }
    val fill: Color
    val statusText: String
    val statusIcon: androidx.compose.ui.graphics.vector.ImageVector
    val statusTint: Color
    when {
        st.ratio > 1f -> {
            fill = ext.critical; statusIcon = Icons.Filled.Error; statusTint = ext.critical
            statusText = "Melebihi anggaran ${money(-st.remaining)}"
        }
        st.ratio >= 0.8f -> {
            fill = ext.warning; statusIcon = Icons.Filled.Warning; statusTint = ext.warning
            statusText = "Hampir habis · sisa ${money(st.remaining)}"
        }
        else -> {
            fill = ext.series[0]; statusIcon = Icons.Filled.CheckCircle; statusTint = ext.good
            statusText = "Aman · sisa ${money(st.remaining)}"
        }
    }
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth().enterStagger()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(st.emoji, st.category?.let { ext.seriesColor(it.color) } ?: MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(st.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("${money(st.spent)} dari ${money(st.limit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(FinanceCalc.percent(st.ratio), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Meter(st.ratio, fill)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(statusIcon, contentDescription = null, tint = statusTint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(statusText, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f, fill = false))
            }
            if (daysLeft > 0 && st.remaining > 0) {
                Text(
                    "≈ ${money(st.remaining / daysLeft)} per hari untuk $daysLeft hari tersisa",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 22.dp, top = 2.dp),
                )
            }
        }
    }
}

private data class BudgetOption(val id: Long, val label: String)

@Composable
private fun BudgetDialog(
    initial: BudgetStatus?,
    categories: List<CategoryEntity>,
    existingIds: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (Long, Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val options = remember(categories) {
        listOf(BudgetOption(0, "💰 Total semua pengeluaran")) +
            categories.filter { it.type == TxType.EXPENSE.name }.map { BudgetOption(it.id, "${it.emoji} ${it.name}") }
    }
    var selected by remember {
        mutableStateOf(
            options.firstOrNull { it.id == initial?.categoryId }
                ?: options.firstOrNull { it.id !in existingIds }
                ?: options.first(),
        )
    }
    var amount by remember { mutableStateOf(initial?.limit?.toDigits() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Atur anggaran bulanan" else "Ubah anggaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (initial == null) {
                    DropdownField("Kategori", options, selected, { it.label }, { selected = it })
                } else {
                    Text(selected.label, style = MaterialTheme.typography.titleMedium)
                }
                AmountField(amount, { amount = it }, "Batas per bulan")
                if (initial == null && selected.id in existingIds) {
                    Text(
                        "Kategori ini sudah punya anggaran; nilainya akan diganti.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount.digitsToLong() > 0,
                onClick = {
                    onSave(selected.id, amount.digitsToLong())
                    onDismiss()
                },
            ) { Text("Simpan") }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(
                        onClick = {
                            onDelete(initial.categoryId)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Hapus") }
                }
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        },
    )
}
