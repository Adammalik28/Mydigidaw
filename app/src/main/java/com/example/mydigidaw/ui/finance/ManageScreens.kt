@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mydigidaw.ui.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mydigidaw.data.db.AccountEntity
import com.example.mydigidaw.data.db.CategoryEntity
import com.example.mydigidaw.data.db.DebtEntity
import com.example.mydigidaw.data.db.GoalEntity
import com.example.mydigidaw.data.db.NEUTRAL_COLOR
import com.example.mydigidaw.data.model.AccountType
import com.example.mydigidaw.data.model.DebtType
import com.example.mydigidaw.data.model.Repeat
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.components.SegmentedTabs
import com.example.mydigidaw.ui.components.Spacing
import com.example.mydigidaw.ui.components.AmountField
import com.example.mydigidaw.ui.components.BackTopBar
import com.example.mydigidaw.ui.components.ColorPickerRow
import com.example.mydigidaw.ui.components.ConfirmDialog
import com.example.mydigidaw.ui.components.DatePickerModal
import com.example.mydigidaw.ui.components.DropdownField
import com.example.mydigidaw.ui.components.EmojiBadge
import com.example.mydigidaw.ui.components.EmojiPicker
import com.example.mydigidaw.ui.components.EmptyState
import com.example.mydigidaw.ui.components.Meter
import com.example.mydigidaw.ui.components.MoneyText
import com.example.mydigidaw.ui.components.StatTile
import com.example.mydigidaw.ui.components.digitsToLong
import com.example.mydigidaw.ui.components.toDigits
import com.example.mydigidaw.ui.navigation.Routes
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.Money
import com.example.mydigidaw.util.startMillis
import com.example.mydigidaw.util.toLocalDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlinx.coroutines.launch

private val categoryEmojis = listOf(
    "🍔", "🍜", "☕", "🧋", "🛒", "🛍️", "👕", "👟", "🚗", "🛵", "⛽", "🚌", "✈️", "🏠", "💡", "💧",
    "📱", "🌐", "🧾", "💳", "🎬", "🎮", "🎵", "📚", "🎓", "💊", "🏥", "🏋️", "⚽", "💇", "🐾", "👶",
    "🎁", "🙏", "🕌", "💼", "🎉", "📈", "🏦", "💰", "🪙", "🛠️", "🧹", "🎂", "❤️", "🚬", "🍺", "📦",
)

private val goalEmojis = listOf(
    "🎯", "🛡️", "🏠", "🚗", "🏍️", "✈️", "🏖️", "💍", "🎓", "💻", "📱", "🎮", "🏥", "👶", "🕋", "💰", "🎁", "📷",
)

// ---------------------------------------------------------------------------------------------
// Dompet
// ---------------------------------------------------------------------------------------------

@Composable
fun AccountsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = appViewModel { AccountsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val hidden by vm.hideAmounts.collectAsStateWithLifecycle()
    val ext = AppColors.current
    var editing by remember { mutableStateOf<AccountEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BackTopBar(
                title = "Dompet",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { onNavigate(Routes.txEdit(type = TxType.TRANSFER)) }) {
                        Icon(Icons.Outlined.SwapHoriz, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Transfer")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, contentDescription = "Tambah dompet") }
        },
    ) { padding ->
        val (balances, total) = state ?: return@Scaffold
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = Spacing.fabClearance)) {
            item {
                BalanceHero(
                    total = total,
                    accountCount = balances.count { it.account.includeInTotal },
                    hidden = hidden,
                    onClick = {},
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(balances, key = { it.account.id }) { b ->
                val type = enumOr(b.account.type, AccountType.OTHER)
                ListItem(
                    headlineContent = { Text(b.account.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text(type.label + if (b.account.includeInTotal) "" else " · tidak dihitung dalam total")
                    },
                    leadingContent = { EmojiBadge(type.emoji, ext.seriesColor(b.account.color)) },
                    trailingContent = {
                        MoneyText(
                            b.balance,
                            hidden = hidden,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (b.balance < 0) ext.expenseText else Color.Unspecified,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { editing = b.account },
                )
            }
            item {
                Text(
                    "Saldo = saldo awal + semua transaksi di dompet tersebut. Ketuk dompet untuk mengubah atau menghapus.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    if (creating || editing != null) {
        AccountDialog(
            initial = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { vm.save(it) },
            onDelete = { vm.delete(it) },
            transactionCount = vm::transactionCount,
        )
    }
}

@Composable
private fun AccountDialog(
    initial: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit,
    onDelete: (Long) -> Unit,
    transactionCount: suspend (Long) -> Int,
) {
    val ext = AppColors.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(enumOr(initial?.type, AccountType.BANK)) }
    var balance by remember { mutableStateOf(initial?.initialBalance?.toDigits() ?: "") }
    var color by remember { mutableIntStateOf(initial?.color ?: 0) }
    var include by remember { mutableStateOf(initial?.includeInTotal ?: true) }
    var deleteCount by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Dompet baru" else "Ubah dompet") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama") },
                    placeholder = { Text("mis. BCA, GoPay, Tunai") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownField("Jenis", AccountType.entries, type, { "${it.emoji} ${it.label}" }, { type = it })
                AmountField(balance, { balance = it }, "Saldo awal")
                Text("Warna", style = MaterialTheme.typography.labelLarge)
                ColorPickerRow(ext.series, color, { color = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hitung dalam total saldo", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = include, onCheckedChange = { include = it })
                }
                if (initial != null) {
                    TextButton(
                        onClick = { scope.launch { deleteCount = transactionCount(initial.id) } },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hapus dompet")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val now = System.currentTimeMillis()
                    val base = initial ?: AccountEntity(name = "", type = type.name, initialBalance = 0, color = 0, createdAt = now)
                    onSave(base.copy(name = name.trim(), type = type.name, initialBalance = balance.digitsToLong(), color = color, includeInTotal = include))
                    onDismiss()
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )

    deleteCount?.let { count ->
        ConfirmDialog(
            title = "Hapus dompet?",
            message = if (count > 0) "Dompet ini beserta $count transaksinya akan dihapus permanen." else "Dompet ini akan dihapus permanen.",
            onConfirm = {
                initial?.let { onDelete(it.id) }
                onDismiss()
            },
            onDismiss = { deleteCount = null },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Kategori
// ---------------------------------------------------------------------------------------------

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val vm = appViewModel { CategoriesViewModel(it) }
    val categories by vm.categories.collectAsStateWithLifecycle()
    val ext = AppColors.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val type = if (tab == 0) TxType.EXPENSE else TxType.INCOME
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BackTopBar(title = "Kategori", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, contentDescription = "Tambah kategori") }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Pengeluaran") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Pemasukan") })
            }
            LazyColumn(contentPadding = PaddingValues(bottom = Spacing.fabClearance)) {
                items(categories.filter { it.type == type.name }, key = { it.id }) { cat ->
                    ListItem(
                        headlineContent = { Text(cat.name) },
                        leadingContent = { EmojiBadge(cat.emoji, ext.seriesColor(cat.color)) },
                        trailingContent = { Icon(Icons.Outlined.Edit, contentDescription = "Ubah") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { editing = cat },
                    )
                }
            }
        }
    }

    if (creating || editing != null) {
        CategoryDialog(
            initial = editing,
            type = editing?.let { enumOr(it.type, TxType.EXPENSE) } ?: type,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { vm.save(it) },
            onDelete = { vm.delete(it) },
        )
    }
}

@Composable
private fun CategoryDialog(
    initial: CategoryEntity?,
    type: TxType,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val ext = AppColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: categoryEmojis.first()) }
    var color by remember { mutableIntStateOf(initial?.color ?: 0) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Kategori ${type.label.lowercase()} baru" else "Ubah kategori") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(emoji, ext.seriesColor(color), size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama kategori") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("Ikon", style = MaterialTheme.typography.labelLarge)
                EmojiPicker(categoryEmojis, emoji, { emoji = it })
                Text("Warna", style = MaterialTheme.typography.labelLarge)
                ColorPickerRow(ext.series + ext.neutralSeries, color, { color = it })
                if (initial != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hapus kategori")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val base = initial ?: CategoryEntity(name = "", type = type.name, emoji = emoji, color = color, sortOrder = 50)
                    onSave(base.copy(name = name.trim(), emoji = emoji, color = color))
                    onDismiss()
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )

    if (confirmDelete && initial != null) {
        ConfirmDialog(
            title = "Hapus kategori \"${initial.name}\"?",
            message = "Transaksi dengan kategori ini tetap tersimpan sebagai \"Tanpa kategori\", dan anggarannya ikut dihapus.",
            onConfirm = {
                onDelete(initial.id)
                onDismiss()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Target tabungan
// ---------------------------------------------------------------------------------------------

@Composable
fun GoalsScreen(onBack: () -> Unit) {
    val vm = appViewModel { GoalsViewModel(it) }
    val goals by vm.goals.collectAsStateWithLifecycle()
    val hidden by vm.hideAmounts.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<GoalEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var funding by remember { mutableStateOf<Pair<GoalEntity, Boolean>?>(null) }

    Scaffold(
        topBar = { BackTopBar(title = "Target Tabungan", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Target") },
            )
        },
    ) { padding ->
        val list = goals ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(
                emoji = "🎯",
                title = "Belum ada target tabungan",
                message = "Buat target seperti dana darurat, liburan, atau gadget baru, lalu catat setiap setoran.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = Spacing.fabClearance),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(list, key = { it.id }) { goal ->
                GoalCard(
                    goal = goal,
                    hidden = hidden,
                    onEdit = { editing = goal },
                    onDeposit = { funding = goal to false },
                    onWithdraw = { funding = goal to true },
                )
            }
        }
    }

    if (creating || editing != null) {
        GoalDialog(
            initial = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { vm.save(it) },
            onDelete = { vm.delete(it) },
        )
    }
    funding?.let { (goal, withdraw) ->
        FundDialog(
            title = if (withdraw) "Tarik dari ${goal.name}" else "Setor ke ${goal.name}",
            confirmLabel = if (withdraw) "Tarik" else "Setor",
            onDismiss = { funding = null },
            onConfirm = { amount -> vm.addFunds(goal, if (withdraw) -amount else amount) },
        )
    }
}

@Composable
private fun GoalCard(goal: GoalEntity, hidden: Boolean, onEdit: () -> Unit, onDeposit: () -> Unit, onWithdraw: () -> Unit) {
    val ext = AppColors.current
    val color = ext.seriesColor(goal.color)
    val progress = if (goal.targetAmount > 0) goal.savedAmount.toFloat() / goal.targetAmount else 0f
    val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0)
    val today = LocalDate.now()
    val deadline = goal.deadline?.toLocalDate()
    val daysLeft = deadline?.let { ChronoUnit.DAYS.between(today, it) }
    val money = { v: Long -> FinanceCalc.moneyOrMasked(v, hidden) }

    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(goal.emoji, color, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        when {
                            deadline == null -> "Tanpa tenggat"
                            daysLeft != null && daysLeft < 0 -> "Tenggat terlewat (${Dates.medium(deadline)})"
                            else -> "Tenggat ${Dates.medium(deadline)} · $daysLeft hari lagi"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (daysLeft != null && daysLeft < 0 && remaining > 0) ext.criticalText else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(FinanceCalc.percent(progress.coerceAtMost(1f)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Meter(progress, color, height = 10.dp)
            Spacer(Modifier.height(8.dp))
            Text("${money(goal.savedAmount)} dari ${money(goal.targetAmount)}", style = MaterialTheme.typography.bodyMedium)
            if (remaining == 0L) {
                Text("🎉 Target tercapai!", style = MaterialTheme.typography.labelLarge, color = ext.goodText)
            } else if (daysLeft != null && daysLeft > 0) {
                val months = ceil(daysLeft / 30.0).toLong().coerceAtLeast(1)
                Text(
                    "Sisihkan ≈ ${money(remaining / months)} per bulan agar tercapai tepat waktu",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onWithdraw, enabled = goal.savedAmount > 0) { Text("Tarik") }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onDeposit) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Setor")
                }
            }
        }
    }
}

@Composable
private fun GoalDialog(initial: GoalEntity?, onDismiss: () -> Unit, onSave: (GoalEntity) -> Unit, onDelete: (Long) -> Unit) {
    val ext = AppColors.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: goalEmojis.first()) }
    var target by remember { mutableStateOf(initial?.targetAmount?.toDigits() ?: "") }
    var saved by remember { mutableStateOf(initial?.savedAmount?.toDigits() ?: "") }
    var deadline by remember { mutableStateOf(initial?.deadline?.toLocalDate()) }
    var color by remember { mutableIntStateOf(initial?.color ?: 2) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Target baru" else "Ubah target") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama target") },
                    placeholder = { Text("mis. Dana darurat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                EmojiPicker(goalEmojis, emoji, { emoji = it })
                AmountField(target, { target = it }, "Jumlah target")
                AmountField(saved, { saved = it }, "Sudah terkumpul")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.CalendarToday, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(deadline?.let { Dates.medium(it) } ?: "Tenggat (opsional)")
                    }
                    if (deadline != null) TextButton(onClick = { deadline = null }) { Text("Hapus") }
                }
                Text("Warna", style = MaterialTheme.typography.labelLarge)
                ColorPickerRow(ext.series, color, { color = it })
                if (initial != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Hapus target") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && target.digitsToLong() > 0,
                onClick = {
                    val base = initial ?: GoalEntity(
                        name = "", emoji = emoji, targetAmount = 0, savedAmount = 0, deadline = null, color = color,
                        createdAt = System.currentTimeMillis(),
                    )
                    onSave(
                        base.copy(
                            name = name.trim(),
                            emoji = emoji,
                            targetAmount = target.digitsToLong(),
                            savedAmount = saved.digitsToLong(),
                            deadline = deadline?.startMillis(),
                            color = color,
                        ),
                    )
                    onDismiss()
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )

    if (pickDate) DatePickerModal(initial = deadline ?: LocalDate.now().plusMonths(6), onDismiss = { pickDate = false }, onPick = { deadline = it })
    if (confirmDelete && initial != null) {
        ConfirmDialog(
            title = "Hapus target?",
            message = "Target \"${initial.name}\" akan dihapus.",
            onConfirm = {
                onDelete(initial.id)
                onDismiss()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun FundDialog(title: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { AmountField(amount, { amount = it }, "Jumlah") },
        confirmButton = {
            TextButton(
                enabled = amount.digitsToLong() > 0,
                onClick = {
                    onConfirm(amount.digitsToLong())
                    onDismiss()
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

// ---------------------------------------------------------------------------------------------
// Utang & piutang
// ---------------------------------------------------------------------------------------------

@Composable
fun DebtsScreen(onBack: () -> Unit) {
    val vm = appViewModel { DebtsViewModel(it) }
    val debts by vm.debts.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<DebtEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<DebtEntity?>(null) }

    Scaffold(
        topBar = { BackTopBar(title = "Utang & Piutang", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Catat") })
        },
    ) { padding ->
        val list = debts ?: return@Scaffold
        val open = list.filter { !it.isSettled }
        val payable = open.filter { it.type == DebtType.PAYABLE.name }.sumOf { it.amount - it.paidAmount }
        val receivable = open.filter { it.type == DebtType.RECEIVABLE.name }.sumOf { it.amount - it.paidAmount }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = Spacing.fabClearance),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Utang saya (sisa)", Money.format(payable), Modifier.weight(1f))
                    StatTile("Piutang (sisa)", Money.format(receivable), Modifier.weight(1f))
                }
            }
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🤝",
                        title = "Belum ada catatan",
                        message = "Catat uang yang Anda pinjam atau pinjamkan agar tidak lupa siapa berutang ke siapa.",
                    )
                }
            }
            items(list, key = { it.id }) { debt ->
                DebtCard(
                    debt = debt,
                    onEdit = { editing = debt },
                    onPay = { paying = debt },
                    onToggleSettled = { vm.setSettled(debt, !debt.isSettled) },
                )
            }
        }
    }

    if (creating || editing != null) {
        DebtDialog(
            initial = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { vm.save(it) },
            onDelete = { vm.delete(it) },
        )
    }
    paying?.let { debt ->
        PayDialog(
            debt = debt,
            accounts = accounts,
            onDismiss = { paying = null },
            onConfirm = { amount, accountId -> vm.pay(debt, amount, accountId) },
        )
    }
}

@Composable
private fun DebtCard(debt: DebtEntity, onEdit: () -> Unit, onPay: () -> Unit, onToggleSettled: () -> Unit) {
    val ext = AppColors.current
    val payable = debt.type == DebtType.PAYABLE.name
    val remaining = (debt.amount - debt.paidAmount).coerceAtLeast(0)
    val due = debt.dueDate?.toLocalDate()
    val overdue = due != null && due.isBefore(LocalDate.now()) && !debt.isSettled
    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(if (payable) "📤" else "📥", if (payable) ext.expense else ext.income)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(debt.person, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        listOfNotNull(
                            if (payable) "Saya berutang" else "Berutang ke saya",
                            due?.let { "jatuh tempo ${Dates.medium(it)}" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) ext.criticalText else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (debt.isSettled) "Lunas" else "Sisa", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(if (debt.isSettled) debt.amount else remaining, fontWeight = FontWeight.SemiBold)
                }
            }
            if (debt.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(debt.note, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Meter(if (debt.amount > 0) debt.paidAmount.toFloat() / debt.amount else 0f, if (debt.isSettled) ext.good else ext.series[0], height = 6.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Terbayar ${Money.format(debt.paidAmount)} dari ${Money.format(debt.amount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onToggleSettled) { Text(if (debt.isSettled) "Batalkan lunas" else "Tandai lunas") }
                if (!debt.isSettled) {
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onPay) { Text(if (payable) "Bayar" else "Terima") }
                }
            }
        }
    }
}

@Composable
private fun DebtDialog(initial: DebtEntity?, onDismiss: () -> Unit, onSave: (DebtEntity) -> Unit, onDelete: (Long) -> Unit) {
    var type by remember { mutableStateOf(enumOr(initial?.type, DebtType.PAYABLE)) }
    var person by remember { mutableStateOf(initial?.person ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toDigits() ?: "") }
    var due by remember { mutableStateOf(initial?.dueDate?.toLocalDate()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Catat utang/piutang" else "Ubah catatan") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SegmentedTabs(
                    labels = DebtType.entries.map { it.label },
                    selectedIndex = DebtType.entries.indexOf(type),
                    onSelect = { type = DebtType.entries[it] },
                )
                Text(type.description, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text(if (type == DebtType.PAYABLE) "Berutang kepada" else "Nama peminjam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AmountField(amount, { amount = it }, "Jumlah")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.CalendarToday, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(due?.let { Dates.medium(it) } ?: "Jatuh tempo (opsional)")
                    }
                    if (due != null) TextButton(onClick = { due = null }) { Text("Hapus") }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (initial != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Hapus catatan") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = person.isNotBlank() && amount.digitsToLong() > 0,
                onClick = {
                    val base = initial ?: DebtEntity(
                        type = type.name, person = "", amount = 0, dueDate = null, note = "",
                        createdAt = System.currentTimeMillis(),
                    )
                    val total = amount.digitsToLong()
                    onSave(
                        base.copy(
                            type = type.name,
                            person = person.trim(),
                            amount = total,
                            paidAmount = base.paidAmount.coerceAtMost(total),
                            dueDate = due?.startMillis(),
                            note = note.trim(),
                            isSettled = base.isSettled || base.paidAmount >= total,
                        ),
                    )
                    onDismiss()
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )

    if (pickDate) DatePickerModal(initial = due ?: LocalDate.now().plusWeeks(2), onDismiss = { pickDate = false }, onPick = { due = it })
    if (confirmDelete && initial != null) {
        ConfirmDialog(
            title = "Hapus catatan?",
            message = "Catatan utang/piutang dengan ${initial.person} akan dihapus.",
            onConfirm = {
                onDelete(initial.id)
                onDismiss()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun PayDialog(debt: DebtEntity, accounts: List<AccountEntity>, onDismiss: () -> Unit, onConfirm: (Long, Long?) -> Unit) {
    val payable = debt.type == DebtType.PAYABLE.name
    var amount by remember { mutableStateOf((debt.amount - debt.paidAmount).coerceAtLeast(0).toDigits()) }
    var record by remember { mutableStateOf(accounts.isNotEmpty()) }
    var account by remember { mutableStateOf(accounts.firstOrNull()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (payable) "Bayar ke ${debt.person}" else "Terima dari ${debt.person}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(amount, { amount = it }, "Jumlah")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (payable) "Catat sebagai pengeluaran" else "Catat sebagai pemasukan",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = record, onCheckedChange = { record = it }, enabled = accounts.isNotEmpty())
                }
                if (record) {
                    DropdownField(
                        "Dompet",
                        accounts,
                        account,
                        { "${enumOr(it.type, AccountType.OTHER).emoji} ${it.name}" },
                        { account = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount.digitsToLong() > 0,
                onClick = {
                    onConfirm(amount.digitsToLong(), if (record) account?.id else null)
                    onDismiss()
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

// ---------------------------------------------------------------------------------------------
// Transaksi berulang
// ---------------------------------------------------------------------------------------------

@Composable
fun RecurringScreen(onBack: () -> Unit) {
    val vm = appViewModel { RecurringViewModel(it) }
    val items by vm.items.collectAsStateWithLifecycle()
    val ext = AppColors.current
    var confirmDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(topBar = { BackTopBar(title = "Transaksi Berulang", onBack = onBack) }) { padding ->
        val list = items ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(
                emoji = "🔁",
                title = "Belum ada transaksi berulang",
                message = "Saat menambah transaksi, pilih \"Ulangi otomatis\" untuk gaji, langganan, cicilan, atau tagihan rutin.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            items(list, key = { it.rule.id }) { item ->
                val rule = item.rule
                val type = enumOr(rule.type, TxType.EXPENSE)
                val repeat = enumOr(rule.repeat, Repeat.MONTHLY)
                val title = if (type == TxType.TRANSFER) "Transfer" else item.category?.name ?: "Tanpa kategori"
                ListItem(
                    headlineContent = {
                        Text(
                            "$title · ${Money.format(rule.amount)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            buildString {
                                append(repeat.label)
                                if (rule.isActive) append(" · berikutnya ${Dates.medium(rule.nextDate.toLocalDate())}") else append(" · dijeda")
                                item.account?.let { append(" · ${it.name}") }
                                if (rule.note.isNotBlank()) append(" · ${rule.note}")
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        EmojiBadge(
                            if (type == TxType.TRANSFER) "🔁" else item.category?.emoji ?: "❔",
                            ext.seriesColor(item.category?.color ?: NEUTRAL_COLOR),
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = rule.isActive, onCheckedChange = { vm.setActive(rule, it) })
                            IconButton(onClick = { confirmDelete = rule.id }) { Icon(Icons.Outlined.Delete, contentDescription = "Hapus") }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            item {
                Text(
                    "Transaksi dicatat otomatis setiap kali aplikasi dibuka pada atau setelah tanggal jatuh tempo. " +
                        "Matikan sakelar untuk menjeda.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    confirmDelete?.let { id ->
        ConfirmDialog(
            title = "Hapus aturan berulang?",
            message = "Transaksi yang sudah tercatat tidak ikut terhapus.",
            onConfirm = { vm.delete(id) },
            onDismiss = { confirmDelete = null },
        )
    }
}
