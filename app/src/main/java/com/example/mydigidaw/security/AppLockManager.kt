package com.example.mydigidaw.security

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.mydigidaw.data.prefs.AppSettings
import com.example.mydigidaw.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Mengunci aplikasi (dan brankas) setelah aplikasi berada di latar belakang melewati batas waktu.
 * Didaftarkan ke ProcessLifecycleOwner sehingga hanya bereaksi saat seluruh aplikasi keluar/masuk.
 */
class AppLockManager(
    settings: SettingsRepository,
    private val vault: VaultManager,
    scope: CoroutineScope,
) : DefaultLifecycleObserver {
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    @Volatile private var current = AppSettings()
    @Volatile private var backgroundAt = 0L
    @Volatile private var externalFlowAt = 0L

    /** True selama dialog autentikasi sistem tampil, agar tidak memicu penguncian ulang. */
    @Volatile var authInProgress = false
        private set

    init {
        scope.launch {
            settings.settings.collect { s ->
                current = s
                if (!_ready.value) {
                    _locked.value = s.appLock
                    _ready.value = true
                }
                if (!s.appLock) _locked.value = false
            }
        }
    }

    /** Panggil sebelum membuka aktivitas lain (pemilih file, berbagi) agar kembali tanpa terkunci. */
    fun allowExternalFlow() {
        externalFlowAt = SystemClock.elapsedRealtime()
    }

    /**
     * Menjalankan dialog autentikasi tanpa memicu penguncian ulang. Status selalu dipulihkan walau
     * dialog dibatalkan (mis. layar diputar), agar kunci aplikasi tidak ikut nonaktif selamanya.
     */
    suspend fun <T> withAuth(block: suspend () -> T): T {
        authInProgress = true
        try {
            return block()
        } finally {
            authInProgress = false
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (authInProgress) return
        backgroundAt = SystemClock.elapsedRealtime()
    }

    override fun onStart(owner: LifecycleOwner) {
        val externalAt = externalFlowAt
        externalFlowAt = 0L
        if (backgroundAt == 0L) return
        val elapsed = SystemClock.elapsedRealtime() - backgroundAt
        // Keringanan hanya bila aplikasi keluar tepat setelah membuka pemilih file/berbagi. Bila
        // pemilihnya dibatalkan tanpa keluar dari aplikasi, penanda ini tidak boleh berlaku untuk
        // kepergian berikutnya, dan kunci tetap aktif bila pengguna terlalu lama berada di luar.
        val viaExternal = externalAt != 0L && backgroundAt - externalAt in 0..EXTERNAL_START_WINDOW_MS
        val grace = if (viaExternal) EXTERNAL_GRACE_MS else 0L
        backgroundAt = 0L
        if (current.appLock && elapsed >= maxOf(current.lockTimeoutSec * 1000L, grace)) _locked.value = true
        if (vault.isUnlocked && elapsed >= maxOf(current.vaultAutoLockSec * 1000L, grace)) vault.lock()
    }

    fun unlock() {
        _locked.value = false
    }

    private companion object {
        const val EXTERNAL_START_WINDOW_MS = 30_000L
        const val EXTERNAL_GRACE_MS = 5 * 60_000L
    }
}
