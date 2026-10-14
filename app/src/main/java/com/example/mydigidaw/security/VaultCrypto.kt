package com.example.mydigidaw.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Primitif kriptografi brankas: PBKDF2-HMAC-SHA256 + AES-256-GCM. */
object VaultCrypto {
    const val ITERATIONS = 150_000
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128

    private val random = SecureRandom()

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    /** Hasil: Base64(IV || ciphertext+tag). */
    fun encrypt(key: SecretKey, plain: ByteArray): String {
        val iv = randomBytes(IV_LENGTH)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return encode(iv + cipher.doFinal(plain))
    }

    /** Melempar [javax.crypto.AEADBadTagException] bila kunci salah atau data rusak. */
    fun decrypt(key: SecretKey, data: String): ByteArray {
        val raw = decode(data)
        require(raw.size > IV_LENGTH) { "Data terenkripsi tidak valid" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, raw, 0, IV_LENGTH))
        return cipher.doFinal(raw, IV_LENGTH, raw.size - IV_LENGTH)
    }

    // java.util.Base64 (bukan android.util) agar bisa diuji di JVM; formatnya identik dengan NO_WRAP.
    fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    fun decode(text: String): ByteArray = Base64.getDecoder().decode(text.filterNot(Char::isWhitespace))
}
