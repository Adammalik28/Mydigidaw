package com.example.mydigidaw.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.MainActivity
import com.example.mydigidaw.MydigidawApp
import com.example.mydigidaw.R
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.repo.FinanceRepository
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.Money
import com.example.mydigidaw.util.endMillis
import com.example.mydigidaw.util.startMillis
import com.example.mydigidaw.util.toLocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class WidgetAgenda(val time: String, val title: String)

data class WidgetData(
    val masked: Boolean,
    val balance: Long,
    val income: Long,
    val expense: Long,
    val agenda: List<WidgetAgenda>,
    val tasksToday: Int,
    val dateLabel: String,
)

private suspend fun loadWidgetData(c: AppContainer): WidgetData {
    val s = c.settings.current()
    val accounts = c.db.accounts().all()
    val txs = c.db.transactions().all()
    val month = YearMonth.now()
    val from = month.atDay(1).startMillis()
    val to = month.plusMonths(1).atDay(1).startMillis()
    var income = 0L
    var expense = 0L
    txs.filter { it.date in from until to }.forEach {
        when (it.type) {
            TxType.INCOME.name -> income += it.amount
            TxType.EXPENSE.name -> expense += it.amount
        }
    }
    val today = LocalDate.now()
    val now = System.currentTimeMillis()
    val agenda = runCatching {
        c.calendar.agenda(c.db.events().all(), s, today.startMillis(), today.plusDays(1).endMillis())
            .filter { it.end > now || it.allDay }
            .take(3)
            .map { a ->
                val day = a.start.toLocalDate()
                val prefix = if (day == today) "" else "Besok "
                WidgetAgenda(if (a.allDay) "${prefix}Seharian" else prefix + Dates.time(a.start), a.title)
            }
    }.getOrDefault(emptyList())
    val tasksToday = c.db.tasks().all().count { !it.isDone && it.dueAt != null && !it.dueAt.toLocalDate().isAfter(today) }
    return WidgetData(
        masked = s.hideAmounts || s.appLock,
        balance = FinanceRepository.totalBalance(accounts, txs),
        income = income,
        expense = expense,
        agenda = agenda,
        tasksToday = tasksToday,
        dateLabel = Dates.weekdayDayMonth(today),
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun widgetFlow(c: AppContainer): Flow<WidgetData> =
    combine(c.finance.transactions, c.finance.accounts, c.calendar.events, c.tasks.tasks, c.settings.settings) { _, _, _, _, _ -> }
        .mapLatest { loadWidgetData(c) }

class SummaryWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as MydigidawApp).container
        val initial = loadWidgetData(container)
        provideContent {
            val data by widgetFlow(container).collectAsState(initial)
            SummaryContent(context, data)
        }
    }
}

class SummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummaryWidget()
}

private val White = ColorProvider(Color.White)
private val WhiteSoft = ColorProvider(Color(0xCCFFFFFF))

private fun deepLink(context: Context, link: String): Intent =
    Intent(context, MainActivity::class.java)
        .setAction(Intent.ACTION_VIEW)
        .setData(link.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

@Composable
private fun SummaryContent(context: Context, data: WidgetData) {
    val size = LocalSize.current
    val compact = size.height < 160.dp
    val showAgenda = size.height >= 190.dp
    val money = { v: Long -> if (data.masked) Money.masked() else Money.format(v) }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ImageProvider(R.drawable.widget_background))
            .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 14.dp)
            .clickable(actionStartActivity(deepLink(context, "mydigidaw://open/home"))),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (compact) "Total saldo" else "Mydigidaw",
                style = TextStyle(color = if (compact) WhiteSoft else White, fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(data.dateLabel, style = TextStyle(color = WhiteSoft, fontSize = 11.sp))
        }
        if (!compact) {
            Spacer(GlanceModifier.height(6.dp))
            Text("Total saldo", style = TextStyle(color = WhiteSoft, fontSize = 11.sp))
        }
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                money(data.balance),
                style = TextStyle(color = White, fontSize = if (compact) 20.sp else 24.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            if (compact) {
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    "↓ ${money(data.income)}  ↑ ${money(data.expense)}",
                    style = TextStyle(color = WhiteSoft, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
        }
        if (!compact) {
            Spacer(GlanceModifier.height(2.dp))
            Text(
                "↓ ${money(data.income)}   ↑ ${money(data.expense)}  · bulan ini",
                style = TextStyle(color = WhiteSoft, fontSize = 12.sp),
                maxLines = 1,
            )
        }
        if (showAgenda) {
            Spacer(GlanceModifier.height(10.dp))
            if (data.agenda.isEmpty()) {
                Text(
                    if (data.tasksToday > 0) "Tidak ada jadwal · ${data.tasksToday} tugas hari ini" else "Tidak ada jadwal hari ini",
                    style = TextStyle(color = WhiteSoft, fontSize = 12.sp),
                )
            } else {
                data.agenda.take(if (size.height >= 230.dp) 3 else 2).forEach { a ->
                    Row(modifier = GlanceModifier.padding(bottom = 2.dp)) {
                        Text(a.time, style = TextStyle(color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        Spacer(GlanceModifier.width(8.dp))
                        Text(a.title, style = TextStyle(color = White, fontSize = 12.sp), maxLines = 1)
                    }
                }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            val h = if (compact) 28.dp else 32.dp
            QuickPill(context, "+ Transaksi", "mydigidaw://new/transaction", h, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            QuickPill(context, "+ Catatan", "mydigidaw://new/note", h, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            QuickPill(context, "+ Jadwal", "mydigidaw://new/event", h, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun QuickPill(context: Context, label: String, link: String, height: androidx.compose.ui.unit.Dp, modifier: GlanceModifier) {
    Box(
        modifier = modifier
            .height(height)
            .cornerRadius(16.dp)
            .background(ImageProvider(R.drawable.widget_pill))
            .clickable(actionStartActivity(deepLink(context, link))),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TextStyle(color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

/** Menyegarkan widget setiap kali data penting berubah (hanya bila widget terpasang). */
object WidgetUpdater {
    suspend fun updateNow(context: Context) {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(SummaryWidget::class.java)
        if (ids.isNotEmpty()) SummaryWidget().updateAll(context)
    }

    @OptIn(FlowPreview::class)
    fun start(context: Context, c: AppContainer) {
        c.appScope.launch {
            combine(c.finance.transactions, c.finance.accounts, c.calendar.events, c.tasks.tasks, c.settings.settings) { _, _, _, _, _ -> }
                .debounce(1_500)
                .collect { runCatching { updateNow(context) } }
        }
    }
}
