@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.mydigidaw.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mydigidaw.data.model.AccountType
import com.example.mydigidaw.data.model.Repeat
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.components.SegmentedTabs
import com.example.mydigidaw.ui.components.AmountField
import com.example.mydigidaw.ui.components.BackTopBar
import com.example.mydigidaw.ui.components.ConfirmDialog
import com.example.mydigidaw.ui.components.DatePickerModal
import com.example.mydigidaw.ui.components.DropdownField
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.TimePickerModal
import com.example.mydigidaw.ui.navigation.Routes
import com.example.mydigidaw.util.Dates
import kotlinx.coroutines.launch

@Composable
fun TransactionEditorScreen(
    txId: Long,
    initialType: String?,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    initialAmount: Long? = null,
    initialNote: String? = null,
) {
    val vm = appViewModel { TransactionEditorViewModel(txId, initialType, it, initialAmount, initialNote) }
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val feedback = LocalFeedback.current
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val accountLabel = { a: com.example.mydigidaw.data.db.AccountEntity -> "${enumOr(a.type, AccountType.OTHER).emoji} ${a.name}" }

    Scaffold(
        topBar = {
            BackTopBar(
                title = if (vm.isNew) "Transaksi baru" else "Edit transaksi",
                onBack = onBack,
                actions = {
                    if (!vm.isNew) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Hapus") }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = {
                        scope.launch {
                            if (vm.save()) {
                                feedback.confirm()
                                feedback.message(if (vm.isNew) "Transaksi tersimpan" else "Perubahan disimpan")
                                onBack()
                            } else {
                                feedback.reject()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp),
                ) { Text("Simpan") }
            }
        },
    ) { padding ->
        if (!vm.loaded) return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SegmentedTabs(
                labels = TxType.entries.map { it.label },
                selectedIndex = TxType.entries.indexOf(vm.type),
                onSelect = { vm.changeType(TxType.entries[it]) },
            )
            AmountField(
                digits = vm.amount,
                onDigitsChange = {
                    vm.amount = it
                    vm.clearError("amount")
                },
                label = "Jumlah",
                isError = "amount" in vm.errors,
                supportingText = if ("amount" in vm.errors) "Masukkan jumlah lebih dari 0" else null,
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            if (vm.type != TxType.TRANSFER) {
                Column {
                    Text("Kategori", style = MaterialTheme.typography.titleSmall)
                    if ("category" in vm.errors) {
                        Text("Pilih salah satu kategori", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.size(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.filter { it.type == vm.type.name }.forEach { cat ->
                            FilterChip(
                                selected = vm.categoryId == cat.id,
                                onClick = {
                                    vm.categoryId = cat.id
                                    vm.clearError("category")
                                },
                                label = { Text("${cat.emoji} ${cat.name}") },
                            )
                        }
                        AssistChip(
                            onClick = { onNavigate(Routes.CATEGORIES) },
                            label = { Text("Kelola") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp)) },
                        )
                    }
                }
            }
            DropdownField(
                label = if (vm.type == TxType.TRANSFER) "Dari dompet" else "Dompet",
                options = accounts,
                selected = accounts.firstOrNull { it.id == vm.accountId },
                optionLabel = accountLabel,
                onSelect = {
                    vm.accountId = it.id
                    vm.clearError("account")
                },
                isError = "account" in vm.errors,
            )
            if (vm.type == TxType.TRANSFER) {
                DropdownField(
                    label = "Ke dompet",
                    options = accounts.filter { it.id != vm.accountId },
                    selected = accounts.firstOrNull { it.id == vm.toAccountId },
                    optionLabel = accountLabel,
                    onSelect = {
                        vm.toAccountId = it.id
                        vm.clearError("toAccount")
                    },
                    isError = "toAccount" in vm.errors,
                )
                if (accounts.size < 2) {
                    Text(
                        "Tambahkan minimal dua dompet untuk melakukan transfer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.CalendarToday, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Dates.relativeDay(vm.date), maxLines = 1)
                }
                OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Dates.time(vm.time))
                }
            }
            OutlinedTextField(
                value = vm.note,
                onValueChange = { vm.note = it },
                label = { Text("Catatan (opsional)") },
                placeholder = { Text("mis. Makan siang bareng tim") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (vm.isNew) {
                DropdownField(
                    label = "Ulangi otomatis",
                    options = Repeat.entries,
                    selected = vm.repeat,
                    optionLabel = { it.label },
                    onSelect = { vm.repeat = it },
                )
                if (vm.repeat != Repeat.NONE) {
                    Text(
                        "Transaksi ini akan dicatat otomatis ${vm.repeat.label.lowercase()} mulai tanggal di atas. " +
                            "Kelola lewat menu Keuangan › Transaksi berulang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (pickDate) DatePickerModal(initial = vm.date, onDismiss = { pickDate = false }, onPick = { vm.date = it })
    if (pickTime) TimePickerModal(initial = vm.time, onDismiss = { pickTime = false }, onPick = { vm.time = it })
    if (confirmDelete) {
        ConfirmDialog(
            title = "Hapus transaksi?",
            message = "Transaksi ini akan dihapus permanen dan saldo dompet diperbarui.",
            onConfirm = {
                scope.launch {
                    val deleted = vm.delete()
                    onBack()
                    if (deleted != null) feedback.undo("Transaksi dihapus") { vm.restore(deleted) }
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
