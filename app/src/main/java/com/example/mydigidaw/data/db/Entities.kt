package com.example.mydigidaw.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String,
    val color: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    /** Catatan berupa daftar centang: setiap baris "[ ] teks" atau "[x] teks". */
    @ColumnInfo(defaultValue = "0") val isChecklist: Boolean = false,
)

/** Isi item brankas (judul, kolom, catatan) tersimpan terenkripsi di [payload]. */
@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val payload: String,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val initialBalance: Long,
    val color: Int,
    val includeInTotal: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val emoji: String,
    val color: Int,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("accountId"), Index("categoryId"), Index("toAccountId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val categoryId: Long?,
    val accountId: Long,
    val toAccountId: Long?,
    val date: Long,
    val note: String,
    val createdAt: Long,
)

/** Anggaran bulanan. [categoryId] = 0 berarti anggaran total semua pengeluaran. */
@Entity(tableName = "budgets", indices = [Index(value = ["categoryId"], unique = true)])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val amount: Long,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val targetAmount: Long,
    val savedAmount: Long,
    val deadline: Long?,
    val color: Int,
    val createdAt: Long,
)

/**
 * Aturan transaksi berulang; [occurrences] = indeks kejadian berikutnya ([nextDate] = kejadian ke-n
 * dari [startDate]), termasuk kejadian yang dilewati selama aturan dijeda.
 */
@Entity(tableName = "recurring")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val categoryId: Long?,
    val accountId: Long,
    val toAccountId: Long?,
    val note: String,
    val repeat: String,
    val startDate: Long,
    val occurrences: Int,
    val nextDate: Long,
    val isActive: Boolean = true,
    val createdAt: Long,
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val person: String,
    val amount: Long,
    val paidAmount: Long = 0,
    val dueDate: Long?,
    val note: String,
    val isSettled: Boolean = false,
    val createdAt: Long,
)

/**
 * Jadwal lokal. Untuk acara sepanjang hari, [startAt] = tengah malam hari mulai dan
 * [endAt] = tengah malam setelah hari terakhir (eksklusif).
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val location: String = "",
    val startAt: Long,
    val endAt: Long,
    val allDay: Boolean = false,
    val color: Int = 0,
    val repeat: String = "NONE",
    val reminderMinutes: Int = -1,
    val googleEventId: Long? = null,
    val googleCalendarId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val priority: String = "MEDIUM",
    val dueAt: Long? = null,
    val hasTime: Boolean = false,
    val remind: Boolean = false,
    val isDone: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
)

/** [daysMask]: bit 0 = Senin … bit 6 = Minggu. [reminderMinutes] = menit sejak tengah malam. */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: Int,
    val daysMask: Int = 0b1111111,
    val targetPerDay: Int = 1,
    val reminderMinutes: Int? = null,
    val isArchived: Boolean = false,
    val createdAt: Long,
)

@Entity(tableName = "habit_logs", primaryKeys = ["habitId", "day"], indices = [Index("day")])
data class HabitLogEntity(
    val habitId: Long,
    val day: Long,
    val count: Int,
)

/** Satu sesi timer fokus (Pomodoro). */
@Entity(tableName = "focus_sessions", indices = [Index("startedAt")])
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val startedAt: Long,
    val plannedMinutes: Int,
    val actualMinutes: Int = 0,
    val completed: Boolean = false,
)

/** Jurnal harian: satu entri per hari dengan suasana hati 1 (buruk) … 5 (luar biasa). */
@Entity(tableName = "journal")
data class JournalEntity(
    @PrimaryKey val day: Long,
    val mood: Int,
    val note: String = "",
    val updatedAt: Long,
)
