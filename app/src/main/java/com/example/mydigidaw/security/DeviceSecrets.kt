package com.example.mydigidaw.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Menyegel rahasia kecil (mis. kunci cadangan otomatis) dengan kunci Android Keystore milik perangkat ini.
 * Hasilnya hanya bisa dibuka oleh aplikasi ini di HP ini, tanpa perlu sidik jari — cocok untuk tugas latar.
 */
object DeviceSecrets {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "mydigidaw_device_secret"
    private const val TAG_BITS = 128

    fun seal(plain: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return VaultCrypto.encode(cipher.iv) + ":" + VaultCrypto.encode(cipher.doFinal(plain))
    }

    /** Melempar [java.security.GeneralSecurityException] bila kunci perangkat hilang atau data rusak. */
    fun open(sealed: String): ByteArray {
        val iv = VaultCrypto.decode(sealed.substringBefore(':'))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(VaultCrypto.decode(sealed.substringAfter(':')))
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply { init(spec) }.generateKey()
    }
}
