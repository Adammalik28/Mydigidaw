package com.example.mydigidaw.data.repo

import androidx.room.withTransaction
import com.example.mydigidaw.data.db.AccountEntity
import com.example.mydigidaw.data.db.AppDatabase
import com.example.mydigidaw.data.db.BudgetEntity
import com.example.mydigidaw.data.db.CategoryEntity
import com.example.mydigidaw.data.db.DebtEntity
import com.example.mydigidaw.data.db.GoalEntity
import com.example.mydigidaw.data.db.RecurringEntity
import com.example.mydigidaw.data.db.TransactionEntity
import com.example.mydigidaw.data.model.Recurrence
import com.example.mydigidaw.data.model.Repeat
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.model.effectOn
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.reminders.ReminderScheduler
import com.example.mydigidaw.util.startMillis
import com.example.mydigidaw.util.toLocalDate
import com.example.mydigidaw.util.toLocalDateTime
import com.example.mydigidaw.util.toMillis
import kotlinx.coroutines.flow.Flow

/** Transaksi lengkap dengan kategori & dompetnya, siap ditampilkan. */
data class TxItem(
    val tx: TransactionEntity,
    val category: CategoryEntity?,
    val account: AccountEntity?,
    val toAccount: AccountEntity?,
) {
    val type: TxType get() = enumOr(tx.type, TxType.EXPENSE)
}

data class AccountBalance(val account: AccountEntity, val balance: Long)

class FinanceRepository(private val db: AppDatabase, private val reminders: ReminderScheduler) {
    val accounts: Flow<List<AccountEntity>> = db.accounts().observeAll()
    val categories: Flow<List<CategoryEntity>> = db.categories().observeAll()
    val transactions: Flow<List<TransactionEntity>> = db.transactions().observeAll()
    val budgets: Flow<List<BudgetEntity>> = db.budgets().observeAll()
    val goals: Flow<List<GoalEntity>> = db.goals().observeAll()
    val recurring: Flow<List<RecurringEntity>> = db.recurring().observeAll()
    val debts: Flow<List<DebtEntity>> = db.debts().observeAll()

    suspend fun getTransaction(id: Long): TransactionEntity? = db.transactions().get(id)
    suspend fun allAccounts(): List<AccountEntity> = db.accounts().all()
    suspend fun allCategories(): List<CategoryEntity> = db.categories().all()

    suspend fun saveTransaction(tx: TransactionEntity): Long =
        if (tx.id == 0L) db.transactions().insert(tx) else tx.id.also { db.transactions().update(tx) }

    suspend fun deleteTransaction(id: Long) = db.transactions().delete(id)

    /** Mengembalikan transaksi yang baru dihapus (untuk "Urungkan"), dengan ID yang sama. */
    suspend fun restoreTransaction(tx: TransactionEntity) = db.transactions().insert(tx)

    suspend fun saveAccount(account: AccountEntity): Long =
        if (account.id == 0L) db.accounts().insert(account) else account.id.also { db.accounts().update(account) }

    suspend fun transactionCountForAccount(id: Long): Int = db.transactions().countForAccount(id)

    suspend fun deleteAccount(id: Long) = db.withTransaction {
        db.transactions().deleteForAccount(id)
        db.recurring().deleteForAccount(id)
        db.accounts().delete(id)
    }

    suspend fun saveCategory(category: CategoryEntity): Long =
        if (category.id == 0L) db.categories().insert(category) else category.id.also { db.categories().update(category) }

    suspend fun deleteCategory(id: Long) = db.withTransaction {
        db.transactions().clearCategory(id)
        db.recurring().clearCategory(id)
        db.budgets().deleteForCategory(id)
        db.categories().delete(id)
    }

    suspend fun setBudget(categoryId: Long, amount: Long) {
        db.withTransaction {
            db.budgets().deleteForCategory(categoryId)
            db.budgets().upsert(BudgetEntity(categoryId = categoryId, amount = amount))
        }
    }

    suspend fun deleteBudget(categoryId: Long) = db.budgets().deleteForCategory(categoryId)

    suspend fun saveGoal(goal: GoalEntity): Long =
        if (goal.id == 0L) db.goals().insert(goal) else goal.id.also { db.goals().update(goal) }

    suspend fun deleteGoal(id: Long) = db.goals().delete(id)

    suspend fun saveDebt(debt: DebtEntity): Long {
        val id = if (debt.id == 0L) db.debts().insert(debt) else debt.id.also { db.debts().update(debt) }
        reminders.scheduleDebt(debt.copy(id = id))
        return id
    }

    suspend fun deleteDebt(id: Long) {
        db.debts().delete(id)
        reminders.cancel(ReminderScheduler.KIND_DEBT, id)
    }

    /** Membuat aturan berulang dari transaksi pertama yang baru saja disimpan. */
    suspend fun addRecurring(first: TransactionEntity, rule: Repeat) {
        if (rule == Repeat.NONE) return
        val base = first.date.toLocalDateTime()
        val ruleEntity = RecurringEntity(
            type = first.type,
            amount = first.amount,
            categoryId = first.categoryId,
            accountId = first.accountId,
            toAccountId = first.toAccountId,
            note = first.note,
            repeat = rule.name,
            startDate = first.date,
            occurrences = 1,
            nextDate = Recurrence.nth(base, rule, 1).toMillis(),
            createdAt = System.currentTimeMillis(),
        )
        val id = db.recurring().insert(ruleEntity)
        reminders.scheduleBill(ruleEntity.copy(id = id))
    }

    suspend fun setRecurringActive(rule: RecurringEntity, active: Boolean, now: Long = System.currentTimeMillis()) {
        var updated = rule.copy(isActive = active)
        // Aktif lagi setelah dijeda: lewati jadwal yang terlewat selama jeda agar tidak tercatat menumpuk.
        if (active && !rule.isActive && rule.nextDate < now) {
            val repeat = enumOr(rule.repeat, Repeat.MONTHLY)
            if (repeat != Repeat.NONE) {
                val base = rule.startDate.toLocalDateTime()
                val todayStart = now.toLocalDate().startMillis()
                var count = rule.occurrences.toLong()
                var next = Recurrence.nth(base, repeat, count).toMillis()
                var guard = 0
                while (next < todayStart && guard++ < 10_000) {
                    count++
                    next = Recurrence.nth(base, repeat, count).toMillis()
                }
                updated = updated.copy(occurrences = count.toInt(), nextDate = next)
            }
        }
        db.recurring().update(updated)
        reminders.scheduleBill(updated)
    }

    suspend fun deleteRecurring(id: Long) {
        db.recurring().delete(id)
        reminders.cancel(ReminderScheduler.KIND_BILL, id)
    }

    /** Mencatat semua transaksi berulang yang sudah jatuh tempo. Mengembalikan jumlah transaksi baru. */
    suspend fun processRecurring(now: Long = System.currentTimeMillis()): Int {
        var created = 0
        db.withTransaction {
            for (rule in db.recurring().due(now)) {
                val repeat = enumOr(rule.repeat, Repeat.MONTHLY)
                if (repeat == Repeat.NONE) continue
                val base = rule.startDate.toLocalDateTime()
                var current = rule
                var guard = 0
                while (current.nextDate <= now && guard++ < 500) {
                    db.transactions().insert(
                        TransactionEntity(
                            type = current.type,
                            amount = current.amount,
                            categoryId = current.categoryId,
                            accountId = current.accountId,
                            toAccountId = current.toAccountId,
                            date = current.nextDate,
                            note = current.note,
                            createdAt = now,
                        ),
                    )
                    created++
                    val count = current.occurrences + 1
                    current = current.copy(occurrences = count, nextDate = Recurrence.nth(base, repeat, count.toLong()).toMillis())
                }
                db.recurring().update(current)
                reminders.scheduleBill(current)
            }
        }
        return created
    }

    companion object {
        fun balances(accounts: List<AccountEntity>, transactions: List<TransactionEntity>): List<AccountBalance> {
            val delta = HashMap<Long, Long>()
            for (tx in transactions) {
                when (tx.type) {
                    TxType.INCOME.name -> delta[tx.accountId] = (delta[tx.accountId] ?: 0) + tx.amount
                    TxType.EXPENSE.name -> delta[tx.accountId] = (delta[tx.accountId] ?: 0) - tx.amount
                    else -> {
                        delta[tx.accountId] = (delta[tx.accountId] ?: 0) - tx.amount
                        tx.toAccountId?.let { delta[it] = (delta[it] ?: 0) + tx.amount }
                    }
                }
            }
            return accounts.map { AccountBalance(it, it.initialBalance + (delta[it.id] ?: 0)) }
        }

        fun totalBalance(accounts: List<AccountEntity>, transactions: List<TransactionEntity>): Long {
            val included = accounts.filter { it.includeInTotal }
            val ids = included.map { it.id }.toSet()
            return included.sumOf { it.initialBalance } + transactions.sumOf { it.effectOn(ids) }
        }

        fun toItems(
            transactions: List<TransactionEntity>,
            categories: Map<Long, CategoryEntity>,
            accounts: Map<Long, AccountEntity>,
        ): List<TxItem> = transactions.map { tx ->
            TxItem(tx, tx.categoryId?.let(categories::get), accounts[tx.accountId], tx.toAccountId?.let(accounts::get))
        }
    }
}
