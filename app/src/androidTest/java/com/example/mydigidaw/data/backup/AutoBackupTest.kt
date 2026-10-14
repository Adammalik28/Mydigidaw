package com.example.mydigidaw.data.backup

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mydigidaw.MydigidawApp
import com.example.mydigidaw.data.db.NoteEntity
import com.example.mydigidaw.data.prefs.AutoBackupKind
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Cadangan otomatis terenkripsi ke satu berkas (jalur yang sama dengan Google Drive) lalu dibuka lagi
 * dengan kata sandi. Berjalan di paket debug (id.mydigidaw.app.debug), tidak menyentuh aplikasi utama.
 */
@RunWith(AndroidJUnit4::class)
class AutoBackupTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container = (context.applicationContext as MydigidawApp).container
    private val file = File(context.cacheDir, "auto-backup-test.json")

    @After
    fun cleanUp() = runBlocking {
        container.settings.clearAutoBackup()
        file.delete()
        Unit
    }

    @Test
    fun encryptedAutoBackup_roundTripsWithPassword() = runBlocking {
        val now = System.currentTimeMillis()
        val noteId = container.notes.save(NoteEntity(title = "Uji cadangan", content = "isi rahasia", category = "UMUM", createdAt = now, updatedAt = now))
        try {
            val key = container.backup.createAutoBackupKey("sandi-uji".toCharArray())
            container.settings.configureAutoBackup(Uri.fromFile(file).toString(), AutoBackupKind.FILE, "Uji", key)

            container.backup.runAutoBackup()

            val text = file.readText()
            assertTrue(BackupFormat.isEncrypted(text))
            assertFalse("isi tidak boleh terbaca tanpa kata sandi", text.contains("isi rahasia"))
            val restored = BackupFormat.parse(BackupFormat.open(text, "sandi-uji".toCharArray()))
            assertTrue(restored.notes.any { it.title == "Uji cadangan" && it.content == "isi rahasia" })
            assertThrows(BackupPasswordException::class.java) { BackupFormat.open(text, "salah".toCharArray()) }

            val settings = container.settings.current()
            assertNull(settings.autoBackupError)
            assertTrue(settings.autoBackupLastAt > 0)
            assertTrue(settings.autoBackupEncrypted)
        } finally {
            container.notes.delete(noteId)
        }
    }

    @Test
    fun shorterBackup_fullyReplacesOlderContent() = runBlocking {
        file.writeText("x".repeat(2_000_000))
        container.settings.configureAutoBackup(Uri.fromFile(file).toString(), AutoBackupKind.FILE, "Uji", null)

        container.backup.runAutoBackup()

        val text = file.readText()
        assertFalse("sisa isi lama harus terpotong", text.endsWith("x"))
        assertEquals(BackupFormat.APP_ID, org.json.JSONObject(text).getString("app"))
    }

    @Test
    fun failure_isRecordedForTheSettingsScreen() = runBlocking {
        val missing = Uri.fromFile(File(context.cacheDir, "tidak-ada/folder/cadangan.json"))
        container.settings.configureAutoBackup(missing.toString(), AutoBackupKind.FILE, "Uji", null)

        runCatching { container.backup.runAutoBackup() }

        assertTrue(container.settings.current().autoBackupError != null)
    }
}
