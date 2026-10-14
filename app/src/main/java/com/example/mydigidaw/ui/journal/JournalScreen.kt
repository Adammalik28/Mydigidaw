@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mydigidaw.ui.journal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.data.db.JournalEntity
import com.example.mydigidaw.data.repo.Moods
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.calendar.gridStart
import com.example.mydigidaw.ui.components.Spacing
import com.example.mydigidaw.ui.components.AppCard
import com.example.mydigidaw.ui.components.BackTopBar
import com.example.mydigidaw.ui.components.EmptyState
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.Meter
import com.example.mydigidaw.ui.components.MonthSwitcher
import com.example.mydigidaw.ui.components.Motion
import com.example.mydigidaw.ui.components.enterStagger
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Dates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class JournalViewModel(private val c: AppContainer) : ViewModel() {
    val entries: StateFlow<List<JournalEntity>?> = c.journal.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val month = MutableStateFlow(YearMonth.now())

    fun shift(step: Long) = month.update { it.plusMonths(step) }
    fun save(day: LocalDate, mood: Int, note: String) = viewModelScope.launch { c.journal.save(day, mood, note) }
    fun delete(day: LocalDate) = viewModelScope.launch { c.journal.delete(day) }
}

/** Warna suasana hati: skala berurutan satu rona (makin baik makin pekat) agar mudah dibaca sekilas. */
@Composable
fun moodColor(mood: Int): Color {
    val base = AppColors.current.series[2]
    return base.copy(alpha = 0.25f + 0.75f * ((mood - 1).coerceIn(0, 4) / 4f))
}

@Composable
fun MoodPicker(selected: Int?, onPick: (Int) -> Unit, modifier: Modifier = Modifier, size: Int = 52) {
    val feedback = LocalFeedback.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        (1..5).forEach { mood ->
            val active = selected == mood
            val scale by animateFloatAsState(if (active) 1.18f else if (selected == null) 1f else 0.9f, spring(dampingRatio = 0.45f), label = "mood")
            val bg by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                tween(Motion.SHORT),
                label = "moodBg",
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .size(size.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .clickable {
                            feedback.toggle(true)
                            onPick(mood)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        Moods.emojiOf(mood),
                        fontSize = (size * 0.55f).sp,
                        modifier = Modifier.graphicsLayer { alpha = if (selected == null || active) 1f else 0.55f },
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    Moods.labelOf(mood),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun JournalScreen(onBack: () -> Unit) {
    val vm = appViewModel { JournalViewModel(it) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<LocalDate?>(null) }
    val list = entries ?: emptyList()
    val byDay = list.associateBy { it.day }

    Scaffold(
        topBar = { BackTopBar(title = "Jurnal & Mood", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = LocalDate.now() },
                icon = { Icon(Icons.Filled.Edit, null) },
                text = { Text("Tulis hari ini") },
            )
        },
    ) { padding ->
        if (entries == null) return@Scaffold
        val monthEntries = list.filter { YearMonth.from(LocalDate.ofEpochDay(it.day)) == month }.sortedByDescending { it.day }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = Spacing.fabClearance),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard(modifier = Modifier.enterStagger(0)) {
                    Column(Modifier.padding(12.dp)) {
                        MonthSwitcher(Dates.monthYear(month), onPrev = { vm.shift(-1) }, onNext = { vm.shift(1) })
                        MoodCalendar(month, byDay, onPick = { editing = it })
                    }
                }
            }
            item { MoodSummary(monthEntries, Modifier.enterStagger(1)) }
            if (monthEntries.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📔",
                        title = "Belum ada jurnal bulan ini",
                        message = "Catat suasana hati dan cerita singkat setiap hari — kebiasaan kecil yang bikin lebih mengenal diri sendiri.",
                    )
                }
            }
            items(monthEntries, key = { it.day }) { entry ->
                AppCard(onClick = { editing = LocalDate.ofEpochDay(entry.day) }, modifier = Modifier.fillMaxWidth().animateItem().enterStagger(2)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(moodColor(entry.mood).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center,
                        ) { Text(Moods.emojiOf(entry.mood), fontSize = 26.sp) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Dates.weekdayFull(LocalDate.ofEpochDay(entry.day)), style = MaterialTheme.typography.titleSmall)
                            Text(Moods.labelOf(entry.mood), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            if (entry.note.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(entry.note, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { day ->
        JournalSheet(
            day = day,
            initial = byDay[day.toEpochDay()],
            onDismiss = { editing = null },
            onSave = { mood, note -> vm.save(day, mood, note) },
            onDelete = { vm.delete(day) },
        )
    }
}

@Composable
private fun MoodCalendar(month: YearMonth, byDay: Map<Long, JournalEntity>, onPick: (LocalDate) -> Unit) {
    val start = gridStart(month, mondayFirst = true)
    val today = LocalDate.now()
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
            val first = start.plusDays(week * 7L)
            if (week == 5 && YearMonth.from(first) != month) break
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until 7) {
                    val date = first.plusDays(i.toLong())
                    val inMonth = YearMonth.from(date) == month
                    val entry = byDay[date.toEpochDay()]
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    !inMonth -> Color.Transparent
                                    entry != null -> moodColor(entry.mood).copy(alpha = 0.22f)
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                                },
                            )
                            .then(if (date == today) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable(enabled = inMonth && !date.isAfter(today)) { onPick(date) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (inMonth) {
                            if (entry != null) Text(Moods.emojiOf(entry.mood), fontSize = 18.sp)
                            else Text(
                                date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (date.isAfter(today)) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodSummary(entries: List<JournalEntity>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) return
    val avg = entries.map { it.mood }.average()
    val rounded = avg.toInt().coerceIn(1, 5).let { if (avg - it >= 0.5) it + 1 else it }.coerceIn(1, 5)
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Moods.emojiOf(rounded), fontSize = 34.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Rata-rata bulan ini: ${Moods.labelOf(rounded)}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${entries.size} hari tercatat · skor ${"%.1f".format(avg).replace('.', ',')} dari 5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            (5 downTo 1).forEach { mood ->
                val count = entries.count { it.mood == mood }
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Moods.emojiOf(mood), fontSize = 16.sp, modifier = Modifier.width(28.dp))
                    Meter(count.toFloat() / entries.size, moodColor(mood), Modifier.weight(1f), height = 8.dp)
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
fun JournalSheet(
    day: LocalDate,
    initial: JournalEntity?,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit,
    onDelete: () -> Unit,
) {
    var mood by remember { mutableIntStateOf(initial?.mood ?: 0) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    val feedback = LocalFeedback.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().imePadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (day == LocalDate.now()) "Bagaimana harimu?" else Dates.full(day), style = MaterialTheme.typography.titleLarge)
            MoodPicker(selected = mood.takeIf { it > 0 }, onPick = { mood = it })
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ceritakan sedikit (opsional)") },
                placeholder = { Text("Apa yang membuatmu senang, atau yang perlu diperbaiki?") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (initial != null) {
                    TextButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Hapus") }
                    Spacer(Modifier.weight(1f))
                }
                TextButton(onClick = onDismiss) { Text("Batal") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = mood > 0,
                    onClick = {
                        feedback.confirm()
                        onSave(mood, note)
                        onDismiss()
                    },
                ) { Text("Simpan") }
            }
        }
    }
}
