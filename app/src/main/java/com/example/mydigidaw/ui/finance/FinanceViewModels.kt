package com.example.mydigidaw.ui.finance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.data.db.AccountEntity
import com.example.mydigidaw.data.db.CategoryEntity
import com.example.mydigidaw.data.db.DebtEntity
import com.example.mydigidaw.data.db.GoalEntity
import com.example.mydigidaw.data.db.RecurringEntity
import com.example.mydigidaw.data.db.TransactionEntity
import com.example.mydigidaw.data.model.DebtType
import com.example.mydigidaw.data.model.Repeat
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.data.repo.AccountBalance
import com.example.mydigidaw.data.repo.FinanceRepository
import com.example.mydigidaw.reminders.Notifications
import com.example.mydigidaw.ui.components.digitsToLong
import com.example.mydigidaw.util.Money
import com.example.mydigidaw.util.toLocalDateTime
import com.example.mydigidaw.util.toMillis
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FinanceViewModel(private val c: AppContainer) : ViewModel() {
    private val base: Flow<FinanceBase> = combine(
        c.finance.accounts,
        c.finance.categories,
        c.finance.transactions,
        c.finance.budgets,
        c.finance.goals,
    ) { a, cat, t, b, g -> FinanceBase(a, cat, t, b, g) }
        .flowOn(Dispatchers.Default)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val hideAmounts: StateFlow<Boolean> = c.settings.settings.map { it.hideAmounts }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val filter = MutableStateFlow(TxFilter())
    val statsMonth = MutableStateFlow(YearMonth.now())
    val statsMode = MutableStateFlow(StatsMode.MONTH)
    val statsType = MutableStateFlow(TxType.EXPENSE)
    val budgetMonth = MutableStateFlow(YearMonth.now())

    val categories: StateFlow<List<CategoryEntity>> = c.finance.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accounts: StateFlow<List<AccountEntity>> = c.finance.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val overview: StateFlow<OverviewState?> = base.map { FinanceCalc.overview(it, LocalDate.now()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val txList: StateFlow<TxListState?> = combine(base, filter) { b, f -> FinanceCalc.txList(b, f, LocalDate.now()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val stats: StateFlow<StatsState?> = combine(base, statsMonth, statsMode, statsType) { b, m, mode, type ->
        FinanceCalc.stats(b, mode, m, type, LocalDate.now())
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val budgets: StateFlow<BudgetState?> = combine(base, budgetMonth) { b, m -> FinanceCalc.budgetState(b, m, LocalDate.now()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateFilter(transform: (TxFilter) -> TxFilter) = filter.update(transform)

    fun shiftStats(step: Long) = statsMonth.update {
        if (statsMode.value == StatsMode.MONTH) it.plusMonths(step) else it.plusYears(step)
    }

    fun shiftBudgetMonth(step: Long) = budgetMonth.update { it.plusMonths(step) }

    fun setBudget(categoryId: Long, amount: Long) = viewModelScope.launch { c.finance.setBudget(categoryId, amount) }
    fun deleteBudget(categoryId: Long) = viewModelScope.launch { c.finance.deleteBudget(categoryId) }

    fun toggleHideAmounts() = viewModelScope.launch { c.settings.setHideAmounts(!hideAmounts.value) }

    fun deleteTransaction(tx: TransactionEntity) = viewModelScope.launch { c.finance.deleteTransaction(tx.id) }
    fun restoreTransaction(tx: TransactionEntity) = c.appScope.launch { c.finance.restoreTransaction(tx) }

    val insights: StateFlow<List<Insight>> = base.map { FinanceCalc.insights(it, LocalDate.now()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Status anggaran terkini untuk satu bulan, langsung dari basis data. */
suspend fun AppContainer.budgetSnapshot(month: YearMonth): List<BudgetStatus> {
    val base = FinanceBase(
        finance.accounts.first(),
        finance.categories.first(),
        finance.transactions.first(),
        finance.budgets.first(),
        emptyList(),
    )
    return FinanceCalc.budgetStatuses(base, month)
}

/** Memberi notifikasi bila sebuah anggaran baru saja melewati 80% atau 100%. */
fun AppContainer.notifyBudgetCrossings(before: List<BudgetStatus>, after: List<BudgetStatus>) {
    after.forEach { st ->
        val prev = before.firstOrNull { it.categoryId == st.categoryId }?.ratio ?: 0f
        val key = 7_000_000 + st.categoryId.toInt()
        when {
            prev < 1f && st.ratio >= 1f -> Notifications.showFinance(
                appContext,
                key,
                "⛔ Anggaran ${st.name} terlampaui",
                "Terpakai ${Money.format(st.spent)} dari ${Money.format(st.limit)} (${FinanceCalc.percent(st.ratio)}).",
            )
            prev < 0.8f && st.ratio >= 0.8f -> Notifications.showFinance(
                appContext,
                key,
                "⚠️ Anggaran ${st.name} hampir habis",
                "Sudah ${FinanceCalc.percent(st.ratio)} terpakai · sisa ${Money.format(st.remaining)} bulan ini.",
            )
        }
    }
}

class TransactionEditorViewModel(
    private val txId: Long,
    initialType: String?,
    private val c: AppContainer,
    initialAmount: Long? = null,
    initialNote: String? = null,
) : ViewModel() {
    val isNew = txId <= 0
    var type by mutableStateOf(enumOr(initialType, TxType.EXPENSE))
        private set
    /** Nominal awal bisa datang dari teks yang dibagikan (mis. SMS bank). */
    var amount by mutableStateOf(initialAmount?.takeIf { it > 0 }?.toString().orEmpty())
    var categoryId by mutableStateOf<Long?>(null)
    var accountId by mutableStateOf<Long?>(null)
    var toAccountId by mutableStateOf<Long?>(null)
    var date by mutableStateOf(LocalDate.now())
    var time by mutableStateOf(LocalTime.now().withSecond(0).withNano(0))
    var note by mutableStateOf(initialNote.orEmpty().take(200))
    var repeat by mutableStateOf(Repeat.NONE)
    var errors by mutableStateOf(emptySet<String>())
        private set
    var loaded by mutableStateOf(false)
        private set
    private var original: TransactionEntity? = null

    val accounts: StateFlow<List<AccountEntity>> = c.finance.accounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories: StateFlow<List<CategoryEntity>> = c.finance.categories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            if (!isNew) {
                c.finance.getTransaction(txId)?.let { tx ->
                    original = tx
                    type = enumOr(tx.type, TxType.EXPENSE)
                    amount = tx.amount.toString()
                    categoryId = tx.categoryId
                    accountId = tx.accountId
                    toAccountId = tx.toAccountId
                    val dt = tx.date.toLocalDateTime()
                    date = dt.toLocalDate()
                    time = dt.toLocalTime().withSecond(0).withNano(0)
                    note = tx.note
                }
            } else {
                val all = c.finance.allAccounts()
                accountId = all.firstOrNull()?.id
                if (type == TxType.TRANSFER) toAccountId = all.getOrNull(1)?.id
            }
            loaded = true
        }
    }

    fun changeType(newType: TxType) {
        if (newType == type) return
        type = newType
        categoryId = null
        errors = emptySet()
        if (newType == TxType.TRANSFER && toAccountId == null) {
            toAccountId = accounts.value.firstOrNull { it.id != accountId }?.id
        }
    }

    fun clearError(key: String) {
        errors = errors - key
    }

    suspend fun save(): Boolean {
        val value = amount.digitsToLong()
        val errs = mutableSetOf<String>()
        if (value <= 0) errs += "amount"
        if (accountId == null) errs += "account"
        if (type != TxType.TRANSFER && categoryId == null) errs += "category"
        if (type == TxType.TRANSFER && (toAccountId == null || toAccountId == accountId)) errs += "toAccount"
        errors = errs
        if (errs.isNotEmpty()) return false
        val tx = TransactionEntity(
            id = original?.id ?: 0,
            type = type.name,
            amount = value,
            categoryId = if (type == TxType.TRANSFER) null else categoryId,
            accountId = accountId!!,
            toAccountId = if (type == TxType.TRANSFER) toAccountId else null,
            date = date.atTime(time).toMillis(),
            note = note.trim(),
            createdAt = original?.createdAt ?: System.currentTimeMillis(),
        )
        val month = YearMonth.from(date)
        val before = if (type == TxType.EXPENSE) c.budgetSnapshot(month) else emptyList()
        val id = c.finance.saveTransaction(tx)
        if (isNew && repeat != Repeat.NONE) c.finance.addRecurring(tx.copy(id = id), repeat)
        if (type == TxType.EXPENSE) c.notifyBudgetCrossings(before, c.budgetSnapshot(month))
        return true
    }

    /** Menghapus transaksi dan mengembalikan salinannya agar bisa diurungkan. */
    suspend fun delete(): TransactionEntity? {
        val tx = original ?: return null
        c.finance.deleteTransaction(tx.id)
        return tx
    }

    fun restore(tx: TransactionEntity) = c.appScope.launch { c.finance.restoreTransaction(tx) }
}

class AccountsViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<Pair<List<AccountBalance>, Long>?> = combine(c.finance.accounts, c.finance.transactions) { a, t ->
        FinanceRepository.balances(a, t) to FinanceRepository.totalBalance(a, t)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val hideAmounts: StateFlow<Boolean> = c.settings.settings.map { it.hideAmounts }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun save(account: AccountEntity) = viewModelScope.launch { c.finance.saveAccount(account) }
    suspend fun transactionCount(id: Long): Int = c.finance.transactionCountForAccount(id)
    fun delete(id: Long) = viewModelScope.launch { c.finance.deleteAccount(id) }
}

class CategoriesViewModel(private val c: AppContainer) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> = c.finance.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(category: CategoryEntity) = viewModelScope.launch { c.finance.saveCategory(category) }
    fun delete(id: Long) = viewModelScope.launch { c.finance.deleteCategory(id) }
}

class GoalsViewModel(private val c: AppContainer) : ViewModel() {
    val goals: StateFlow<List<GoalEntity>?> = c.finance.goals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val hideAmounts: StateFlow<Boolean> = c.settings.settings.map { it.hideAmounts }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun save(goal: GoalEntity) = viewModelScope.launch { c.finance.saveGoal(goal) }
    fun delete(id: Long) = viewModelScope.launch { c.finance.deleteGoal(id) }
    fun addFunds(goal: GoalEntity, delta: Long) = viewModelScope.launch {
        c.finance.saveGoal(goal.copy(savedAmount = (goal.savedAmount + delta).coerceAtLeast(0)))
    }
}

class DebtsViewModel(private val c: AppContainer) : ViewModel() {
    val debts: StateFlow<List<DebtEntity>?> = c.finance.debts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val accounts: StateFlow<List<AccountEntity>> = c.finance.accounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun save(debt: DebtEntity) = viewModelScope.launch { c.finance.saveDebt(debt) }
    fun delete(id: Long) = viewModelScope.launch { c.finance.deleteDebt(id) }

    fun setSettled(debt: DebtEntity, settled: Boolean) = viewModelScope.launch {
        c.finance.saveDebt(debt.copy(isSettled = settled, paidAmount = if (settled) debt.amount else debt.paidAmount))
    }

    /** Mencatat pembayaran sebagian/penuh; opsional sekaligus dicatat sebagai transaksi dompet. */
    fun pay(debt: DebtEntity, amount: Long, recordAccountId: Long?) = viewModelScope.launch {
        val paid = (debt.paidAmount + amount).coerceAtMost(debt.amount)
        // Pembayaran melebihi sisa dipotong; dompet hanya mencatat bagian yang benar-benar melunasi.
        val applied = paid - debt.paidAmount
        c.finance.saveDebt(debt.copy(paidAmount = paid, isSettled = paid >= debt.amount))
        if (recordAccountId != null && applied > 0) {
            val payable = debt.type == DebtType.PAYABLE.name
            c.finance.saveTransaction(
                TransactionEntity(
                    type = if (payable) TxType.EXPENSE.name else TxType.INCOME.name,
                    amount = applied,
                    categoryId = null,
                    accountId = recordAccountId,
                    toAccountId = null,
                    date = System.currentTimeMillis(),
                    note = if (payable) "Bayar utang ke ${debt.person}" else "Terima piutang dari ${debt.person}",
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}

data class RecurringItem(
    val rule: RecurringEntity,
    val category: CategoryEntity?,
    val account: AccountEntity?,
    val toAccount: AccountEntity?,
)

class RecurringViewModel(private val c: AppContainer) : ViewModel() {
    val items: StateFlow<List<RecurringItem>?> = combine(c.finance.recurring, c.finance.categories, c.finance.accounts) { rules, cats, accs ->
        val catMap = cats.associateBy { it.id }
        val accMap = accs.associateBy { it.id }
        rules.map { r -> RecurringItem(r, r.categoryId?.let(catMap::get), accMap[r.accountId], r.toAccountId?.let(accMap::get)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setActive(rule: RecurringEntity, active: Boolean) = viewModelScope.launch { c.finance.setRecurringActive(rule, active) }
    fun delete(id: Long) = viewModelScope.launch { c.finance.deleteRecurring(id) }
}
