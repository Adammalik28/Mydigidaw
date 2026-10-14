@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.mydigidaw.ui.vault

import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mydigidaw.data.model.VaultType
import com.example.mydigidaw.data.repo.VaultItem
import com.example.mydigidaw.security.AuthResult
import com.example.mydigidaw.security.BiometricAuth
import com.example.mydigidaw.security.SecureClipboard
import com.example.mydigidaw.security.SecureWindow
import com.example.mydigidaw.security.findActivity
import com.example.mydigidaw.ui.LocalAppContainer
import com.example.mydigidaw.ui.components.Spacing
import com.example.mydigidaw.ui.components.ConfirmDialog
import com.example.mydigidaw.ui.components.EmojiBadge
import com.example.mydigidaw.ui.components.EmptyState
import com.example.mydigidaw.ui.components.Meter
import com.example.mydigidaw.ui.navigation.Routes
import com.example.mydigidaw.ui.theme.AppColors
import kotlinx.coroutines.launch

/** Mencegah tangkapan layar & pratinjau aplikasi terbaru selama layar sensitif tampil. */
@Composable
fun SecureScreenEffect() {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        SecureWindow.acquire(activity)
        onDispose { SecureWindow.release(activity) }
    }
}

@Composable
fun VaultTab(
    vm: VaultViewModel,
    showTypePicker: Boolean,
    onDismissTypePicker: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    SecureScreenEffect()
    val configured by vm.configured.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    when {
        configured == null -> Box(Modifier.fillMaxSize())
        configured == false -> VaultSetup(vm)
        !unlocked -> VaultUnlock(vm)
        else -> VaultList(vm, onNavigate)
    }
    if (showTypePicker) {
        VaultTypeSheet(
            onDismiss = onDismissTypePicker,
            onPick = { type ->
                onDismissTypePicker()
                onNavigate(Routes.vaultEdit(type = type.name))
            },
        )
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Sembunyikan" else "Tampilkan",
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun StrengthIndicator(password: String, modifier: Modifier = Modifier) {
    val ext = AppColors.current
    val score = PasswordGenerator.strength(password)
    val color = when (score) {
        0, 1 -> ext.critical
        2 -> ext.warning
        else -> ext.good
    }
    Column(modifier.fillMaxWidth()) {
        Meter(progress = if (password.isEmpty()) 0f else (score + 1) / 5f, color = color, height = 6.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            if (password.isEmpty()) "Kekuatan kata sandi" else "Kekuatan: ${PasswordGenerator.strengthLabels[score]}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VaultSetup(vm: VaultViewModel) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🔐", fontSize = 56.sp)
        Text("Buat Brankas Rahasia", style = MaterialTheme.typography.titleLarge)
        Text(
            "Simpan kata sandi, PIN, nomor kartu, rekening, dan data sensitif lain. Seluruh isi brankas " +
                "dienkripsi AES-256 menggunakan kata sandi utama Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PasswordField(password, { password = it }, "Kata sandi utama")
        StrengthIndicator(password)
        PasswordField(confirm, { confirm = it }, "Ulangi kata sandi utama", imeAction = ImeAction.Done, onDone = { vm.setup(password, confirm) })
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(
                "⚠️ Kata sandi utama tidak dapat dipulihkan. Jika lupa, isi brankas tidak bisa dibuka oleh siapa pun.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
            )
        }
        Button(onClick = { vm.setup(password, confirm) }, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Buat brankas")
        }
    }
}

@Composable
private fun VaultUnlock(vm: VaultViewModel) {
    val bioEnabled by vm.biometricEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity() as? FragmentActivity
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }

    val tryBiometric: () -> Unit = {
        scope.launch {
            val cipher = vm.biometricCipher()
            if (cipher == null) {
                vm.error = "Biometrik tidak tersedia. Gunakan kata sandi utama."
                return@launch
            }
            if (activity == null) return@launch
            val result = container.appLock.withAuth {
                BiometricAuth.authenticate(
                    activity,
                    title = "Buka Brankas",
                    subtitle = "Verifikasi sidik jari atau wajah",
                    authenticators = BIOMETRIC_STRONG,
                    negativeText = "Pakai kata sandi",
                    crypto = BiometricPrompt.CryptoObject(cipher),
                )
            }
            if (result is AuthResult.Success) result.cryptoObject?.cipher?.let { vm.unlockWithCipher(it) }
        }
    }
    LaunchedEffect(bioEnabled) { if (bioEnabled) tryBiometric() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("🔒", fontSize = 56.sp)
        Text("Brankas terkunci", style = MaterialTheme.typography.titleLarge)
        Text(
            "Masukkan kata sandi utama untuk melihat isi brankas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PasswordField(password, { password = it }, "Kata sandi utama", imeAction = ImeAction.Done, onDone = { vm.unlock(password) })
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Button(onClick = { vm.unlock(password) }, enabled = !vm.busy && password.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            if (vm.busy) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.LockOpen, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Buka brankas")
            }
        }
        if (bioEnabled) {
            OutlinedButton(onClick = tryBiometric, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Fingerprint, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Gunakan biometrik")
            }
        }
    }
}

@Composable
private fun VaultList(vm: VaultViewModel, onNavigate: (String) -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val typeFilter by vm.typeFilter.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val container = LocalAppContainer.current
    var confirmDelete by remember { mutableStateOf<VaultItem?>(null) }

    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.fabClearance), modifier = Modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                placeholder = { Text("Cari di brankas…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Filled.Close, "Hapus") }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = typeFilter == null, onClick = { vm.typeFilter.value = null }, label = { Text("Semua") })
                VaultType.entries.forEach { t ->
                    FilterChip(
                        selected = typeFilter == t,
                        onClick = { vm.typeFilter.value = if (typeFilter == t) null else t },
                        label = { Text("${t.emoji} ${t.label}") },
                    )
                }
            }
        }
        item {
            Text(
                "🔓 Brankas terbuka — otomatis terkunci saat aplikasi ditinggalkan.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (items.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🗝️",
                    title = if (query.isBlank() && typeFilter == null) "Brankas masih kosong" else "Tidak ditemukan",
                    message = if (query.isBlank() && typeFilter == null) "Ketuk Item rahasia untuk menyimpan kata sandi, PIN, atau nomor kartu." else null,
                )
            }
        }
        items(items, key = { it.id }) { item ->
            VaultRow(
                item = item,
                onOpen = { onNavigate(Routes.vaultEdit(item.id)) },
                onCopy = {
                    val secret = item.fields.firstOrNull { it.secret && it.value.isNotBlank() }
                        ?: item.fields.firstOrNull { it.value.isNotBlank() }
                    if (secret != null) {
                        SecureClipboard.copy(context, secret.label, secret.value, secret.secret, container.appScope)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            Toast.makeText(context, "${secret.label} disalin", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onFavorite = { vm.toggleFavorite(item) },
                onDelete = { confirmDelete = item },
            )
        }
    }

    confirmDelete?.let { item ->
        ConfirmDialog(
            title = "Hapus \"${item.title}\"?",
            message = "Item brankas ini akan dihapus permanen.",
            onConfirm = { vm.delete(item) },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun VaultRow(item: VaultItem, onOpen: () -> Unit, onCopy: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        ListItem(
            headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(item.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingContent = { EmojiBadge(item.type.emoji, MaterialTheme.colorScheme.primary) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.favorite) {
                        Icon(Icons.Filled.Star, contentDescription = "Favorit", tint = AppColors.current.warning, modifier = Modifier.size(18.dp))
                    }
                    if (item.fields.any { it.value.isNotBlank() }) {
                        IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Salin") }
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.combinedClickable(onClick = onOpen, onLongClick = { menu = true }),
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (item.favorite) "Hapus dari favorit" else "Jadikan favorit") },
                leadingIcon = { Icon(if (item.favorite) Icons.Outlined.StarOutline else Icons.Filled.Star, null) },
                onClick = {
                    menu = false
                    onFavorite()
                },
            )
            DropdownMenuItem(
                text = { Text("Hapus") },
                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                onClick = {
                    menu = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun VaultTypeSheet(onDismiss: () -> Unit, onPick: (VaultType) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            "Simpan apa?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        VaultType.entries.forEach { type ->
            ListItem(
                headlineContent = { Text(type.label) },
                supportingContent = {
                    Text(
                        type.template.joinToString(", ") { it.label }.ifBlank { "Teks rahasia bebas" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = { EmojiBadge(type.emoji, MaterialTheme.colorScheme.primary) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.combinedClickable(onClick = { onPick(type) }),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
