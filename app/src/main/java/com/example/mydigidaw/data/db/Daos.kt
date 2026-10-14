package com.example.mydigidaw.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): NoteEntity?

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE notes SET isArchived = :archived, isPinned = 0 WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("UPDATE notes SET isTrashed = :trashed, isPinned = 0 WHERE id = :id")
    suspend fun setTrashed(id: Long, trashed: Boolean)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("SELECT * FROM notes")
    suspend fun all(): List<NoteEntity>

    @Insert
    suspend fun insertAll(items: List<NoteEntity>)

    @Query("DELETE FROM notes")
    suspend fun clear()
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY isFavorite DESC, updatedAt DESC")
    fun observeAll(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun get(id: Long): VaultItemEntity?

    @Insert
    suspend fun insert(item: VaultItemEntity): Long

    @Update
    suspend fun update(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM vault_items")
    suspend fun all(): List<VaultItemEntity>

    @Insert
    suspend fun insertAll(items: List<VaultItemEntity>)

    @Query("DELETE FROM vault_items")
    suspend fun clear()
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id")
    suspend fun all(): List<AccountEntity>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertAll(items: List<AccountEntity>)

    @Query("DELETE FROM accounts")
    suspend fun clear()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    suspend fun all(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertAll(items: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clear()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): TransactionEntity?

    @Insert
    suspend fun insert(tx: TransactionEntity): Long

    @Update
    suspend fun update(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun deleteForAccount(accountId: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun countForAccount(accountId: Long): Int

    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun all(): List<TransactionEntity>

    @Insert
    suspend fun insertAll(items: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun clear()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteForCategory(categoryId: Long)

    @Query("SELECT * FROM budgets")
    suspend fun all(): List<BudgetEntity>

    @Insert
    suspend fun insertAll(items: List<BudgetEntity>)

    @Query("DELETE FROM budgets")
    suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt")
    fun observeAll(): Flow<List<GoalEntity>>

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM goals")
    suspend fun all(): List<GoalEntity>

    @Insert
    suspend fun insertAll(items: List<GoalEntity>)

    @Query("DELETE FROM goals")
    suspend fun clear()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY nextDate")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring WHERE isActive = 1 AND nextDate <= :now")
    suspend fun due(now: Long): List<RecurringEntity>

    @Insert
    suspend fun insert(rule: RecurringEntity): Long

    @Update
    suspend fun update(rule: RecurringEntity)

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM recurring WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun deleteForAccount(accountId: Long)

    @Query("UPDATE recurring SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)

    @Query("SELECT * FROM recurring")
    suspend fun all(): List<RecurringEntity>

    @Insert
    suspend fun insertAll(items: List<RecurringEntity>)

    @Query("DELETE FROM recurring")
    suspend fun clear()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY isSettled, CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate, createdAt DESC")
    fun observeAll(): Flow<List<DebtEntity>>

    @Insert
    suspend fun insert(debt: DebtEntity): Long

    @Update
    suspend fun update(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM debts")
    suspend fun all(): List<DebtEntity>

    @Insert
    suspend fun insertAll(items: List<DebtEntity>)

    @Query("DELETE FROM debts")
    suspend fun clear()
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startAt")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY startAt")
    suspend fun all(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: Long): EventEntity?

    @Insert
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertAll(items: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun clear()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isDone, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun all(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET isDone = :done, completedAt = :completedAt WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, completedAt: Long?)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun deleteDone()

    @Insert
    suspend fun insertAll(items: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun clear()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits")
    suspend fun allHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun get(id: Long): HabitEntity?

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsFor(habitId: Long)

    @Query("SELECT * FROM habit_logs WHERE day >= :fromDay")
    fun observeLogs(fromDay: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    fun observeLogsFor(habitId: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND day = :day")
    suspend fun log(habitId: Long, day: Long): HabitLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND day = :day")
    suspend fun deleteLog(habitId: Long, day: Long)

    @Query("SELECT * FROM habit_logs")
    suspend fun allLogs(): List<HabitLogEntity>

    @Insert
    suspend fun insertAllHabits(items: List<HabitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(items: List<HabitLogEntity>)

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("DELETE FROM habit_logs")
    suspend fun clearLogs()
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun get(id: Long): FocusSessionEntity?

    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM focus_sessions")
    suspend fun all(): List<FocusSessionEntity>

    @Insert
    suspend fun insertAll(items: List<FocusSessionEntity>)

    @Query("DELETE FROM focus_sessions")
    suspend fun clear()
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal ORDER BY day DESC")
    fun observeAll(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journal WHERE day = :day")
    fun observeDay(day: Long): Flow<JournalEntity?>

    @Upsert
    suspend fun upsert(entry: JournalEntity)

    @Query("DELETE FROM journal WHERE day = :day")
    suspend fun delete(day: Long)

    @Query("SELECT * FROM journal")
    suspend fun all(): List<JournalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<JournalEntity>)

    @Query("DELETE FROM journal")
    suspend fun clear()
}
