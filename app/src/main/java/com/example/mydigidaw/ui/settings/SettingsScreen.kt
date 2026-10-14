@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mydigidaw.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.outlined.AddToDrive
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.documentfile.provider.DocumentFile
import com.example.mydigidaw.data.prefs.AutoBackupKind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import com.example.mydigidaw.ui.components.AppCard
import com.example.mydigidaw.ui.components.IconTile
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.data.backup.BackupPasswordException
import com.example.mydigidaw.data.google.DeviceCalendar
import com.example.mydigidaw.data.model.ThemeMode
import com.example.mydigidaw.data.prefs.AppSettings
import com.example.mydigidaw.reminders.Notifications
import com.example.mydigidaw.security.AuthResult
import com.example.mydigidaw.security.BiometricAuth
import com.example.mydigidaw.security.findActivity
import com.example.mydigidaw.ui.LocalAppContainer
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.components.BackTopBar
import com.example.mydigidaw.ui.components.ConfirmDialog
import com.example.mydigidaw.ui.vault.PasswordField
import com.example.mydigidaw.ui.vault.StrengthIndicator
import com.example.mydigidaw.util.Dates
import java.time.LocalDate
import javax.crypto.Cipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(val c: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings?> = c.settings.settings.map<AppSettings, AppSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val vaultConfigured: StateFlow<Boolean> = c.vault.configured.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val vaultBio: StateFlow<Boolean> = c.vault.biometricEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val vaultUnlocked: StateFlow<Boolean> = c.vault.unlocked

    var calendars by mutableStateOf<List<DeviceCalendar>>(emptyList())
        private set
    var calendarPermission by mutableStateOf(c.google.hasPermission())
        private set

    fun refreshCalendars() = viewModelScope.launch {
        calendarPermission = c.google.hasPermission()
        calendars = c.google.calendars()
    }

    fun onCalendarPermission(granted: Boolean) = viewModelScope.launch {
        calendarPermission = granted
        if (!granted) return@launch
        calendars = c.google.calendars()
        c.settings.setGcalShow(true)
        if (c.settings.current().gcalTargetId < 0) {
            val writable = calendars.filter { it.writable }
            (writable.firstOrNull { it.isGoogle && it.isPrimary } ?: writable.firstOrNull { it.isGoogle } ?: writable.firstOrNull())?.let {
                c.settings.setGcalTarget(it.id)
                c.settings.setGcalSyncNew(true)
            }
        }
    }

    private fun edit(block: suspend () -> Unit) = viewModelScope.launch { block() }

    fun setName(v: String) = edit { c.settings.setUserName(v) }
    fun setTheme(v: ThemeMode) = edit { c.settings.setThemeMode(v) }
    fun setDynamic(v: Boolean) = edit { c.settings.setDynamicColor(v) }
    fun setWeekMonday(v: Boolean) = edit { c.settings.setWeekStartsMonday(v) }
    fun setAppLock(v: Boolean) = edit { c.settings.setAppLock(v) }
    fun setLockTimeout(v: Int) = edit { c.settings.setLockTimeout(v) }
    fun setSecure(v: Boolean) = edit { c.settings.setSecureScreen(v) }
    fun setHideAmounts(v: Boolean) = edit { c.settings.setHideAmounts(v) }
    fun setVaultAutoLock(v: Int) = edit { c.settings.setVaultAutoLock(v) }
    fun setGcalShow(v: Boolean) = edit { c.settings.setGcalShow(v) }
    fun setGcalSyncNew(v: Boolean) = edit { c.settings.setGcalSyncNew(v) }
    fun setGcalTarget(id: Long) = edit { c.settings.setGcalTarget(id) }
    fun setGcalHidden(ids: Set<Long>) = edit { c.settings.setGcalHidden(ids) }
    fun setAutoBackupEvery(days: Int) = edit { c.settings.setAutoBackupEvery(days) }

    suspend fun syncAllToGoogle(): Int {
        val target = c.settings.current().gcalTargetId
        return if (target >= 0) c.calendar.syncAllToGoogle(target) else -1
    }

    suspend fun changeVaultPassword(old: String, new: String): Boolean = c.vault.changePassword(old.toCharArray(), new.toCharArray())

    suspend fun resetVault() {
        c.vaultItems.deleteAll()
        c.vault.reset()
    }

    fun biometricEncryptCipher(): Cipher? = runCatching { c.vault.biometricEncryptCipher() }.getOrNull()
    suspend fun enableVaultBiometric(cipher: Cipher): Boolean = c.vault.enableBiometric(cipher)
    fun disableVaultBiometric() = edit { c.vault.disableBiometric() }
}

private enum class SettingsDialog { NAME, THEME, LOCK_TIMEOUT, VAULT_AUTOLOCK, VAULT_PASSWORD, VAULT_RESET, CAL_TARGET, CAL_VISIBLE, AUTO_FREQ }

private val lockTimeouts = listOf(0 to "Segera", 30 to "Setelah 30 detik", 60 to "Setelah 1 menit", 300 to "Setelah 5 menit", 900 to "Setelah 15 menit")
private val vaultTimeouts = listOf(0 to "Segera saat keluar aplikasi", 60 to "Setelah 1 menit", 300 to "Setelah 5 menit", 900 to "Setelah 15 menit")

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it) }
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val activity = context.findActivity() as? FragmentActivity
    val scope = rememberCoroutineScope()
    val state by vm.settings.collectAsStateWithLifecycle()
    val vaultConfigured by vm.vaultConfigured.collectAsStateWithLifecycle()
    val vaultBio by vm.vaultBio.collectAsStateWithLifecycle()
    val vaultUnlocked by vm.vaultUnlocked.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var notificationsAllowed by remember { mutableStateOf(Notifications.canPost(context)) }

    LifecycleResumeEffect(Unit) {
        vm.refreshCalendars()
        notificationsAllowed = Notifications.canPost(context)
        onPauseOrDispose { }
    }

    val toast = { msg: String -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    fun runInApp(block: suspend () -> String) {
        container.appScope.launch {
            val message = runCatching { block() }.getOrElse { "Gagal: ${it.message}" }
            withContext(Dispatchers.Main) { toast(message) }
        }
    }

    var backupPassword by remember { mutableStateOf<String?>(null) }
    var askBackupPassword by remember { mutableStateOf(false) }
    var restorePasswordFor by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val password = backupPassword?.takeIf { it.isNotEmpty() }
        backupPassword = null
        if (uri != null) {
            runInApp {
                val count = container.backup.export(uri, password?.toCharArray())
                if (password != null) "Cadangan terenkripsi tersimpan ($count data)" else "Cadangan tersimpan ($count data)"
            }
        }
    }
    // Kata sandi cadangan otomatis hanya dipegang di memori sampai lokasi dipilih, lalu diubah jadi kunci.
    var showAutoSetup by remember { mutableStateOf(false) }
    var pendingAutoPassword by remember { mutableStateOf<String?>(null) }
    fun finishAutoSetup(uri: Uri?, kind: AutoBackupKind) {
        val password = pendingAutoPassword
        pendingAutoPassword = null
        if (uri == null) return
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        runInApp {
            val key = password?.takeIf { it.isNotEmpty() }?.let { container.backup.createAutoBackupKey(it.toCharArray()) }
            container.settings.configureAutoBackup(uri.toString(), kind, describeBackupLocation(context, uri, kind), key)
            val name = container.backup.runAutoBackup()
            "Cadangan otomatis aktif · $name tersimpan"
        }
    }
    val autoFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        finishAutoSetup(uri, AutoBackupKind.FILE)
    }
    val autoFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        finishAutoSetup(uri, AutoBackupKind.FOLDER)
    }
    fun disableAutoBackup(current: AppSettings) {
        current.autoBackupUri?.let { uri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(uri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        scope.launch { container.settings.clearAutoBackup() }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runInApp { "${container.backup.exportTransactionsCsv(uri)} transaksi diekspor" }
    }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        vm.onCalendarPermission(result.values.all { it })
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted && Notifications.canPost(context)
    }

    val enableAppLock: () -> Unit = {
        if (!BiometricAuth.canUseDeviceAuth(context)) {
            toast("Atur dulu kunci layar (PIN, pola, atau sidik jari) di pengaturan HP")
        } else if (activity != null) {
            scope.launch {
                val result = container.appLock.withAuth {
                    BiometricAuth.authenticate(activity, "Aktifkan kunci aplikasi", "Konfirmasi identitas Anda")
                }
                if (result is AuthResult.Success) vm.setAppLock(true)
            }
        }
    }

    val enableVaultBio: () -> Unit = enable@{
        if (!vaultUnlocked) {
            toast("Buka brankas terlebih dahulu (Catatan › Brankas), lalu aktifkan biometrik")
            return@enable
        }
        val cipher = vm.biometricEncryptCipher()
        if (cipher == null || activity == null) {
            toast("Biometrik tidak tersedia di perangkat ini")
            return@enable
        }
        scope.launch {
            val result = container.appLock.withAuth {
                BiometricAuth.authenticate(
                    activity,
                    title = "Aktifkan biometrik brankas",
                    subtitle = "Verifikasi sidik jari atau wajah",
                    authenticators = BIOMETRIC_STRONG,
                    negativeText = "Batal",
                    crypto = BiometricPrompt.CryptoObject(cipher),
                )
            }
            if (result is AuthResult.Success) {
                val ok = result.cryptoObject?.cipher?.let { vm.enableVaultBiometric(it) } ?: false
                toast(if (ok) "Brankas kini bisa dibuka dengan biometrik" else "Gagal mengaktifkan biometrik")
            }
        }
    }

    Scaffold(topBar = { BackTopBar(title = "Pengaturan", onBack = onBack) }) { padding ->
        val s = state ?: return@Scaffold
        val canBio = BiometricAuth.canUseStrongBiometric(context)
        val target = vm.calendars.firstOrNull { it.id == s.gcalTargetId }
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
        ) {
            SettingsGroup("Profil") {
                SettingRow(Icons.Outlined.Person, "Nama panggilan", s.userName.ifBlank { "Belum diatur" }) { dialog = SettingsDialog.NAME }
            }

            SettingsGroup("Tampilan") {
                SettingRow(Icons.Outlined.DarkMode, "Tema", s.themeMode.label) { dialog = SettingsDialog.THEME }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(Icons.Outlined.Palette, "Warna dinamis (Material You)", "Ikuti warna wallpaper HP", s.dynamicColor, onChange = vm::setDynamic)
                }
                SettingRow(Icons.Outlined.CalendarViewWeek, "Awal minggu di kalender", if (s.weekStartsMonday) "Senin" else "Minggu") {
                    vm.setWeekMonday(!s.weekStartsMonday)
                }
            }

            SettingsGroup("Keamanan & privasi") {
                SwitchRow(Icons.Outlined.Lock, "Kunci aplikasi", "Minta sidik jari, wajah, atau PIN saat membuka", s.appLock) { on ->
                    if (on) enableAppLock() else vm.setAppLock(false)
                }
                if (s.appLock) {
                    SettingRow(Icons.Outlined.Timer, "Kunci otomatis", lockTimeouts.firstOrNull { it.first == s.lockTimeoutSec }?.second ?: "Segera") {
                        dialog = SettingsDialog.LOCK_TIMEOUT
                    }
                }
                SwitchRow(Icons.Outlined.NoPhotography, "Blokir tangkapan layar", "Sembunyikan isi aplikasi dari screenshot & layar terbaru", s.secureScreen, onChange = vm::setSecure)
                SwitchRow(Icons.Outlined.VisibilityOff, "Sembunyikan nominal uang", "Samarkan saldo di Beranda & Keuangan", s.hideAmounts, onChange = vm::setHideAmounts)
            }

            SettingsGroup("Brankas") {
                if (!vaultConfigured) {
                    InfoText("Brankas belum dibuat. Buka menu Catatan › Brankas untuk membuat kata sandi utama.")
                } else {
                    SwitchRow(
                        Icons.Outlined.Fingerprint,
                        "Buka brankas dengan biometrik",
                        if (canBio) "Sidik jari atau wajah" else "Daftarkan sidik jari/wajah di pengaturan HP terlebih dahulu",
                        vaultBio,
                        enabled = canBio || vaultBio,
                    ) { on -> if (on) enableVaultBio() else vm.disableVaultBiometric() }
                    SettingRow(Icons.Outlined.LockClock, "Kunci brankas otomatis", vaultTimeouts.firstOrNull { it.first == s.vaultAutoLockSec }?.second ?: "Setelah 1 menit") {
                        dialog = SettingsDialog.VAULT_AUTOLOCK
                    }
                    SettingRow(Icons.Outlined.Password, "Ubah kata sandi utama", null) { dialog = SettingsDialog.VAULT_PASSWORD }
                    SettingRow(
                        Icons.Outlined.DeleteForever,
                        "Reset brankas",
                        "Hapus seluruh isi brankas bila lupa kata sandi utama",
                        destructive = true,
                    ) { dialog = SettingsDialog.VAULT_RESET }
                }
            }

            SettingsGroup("Google Calendar") {
                if (!vm.calendarPermission) {
                    SettingRow(Icons.Outlined.Event, "Hubungkan Google Calendar", "Izinkan akses ke kalender di HP (akun Google)") {
                        calendarPermission.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                    }
                    InfoText(
                        "Mydigidaw memakai kalender bawaan Android yang tersinkron dengan akun Google di HP Anda, " +
                            "sehingga jadwal otomatis muncul di Google Calendar (dan sebaliknya) tanpa login tambahan.",
                    )
                } else {
                    SwitchRow(Icons.Outlined.Visibility, "Tampilkan acara Google", "Acara dari Google Calendar muncul di kalender aplikasi", s.gcalShow, onChange = vm::setGcalShow)
                    SettingRow(Icons.Outlined.EventAvailable, "Kalender tujuan", target?.let { "${it.name} · ${it.account}" } ?: "Pilih kalender") {
                        dialog = SettingsDialog.CAL_TARGET
                    }
                    SwitchRow(Icons.Outlined.CloudUpload, "Simpan jadwal baru ke Google", "Aktif secara bawaan saat membuat jadwal", s.gcalSyncNew, onChange = vm::setGcalSyncNew)
                    val visible = vm.calendars.count { it.id !in s.gcalHiddenIds }
                    SettingRow(Icons.Outlined.FilterList, "Kalender yang ditampilkan", "$visible dari ${vm.calendars.size} kalender") {
                        dialog = SettingsDialog.CAL_VISIBLE
                    }
                    SettingRow(Icons.Outlined.Sync, "Sinkronkan semua jadwal lokal", "Kirim jadwal yang belum tersinkron ke kalender tujuan") {
                        scope.launch {
                            val n = vm.syncAllToGoogle()
                            toast(if (n < 0) "Pilih kalender tujuan terlebih dahulu" else "$n jadwal dikirim ke Google Calendar")
                        }
                    }
                }
            }

            SettingsGroup("Notifikasi") {
                SettingRow(
                    Icons.Outlined.Notifications,
                    "Izin notifikasi",
                    if (notificationsAllowed) "Diizinkan" else "Belum diizinkan — ketuk untuk mengizinkan",
                ) {
                    if (!notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        container.appLock.allowExternalFlow()
                        openNotificationSettings(context)
                    }
                }
                SettingRow(Icons.Outlined.NotificationsActive, "Uji notifikasi", "Kirim contoh notifikasi sekarang") {
                    if (notificationsAllowed) Notifications.showTest(context) else toast("Izinkan notifikasi terlebih dahulu")
                }
            }

            SettingsGroup("Data") {
                SettingRow(Icons.Outlined.Backup, "Cadangkan data", "Simpan semua data ke berkas JSON, bisa dilindungi kata sandi") {
                    askBackupPassword = true
                }
                if (s.autoBackupUri == null) {
                    SettingRow(Icons.Outlined.CloudSync, "Cadangan otomatis", "Simpan cadangan rutin ke Google Drive atau folder di HP") {
                        showAutoSetup = true
                    }
                } else {
                    val status = buildList {
                        add(s.autoBackupLabel.ifBlank { "Lokasi pilihan" })
                        add(if (s.autoBackupLastAt > 0) "terakhir ${Dates.dateTime(s.autoBackupLastAt)}" else "belum pernah")
                        if (s.autoBackupEncrypted) add("terenkripsi")
                    }.joinToString(" · ")
                    SwitchRow(Icons.Outlined.CloudSync, "Cadangan otomatis", status, checked = true) { on ->
                        if (!on) disableAutoBackup(s)
                    }
                    s.autoBackupError?.let { error ->
                        Text(
                            "Cadangan terakhir gagal: $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 72.dp, end = 16.dp, bottom = 8.dp),
                        )
                    }
                    SettingRow(Icons.Outlined.Schedule, "Frekuensi", if (s.autoBackupEveryDays <= 1) "Setiap hari" else "Setiap minggu") {
                        dialog = SettingsDialog.AUTO_FREQ
                    }
                    SettingRow(Icons.Outlined.CloudUpload, "Cadangkan sekarang", "Jalankan cadangan otomatis saat ini juga") {
                        runInApp { "Cadangan tersimpan · ${container.backup.runAutoBackup()}" }
                    }
                }
                SettingRow(Icons.Outlined.Restore, "Pulihkan data", "Ganti semua data dengan isi berkas cadangan") {
                    container.appLock.allowExternalFlow()
                    importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
                }
                SettingRow(Icons.Outlined.TableChart, "Ekspor transaksi (CSV)", "Untuk dibuka di Excel atau Google Sheets") {
                    container.appLock.allowExternalFlow()
                    csvLauncher.launch("transaksi-mydigidaw-${LocalDate.now()}.csv")
                }
            }

            SettingsGroup("Tentang") {
                SettingRow(Icons.Outlined.Info, "Mydigidaw", "Versi ${versionName(context)} · dibuat untuk Android 16") {}
                SettingRow(Icons.Outlined.FontDownload, "Huruf Plus Jakarta Sans", "© Tokotype — SIL Open Font License 1.1") {}
            }
        }
    }

    val s = state ?: return
    when (dialog) {
        SettingsDialog.NAME -> TextInputDialog("Nama panggilan", s.userName, onDismiss = { dialog = null }, onSave = vm::setName)
        SettingsDialog.THEME -> ChoiceDialog(
            title = "Tema",
            options = ThemeMode.entries.map { it to it.label },
            selected = s.themeMode,
            onDismiss = { dialog = null },
            onSelect = vm::setTheme,
        )
        SettingsDialog.LOCK_TIMEOUT -> ChoiceDialog(
            title = "Kunci otomatis",
            options = lockTimeouts,
            selected = s.lockTimeoutSec,
            onDismiss = { dialog = null },
            onSelect = vm::setLockTimeout,
        )
        SettingsDialog.VAULT_AUTOLOCK -> ChoiceDialog(
            title = "Kunci brankas otomatis",
            options = vaultTimeouts,
            selected = s.vaultAutoLockSec,
            onDismiss = { dialog = null },
            onSelect = vm::setVaultAutoLock,
        )
        SettingsDialog.VAULT_PASSWORD -> ChangeVaultPasswordDialog(
            onDismiss = { dialog = null },
            onSubmit = { old, new ->
                scope.launch {
                    val ok = vm.changeVaultPassword(old, new)
                    toast(if (ok) "Kata sandi utama berhasil diubah" else "Kata sandi lama salah")
                    if (ok) dialog = null
                }
            },
        )
        SettingsDialog.VAULT_RESET -> ConfirmDialog(
            title = "Reset brankas?",
            message = "SELURUH isi brankas akan dihapus permanen dan tidak bisa dipulihkan. Lanjutkan hanya jika Anda lupa kata sandi utama.",
            confirmLabel = "Hapus semua",
            onConfirm = {
                scope.launch {
                    vm.resetVault()
                    toast("Brankas telah direset")
                }
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.CAL_TARGET -> ChoiceDialog(
            title = "Kalender tujuan",
            options = vm.calendars.filter { it.writable }.map { it.id to "${it.name} · ${it.account}" },
            selected = s.gcalTargetId,
            onDismiss = { dialog = null },
            onSelect = vm::setGcalTarget,
        )
        SettingsDialog.AUTO_FREQ -> ChoiceDialog(
            title = "Frekuensi cadangan",
            options = listOf(1 to "Setiap hari", 7 to "Setiap minggu"),
            selected = if (s.autoBackupEveryDays <= 1) 1 else 7,
            onDismiss = { dialog = null },
            onSelect = vm::setAutoBackupEvery,
        )
        SettingsDialog.CAL_VISIBLE -> VisibleCalendarsDialog(
            calendars = vm.calendars,
            hidden = s.gcalHiddenIds,
            onDismiss = { dialog = null },
            onSave = vm::setGcalHidden,
        )
        null -> Unit
    }

    pendingRestore?.let { uri ->
        ConfirmDialog(
            title = "Pulihkan dari cadangan?",
            message = "Semua data saat ini (catatan, keuangan, jadwal, tugas, kebiasaan, brankas) akan DIGANTI dengan isi berkas cadangan.",
            confirmLabel = "Pulihkan",
            onConfirm = {
                scope.launch {
                    if (container.backup.isEncrypted(uri)) restorePasswordFor = uri
                    else runInApp { "Pemulihan selesai (${container.backup.import(uri)} data)" }
                }
            },
            onDismiss = { pendingRestore = null },
        )
    }
    if (showAutoSetup) {
        AutoBackupSetupDialog(
            onDismiss = { showAutoSetup = false },
            onConfirm = { kind, password ->
                showAutoSetup = false
                pendingAutoPassword = password
                container.appLock.allowExternalFlow()
                when (kind) {
                    AutoBackupKind.FILE -> autoFileLauncher.launch("mydigidaw-cadangan.json")
                    AutoBackupKind.FOLDER -> autoFolderLauncher.launch(null)
                }
            },
        )
    }
    if (askBackupPassword) {
        BackupPasswordDialog(
            onDismiss = { askBackupPassword = false },
            onConfirm = { password ->
                askBackupPassword = false
                backupPassword = password
                container.appLock.allowExternalFlow()
                exportLauncher.launch("mydigidaw-cadangan-${LocalDate.now()}.json")
            },
        )
    }
    restorePasswordFor?.let { uri ->
        RestorePasswordDialog(
            onDismiss = { restorePasswordFor = null },
            onConfirm = { password ->
                restorePasswordFor = null
                runInApp {
                    try {
                        "Pemulihan selesai (${container.backup.import(uri, password.toCharArray())} data)"
                    } catch (e: BackupPasswordException) {
                        e.message ?: "Kata sandi salah"
                    }
                }
            },
        )
    }
}

private fun versionName(context: Context): String = runCatching {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    info.versionName ?: "1.0"
}.getOrDefault("1.0")

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/** Satu kelompok pengaturan: judul kecil di atas kartu berisi baris-baris terkait. */
@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, top = 20.dp, bottom = 8.dp),
    )
    AppCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun InfoText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        headlineContent = { Text(title, color = color) },
        supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        leadingContent = { IconTile(icon, if (destructive) color else MaterialTheme.colorScheme.primary, size = 36.dp) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        leadingContent = { IconTile(icon, MaterialTheme.colorScheme.primary, size = 36.dp) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(enabled = enabled) { onChange(!checked) },
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (options.isEmpty()) {
                    Text("Tidak ada pilihan tersedia.", style = MaterialTheme.typography.bodyMedium)
                }
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

@Composable
private fun TextInputDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = value, onValueChange = { value = it.take(30) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(value)
                onDismiss()
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun ChangeVaultPasswordDialog(onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val error = when {
        new.isNotEmpty() && new.length < 6 -> "Minimal 6 karakter"
        confirm.isNotEmpty() && confirm != new -> "Konfirmasi tidak cocok"
        else -> null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah kata sandi utama") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PasswordField(old, { old = it }, "Kata sandi lama")
                PasswordField(new, { new = it }, "Kata sandi baru")
                StrengthIndicator(new)
                PasswordField(confirm, { confirm = it }, "Ulangi kata sandi baru")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = old.isNotEmpty() && new.length >= 6 && new == confirm,
                onClick = { onSubmit(old, new) },
            ) { Text("Ubah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun VisibleCalendarsDialog(
    calendars: List<DeviceCalendar>,
    hidden: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (Set<Long>) -> Unit,
) {
    var current by remember { mutableStateOf(hidden) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kalender yang ditampilkan") },
        text = {
            LazyColumn {
                items(calendars, key = { it.id }) { cal ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { current = if (cal.id in current) current - cal.id else current + cal.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = cal.id !in current, onCheckedChange = null)
                        Column(Modifier.padding(start = 8.dp, top = 6.dp, bottom = 6.dp)) {
                            Text(cal.name, style = MaterialTheme.typography.bodyLarge)
                            Text(cal.account, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(current)
                onDismiss()
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun BackupPasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = password.isNotEmpty() && confirm.isNotEmpty() && password != confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadangkan data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Lindungi berkas cadangan dengan kata sandi (disarankan) — catatan dan keuangan ikut terenkripsi. " +
                        "Kosongkan bila tidak perlu.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                PasswordField(password, { password = it }, "Kata sandi cadangan (opsional)")
                if (password.isNotEmpty()) PasswordField(confirm, { confirm = it }, "Ulangi kata sandi")
                if (mismatch) Text("Kata sandi tidak cocok", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = password.isEmpty() || (password.length >= 4 && password == confirm),
                onClick = { onConfirm(password) },
            ) { Text(if (password.isEmpty()) "Lanjut tanpa sandi" else "Lanjut") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun RestorePasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Berkas terenkripsi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Masukkan kata sandi yang dipakai saat membuat cadangan ini.", style = MaterialTheme.typography.bodyMedium)
                PasswordField(password, { password = it }, "Kata sandi cadangan")
            }
        },
        confirmButton = { TextButton(enabled = password.isNotEmpty(), onClick = { onConfirm(password) }) { Text("Pulihkan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

/** Nama lokasi cadangan yang mudah dibaca, mis. "Google Drive · mydigidaw-cadangan.json". */
private fun describeBackupLocation(context: Context, uri: Uri, kind: AutoBackupKind): String {
    val authority = uri.authority.orEmpty()
    val provider = when {
        authority.startsWith("com.google.android.apps.docs") -> "Google Drive"
        authority == "com.android.externalstorage.documents" -> "Penyimpanan HP"
        authority.contains("downloads") -> "Unduhan"
        else -> null
    }
    val name = runCatching {
        if (kind == AutoBackupKind.FOLDER) DocumentFile.fromTreeUri(context, uri)?.name else DocumentFile.fromSingleUri(context, uri)?.name
    }.getOrNull()
    return listOfNotNull(provider, name).joinToString(" · ").ifBlank { "Lokasi pilihan" }
}

@Composable
private fun AutoBackupSetupDialog(onDismiss: () -> Unit, onConfirm: (AutoBackupKind, String) -> Unit) {
    var kind by remember { mutableStateOf(AutoBackupKind.FILE) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = password.isNotEmpty() && confirm.isNotEmpty() && password != confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadangan otomatis") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Simpan ke mana?", style = MaterialTheme.typography.titleSmall)
                BackupLocationOption(
                    selected = kind == AutoBackupKind.FILE,
                    icon = Icons.Outlined.AddToDrive,
                    title = "Google Drive",
                    subtitle = "Satu berkas di Drive yang diperbarui otomatis. Pilih \"Drive\" di layar berikutnya.",
                    onClick = { kind = AutoBackupKind.FILE },
                )
                BackupLocationOption(
                    selected = kind == AutoBackupKind.FOLDER,
                    icon = Icons.Outlined.Folder,
                    title = "Folder di HP",
                    subtitle = "Menyimpan 5 cadangan terbaru di folder pilihan Anda.",
                    onClick = { kind = AutoBackupKind.FOLDER },
                )
                Text("Kata sandi (disarankan)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
                Text(
                    "Cadangan dienkripsi AES-256 sehingga isinya tak terbaca tanpa kata sandi ini. Simpan baik-baik — " +
                        "kata sandi diperlukan saat memulihkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PasswordField(password, { password = it }, "Kata sandi cadangan (opsional)")
                if (password.isNotEmpty()) PasswordField(confirm, { confirm = it }, "Ulangi kata sandi")
                if (mismatch) Text("Kata sandi tidak cocok", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = password.isEmpty() || (password.length >= 4 && password == confirm),
                onClick = { onConfirm(kind, password) },
            ) { Text("Pilih lokasi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun BackupLocationOption(selected: Boolean, icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surfaceContainerHigh)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, scheme.primary, size = 36.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = null)
    }
}
