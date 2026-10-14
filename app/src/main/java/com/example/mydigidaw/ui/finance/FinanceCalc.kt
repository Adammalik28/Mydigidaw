package com.example.mydigidaw.ui.finance

import com.example.mydigidaw.data.db.AccountEntity
import com.example.mydigidaw.data.db.BudgetEntity
import com.example.mydigidaw.data.db.CategoryEntity
import com.example.mydigidaw.data.db.GoalEntity
import com.example.mydigidaw.data.db.NEUTRAL_COLOR
import com.example.mydigidaw.data.db.TransactionEntity
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.model.effectOn
import com.example.mydigidaw.data.repo.AccountBalance
import com.example.mydigidaw.data.repo.FinanceRepository
import com.example.mydigidaw.data.repo.TxItem
import com.example.mydigidaw.ui.components.BarGroup
import com.example.mydigidaw.ui.components.LinePoint
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.Money
import com.example.mydigidaw.util.endMillis
import com.example.mydigidaw.util.startMillis
import com.example.mydigidaw.util.toLocalDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class FinanceBase(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val goals: List<GoalEntity>,
) {
    val categoryMap: Map<Long, CategoryEntity> = categories.associateBy { it.id }
    val accountMap: Map<Long, AccountEntity> = accounts.associateBy { it.id }

    fun item(tx: TransactionEntity) =
        TxItem(tx, tx.categoryId?.let(categoryMap::get), accountMap[tx.accountId], tx.toAccountId?.let(accountMap::get))
}

enum class Period(val label: String) {
    TODAY("Hari ini"),
    LAST7("7 hari terakhir"),
    LAST30("30 hari terakhir"),
    THIS_MONTH("Bulan ini"),
    LAST_MONTH("Bulan lalu"),
    LAST3M("3 bulan terakhir"),
    THIS_YEAR("Tahun ini"),
    ALL("Semua waktu"),
    CUSTOM("Rentang kustom…"),
}

enum class TxSort(val label: String) {
    NEWEST("Terbaru"),
    OLDEST("Terlama"),
    LARGEST("Nominal terbesar"),
    SMALLEST("Nominal terkecil"),
}

data class TxFilter(
    val period: Period = Period.THIS_MONTH,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
    val type: TxType? = null,
    val categoryIds: Set<Long> = emptySet(),
    val accountId: Long? = null,
    val query: String = "",
    val sort: TxSort = TxSort.NEWEST,
) {
    val hasExtraFilters: Boolean
        get() = type != null || categoryIds.isNotEmpty() || accountId != null || query.isNotBlank() || sort != TxSort.NEWEST
}

data class TxDayGroup(val date: LocalDate, val items: List<TxItem>, val net: Long)

data class TxListState(
    val groups: List<TxDayGroup>,
    val flat: List<TxItem>,
    val income: Long,
    val expense: Long,
    val rangeLabel: String,
)

data class OverviewState(
    val totalBalance: Long,
    val balances: List<AccountBalance>,
    val monthIncome: Long,
    val monthExpense: Long,
    val prevIncome: Long,
    val prevExpense: Long,
    val sixMonths: List<BarGroup>,
    val recent: List<TxItem>,
    val goals: List<GoalEntity>,
    val budgetAlerts: List<BudgetStatus>,
)

enum class StatsMode(val label: String) { MONTH("Bulanan"), YEAR("Tahunan") }

data class CategoryShare(
    val name: String,
    val emoji: String,
    val colorIndex: Int,
    val amount: Long,
    val fraction: Float,
)

data class MonthRow(val label: String, val income: Long, val expense: Long)

data class StatsState(
    val title: String,
    val shares: List<CategoryShare>,
    val sharesTotal: Long,
    val bars: List<BarGroup>,
    val barRows: List<MonthRow>,
    val trend: List<LinePoint>,
    val income: Long,
    val expense: Long,
    val avgDailyExpense: Long,
    val largestExpense: TxItem?,
    val count: Int,
)

data class BudgetStatus(
    val categoryId: Long,
    val category: CategoryEntity?,
    val limit: Long,
    val spent: Long,
) {
    val ratio: Float get() = if (limit <= 0) 0f else spent.toFloat() / limit
    val remaining: Long get() = limit - spent
    val name: String get() = if (categoryId == 0L) "Total pengeluaran" else category?.name ?: "Kategori terhapus"
    val emoji: String get() = if (categoryId == 0L) "💰" else category?.emoji ?: "❔"
}

enum class InsightTone { WARN, GOOD, NEUTRAL }

data class Insight(val emoji: String, val title: String, val body: String, val tone: InsightTone)

data class BudgetState(val month: YearMonth, val statuses: List<BudgetStatus>, val daysLeft: Int)

object FinanceCalc {
    fun range(f: TxFilter, today: LocalDate): Pair<Long, Long>? {
        val month = YearMonth.from(today)
        return when (f.period) {
            Period.TODAY -> today.startMillis() to today.endMillis()
            Period.LAST7 -> today.minusDays(6).startMillis() to today.endMillis()
            Period.LAST30 -> today.minusDays(29).startMillis() to today.endMillis()
            Period.THIS_MONTH -> month.atDay(1).startMillis() to month.plusMonths(1).atDay(1).startMillis()
            Period.LAST_MONTH -> month.minusMonths(1).atDay(1).startMillis() to month.atDay(1).startMillis()
            Period.LAST3M -> month.minusMonths(2).atDay(1).startMillis() to month.plusMonths(1).atDay(1).startMillis()
            Period.THIS_YEAR -> LocalDate.of(today.year, 1, 1).startMillis() to LocalDate.of(today.year + 1, 1, 1).startMillis()
            Period.ALL -> null
            Period.CUSTOM -> {
                val s = f.customStart
                val e = f.customEnd
                if (s != null && e != null) s.startMillis() to e.endMillis() else null
            }
        }
    }

    fun rangeLabel(f: TxFilter, today: LocalDate): String = when (f.period) {
        Period.CUSTOM -> {
            val s = f.customStart
            val e = f.customEnd
            if (s != null && e != null) "${Dates.medium(s)} – ${Dates.medium(e)}" else Period.ALL.label
        }
        Period.THIS_MONTH -> Dates.monthYear(YearMonth.from(today))
        Period.LAST_MONTH -> Dates.monthYear(YearMonth.from(today).minusMonths(1))
        Period.THIS_YEAR -> "Tahun ${today.year}"
        else -> f.period.label
    }

    /** (pemasukan, pengeluaran); transfer antardompet tidak dihitung. */
    fun sums(transactions: Iterable<TransactionEntity>): Pair<Long, Long> {
        var income = 0L
        var expense = 0L
        for (t in transactions) {
            when (t.type) {
                TxType.INCOME.name -> income += t.amount
                TxType.EXPENSE.name -> expense += t.amount
            }
        }
        return income to expense
    }

    fun signed(item: TxItem): Long = when (item.type) {
        TxType.INCOME -> item.tx.amount
        TxType.EXPENSE -> -item.tx.amount
        TxType.TRANSFER -> 0
    }

    private fun inMonth(base: FinanceBase, month: YearMonth): List<TransactionEntity> {
        val s = month.atDay(1).startMillis()
        val e = month.plusMonths(1).atDay(1).startMillis()
        return base.transactions.filter { it.date >= s && it.date < e }
    }

    fun txList(base: FinanceBase, f: TxFilter, today: LocalDate): TxListState {
        val r = range(f, today)
        val q = f.query.trim()
        val filtered = base.transactions.asSequence()
            .filter { r == null || (it.date >= r.first && it.date < r.second) }
            .filter { f.type == null || it.type == f.type.name }
            .filter { f.categoryIds.isEmpty() || (it.categoryId != null && it.categoryId in f.categoryIds) }
            .filter { f.accountId == null || it.accountId == f.accountId || it.toAccountId == f.accountId }
            .map(base::item)
            .filter { item ->
                q.isEmpty() ||
                    item.tx.note.contains(q, true) ||
                    item.category?.name?.contains(q, true) == true ||
                    item.account?.name?.contains(q, true) == true ||
                    item.tx.amount.toString().contains(q.filter(Char::isDigit).ifEmpty { "\u0000" })
            }
            .toList()
        val sorted = when (f.sort) {
            TxSort.NEWEST -> filtered
            TxSort.OLDEST -> filtered.sortedWith(compareBy({ it.tx.date }, { it.tx.id }))
            TxSort.LARGEST -> filtered.sortedByDescending { it.tx.amount }
            TxSort.SMALLEST -> filtered.sortedBy { it.tx.amount }
        }
        val (income, expense) = sums(filtered.map { it.tx })
        val groups = if (f.sort == TxSort.NEWEST || f.sort == TxSort.OLDEST) {
            sorted.groupBy { it.tx.date.toLocalDate() }.map { (date, items) -> TxDayGroup(date, items, items.sumOf(::signed)) }
        } else {
            emptyList()
        }
        return TxListState(groups, sorted, income, expense, rangeLabel(f, today))
    }

    fun overview(base: FinanceBase, today: LocalDate): OverviewState {
        val month = YearMonth.from(today)
        val (mi, me) = sums(inMonth(base, month))
        val (pi, pe) = sums(inMonth(base, month.minusMonths(1)))
        val bars = (5 downTo 0).map { back ->
            val ym = month.minusMonths(back.toLong())
            val (i, e) = sums(inMonth(base, ym))
            BarGroup(Dates.monthShort(ym), i, e)
        }
        return OverviewState(
            totalBalance = FinanceRepository.totalBalance(base.accounts, base.transactions),
            balances = FinanceRepository.balances(base.accounts, base.transactions),
            monthIncome = mi,
            monthExpense = me,
            prevIncome = pi,
            prevExpense = pe,
            sixMonths = bars,
            recent = base.transactions.take(5).map(base::item),
            goals = base.goals,
            budgetAlerts = budgetStatuses(base, month).filter { it.ratio >= 0.8f },
        )
    }

    fun budgetStatuses(base: FinanceBase, month: YearMonth): List<BudgetStatus> {
        val expenses = inMonth(base, month).filter { it.type == TxType.EXPENSE.name }
        val byCategory = expenses.groupBy { it.categoryId }.mapValues { (_, list) -> list.sumOf { it.amount } }
        val total = expenses.sumOf { it.amount }
        return base.budgets.map { b ->
            BudgetStatus(
                categoryId = b.categoryId,
                category = if (b.categoryId == 0L) null else base.categoryMap[b.categoryId],
                limit = b.amount,
                spent = if (b.categoryId == 0L) total else byCategory[b.categoryId] ?: 0,
            )
        }.sortedWith(compareBy<BudgetStatus> { it.categoryId != 0L }.thenByDescending { it.ratio })
    }

    fun budgetState(base: FinanceBase, month: YearMonth, today: LocalDate): BudgetState {
        val daysLeft = if (YearMonth.from(today) == month) month.lengthOfMonth() - today.dayOfMonth + 1 else 0
        return BudgetState(month, budgetStatuses(base, month), daysLeft)
    }

    fun stats(base: FinanceBase, mode: StatsMode, month: YearMonth, type: TxType, today: LocalDate): StatsState {
        val start = if (mode == StatsMode.MONTH) month.atDay(1) else LocalDate.of(month.year, 1, 1)
        val end = if (mode == StatsMode.MONTH) month.plusMonths(1).atDay(1) else LocalDate.of(month.year + 1, 1, 1)
        val startMs = start.startMillis()
        val endMs = end.startMillis()
        val inPeriod = base.transactions.filter { it.date >= startMs && it.date < endMs }
        val (income, expense) = sums(inPeriod)

        // Komposisi per kategori: maksimal 6 irisan, sisanya dilipat ke "Lainnya".
        val ofType = inPeriod.filter { it.type == type.name }
        val typeTotal = ofType.sumOf { it.amount }
        val grouped = ofType.groupBy { it.categoryId }
            .map { (id, list) -> id to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
        val shares = mutableListOf<CategoryShare>()
        val head = if (grouped.size > 6) grouped.take(5) else grouped
        head.forEach { (id, amount) ->
            val cat = id?.let(base.categoryMap::get)
            shares += CategoryShare(
                name = cat?.name ?: "Tanpa kategori",
                emoji = cat?.emoji ?: "❔",
                colorIndex = cat?.color ?: NEUTRAL_COLOR,
                amount = amount,
                fraction = if (typeTotal > 0) amount.toFloat() / typeTotal else 0f,
            )
        }
        if (grouped.size > 6) {
            val rest = grouped.drop(5)
            val amount = rest.sumOf { it.second }
            shares += CategoryShare("Lainnya (${rest.size} kategori)", "📦", NEUTRAL_COLOR, amount, amount.toFloat() / typeTotal)
        }

        val months = if (mode == StatsMode.MONTH) (5 downTo 0).map { month.minusMonths(it.toLong()) }
        else (1..12).map { YearMonth.of(month.year, it) }
        val rows = months.map { ym ->
            val (i, e) = sums(inMonth(base, ym))
            MonthRow(if (mode == StatsMode.MONTH) Dates.monthShortYear(ym) else Dates.monthShort(ym), i, e)
        }
        val bars = months.zip(rows).map { (ym, r) -> BarGroup(Dates.monthShort(ym), r.income, r.expense) }

        // Tren saldo gabungan dompet yang dihitung dalam total; tidak memproyeksikan hari depan.
        val included = base.accounts.filter { it.includeInTotal }
        val ids = included.map { it.id }.toSet()
        val ascending = base.transactions.sortedBy { it.date }
        var balance = included.sumOf { it.initialBalance }
        var idx = 0
        while (idx < ascending.size && ascending[idx].date < startMs) {
            balance += ascending[idx].effectOn(ids)
            idx++
        }
        val lastDay = minOf(end.minusDays(1), today)
        val trend = mutableListOf<LinePoint>()
        if (!start.isAfter(today)) {
            if (mode == StatsMode.MONTH) {
                var d = start
                while (!d.isAfter(lastDay)) {
                    val dayEnd = d.endMillis()
                    while (idx < ascending.size && ascending[idx].date < dayEnd) {
                        balance += ascending[idx].effectOn(ids)
                        idx++
                    }
                    trend += LinePoint(Dates.dayMonth(d), balance)
                    d = d.plusDays(1)
                }
            } else {
                var ym = YearMonth.from(start)
                val lastMonth = YearMonth.from(lastDay)
                while (!ym.isAfter(lastMonth)) {
                    val monthEnd = ym.plusMonths(1).atDay(1).startMillis()
                    while (idx < ascending.size && ascending[idx].date < monthEnd) {
                        balance += ascending[idx].effectOn(ids)
                        idx++
                    }
                    trend += LinePoint(Dates.monthShort(ym), balance)
                    ym = ym.plusMonths(1)
                }
            }
        }

        val daysElapsed = when {
            today.isBefore(start) -> 0L
            !today.isBefore(end) -> ChronoUnit.DAYS.between(start, end)
            else -> ChronoUnit.DAYS.between(start, today) + 1
        }
        return StatsState(
            title = if (mode == StatsMode.MONTH) Dates.monthYear(month) else month.year.toString(),
            shares = shares,
            sharesTotal = typeTotal,
            bars = bars,
            barRows = rows,
            trend = trend,
            income = income,
            expense = expense,
            avgDailyExpense = if (daysElapsed > 0) expense / daysElapsed else 0,
            largestExpense = inPeriod.filter { it.type == TxType.EXPENSE.name }.maxByOrNull { it.amount }?.let(base::item),
            count = inPeriod.size,
        )
    }

    /** Wawasan otomatis dari pola keuangan bulan ini, diurutkan dari yang paling perlu perhatian. */
    fun insights(base: FinanceBase, today: LocalDate): List<Insight> {
        val out = mutableListOf<Insight>()
        val month = YearMonth.from(today)
        val day = today.dayOfMonth
        val thisMonth = inMonth(base, month)
        val (income, expense) = sums(thisMonth)

        val prev = month.minusMonths(1)
        val prevEnd = prev.atDay(minOf(day, prev.lengthOfMonth())).endMillis()
        val prevExpense = sums(base.transactions.filter { it.date >= prev.atDay(1).startMillis() && it.date < prevEnd }).second
        if (prevExpense > 0 && expense > 0) {
            val pct = (expense - prevExpense) * 100.0 / prevExpense
            if (pct >= 10) {
                out += Insight(
                    "📈",
                    "Pengeluaran naik ${pct.toInt()}%",
                    "Dibanding periode yang sama bulan lalu (${Money.compact(prevExpense)} → ${Money.compact(expense)}).",
                    InsightTone.WARN,
                )
            } else if (pct <= -10) {
                out += Insight(
                    "🎉",
                    "Pengeluaran turun ${(-pct).toInt()}%",
                    "Lebih hemat dibanding periode yang sama bulan lalu. Pertahankan!",
                    InsightTone.GOOD,
                )
            }
        }

        if (income > 0) {
            val rate = (income - expense) * 100 / income
            out += when {
                rate < 0 -> Insight(
                    "⚠️",
                    "Pengeluaran melebihi pemasukan",
                    "Bulan ini defisit ${Money.format(expense - income)}. Cek kategori terbesar.",
                    InsightTone.WARN,
                )
                rate >= 20 -> Insight("💪", "Menabung $rate% dari pemasukan", "Rasio tabungan yang sehat — di atas 20%.", InsightTone.GOOD)
                else -> Insight("💡", "Menabung $rate% dari pemasukan", "Targetkan minimal 20% agar dana darurat cepat terkumpul.", InsightTone.NEUTRAL)
            }
        }

        val last30 = base.transactions
            .filter { it.type == TxType.EXPENSE.name && it.date >= today.minusDays(29).startMillis() }
            .sumOf { it.amount }
        val total = FinanceRepository.totalBalance(base.accounts, base.transactions)
        if (last30 > 0 && total > 0) {
            val days = total * 30 / last30
            out += Insight(
                "🛟",
                "Saldo cukup ±$days hari",
                "Berdasarkan rata-rata pengeluaran 30 hari terakhir.",
                if (days < 30) InsightTone.WARN else InsightTone.GOOD,
            )
        }

        val expenses = thisMonth.filter { it.type == TxType.EXPENSE.name }
        val top = expenses.groupBy { it.categoryId }.maxByOrNull { (_, list) -> list.sumOf { it.amount } }
        if (top != null && expense > 0) {
            val cat = top.key?.let(base.categoryMap::get)
            val amount = top.value.sumOf { it.amount }
            out += Insight(
                cat?.emoji ?: "📦",
                "Terbanyak: ${cat?.name ?: "Tanpa kategori"}",
                "${percent(amount.toFloat() / expense)} dari pengeluaran bulan ini (${Money.format(amount)}).",
                InsightTone.NEUTRAL,
            )
        }

        if (day >= 5 && day < month.lengthOfMonth() && expense > 0) {
            val projected = expense * month.lengthOfMonth() / day
            out += Insight(
                "🔮",
                "Perkiraan akhir bulan",
                "Dengan laju sekarang, pengeluaran bulan ini sekitar ${Money.format(projected)}.",
                if (income > 0 && projected > income) InsightTone.WARN else InsightTone.NEUTRAL,
            )
        }

        // Transaksi bertanggal ke depan (mis. tagihan yang sudah dicatat) tidak mengurangi hari yang sudah lewat.
        val spendDays = expenses.filter { it.date < today.endMillis() }.map { it.date.toLocalDate() }.toSet().size
        val noSpendDays = day - spendDays
        if (noSpendDays >= 3 && thisMonth.isNotEmpty()) {
            out += Insight("🌿", "$noSpendDays hari tanpa belanja", "Hari-hari tanpa pengeluaran bulan ini. Bagus untuk dompet!", InsightTone.GOOD)
        }
        return out.sortedBy { it.tone.ordinal }
    }

    /** Persentase perubahan untuk label delta, mis. "▲ 12% vs bulan lalu". */
    fun deltaLabel(current: Long, previous: Long): String? {
        if (previous <= 0) return null
        val pct = (current - previous) * 100.0 / previous
        val arrow = if (pct >= 0) "▲" else "▼"
        return "$arrow ${"%.0f".format(kotlin.math.abs(pct))}% vs bulan lalu"
    }

    fun percent(fraction: Float): String = "${"%.1f".format(fraction * 100).replace('.', ',')}%"

    fun moneyOrMasked(amount: Long, hidden: Boolean, withSign: Boolean = false): String =
        if (hidden) Money.masked() else Money.format(amount, withSign)
}
