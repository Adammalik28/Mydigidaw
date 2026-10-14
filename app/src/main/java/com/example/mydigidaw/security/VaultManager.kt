package com.example.mydigidaw.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.example.mydigidaw.data.prefs.SettingsRepository
import com.example.mydigidaw.data.prefs.VaultConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Mengelola kunci brankas.
 *
 * Data brankas dienkripsi dengan kunci data acak (DEK). DEK disimpan terbungkus oleh kunci yang
 * diturunkan dari kata sandi utama, sehingga cadangan tetap bisa dipulihkan di HP lain dengan kata
 * sandi yang sama. Untuk buka dengan biometrik, DEK juga dibungkus kunci Android Keystore yang
 * hanya bisa dipakai setelah sidik jari/wajah terverifikasi.
 */
class VaultManager(private val settings: SettingsRepository) {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    @Volatile
    private var dataKey: SecretKey? = null

    val isUnlocked: Boolean get() = dataKey != null

    val configured: Flow<Boolean> = settings.vaultConfig.map { it != null }
    val biometricEnabled: Flow<Boolean> = settings.vaultConfig.map { it?.bioWrappedKey != null }

    suspend fun setup(password: CharArray) = withContext(Dispatchers.Default) {
        val salt = VaultCrypto.randomBytes(16)
        val kek = VaultCrypto.deriveKey(password, salt, VaultCrypto.ITERATIONS)
        val dek = VaultCrypto.randomBytes(32)
        val wrapped = VaultCrypto.encrypt(kek, dek)
        settings.saveVaultConfig(VaultConfig(VaultCrypto.encode(salt), VaultCrypto.ITERATIONS, wrapped, null))
        open(dek)
    }

    suspend fun unlock(password: CharArray): Boolean = withContext(Dispatchers.Default) {
        val dek = unwrapWithPassword(password) ?: return@withContext false
        open(dek)
        true
    }

    suspend fun verifyPassword(password: CharArray): Boolean = withContext(Dispatchers.Default) {
        unwrapWithPassword(password) != null
    }

    private suspend fun unwrapWithPassword(password: CharArray): ByteArray? {
        val cfg = settings.vaultConfigNow() ?: return null
        return try {
            val kek = VaultCrypto.deriveKey(password, VaultCrypto.decode(cfg.salt), cfg.iterations)
            VaultCrypto.decrypt(kek, cfg.wrappedKey)
        } catch (e: GeneralSecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    suspend fun changePassword(old: CharArray, new: CharArray): Boolean = withContext(Dispatchers.Default) {
        val cfg = settings.vaultConfigNow() ?: return@withContext false
        val dek = unwrapWithPassword(old) ?: return@withContext false
        val salt = VaultCrypto.randomBytes(16)
        val kek = VaultCrypto.deriveKey(new, salt, VaultCrypto.ITERATIONS)
        settings.saveVaultConfig(
            cfg.copy(salt = VaultCrypto.encode(salt), iterations = VaultCrypto.ITERATIONS, wrappedKey = VaultCrypto.encrypt(kek, dek)),
        )
        open(dek)
        true
    }

    private fun open(dek: ByteArray) {
        dataKey = SecretKeySpec(dek, "AES")
        _unlocked.value = true
    }

    fun lock() {
        dataKey = null
        _unlocked.value = false
    }

    /** Menghapus konfigurasi brankas (dipakai saat reset atau sebelum memulihkan cadangan). */
    suspend fun reset() {
        lock()
        settings.saveVaultConfig(null)
        deleteBioKey()
    }

    fun encrypt(plain: String): String {
        val key = dataKey ?: throw IllegalStateException("Brankas terkunci")
        return VaultCrypto.encrypt(key, plain.toByteArray(Charsets.UTF_8))
    }

    fun decrypt(data: String): String {
        val key = dataKey ?: throw IllegalStateException("Brankas terkunci")
        return String(VaultCrypto.decrypt(key, data), Charsets.UTF_8)
    }

    // ---------------- Biometrik ----------------

    /** Cipher untuk membungkus DEK; harus diautentikasi lewat BiometricPrompt sebelum dipakai. */
    fun biometricEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        try {
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateBioKey())
        } catch (e: KeyPermanentlyInvalidatedException) {
            deleteBioKey()
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateBioKey())
        }
        return cipher
    }

    suspend fun enableBiometric(authenticatedCipher: Cipher): Boolean {
        val key = dataKey ?: return false
        val cfg = settings.vaultConfigNow() ?: return false
        return try {
            val encrypted = authenticatedCipher.doFinal(key.encoded)
            val blob = VaultCrypto.encode(authenticatedCipher.iv) + ":" + VaultCrypto.encode(encrypted)
            settings.saveVaultConfig(cfg.copy(bioWrappedKey = blob))
            true
        } catch (e: GeneralSecurityException) {
            false
        }
    }

    suspend fun disableBiometric() {
        val cfg = settings.vaultConfigNow() ?: return
        settings.saveVaultConfig(cfg.copy(bioWrappedKey = null))
        deleteBioKey()
    }

    /** Null bila biometrik tidak aktif atau kunci sudah tidak berlaku (mis. sidik jari baru didaftarkan). */
    suspend fun biometricDecryptCipher(): Cipher? {
        val cfg = settings.vaultConfigNow() ?: return null
        val blob = cfg.bioWrappedKey ?: return null
        val iv = VaultCrypto.decode(blob.substringBefore(':'))
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, getOrCreateBioKey(), GCMParameterSpec(128, iv))
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            disableBiometric()
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun unlockWithCipher(authenticatedCipher: Cipher): Boolean {
        val cfg = settings.vaultConfigNow() ?: return false
        val blob = cfg.bioWrappedKey ?: return false
        return try {
            open(authenticatedCipher.doFinal(VaultCrypto.decode(blob.substringAfter(':'))))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getOrCreateBioKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(BIO_ALIAS, null) as? SecretKey)?.let { return it }
        val builder = KeyGenParameterSpec.Builder(BIO_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(builder.build())
        return generator.generateKey()
    }

    private fun deleteBioKey() {
        runCatching { KeyStore.getInstance(KEYSTORE).apply { load(null) }.deleteEntry(BIO_ALIAS) }
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val BIO_ALIAS = "mydigidaw_vault_bio"
    }
}
