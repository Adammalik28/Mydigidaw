package com.example.mydigidaw.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mydigidaw.data.backup.BackupSettings
import com.example.mydigidaw.data.model.ThemeMode
import com.example.mydigidaw.data.model.enumOr
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val weekStartsMonday: Boolean = true,
    val appLock: Boolean = false,
    val lockTimeoutSec: Int = 0,
    val secureScreen: Boolean = false,
    val hideAmounts: Boolean = false,
    val gcalShow: Boolean = false,
    val gcalSyncNew: Boolean = false,
    val gcalTargetId: Long = -1,
    val gcalHiddenIds: Set<Long> = emptySet(),
    val vaultAutoLockSec: Int = 60,
    val onboarded: Boolean = false,
    val autoBackupUri: String? = null,
    val autoBackupLastAt: Long = 0,
    val autoBackupKind: AutoBackupKind = AutoBackupKind.FOLDER,
    val autoBackupEveryDays: Int = 7,
    /** Nama lokasi untuk ditampilkan, mis. "Google Drive · mydigidaw-cadangan.json". */
    val autoBackupLabel: String = "",
    val autoBackupEncrypted: Boolean = false,
    /** Pesan galat cadangan otomatis terakhir; null bila berhasil. */
    val autoBackupError: String? = null,
)

/** FOLDER = folder di HP (5 berkas bergilir); FILE = satu berkas yang ditimpa, mis. di Google Drive. */
enum class AutoBackupKind { FOLDER, FILE }

/** Kunci cadangan otomatis (turunan kata sandi) yang disegel kunci perangkat, beserta parameter PBKDF2-nya. */
data class AutoBackupKey(val sealedKey: String, val salt: String, val iterations: Int)

/** Status timer fokus yang bertahan walau aplikasi ditutup. */
data class FocusState(
    val sessionId: Long = -1,
    val endAt: Long = 0,
    val remainingMs: Long = 0,
    val plannedMinutes: Int = 25,
    val label: String = "",
    val isBreak: Boolean = false,
) {
    val running: Boolean get() = endAt > 0
    val paused: Boolean get() = endAt == 0L && remainingMs > 0
    val active: Boolean get() = running || paused
}

/** Konfigurasi brankas: kunci data dibungkus kunci turunan kata sandi utama (PBKDF2). */
data class VaultConfig(
    val salt: String,
    val iterations: Int,
    val wrappedKey: String,
    val bioWrappedKey: String?,
)

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object K {
        val userName = stringPreferencesKey("user_name")
        val theme = stringPreferencesKey("theme_mode")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val weekMonday = booleanPreferencesKey("week_monday")
        val appLock = booleanPreferencesKey("app_lock")
        val lockTimeout = intPreferencesKey("lock_timeout")
        val secureScreen = booleanPreferencesKey("secure_screen")
        val hideAmounts = booleanPreferencesKey("hide_amounts")
        val gcalShow = booleanPreferencesKey("gcal_show")
        val gcalSyncNew = booleanPreferencesKey("gcal_sync_new")
        val gcalTarget = longPreferencesKey("gcal_target")
        val gcalHidden = stringSetPreferencesKey("gcal_hidden")
        val vaultAutoLock = intPreferencesKey("vault_autolock")
        val onboarded = booleanPreferencesKey("onboarded")
        val vaultSalt = stringPreferencesKey("vault_salt")
        val vaultIter = intPreferencesKey("vault_iter")
        val vaultWrapped = stringPreferencesKey("vault_wrapped")
        val vaultBio = stringPreferencesKey("vault_bio")
        val autoBackupUri = stringPreferencesKey("auto_backup_uri")
        val autoBackupLast = longPreferencesKey("auto_backup_last")
        val autoBackupKind = stringPreferencesKey("auto_backup_kind")
        val autoBackupEvery = intPreferencesKey("auto_backup_every")
        val autoBackupLabel = stringPreferencesKey("auto_backup_label")
        val autoBackupError = stringPreferencesKey("auto_backup_error")
        val autoBackupKey = stringPreferencesKey("auto_backup_key")
        val autoBackupSalt = stringPreferencesKey("auto_backup_salt")
        val autoBackupIter = intPreferencesKey("auto_backup_iter")
        val focusSession = longPreferencesKey("focus_session")
        val focusEnd = longPreferencesKey("focus_end")
        val focusRemaining = longPreferencesKey("focus_remaining")
        val focusPlanned = intPreferencesKey("focus_planned")
        val focusLabel = stringPreferencesKey("focus_label")
        val focusBreak = booleanPreferencesKey("focus_break")
    }

    private val prefs: Flow<Preferences> = store.data.catch { emit(emptyPreferences()) }

    val settings: Flow<AppSettings> = prefs.map { p ->
        AppSettings(
            userName = p[K.userName] ?: "",
            themeMode = enumOr(p[K.theme], ThemeMode.SYSTEM),
            dynamicColor = p[K.dynamic] ?: false,
            weekStartsMonday = p[K.weekMonday] ?: true,
            appLock = p[K.appLock] ?: false,
            lockTimeoutSec = p[K.lockTimeout] ?: 0,
            secureScreen = p[K.secureScreen] ?: false,
            hideAmounts = p[K.hideAmounts] ?: false,
            gcalShow = p[K.gcalShow] ?: false,
            gcalSyncNew = p[K.gcalSyncNew] ?: false,
            gcalTargetId = p[K.gcalTarget] ?: -1L,
            gcalHiddenIds = p[K.gcalHidden].orEmpty().mapNotNull { it.toLongOrNull() }.toSet(),
            vaultAutoLockSec = p[K.vaultAutoLock] ?: 60,
            onboarded = p[K.onboarded] ?: false,
            autoBackupUri = p[K.autoBackupUri],
            autoBackupLastAt = p[K.autoBackupLast] ?: 0L,
            autoBackupKind = enumOr(p[K.autoBackupKind], AutoBackupKind.FOLDER),
            autoBackupEveryDays = p[K.autoBackupEvery] ?: 7,
            autoBackupLabel = p[K.autoBackupLabel] ?: "",
            autoBackupEncrypted = p[K.autoBackupKey] != null,
            autoBackupError = p[K.autoBackupError],
        )
    }.distinctUntilChanged()

    val vaultConfig: Flow<VaultConfig?> = prefs.map { p ->
        val salt = p[K.vaultSalt]
        val wrapped = p[K.vaultWrapped]
        if (salt == null || wrapped == null) null
        else VaultConfig(salt, p[K.vaultIter] ?: 0, wrapped, p[K.vaultBio])
    }.distinctUntilChanged()

    val focusState: Flow<FocusState> = prefs.map { p ->
        FocusState(
            sessionId = p[K.focusSession] ?: -1L,
            endAt = p[K.focusEnd] ?: 0L,
            remainingMs = p[K.focusRemaining] ?: 0L,
            plannedMinutes = p[K.focusPlanned] ?: 25,
            label = p[K.focusLabel] ?: "",
            isBreak = p[K.focusBreak] ?: false,
        )
    }.distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()
    suspend fun focusNow(): FocusState = focusState.first()
    suspend fun vaultConfigNow(): VaultConfig? = vaultConfig.first()

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    suspend fun setUserName(value: String) = edit { it[K.userName] = value.trim() }
    suspend fun setThemeMode(value: ThemeMode) = edit { it[K.theme] = value.name }
    suspend fun setDynamicColor(value: Boolean) = edit { it[K.dynamic] = value }
    suspend fun setWeekStartsMonday(value: Boolean) = edit { it[K.weekMonday] = value }
    suspend fun setAppLock(value: Boolean) = edit { it[K.appLock] = value }
    suspend fun setLockTimeout(seconds: Int) = edit { it[K.lockTimeout] = seconds }
    suspend fun setSecureScreen(value: Boolean) = edit { it[K.secureScreen] = value }
    suspend fun setHideAmounts(value: Boolean) = edit { it[K.hideAmounts] = value }
    suspend fun setGcalShow(value: Boolean) = edit { it[K.gcalShow] = value }
    suspend fun setGcalSyncNew(value: Boolean) = edit { it[K.gcalSyncNew] = value }
    suspend fun setGcalTarget(id: Long) = edit { it[K.gcalTarget] = id }
    suspend fun setGcalHidden(ids: Set<Long>) = edit { it[K.gcalHidden] = ids.map(Long::toString).toSet() }
    suspend fun setVaultAutoLock(seconds: Int) = edit { it[K.vaultAutoLock] = seconds }
    suspend fun setOnboarded() = edit { it[K.onboarded] = true }
    suspend fun autoBackupKeyNow(): AutoBackupKey? = prefs.first().let { p ->
        val key = p[K.autoBackupKey] ?: return@let null
        AutoBackupKey(key, p[K.autoBackupSalt] ?: return@let null, p[K.autoBackupIter] ?: return@let null)
    }

    suspend fun configureAutoBackup(uri: String, kind: AutoBackupKind, label: String, key: AutoBackupKey?) = edit {
        it[K.autoBackupUri] = uri
        it[K.autoBackupKind] = kind.name
        it[K.autoBackupLabel] = label
        it.remove(K.autoBackupError)
        it[K.autoBackupLast] = 0L
        if (key == null) {
            it.remove(K.autoBackupKey); it.remove(K.autoBackupSalt); it.remove(K.autoBackupIter)
        } else {
            it[K.autoBackupKey] = key.sealedKey
            it[K.autoBackupSalt] = key.salt
            it[K.autoBackupIter] = key.iterations
        }
    }

    suspend fun clearAutoBackup() = edit {
        listOf(K.autoBackupUri, K.autoBackupKind, K.autoBackupLabel, K.autoBackupError, K.autoBackupKey, K.autoBackupSalt)
            .forEach(it::remove)
        it.remove(K.autoBackupIter)
        it.remove(K.autoBackupLast)
    }

    suspend fun setAutoBackupEvery(days: Int) = edit { it[K.autoBackupEvery] = days }

    /** Mencatat hasil cadangan otomatis: berhasil memperbarui waktu terakhir, gagal menyimpan pesannya. */
    suspend fun setAutoBackupResult(at: Long, error: String?) = edit {
        if (error == null) {
            it[K.autoBackupLast] = at
            it.remove(K.autoBackupError)
        } else {
            it[K.autoBackupError] = error
        }
    }

    /** Pengaturan yang ikut ke berkas cadangan. */
    suspend fun backupSettings(): BackupSettings = current().let { s ->
        BackupSettings(
            userName = s.userName, themeMode = s.themeMode.name, dynamicColor = s.dynamicColor,
            weekStartsMonday = s.weekStartsMonday, appLock = s.appLock, lockTimeoutSec = s.lockTimeoutSec,
            secureScreen = s.secureScreen, hideAmounts = s.hideAmounts, vaultAutoLockSec = s.vaultAutoLockSec,
        )
    }

    /** Menerapkan pengaturan dari cadangan; pengenalan aplikasi dianggap sudah selesai. */
    suspend fun applyBackupSettings(b: BackupSettings) = edit {
        it[K.userName] = b.userName.trim()
        it[K.theme] = enumOr(b.themeMode, ThemeMode.SYSTEM).name
        it[K.dynamic] = b.dynamicColor
        it[K.weekMonday] = b.weekStartsMonday
        it[K.appLock] = b.appLock
        it[K.lockTimeout] = b.lockTimeoutSec
        it[K.secureScreen] = b.secureScreen
        it[K.hideAmounts] = b.hideAmounts
        it[K.vaultAutoLock] = b.vaultAutoLockSec
        it[K.onboarded] = true
    }

    /** Menyimpan status timer fokus; null mengembalikan ke keadaan tidak aktif. */
    suspend fun saveFocusState(state: FocusState?) = edit {
        val s = state ?: FocusState()
        it[K.focusSession] = s.sessionId
        it[K.focusEnd] = s.endAt
        it[K.focusRemaining] = s.remainingMs
        it[K.focusPlanned] = s.plannedMinutes
        it[K.focusLabel] = s.label
        it[K.focusBreak] = s.isBreak
    }

    suspend fun saveVaultConfig(config: VaultConfig?) = edit {
        if (config == null) {
            it.remove(K.vaultSalt); it.remove(K.vaultIter); it.remove(K.vaultWrapped); it.remove(K.vaultBio)
        } else {
            it[K.vaultSalt] = config.salt
            it[K.vaultIter] = config.iterations
            it[K.vaultWrapped] = config.wrappedKey
            if (config.bioWrappedKey == null) it.remove(K.vaultBio) else it[K.vaultBio] = config.bioWrappedKey
        }
    }
}
