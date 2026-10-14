@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.mydigidaw.ui.habits

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.data.db.HabitEntity
import com.example.mydigidaw.data.db.HabitLogEntity
import com.example.mydigidaw.data.model.HabitLogic
import com.example.mydigidaw.reminders.Notifications
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.calendar.gridStart
import com.example.mydigidaw.ui.components.Spacing
import com.example.mydigidaw.ui.components.AppCard
import com.example.mydigidaw.ui.components.BackTopBar
import com.example.mydigidaw.ui.components.ColorPickerRow
import com.example.mydigidaw.ui.components.ConfirmDialog
import com.example.mydigidaw.ui.components.EmojiBadge
import com.example.mydigidaw.ui.components.EmojiPicker
import com.example.mydigidaw.ui.components.EmptyState
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.Meter
import com.example.mydigidaw.ui.components.MonthSwitcher
import com.example.mydigidaw.ui.components.Motion
import com.example.mydigidaw.ui.components.StatTile
import com.example.mydigidaw.ui.components.TimePickerModal
import com.example.mydigidaw.ui.components.enterStagger
import com.example.mydigidaw.ui.components.heroBrush
import com.example.mydigidaw.ui.components.heroDecoration
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Dates
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val habitEmojis = listOf(
    "🏋️", "🏃", "🚶", "🧘", "🚴", "🏊", "💧", "📖", "✍️", "🛌", "🥗", "🍎",
    "🚭", "💊", "🧹", "💰", "🙏", "🕌", "🎸", "🧠", "🌅", "📵", "🦷", "✅",
)

data class HabitDay(val date: LocalDate, val scheduled: Boolean, val count: Int)

data class HabitUi(
    val habit: HabitEntity,
    val todayCount: Int,
    val week: List<HabitDay>,
    val streak: Int,
    val scheduledToday: Boolean,
) {
    val doneToday: Boolean get() = todayCount >= habit.targetPerDay
}

object HabitsCalc {
    fun build(habits: List<HabitEntity>, logs: List<HabitLogEntity>, today: LocalDate): List<HabitUi> {
        val byHabit = logs.groupBy { it.habitId }.mapValues { (_, list) -> list.associate { it.day to it.count } }
        return habits.map { h ->
            val counts = byHabit[h.id].orEmpty()
            HabitUi(
                habit = h,
                todayCount = counts[today.toEpochDay()] ?: 0,
                week = (6 downTo 0).map { back ->
                    val d = today.minusDays(back.toLong())
                    HabitDay(d, HabitLogic.isScheduled(h, d), counts[d.toEpochDay()] ?: 0)
                },
                streak = HabitLogic.currentStreak(h, counts, today),
                scheduledToday = HabitLogic.isScheduled(h, today),
            )
        }
    }

    fun scheduleLabel(habit: HabitEntity): String = when (habit.daysMask) {
        0b1111111 -> "Setiap hari"
        0b0011111 -> "Hari kerja"
        0b1100000 -> "Akhir pekan"
        else -> (0 until 7).filter { habit.daysMask and (1 shl it) != 0 }.joinToString(", ") { Dates.weekdayShort[it] }
    }
}

class HabitsViewModel(private val c: AppContainer) : ViewModel() {
    val habits: StateFlow<List<HabitUi>?> = combine(c.habits.habits, c.habits.logsSince(LocalDate.now().minusDays(400))) { h, l ->
        HabitsCalc.build(h, l, LocalDate.now())
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggle(habit: HabitEntity, date: LocalDate) = viewModelScope.launch { c.habits.toggle(habit, date) }
    fun increment(habit: HabitEntity) = viewModelScope.launch { c.habits.increment(habit.id, LocalDate.now()) }
    fun decrement(habit: HabitEntity) = viewModelScope.launch { c.habits.decrement(habit.id, LocalDate.now()) }
    fun save(habit: HabitEntity) = viewModelScope.launch { c.habits.save(habit) }
}

class HabitDetailViewModel(private val habitId: Long, private val c: AppContainer) : ViewModel() {
    val habit: StateFlow<HabitEntity?> = c.habits.habits.map { list -> list.firstOrNull { it.id == habitId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val counts: StateFlow<Map<Long, Int>> = c.habits.logsFor(habitId).map { list -> list.associate { it.day to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val month = MutableStateFlow(YearMonth.now())

    fun toggle(habit: HabitEntity, date: LocalDate) = viewModelScope.launch { c.habits.toggle(habit, date) }
    fun save(habit: HabitEntity) = viewModelScope.launch { c.habits.save(habit) }
    fun delete() = viewModelScope.launch { c.habits.delete(habitId) }
    fun shiftMonth(step: Long) = month.update { it.plusMonths(step) }
}

@Composable
fun HabitsScreen(onBack: () -> Unit, onOpenHabit: (Long) -> Unit) {
    val vm = appViewModel { HabitsViewModel(it) }
    val feedback = LocalFeedback.current
    val habits by vm.habits.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BackTopBar(title = "Kebiasaan", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Kebiasaan") })
        },
    ) { padding ->
        val list = habits ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(
                emoji = "🔥",
                title = "Bangun kebiasaan baik",
                message = "Lacak rutinitas seperti ke gym, minum air, atau membaca. Centang setiap hari dan jaga streak Anda!",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = Spacing.fabClearance),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TodaySummary(list) }
            items(list, key = { it.habit.id }) { ui ->
                HabitCard(
                    ui = ui,
                    onOpen = { onOpenHabit(ui.habit.id) },
                    onToggleDay = {
                        feedback.toggle(true)
                        vm.toggle(ui.habit, it)
                    },
                    onIncrement = {
                        feedback.tick()
                        vm.increment(ui.habit)
                    },
                    onDecrement = { vm.decrement(ui.habit) },
                )
            }
        }
    }

    if (creating) HabitDialog(initial = null, onDismiss = { creating = false }, onSave = { vm.save(it) })
}

@Composable
private fun TodaySummary(list: List<HabitUi>) {
    val scheduled = list.filter { it.scheduledToday }
    val done = scheduled.count { it.doneToday }
    val ext = AppColors.current
    AppCard(brush = heroBrush(), modifier = Modifier.enterStagger()) {
        Column(Modifier.fillMaxWidth().heroDecoration().padding(20.dp)) {
            Text("Hari ini", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
            Text(
                if (scheduled.isEmpty()) "Tidak ada kebiasaan terjadwal" else "$done dari ${scheduled.size} kebiasaan tuntas",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            if (scheduled.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Meter(done.toFloat() / scheduled.size, Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        done == scheduled.size -> "Luar biasa! Semua kebiasaan hari ini selesai 🎉"
                        done == 0 -> "Yuk mulai dari yang paling mudah."
                        else -> "Sedikit lagi, tetap semangat!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
fun HabitCard(
    ui: HabitUi,
    onOpen: () -> Unit,
    onToggleDay: (LocalDate) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val color = AppColors.current.seriesColor(ui.habit.color)
    val target = ui.habit.targetPerDay
    AppCard(onClick = onOpen, modifier = Modifier.fillMaxWidth().enterStagger()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(ui.habit.emoji, color, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(ui.habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        if (ui.streak > 0) "🔥 ${ui.streak} hari beruntun" else HabitsCalc.scheduleLabel(ui.habit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (target <= 1) {
                    CheckToggle(done = ui.doneToday, color = color, onClick = { onToggleDay(LocalDate.now()) })
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDecrement, enabled = ui.todayCount > 0) { Icon(Icons.Filled.Remove, contentDescription = "Kurangi") }
                        Text(
                            "${ui.todayCount}/$target",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        FilledIconButton(onClick = onIncrement) { Icon(Icons.Filled.Add, contentDescription = "Tambah") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ui.week.forEach { day -> DayDot(day, target, color, onClick = { onToggleDay(day.date) }) }
            }
        }
    }
}

@Composable
private fun CheckToggle(done: Boolean, color: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(if (done) color else Color.Transparent, tween(Motion.SHORT), label = "checkBg")
    val scale = remember { Animatable(1f) }
    LaunchedEffect(done) {
        if (done) {
            scale.animateTo(1.18f, tween(120))
            scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 400f))
        }
    }
    Box(
        Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .size(44.dp)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, color, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.animation.AnimatedVisibility(done, enter = scaleIn(spring(dampingRatio = 0.4f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(Icons.Filled.Check, contentDescription = "Selesai", tint = Color.White)
        }
    }
}

@Composable
private fun DayDot(day: HabitDay, target: Int, color: Color, onClick: () -> Unit) {
    val complete = day.count >= target
    val partial = day.count in 1 until target
    val isToday = day.date == LocalDate.now()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            Dates.weekdayShort[day.date.dayOfWeek.value - 1],
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isToday) FontWeight.Bold else null,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    when {
                        complete -> color
                        partial -> color.copy(alpha = 0.35f)
                        else -> Color.Transparent
                    },
                )
                .border(
                    1.5.dp,
                    if (day.scheduled) color.copy(alpha = if (complete) 1f else 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (complete) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.scheduled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun HabitDetailScreen(habitId: Long, onBack: () -> Unit) {
    val vm = appViewModel { HabitDetailViewModel(habitId, it) }
    val habit by vm.habit.collectAsStateWithLifecycle()
    val counts by vm.counts.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var seen by remember { mutableStateOf(false) }

    val h = habit
    LaunchedEffect(h) {
        if (h != null) seen = true else if (seen) onBack()
    }

    Scaffold(
        topBar = {
            BackTopBar(
                title = h?.name ?: "Kebiasaan",
                onBack = onBack,
                actions = {
                    if (h != null) {
                        IconButton(onClick = { editing = true }) { Icon(Icons.Outlined.Edit, contentDescription = "Ubah") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Hapus") }
                    }
                },
            )
        },
    ) { padding ->
        if (h == null) return@Scaffold
        val today = LocalDate.now()
        val color = AppColors.current.seriesColor(h.color)
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(h.emoji, color, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(h.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        buildString {
                            append(HabitsCalc.scheduleLabel(h))
                            if (h.targetPerDay > 1) append(" · ${h.targetPerDay}× per hari")
                            h.reminderMinutes?.let { append(" · pengingat ${Dates.time(LocalTime.of(it / 60, it % 60))}") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Streak saat ini", "${HabitLogic.currentStreak(h, counts, today)} hari", Modifier.weight(1f))
                StatTile("Streak terbaik", "${HabitLogic.bestStreak(h, counts, today)} hari", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Konsistensi 30 hari", "${(HabitLogic.completionRate(h, counts, today) * 100).toInt()}%", Modifier.weight(1f))
                StatTile("Total hari tuntas", "${counts.count { it.value >= h.targetPerDay }} hari", Modifier.weight(1f))
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(12.dp)) {
                    MonthSwitcher(Dates.monthYear(month), onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
                    HabitHeatmap(month, h, counts, color, onToggle = { vm.toggle(h, it) })
                    Text(
                        "Ketuk tanggal untuk menandai tuntas atau membatalkannya.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }

    if (editing && h != null) HabitDialog(initial = h, onDismiss = { editing = false }, onSave = { vm.save(it) })
    if (confirmDelete && h != null) {
        ConfirmDialog(
            title = "Hapus kebiasaan?",
            message = "\"${h.name}\" beserta seluruh riwayatnya akan dihapus.",
            onConfirm = { vm.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun HabitHeatmap(month: YearMonth, habit: HabitEntity, counts: Map<Long, Int>, color: Color, onToggle: (LocalDate) -> Unit) {
    val start = gridStart(month, mondayFirst = true)
    val today = LocalDate.now()
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Dates.weekdayShort.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (week in 0 until 6) {
            val firstOfWeek = start.plusDays(week * 7L)
            if (week == 5 && YearMonth.from(firstOfWeek) != month) break
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (d in 0 until 7) {
                    val date = firstOfWeek.plusDays(d.toLong())
                    val inMonth = YearMonth.from(date) == month
                    val count = counts[date.toEpochDay()] ?: 0
                    val ratio = (count.toFloat() / habit.targetPerDay).coerceIn(0f, 1f)
                    val scheduled = HabitLogic.isScheduled(habit, date)
                    val future = date.isAfter(today)
                    val fill = when {
                        !inMonth -> Color.Transparent
                        ratio >= 1f -> color
                        ratio > 0f -> color.copy(alpha = 0.25f + 0.5f * ratio)
                        else -> empty.copy(alpha = if (scheduled) 1f else 0.4f)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(fill)
                            .then(if (date == today) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
                            .clickable(enabled = inMonth && !future) { onToggle(date) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (inMonth) {
                            Text(
                                date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    ratio >= 1f -> Color.White
                                    future -> MaterialTheme.colorScheme.outline
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitDialog(initial: HabitEntity?, onDismiss: () -> Unit, onSave: (HabitEntity) -> Unit) {
    val ext = AppColors.current
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: habitEmojis.first()) }
    var color by remember { mutableIntStateOf(initial?.color ?: 0) }
    var mask by remember { mutableIntStateOf(initial?.daysMask ?: 0b1111111) }
    var target by remember { mutableIntStateOf(initial?.targetPerDay ?: 1) }
    var reminderOn by remember { mutableStateOf(initial?.reminderMinutes != null) }
    var reminder by remember {
        mutableStateOf(initial?.reminderMinutes?.let { LocalTime.of(it / 60, it % 60) } ?: LocalTime.of(7, 0))
    }
    var pickTime by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Kebiasaan baru" else "Ubah kebiasaan") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(emoji, ext.seriesColor(color), size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama kebiasaan") },
                        placeholder = { Text("mis. Ke gym") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                EmojiPicker(habitEmojis, emoji, { emoji = it })
                Text("Hari", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Dates.weekdayShort.forEachIndexed { i, label ->
                        val on = mask and (1 shl i) != 0
                        FilterChip(selected = on, onClick = { mask = mask xor (1 shl i) }, label = { Text(label) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { mask = 0b1111111 }) { Text("Setiap hari") }
                    TextButton(onClick = { mask = 0b0011111 }) { Text("Hari kerja") }
                    TextButton(onClick = { mask = 0b1100000 }) { Text("Akhir pekan") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Target per hari", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { target = (target - 1).coerceAtLeast(1) }) { Icon(Icons.Filled.Remove, contentDescription = "Kurangi") }
                    Text("$target×", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { target = (target + 1).coerceAtMost(30) }) { Icon(Icons.Filled.Add, contentDescription = "Tambah") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pengingat harian", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = reminderOn,
                        onCheckedChange = {
                            reminderOn = it
                            if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(context)) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }
                if (reminderOn) {
                    OutlinedButton(onClick = { pickTime = true }) { Text("Jam ${Dates.time(reminder)}") }
                }
                Text("Warna", style = MaterialTheme.typography.labelLarge)
                ColorPickerRow(ext.series, color, { color = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && mask != 0,
                onClick = {
                    val base = initial ?: HabitEntity(name = "", emoji = emoji, color = color, createdAt = System.currentTimeMillis())
                    onSave(
                        base.copy(
                            name = name.trim(),
                            emoji = emoji,
                            color = color,
                            daysMask = mask,
                            targetPerDay = target,
                            reminderMinutes = if (reminderOn) reminder.hour * 60 + reminder.minute else null,
                        ),
                    )
                    onDismiss()
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )

    if (pickTime) TimePickerModal(initial = reminder, onDismiss = { pickTime = false }, onPick = { reminder = it })
}
