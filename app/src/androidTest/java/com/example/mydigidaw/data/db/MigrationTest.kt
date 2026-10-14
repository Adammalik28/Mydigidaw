package com.example.mydigidaw.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Memastikan data pengguna versi 1 tetap utuh setelah pembaruan ke skema versi 2. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To2_keepsDataAndAddsNewTables() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                "INSERT INTO notes (id, title, content, category, color, isPinned, isArchived, isTrashed, createdAt, updatedAt) " +
                    "VALUES (1, 'Belanja', 'susu, roti', 'PRIBADI', 2, 1, 0, 0, 100, 200)",
            )
            execSQL("INSERT INTO accounts (id, name, type, initialBalance, color, includeInTotal, sortOrder, createdAt) VALUES (1, 'Tunai', 'CASH', 50000, 0, 1, 0, 1)")
            execSQL(
                "INSERT INTO transactions (id, type, amount, categoryId, accountId, toAccountId, date, note, createdAt) " +
                    "VALUES (1, 'EXPENSE', 25000, NULL, 1, NULL, 300, 'Makan', 300)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true)

        db.query("SELECT title, content, isPinned, isChecklist FROM notes WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Belanja", c.getString(0))
            assertEquals("susu, roti", c.getString(1))
            assertEquals(1, c.getInt(2))
            assertEquals("catatan lama bukan daftar centang", 0, c.getInt(3))
        }
        db.query("SELECT amount, note FROM transactions WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(25_000L, c.getLong(0))
            assertEquals("Makan", c.getString(1))
        }
        db.query("SELECT COUNT(*) FROM focus_sessions").use { c -> assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM journal").use { c -> assertTrue(c.moveToFirst()); assertEquals(0, c.getInt(0)) }
        db.close()
    }

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
