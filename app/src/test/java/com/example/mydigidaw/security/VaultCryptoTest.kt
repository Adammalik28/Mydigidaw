package com.example.mydigidaw.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.AEADBadTagException

class VaultCryptoTest {
    private val salt = ByteArray(16) { it.toByte() }

    // Iterasi kecil agar tes cepat; algoritmanya tetap sama.
    private fun key(password: String) = VaultCrypto.deriveKey(password.toCharArray(), salt, 1_000)

    @Test
    fun encryptThenDecrypt_returnsOriginal() {
        val plain = "PIN ATM: 123456 · catatan rahasia ✓".toByteArray()
        val sealed = VaultCrypto.encrypt(key("rahasia"), plain)
        assertArrayEquals(plain, VaultCrypto.decrypt(key("rahasia"), sealed))
    }

    @Test
    fun sameInput_producesDifferentCiphertext() {
        val plain = "sama".toByteArray()
        assertNotEquals(VaultCrypto.encrypt(key("a"), plain), VaultCrypto.encrypt(key("a"), plain))
    }

    @Test
    fun wrongPassword_isRejected() {
        val sealed = VaultCrypto.encrypt(key("benar"), "isi".toByteArray())
        assertThrows(AEADBadTagException::class.java) { VaultCrypto.decrypt(key("salah"), sealed) }
    }

    @Test
    fun tamperedData_isRejected() {
        val sealed = VaultCrypto.decode(VaultCrypto.encrypt(key("k"), "saldo 1.000.000".toByteArray()))
        sealed[sealed.size - 1] = (sealed[sealed.size - 1].toInt() xor 1).toByte()
        assertThrows(AEADBadTagException::class.java) { VaultCrypto.decrypt(key("k"), VaultCrypto.encode(sealed)) }
    }

    @Test
    fun deriveKey_isDeterministicAndSaltDependent() {
        assertArrayEquals(key("x").encoded, key("x").encoded)
        val other = VaultCrypto.deriveKey("x".toCharArray(), ByteArray(16), 1_000)
        assertNotEquals(key("x").encoded.toList(), other.encoded.toList())
        assertEquals(32, key("x").encoded.size)
    }

    @Test
    fun base64_matchesAndroidNoWrapFormatAndToleratesLineBreaks() {
        val bytes = ByteArray(64) { (it * 7).toByte() }
        val text = VaultCrypto.encode(bytes)
        assertEquals(false, text.contains('\n'))
        assertArrayEquals(bytes, VaultCrypto.decode(text.chunked(20).joinToString("\n")))
    }
}
