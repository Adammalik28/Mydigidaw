package com.example.mydigidaw.data.backup

import com.example.mydigidaw.data.db.AccountEntity
import com.example.mydigidaw.data.db.BudgetEntity
import com.example.mydigidaw.data.db.CategoryEntity
import com.example.mydigidaw.data.db.DebtEntity
import com.example.mydigidaw.data.db.EventEntity
import com.example.mydigidaw.data.db.FocusSessionEntity
import com.example.mydigidaw.data.db.GoalEntity
import com.example.mydigidaw.data.db.HabitEntity
import com.example.mydigidaw.data.db.HabitLogEntity
import com.example.mydigidaw.data.db.JournalEntity
import com.example.mydigidaw.data.db.NoteEntity
import com.example.mydigidaw.data.db.RecurringEntity
import com.example.mydigidaw.data.db.TaskEntity
import com.example.mydigidaw.data.db.TransactionEntity
import com.example.mydigidaw.data.db.VaultItemEntity
import com.example.mydigidaw.data.prefs.VaultConfig
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {
    private val sample = BackupData(
        notes = listOf(
            NoteEntity(1, "Belanja", "[ ] susu\n[x] roti", "PRIBADI", 3, isPinned = true, createdAt = 10, updatedAt = 20, isChecklist = true),
            NoteEntity(2, "Ide \"kutip\", koma", "baris 1\nbaris 2 — ✓ émoji 🎉", "IDE", isArchived = true, isTrashed = true, createdAt = 11, updatedAt = 21),
        ),
        vault = listOf(VaultItemEntity(5, "PASSWORD", "c2VjcmV0", isFavorite = true, createdAt = 1, updatedAt = 2)),
        accounts = listOf(
            AccountEntity(1, "Dompet Tunai", "CASH", 50_000, 2, createdAt = 3),
            AccountEntity(2, "w2", "EWALLET", 20_000, 4, includeInTotal = false, sortOrder = 1, createdAt = 4),
        ),
        categories = listOf(CategoryEntity(7, "Makanan & Minuman", "EXPENSE", "🍔", 1, sortOrder = 2)),
        transactions = listOf(
            TransactionEntity(1, "EXPENSE", 25_000, 7, 1, null, 1_700_000_000_000, "Makan siang", 5),
            TransactionEntity(2, "TRANSFER", 10_000, null, 1, 2, 1_700_000_100_000, "", 6),
        ),
        budgets = listOf(BudgetEntity(1, 0, 3_000_000), BudgetEntity(2, 7, 1_000_000)),
        goals = listOf(GoalEntity(1, "Liburan", "🏖️", 5_000_000, 1_250_000, null, 3, 7), GoalEntity(2, "HP", "📱", 1, 0, 99, 0, 8)),
        recurring = listOf(RecurringEntity(1, "INCOME", 8_000_000, null, 1, null, "Gaji", "MONTHLY", 100, 3, 200, false, 9)),
        debts = listOf(DebtEntity(1, "RECEIVABLE", "Budi", 300_000, 100_000, 123, "pinjam", isSettled = false, createdAt = 10)),
        events = listOf(EventEntity(1, "Gym", "kaki", "Fitness", 1_000, 2_000, false, 2, "WEEKLY", 30, createdAt = 11, updatedAt = 12)),
        tasks = listOf(
            TaskEntity(1, "Bayar listrik", "", "HIGH", 5_000, true, true, false, null, 13),
            TaskEntity(2, "Selesai", isDone = true, completedAt = 77, createdAt = 14),
        ),
        habits = listOf(HabitEntity(1, "buku", "📖", 0, 0b0011111, 2, 1_200, false, 15), HabitEntity(2, "air", "💧", 1, createdAt = 16)),
        habitLogs = listOf(HabitLogEntity(1, 20_000, 2), HabitLogEntity(2, 20_001, 1)),
        focus = listOf(FocusSessionEntity(1, "Belajar", 17, 25, 25, true)),
        journal = listOf(JournalEntity(20_000, 5, "hari yang baik", 18)),
        vaultConfig = VaultConfig("c2FsdA==", 150_000, "d3JhcHBlZA==", null),
        settings = BackupSettings("king", "DARK", false, true, true, 60, true, false, 300),
    )

    @Test
    fun jsonRoundTrip_keepsEveryField() {
        val text = BackupFormat.toJson(sample, exportedAt = 42).toString(2)
        assertEquals(sample, BackupFormat.parse(BackupFormat.open(text, null)))
    }

    @Test
    fun count_sumsAllRecords() {
        assertEquals(23, sample.count)
    }

    @Test
    fun encryptedBackup_needsTheRightPassword() {
        val json = BackupFormat.toJson(sample).toString()
        val sealed = BackupFormat.encrypt(json, "sandi-cadangan".toCharArray())

        assertTrue(BackupFormat.isEncrypted(sealed))
        assertFalse(sealed.contains("Belanja"))
        assertEquals(sample, BackupFormat.parse(BackupFormat.open(sealed, "sandi-cadangan".toCharArray())))
        assertThrows(BackupPasswordException::class.java) { BackupFormat.open(sealed, null) }
        assertThrows(BackupPasswordException::class.java) { BackupFormat.open(sealed, "salah".toCharArray()) }
    }

    @Test
    fun oldVersion2Backup_withoutSettings_stillRestores() {
        val v2 = JSONObject()
            .put("app", "mydigidaw")
            .put("version", 2)
            .put("notes", org.json.JSONArray().put(JSONObject().put("id", 1).put("title", "efee").put("content", "").put("category", "UMUM")))
            .toString()
        val data = BackupFormat.parse(BackupFormat.open(v2, null))
        assertEquals("efee", data.notes.single().title)
        assertFalse(data.notes.single().isChecklist)
        assertNull(data.settings)
        assertNull(data.vaultConfig)
        assertTrue(data.transactions.isEmpty())
    }

    @Test
    fun foreignFiles_areRejectedWithAClearMessage() {
        val notJson = assertThrows(IllegalArgumentException::class.java) { BackupFormat.open("bukan json", null) }
        assertEquals("Berkas ini bukan cadangan Mydigidaw", notJson.message)
        assertThrows(IllegalArgumentException::class.java) { BackupFormat.open("""{"app":"lain"}""", null) }
    }

    @Test
    fun biometricKey_isNeverWrittenToBackups() {
        val withBio = sample.copy(vaultConfig = VaultConfig("s", 1, "w", "rahasia-biometrik"))
        val text = BackupFormat.toJson(withBio).toString()
        assertFalse(text.contains("rahasia-biometrik"))
    }
}
