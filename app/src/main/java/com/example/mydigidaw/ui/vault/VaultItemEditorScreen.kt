@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.mydigidaw.ui.vault

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mydigidaw.data.model.VaultType
import com.example.mydigidaw.data.repo.VaultField
import com.example.mydigidaw.security.SecureClipboard
import com.example.mydigidaw.ui.LocalAppContainer
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.components.BackTopBar
import com.example.mydigidaw.ui.components.ConfirmDialog
import com.example.mydigidaw.ui.components.EmojiBadge
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun VaultItemEditorScreen(itemId: Long, initialType: String?, onBack: () -> Unit) {
    SecureScreenEffect()
    val vm = appViewModel { VaultEditorViewModel(itemId, initialType, it) }
    val feedback = LocalFeedback.current
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val container = LocalAppContainer.current
    var confirmDelete by remember { mutableStateOf(false) }
    var addField by remember { mutableStateOf(false) }
    var generatorFor by remember { mutableStateOf<Int?>(null) }
    val revealed = remember { mutableStateMapOf<Int, Boolean>() }

    // Brankas terkunci otomatis (mis. aplikasi lama di latar belakang): keluar dari layar ini.
    if (!unlocked) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val copy: (VaultField) -> Unit = { field ->
        SecureClipboard.copy(context, field.label, field.value, field.secret, container.appScope)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "${field.label} disalin", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            BackTopBar(
                title = if (vm.isNew) "Item baru" else "Detail item",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { vm.favorite = !vm.favorite }) {
                        Icon(
                            if (vm.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Favorit",
                            tint = if (vm.favorite) AppColors.current.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!vm.isNew) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Hapus") }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            if (vm.save()) {
                                feedback.confirm()
                                feedback.message("Tersimpan aman di brankas")
                                onBack()
                            } else {
                                feedback.reject()
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = "Simpan")
                    }
                },
            )
        },
    ) { padding ->
        if (!vm.loaded) return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vm.isNew) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VaultType.entries.forEach { t ->
                        FilterChip(
                            selected = vm.type == t,
                            onClick = { vm.changeType(t) },
                            label = { Text("${t.emoji} ${t.label}") },
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(vm.type.emoji, MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(vm.type.label, style = MaterialTheme.typography.titleMedium)
                }
            }
            OutlinedTextField(
                value = vm.title,
                onValueChange = {
                    vm.title = it
                    vm.titleError = false
                },
                label = { Text("Judul") },
                placeholder = { Text("mis. Gmail, BCA, Wi-Fi Rumah") },
                isError = vm.titleError,
                supportingText = if (vm.titleError) ({ Text("Judul wajib diisi") }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            vm.fields.forEachIndexed { index, field ->
                VaultFieldInput(
                    field = field,
                    revealed = revealed[index] == true,
                    onToggleReveal = { revealed[index] = revealed[index] != true },
                    onValueChange = { vm.updateField(index, it) },
                    onCopy = { copy(field) },
                    onGenerate = { generatorFor = index },
                    onRemove = {
                        revealed.clear()
                        vm.removeField(index)
                    },
                )
            }
            TextButton(onClick = { addField = true }) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Tambah kolom")
            }
            OutlinedTextField(
                value = vm.notes,
                onValueChange = { vm.notes = it },
                label = { Text(if (vm.type == VaultType.SECURE_NOTE) "Isi catatan rahasia" else "Catatan") },
                minLines = if (vm.type == VaultType.SECURE_NOTE) 8 else 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Hapus item ini?",
            message = "Data rahasia ini akan dihapus permanen dari brankas.",
            onConfirm = {
                scope.launch {
                    vm.delete()
                    onBack()
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
    if (addField) {
        AddFieldDialog(onDismiss = { addField = false }, onAdd = { label, secret -> vm.addField(label, secret) })
    }
    generatorFor?.let { index ->
        PasswordGeneratorDialog(
            onDismiss = { generatorFor = null },
            onUse = { value ->
                if (index in vm.fields.indices) vm.updateField(index, value)
                revealed[index] = true
            },
        )
    }
}

@Composable
private fun VaultFieldInput(
    field: VaultField,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    onValueChange: (String) -> Unit,
    onCopy: () -> Unit,
    onGenerate: () -> Unit,
    onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = field.value,
        onValueChange = onValueChange,
        label = { Text(field.label) },
        singleLine = true,
        visualTransformation = if (field.secret && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (field.secret) {
            KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)
        } else {
            KeyboardOptions.Default
        },
        trailingIcon = {
            Row {
                if (field.secret) {
                    IconButton(onClick = onToggleReveal) {
                        Icon(
                            if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (revealed) "Sembunyikan" else "Tampilkan",
                        )
                    }
                }
                if (field.value.isNotEmpty()) {
                    IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Salin") }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Opsi kolom") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (field.secret) {
                            DropdownMenuItem(
                                text = { Text("Buat kata sandi acak") },
                                leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) },
                                onClick = {
                                    menu = false
                                    onGenerate()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Hapus kolom") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            onClick = {
                                menu = false
                                onRemove()
                            },
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AddFieldDialog(onDismiss: () -> Unit, onAdd: (String, Boolean) -> Unit) {
    var label by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah kolom") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nama kolom") },
                    placeholder = { Text("mis. Pertanyaan keamanan") },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rahasiakan isinya", modifier = Modifier.weight(1f))
                    Switch(checked = secret, onCheckedChange = { secret = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank(),
                onClick = {
                    onAdd(label, secret)
                    onDismiss()
                },
            ) { Text("Tambah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun PasswordGeneratorDialog(onDismiss: () -> Unit, onUse: (String) -> Unit) {
    var length by remember { mutableFloatStateOf(16f) }
    var upper by remember { mutableStateOf(true) }
    var lower by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var value by remember { mutableStateOf(PasswordGenerator.generate(16, true, true, true, true)) }
    fun regenerate() {
        value = PasswordGenerator.generate(length.toInt(), upper, lower, digits, symbols)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat kata sandi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(value, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        IconButton(onClick = { regenerate() }) { Icon(Icons.Filled.Refresh, contentDescription = "Buat ulang") }
                    }
                }
                StrengthIndicator(value)
                Text("Panjang: ${length.toInt()} karakter", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = length,
                    onValueChange = {
                        length = it
                        regenerate()
                    },
                    valueRange = 8f..40f,
                    steps = 31,
                )
                OptionSwitch("Huruf besar (A–Z)", upper) { upper = it; regenerate() }
                OptionSwitch("Huruf kecil (a–z)", lower) { lower = it; regenerate() }
                OptionSwitch("Angka (0–9)", digits) { digits = it; regenerate() }
                OptionSwitch("Simbol (!@#…)", symbols) { symbols = it; regenerate() }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUse(value)
                onDismiss()
            }) { Text("Gunakan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun OptionSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
