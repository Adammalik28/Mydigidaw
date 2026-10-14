package com.example.mydigidaw.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.security.GeneralSecurityException

@RunWith(AndroidJUnit4::class)
class DeviceSecretsTest {
    @Test
    fun sealThenOpen_returnsOriginal() {
        val secret = ByteArray(32) { (it * 3).toByte() }
        val sealed = DeviceSecrets.seal(secret)
        assertArrayEquals(secret, DeviceSecrets.open(sealed))
        assertNotEquals("tiap segel memakai IV baru", sealed, DeviceSecrets.seal(secret))
    }

    @Test
    fun tamperedSecret_isRejected() {
        val sealed = DeviceSecrets.seal("kunci".toByteArray())
        val body = VaultCrypto.decode(sealed.substringAfter(':'))
        body[0] = (body[0].toInt() xor 1).toByte()
        val tampered = sealed.substringBefore(':') + ":" + VaultCrypto.encode(body)
        assertThrows(GeneralSecurityException::class.java) { DeviceSecrets.open(tampered) }
    }
}
