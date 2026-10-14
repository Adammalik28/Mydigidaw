package com.example.mydigidaw.ui.finance

import com.example.mydigidaw.data.db.AccountEntity
import com.example.mydigidaw.data.db.BudgetEntity
import com.example.mydigidaw.data.db.CategoryEntity
import com.example.mydigidaw.data.db.TransactionEntity
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.repo.FinanceRepository
import com.example.mydigidaw.util.toMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FinanceCalcTest {
    private val today = LocalDate.of(2026, 10, 15)

    private val cash = AccountEntity(1, "Tunai", "CASH", 100_000, 0, createdAt = 0)
    private val bank = AccountEntity(2, "Bank", "BANK", 1_000_000, 1, createdAt = 0)
    private val hidden = AccountEntity(3, "Dompet rahasia", "EWALLET", 50_000, 2, includeInTotal = false, createdAt = 0)

    private val food = CategoryEntity(10, "Makan", "EXPENSE", "🍔", 0)
    private val transport = CategoryEntity(11, "Transport", "EXPENSE", "🚗", 1)
    private val salary = CategoryEntity(20, "Gaji", "INCOME", "💼", 2)

    private fun at(month: Int, day: Int) = LocalDate.of(2026, month, day).atTime(12, 0).toMillis()

    private var nextId = 1L
    private fun tx(type: TxType, amount: Long, category: CategoryEntity?, account: AccountEntity, date: Long, to: AccountEntity? = null, note: String = "") =
        TransactionEntity(nextId++, type.name, amount, category?.id, account.id, to?.id, date, note, date)

    // Urut terbaru dulu, seperti hasil DAO.
    private val transactions = listOf(
        tx(TxType.EXPENSE, 30_000, transport, cash, at(10, 10), note = "Ojek"),
        tx(TxType.TRANSFER, 200_000, null, bank, at(10, 5), to = cash),
        tx(TxType.EXPENSE, 20_000, food, hidden, at(10, 3)),
        tx(TxType.EXPENSE, 50_000, food, cash, at(10, 2), note = "Nasi padang"),
        tx(TxType.INCOME, 5_000_000, salary, bank, at(10, 1), note = "Gaji Oktober"),
        tx(TxType.EXPENSE, 100_000, food, cash, at(9, 5)),
    )

    private val base = FinanceBase(
        accounts = listOf(cash, bank, hidden),
        categories = listOf(food, transport, salary),
        transactions = transactions,
        budgets = listOf(BudgetEntity(1, 0, 1_000_000), BudgetEntity(2, food.id, 60_000), BudgetEntity(3, transport.id, 100_000)),
        goals = emptyList(),
    )

    @Test
    fun sums_ignoreTransfers() {
        val october = transactions.filter { it.date >= at(10, 1) - 12 * 3_600_000 }
        assertEquals(5_000_000L to 100_000L, FinanceCalc.sums(october))
    }

    @Test
    fun totalBalance_countsOnlyIncludedAccountsAndTransfersNetToZero() {
        // 1.100.000 awal + 5.000.000 gaji − 50.000 − 30.000 − 100.000; transfer antar dompet yang dihitung = 0.
        assertEquals(5_920_000, FinanceRepository.totalBalance(base.accounts, base.transactions))
    }

    @Test
    fun balances_perAccountIncludeTransfers() {
        val byId = FinanceRepository.balances(base.accounts, base.transactions).associate { it.account.id to it.balance }
        assertEquals(120_000L, byId[cash.id])
        assertEquals(5_800_000L, byId[bank.id])
        assertEquals(30_000L, byId[hidden.id])
    }

    @Test
    fun budgetStatuses_totalFirstThenMostUsed() {
        val statuses = FinanceCalc.budgetStatuses(base, YearMonth.of(2026, 10))
        assertEquals(listOf(0L, food.id, transport.id), statuses.map { it.categoryId })
        assertEquals(100_000L, statuses[0].spent)
        assertEquals(70_000L, statuses[1].spent)
        assertTrue("anggaran makan terlampaui", statuses[1].ratio > 1f)
        assertEquals(70_000L, statuses[2].remaining)
    }

    @Test
    fun overview_flagsBudgetsAbove80Percent() {
        val overview = FinanceCalc.overview(base, today)
        assertEquals(5_000_000L, overview.monthIncome)
        assertEquals(100_000L, overview.monthExpense)
        assertEquals(100_000L, overview.prevExpense)
        assertEquals(listOf(food.id), overview.budgetAlerts.map { it.categoryId })
        assertEquals(6, overview.sixMonths.size)
    }

    @Test
    fun txList_filtersByTypeQueryAndAmount() {
        val expenses = FinanceCalc.txList(base, TxFilter(type = TxType.EXPENSE), today)
        assertEquals(listOf(30_000L, 20_000L, 50_000L), expenses.flat.map { it.tx.amount })
        assertEquals(100_000L, expenses.expense)

        val makan = FinanceCalc.txList(base, TxFilter(query = "makan"), today)
        assertEquals(listOf(20_000L, 50_000L), makan.flat.map { it.tx.amount })

        val byNote = FinanceCalc.txList(base, TxFilter(query = "padang"), today)
        assertEquals(1, byNote.flat.size)

        val byAmount = FinanceCalc.txList(base, TxFilter(query = "30000"), today)
        assertEquals(listOf(30_000L), byAmount.flat.map { it.tx.amount })
    }

    @Test
    fun txList_sortingAndPeriods() {
        val largest = FinanceCalc.txList(base, TxFilter(sort = TxSort.LARGEST), today)
        assertEquals(5_000_000L, largest.flat.first().tx.amount)
        assertTrue("urutan nominal tidak dikelompokkan per hari", largest.groups.isEmpty())

        val lastMonth = FinanceCalc.txList(base, TxFilter(period = Period.LAST_MONTH), today)
        assertEquals(listOf(100_000L), lastMonth.flat.map { it.tx.amount })

        val all = FinanceCalc.txList(base, TxFilter(period = Period.ALL), today)
        assertEquals(transactions.size, all.flat.size)
    }

    @Test
    fun txList_groupsByDayWithNet() {
        val list = FinanceCalc.txList(base, TxFilter(), today)
        val oct2 = list.groups.single { it.date == LocalDate.of(2026, 10, 2) }
        assertEquals(-50_000L, oct2.net)
        val oct5 = list.groups.single { it.date == LocalDate.of(2026, 10, 5) }
        assertEquals("transfer tidak mengubah selisih harian", 0L, oct5.net)
    }

    @Test
    fun insights_reportSavingsRate() {
        val insights = FinanceCalc.insights(base, today)
        assertTrue(insights.any { it.title == "Menabung 98% dari pemasukan" && it.tone == InsightTone.GOOD })
        // Diurutkan: peringatan lebih dulu daripada kabar baik dan info.
        assertEquals(insights.sortedBy { it.tone.ordinal }, insights)
    }

    @Test
    fun deltaAndPercentLabels() {
        assertEquals("▲ 20% vs bulan lalu", FinanceCalc.deltaLabel(120, 100))
        assertEquals("▼ 20% vs bulan lalu", FinanceCalc.deltaLabel(80, 100))
        assertNull(FinanceCalc.deltaLabel(80, 0))
        assertEquals("12,5%", FinanceCalc.percent(0.125f))
    }
}
